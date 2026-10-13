package com.example.data.remote

import com.example.BuildConfig
import com.example.data.local.KnowledgeDao
import com.example.data.model.FactCategory
import com.example.data.model.KnowledgeSource
import com.example.data.model.LearnedFact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.data.model.AuroraMode

enum class ApiDiagnosticStatus {
    SUCCESS,
    UNAUTHORIZED,
    NOT_FOUND,
    QUOTA_EXCEEDED,
    MISSING_KEY,
    NETWORK_ERROR,
    SERVER_ERROR
}

data class ApiDiagnosticResult(
    val isSuccess: Boolean,
    val status: ApiDiagnosticStatus,
    val httpCode: Int?,
    val modelTested: String,
    val latencyMs: Long,
    val headline: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class PersonaSettings(
    val humorLevel: Float = 0.8f, // 0.0 to 1.0
    val boldnessLevel: Float = 0.9f, // 0.0 to 1.0
    val temperature: Float = 0.85f,
    val selectedModel: String = "gemini-3.5-flash-lite",
    val autoLearn: Boolean = true,
    val speechFriendlyOutput: Boolean = true
)

class GeminiRepository(
    private val knowledgeDao: KnowledgeDao
) {
    companion object {
        fun resolveApiKey(): String {
            val k1 = runCatching { BuildConfig.GEMINI_API_KEY }.getOrNull()
            if (!k1.isNullOrBlank() && k1 != "MY_GEMINI_API_KEY" && !k1.startsWith("MY_")) {
                return k1.trim()
            }
            val k2 = runCatching { BuildConfig.API_KEY }.getOrNull()
            if (!k2.isNullOrBlank() && k2 != "MY_API_KEY" && !k2.startsWith("MY_")) {
                return k2.trim()
            }
            return ""
        }
    }

    suspend fun generateResponse(
        messages: List<Pair<String, String>>, // role to text
        settings: PersonaSettings,
        mode: AuroraMode = AuroraMode.FREE
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey()
        val hasValidKey = apiKey.isNotBlank()

        try {
            val activeSources = knowledgeDao.getActiveSources()
            // Strictly enforce memory boundaries
            val activeFacts = if (mode == AuroraMode.SUPPORT) {
                knowledgeDao.getActiveFacts()
            } else {
                knowledgeDao.getFactsExcludingCategory(com.example.data.model.FactCategory.SUPPORT_MEMORY)
            }

            if (!hasValidKey) {
                // If API key is not configured yet, provide helpful localized response based on active sources
                val lastUserMsg = messages.lastOrNull { it.first == "user" }?.second.orEmpty()
                val fallbackReply = generateOfflineGroundedResponse(lastUserMsg, activeSources, activeFacts, settings, mode)
                return@withContext Result.success(fallbackReply)
            }

            val systemInstructionText = buildSystemInstruction(activeSources, activeFacts, settings, mode)

            val contents = messages.map { (role, text) ->
                GeminiContent(
                    role = if (role == "user") "user" else "model",
                    parts = listOf(GeminiPart(text = text))
                )
            }

            val request = GeminiRequest(
                contents = contents,
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemInstructionText))
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = if (mode == AuroraMode.SUPPORT) 0.7f else settings.temperature,
                    topP = 0.95f,
                    topK = 40,
                    maxOutputTokens = 2048
                )
            )

            val candidateModels = listOf(
                settings.selectedModel,
                "gemini-3.5-flash-lite",
                "gemini-3.1-flash-lite-preview",
                "gemini-3.8-flash",
                "gemini-3.5-flash",
                "gemini-3.1-pro-preview"
            ).distinct()

            var successfulText: String? = null
            var lastErrorMsg: String? = null

            for (modelToTry in candidateModels) {
                try {
                    val response = GeminiApiClient.service.generateContent(
                        model = modelToTry,
                        apiKey = apiKey,
                        request = request
                    )

                    val parsed = GeminiErrorHandler.handleResponse(response, modelToTry) { resp ->
                        resp.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    }

                    if (parsed is GeminiResult.Success) {
                        successfulText = parsed.data
                        break
                    } else if (parsed is GeminiResult.Error) {
                        lastErrorMsg = parsed.userMessage
                    }
                } catch (t: Throwable) {
                    val handled = GeminiErrorHandler.handleException(t)
                    lastErrorMsg = handled.userMessage
                }
            }

            if (successfulText != null) {
                if (settings.autoLearn && mode == AuroraMode.FREE) {
                    val lastUserMsg = messages.lastOrNull { it.first == "user" }?.second
                    if (lastUserMsg != null && lastUserMsg.length > 10) {
                        tryAutoExtractFact(lastUserMsg)
                    }
                }
                Result.success(successfulText)
            } else {
                val lastUserMsg = messages.lastOrNull { it.first == "user" }?.second.orEmpty()
                val fallbackReply = generateOfflineGroundedResponse(lastUserMsg, activeSources, activeFacts, settings, mode)
                Result.success(fallbackReply)
            }
        } catch (e: Throwable) {
            val lastUserMsg = messages.lastOrNull { it.first == "user" }?.second.orEmpty()
            val fallbackReply = generateOfflineGroundedResponse(lastUserMsg, emptyList(), emptyList(), settings, mode)
            Result.success(fallbackReply)
        }
    }

    private fun buildSystemInstruction(
        sources: List<KnowledgeSource>,
        facts: List<LearnedFact>,
        settings: PersonaSettings,
        mode: AuroraMode
    ): String {
        return buildString {
            append("Nazywasz się Aurora AI (lub po prostu Aurora). ")
            append("Jesteś jednym spójnym, trwałym asystentem AI. ")
            append("GŁÓWNA ZASADA ARCHITEKTURY: TO UŻYTKOWNIK KONTROLUJE KONTEKST.\n\n")

            if (mode == AuroraMode.SUPPORT) {
                append("=== AKTUALNY TRYB: TRYB WSPARCIA (SUPPORT MODE) ===\n")
                append("- Cel: Poważne rozmowy, autorefleksja, bezpieczne wysłuchanie i wsparcie emocjonalne.\n")
                append("- Granice: NIE jesteś psychologiem, psychiatrą ani substytutem terapii. Nigdy nie stawiaj diagnoz medycznych ani klinicznych.\n")
                append("- Ton: Spokojny, uziemiony, bezpieczny, empatyczny i pełen szacunku. NIE używaj wulgarnego slangu ani frywolnych żartów z trybu swobodnego.\n")
                append("- Pamięć: Informacje z tego trybu mogą być zapamiętywane w pamięci długoterminowej WYŁĄCZNIE za wyraźną zgodą użytkownika. Użytkownik ma pełne prawo do przeglądania, korekty i usuwania tych wspomnień.\n")
            } else {
                append("=== AKTUALNY TRYB: TRYB SWOBODNY (FREE MODE - DOMYŚLNY) ===\n")
                append("- Cel: Codzienna rozmowa, luźne spędzanie czasu, hobby, gry, filmy i bieżące tematy.\n")
                append("- Styl: Dopasuj się do klimatu użytkownika. ")
                when {
                    settings.humorLevel > 0.7f -> append("Bądź dowcipny, bystry, używaj ciętego humoru i inteligentnych ripost. ")
                    settings.humorLevel > 0.3f -> append("Bądź przyjazny, z lekkim poczuciem humoru. ")
                    else -> append("Bądź konkretny i wyważony. ")
                }
                if (settings.boldnessLevel > 0.6f) {
                    append("Bądź bezpośredni, szczery, bez owijania w bawełnę.\n")
                }
                append("- Granice pamięci: Luźne rozmowy NIE trafiają automatycznie do pamięci Trybu Wsparcia. Wrażliwe tematy z Trybu Wsparcia NIGDY nie są proaktywnie wyciągane w Trybie Swobodnym!\n")
            }

            if (settings.speechFriendlyOutput) {
                append("Ponieważ twoje odpowiedzi są często czytane na głos przez syntezator mowy (TTS), twórz płynne, naturalne zdania.\n")
            }

            if (facts.isNotEmpty()) {
                append("\n--- ZAPAMIĘTANY STYL I INFORMACJE O UŻYTKOWNIKU ---\n")
                facts.forEach { f ->
                    append("- [${f.category.name}]: ${f.fact}\n")
                }
            }

            if (sources.isNotEmpty()) {
                append("\n--- BAZA WIEDZY I ŹRÓDŁA UŻYTKOWNIKA ---\n")
                sources.forEach { s ->
                    append("### Źródło: ${s.title} (${s.sourceType.name})\n")
                    append("${s.content}\n\n")
                }
            }
        }
    }

    private suspend fun tryAutoExtractFact(userMessage: String) {
        val lower = userMessage.lowercase()
        val preferencePhrases = listOf(
            "lubię " to FactCategory.PREFERENCE,
            "wolę " to FactCategory.PREFERENCE,
            "mój ulubiony " to FactCategory.PREFERENCE,
            "moja ulubiona " to FactCategory.PREFERENCE,
            "moje ulubione " to FactCategory.PREFERENCE,
            "nazywam się " to FactCategory.GENERAL,
            "pracuję jako " to FactCategory.GENERAL,
            "zajmuję się " to FactCategory.GENERAL,
            "pamiętaj, że " to FactCategory.STYLE,
            "zawsze zwracaj się do mnie " to FactCategory.STYLE,
            "nie znoszę " to FactCategory.PREFERENCE
        )

        for ((phrase, category) in preferencePhrases) {
            val idx = lower.indexOf(phrase)
            if (idx != -1) {
                val extracted = userMessage.substring(idx).take(120).trim()
                if (extracted.length > 5) {
                    val existing = knowledgeDao.getActiveFacts()
                    if (existing.none { it.fact.contains(extracted, ignoreCase = true) }) {
                        knowledgeDao.insertFact(
                            LearnedFact(
                                fact = extracted,
                                category = category
                            )
                        )
                    }
                }
                break
            }
        }
    }

    private fun generateOfflineGroundedResponse(
        prompt: String,
        sources: List<KnowledgeSource>,
        facts: List<LearnedFact>,
        settings: PersonaSettings,
        mode: AuroraMode
    ): String {
        val lower = prompt.lowercase()
        val matchingSource = sources.firstOrNull { source ->
            source.title.lowercase().split(" ").any { word -> word.length > 3 && lower.contains(word) } ||
            source.content.lowercase().split(" ").any { word -> word.length > 4 && lower.contains(word) }
        }

        if (mode == AuroraMode.SUPPORT) {
            return when {
                matchingSource != null -> {
                    "W oparciu o twoje zapiski '${matchingSource.title}': ${matchingSource.content.take(280)}... Jestem tutaj, aby cię spokojnie wysłuchać."
                }
                lower.contains("pomoc") || lower.contains("źle") || lower.contains("trudn") || lower.contains("czuj") -> {
                    "Słyszę cię i jestem tu przy tobie. Możesz swobodnie opowiedzieć o tym, co teraz przeżywasz. Pamiętaj, że to bezpieczna przestrzeń do spokojnej refleksji."
                }
                else -> {
                    "Jestem w Trybie Wsparcia – gotowa cię wysłuchać w spokojnej i bezpiecznej atmosferze. O czym chciałbyś teraz porozmawiać?"
                }
            }
        }

        return when {
            matchingSource != null -> {
                "Na podstawie twojego źródła '${matchingSource.title}': ${matchingSource.content.take(280)}..."
            }
            lower.contains("źródł") || lower.contains("wiedz") || lower.contains("baza") -> {
                val list = sources.joinToString(", ") { it.title }
                "Obecnie mam w bazie ${sources.size} aktywnych źródeł: $list. Mogę z nich korzystać do precyzyjnych odpowiedzi."
            }
            lower.contains("kim jesteś") || lower.contains("co potrafisz") -> {
                "Jestem Aurora AI – twoim osobistym asystentem! Rozmawiam swobodnie lub w trybie wsparcia, uczę się twojego stylu i korzystam z twojej bazy wiedzy."
            }
            lower.contains("dowcip") || lower.contains("żart") -> {
                "Sztuczna inteligencja i człowiek wchodzą do baru. Barman pyta: 'Co podać?'. Człowiek mówi: 'Piwo', a AI: 'To zależy od promptu i liczby tokenów!' 😄"
            }
            facts.isNotEmpty() && (lower.contains("mnie") || lower.contains("styl") || lower.contains("pamięt")) -> {
                val factsSummary = facts.joinToString("; ") { it.fact }
                "Pamiętam o tobie: $factsSummary. Dostosowuję mój styl do twoich preferencji!"
            }
            else -> {
                "Słyszę cię doskonale! Twoja baza wiedzy zawiera ${sources.size} aktywnych źródeł, a tryb Live Voice działa w czasie rzeczywistym. O czym chcesz teraz porozmawiać?"
            }
        }
    }

    suspend fun testConnection(model: String = "gemini-3.5-flash-lite"): ApiDiagnosticResult = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey()
        val hasValidKey = apiKey.isNotBlank()

        if (!hasValidKey) {
            return@withContext ApiDiagnosticResult(
                isSuccess = false,
                status = ApiDiagnosticStatus.MISSING_KEY,
                httpCode = null,
                modelTested = model,
                latencyMs = 0L,
                headline = "Klucz API nie jest skonfigurowany",
                details = "Brak klucza GEMINI_API_KEY lub wartość zastępcza. Wprowadź klucz w panelu Secrets w AI Studio."
            )
        }

        val startTime = System.currentTimeMillis()
        val candidateModels = listOf(
            model,
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash-lite-preview",
            "gemini-3.8-flash",
            "gemini-3.5-flash"
        ).distinct()

        val testRequest = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = "Aurora API test probe. Odpowiedz krótko 'OK'."))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                maxOutputTokens = 16,
                temperature = 0.2f
            )
        )

        var lastCode: Int? = null
        var lastError: String? = null

        for (targetModel in candidateModels) {
            try {
                val response = GeminiApiClient.service.generateContent(
                    model = targetModel,
                    apiKey = apiKey,
                    request = testRequest
                )
                val latency = System.currentTimeMillis() - startTime
                lastCode = response.code()

                if (response.isSuccessful && response.body()?.candidates?.firstOrNull() != null) {
                    return@withContext ApiDiagnosticResult(
                        isSuccess = true,
                        status = ApiDiagnosticStatus.SUCCESS,
                        httpCode = 200,
                        modelTested = targetModel,
                        latencyMs = latency,
                        headline = "Połączenie z Gemini API działa prawidłowo",
                        details = "Klucz API zweryfikowany pomyślnie. Czas odpowiedzi: ${latency} ms. Model: $targetModel."
                    )
                } else {
                    val errorRaw = try { response.errorBody()?.string() } catch (_: Exception) { null }
                    lastError = errorRaw
                    if (response.code() == 401 || response.code() == 403) {
                        return@withContext ApiDiagnosticResult(
                            isSuccess = false,
                            status = ApiDiagnosticStatus.UNAUTHORIZED,
                            httpCode = response.code(),
                            modelTested = targetModel,
                            latencyMs = latency,
                            headline = "Błąd autoryzacji klucza API (HTTP ${response.code()})",
                            details = "Podany klucz GEMINI_API_KEY został odrzucony przez serwery Google. Upewnij się, że klucz jest poprawny w Secrets."
                        )
                    } else if (response.code() == 429) {
                        return@withContext ApiDiagnosticResult(
                            isSuccess = false,
                            status = ApiDiagnosticStatus.QUOTA_EXCEEDED,
                            httpCode = 429,
                            modelTested = targetModel,
                            latencyMs = latency,
                            headline = "Limit zapytań wyczerpany (HTTP 429)",
                            details = "Klucz jest poprawny, ale przekroczono limit zapytań (Quota Exceeded). Spróbuj ponownie za chwilę."
                        )
                    }
                }
            } catch (e: Exception) {
                val latency = System.currentTimeMillis() - startTime
                val handled = GeminiErrorHandler.handleException(e)
                return@withContext ApiDiagnosticResult(
                    isSuccess = false,
                    status = ApiDiagnosticStatus.NETWORK_ERROR,
                    httpCode = null,
                    modelTested = targetModel,
                    latencyMs = latency,
                    headline = "Błąd sieci lub timeout",
                    details = handled.userMessage
                )
            }
        }

        return@withContext ApiDiagnosticResult(
            isSuccess = false,
            status = ApiDiagnosticStatus.SERVER_ERROR,
            httpCode = lastCode,
            modelTested = model,
            latencyMs = System.currentTimeMillis() - startTime,
            headline = "Nie udało się nawiązać połączenia",
            details = lastError ?: "Serwer zwrócił błąd HTTP $lastCode"
        )
    }
}
