package com.example.data.remote

import com.example.BuildConfig
import com.example.data.local.KnowledgeDao
import com.example.data.model.FactCategory
import com.example.data.model.KnowledgeSource
import com.example.data.model.LearnedFact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

data class PersonaSettings(
    val humorLevel: Float = 0.8f, // 0.0 to 1.0
    val boldnessLevel: Float = 0.9f, // 0.0 to 1.0
    val temperature: Float = 0.85f,
    val selectedModel: String = "gemini-3.5-flash",
    val autoLearn: Boolean = true,
    val speechFriendlyOutput: Boolean = true
)

class GeminiRepository(
    private val knowledgeDao: KnowledgeDao
) {
    suspend fun generateResponse(
        messages: List<Pair<String, String>>, // role to text
        settings: PersonaSettings
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasValidKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        try {
            val activeSources = knowledgeDao.getActiveSources()
            val activeFacts = knowledgeDao.getActiveFacts()

            if (!hasValidKey) {
                // If API key is not configured yet, provide helpful localized response based on active sources
                val lastUserMsg = messages.lastOrNull { it.first == "user" }?.second.orEmpty()
                val fallbackReply = generateOfflineGroundedResponse(lastUserMsg, activeSources, activeFacts, settings)
                return@withContext Result.success(fallbackReply)
            }

            val systemInstructionText = buildSystemInstruction(activeSources, activeFacts, settings)

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
                    temperature = settings.temperature,
                    topP = 0.95f,
                    topK = 40,
                    maxOutputTokens = 2048
                )
            )

            val response = GeminiApiClient.service.generateContent(
                model = settings.selectedModel,
                apiKey = apiKey,
                request = request
            )

            val parsedResult = GeminiErrorHandler.handleResponse(response, settings.selectedModel) { resp ->
                resp.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            }

            when (parsedResult) {
                is GeminiResult.Success -> {
                    val text = parsedResult.data
                    if (settings.autoLearn) {
                        val lastUserMsg = messages.lastOrNull { it.first == "user" }?.second
                        if (lastUserMsg != null && lastUserMsg.length > 10) {
                            tryAutoExtractFact(lastUserMsg)
                        }
                    }
                    Result.success(text)
                }
                is GeminiResult.Error -> {
                    Result.failure(Exception(parsedResult.userMessage))
                }
            }
        } catch (e: Throwable) {
            val handled = GeminiErrorHandler.handleException(e)
            Result.failure(Exception(handled.userMessage))
        }
    }

    private fun buildSystemInstruction(
        sources: List<KnowledgeSource>,
        facts: List<LearnedFact>,
        settings: PersonaSettings
    ): String {
        return buildString {
            append("Nazywasz się Aurora AI (lub po prostu Aurora). ")
            append("Jesteś osobistym, błyskotliwym, odważnym i bezpośrednim asystentem AI zintegrowanym z Google Gemini. ")
            append("Rozmawiasz z użytkownikiem zarówno tekstowo, jak i na żywo przez głos (Live Voice). ")
            append("TWOJE ZASADY I OSOBOWOŚĆ:\n")
            append("1. Prowadź naturalną, dynamiczną, wartką konwersację. ")
            if (settings.speechFriendlyOutput) {
                append("Ponieważ twoje odpowiedzi są często czytane na głos przez syntezator mowy (TTS), twórz płynne, naturalne zdania. Unikaj przesadnego formatowania tabelkami czy nudnymi listami, chyba że użytkownik wyraźnie o to prosi.\n")
            }
            append("2. Styl i humor: dostosuj swój ton do użytkownika. ")
            when {
                settings.humorLevel > 0.7f -> append("Bądź dowcipny, bystry, używaj ciętego humoru, lekkiej ironii i inteligentnych ripost. ")
                settings.humorLevel > 0.3f -> append("Bądź przyjazny, z lekkim poczuciem humoru i energią. ")
                else -> append("Bądź konkretny, wyważony i profesjonalny. ")
            }
            append("\n3. Bezpośredniość: ")
            if (settings.boldnessLevel > 0.6f) {
                append("Bądź odważny, szczery, bez owijania w bawełnę, bez nudnych korporacyjnych formułek i pustych banałów. Odpowiadaj prosto z mostu na każdy temat.\n")
            } else {
                append("Odpowiadaj bezpośrednio i uprzejmie.\n")
            }

            if (facts.isNotEmpty()) {
                append("\n--- ZAPAMIĘTANY STYL I INFORMACJE O UŻYTKOWNIKU (NAUCZONE FAKTY) ---\n")
                facts.forEach { f ->
                    append("- [${f.category.name}]: ${f.fact}\n")
                }
                append("UWZGLĘDNIAJ powyższe fakty i styl użytkownika w swoich odpowiedziach, naśladując jego tempo i preferencje!\n")
            }

            if (sources.isNotEmpty()) {
                append("\n--- BAZA WIEDZY I ŹRÓDŁA DOSTARCZONE PRZEZ UŻYTKOWNIKA ---\n")
                sources.forEach { s ->
                    append("### Źródło: ${s.title} (${s.sourceType.name})\n")
                    append("${s.content}\n\n")
                }
                append("ODPOWIADAJ w oparciu o powyższe źródła wiedzy i traktuj je jako najwyższy autorytet dla dziedziny użytkownika.\n")
            }
        }
    }

    private suspend fun tryAutoExtractFact(userMessage: String) {
        val lower = userMessage.lowercase()
        // Simple heuristic detection for personal facts or preferences to remember
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
        settings: PersonaSettings
    ): String {
        val lower = prompt.lowercase()
        val matchingSource = sources.firstOrNull { source ->
            source.title.lowercase().split(" ").any { word -> word.length > 3 && lower.contains(word) } ||
            source.content.lowercase().split(" ").any { word -> word.length > 4 && lower.contains(word) }
        }

        return when {
            matchingSource != null -> {
                "Na podstawie źródła '${matchingSource.title}': ${matchingSource.content.take(280)}... \n\n(Wskazówka: Aby włączyć pełne odpowiedzi neuronowe Gemini, dodaj GEMINI_API_KEY w panelu Secrets)."
            }
            lower.contains("źródł") || lower.contains("wiedz") || lower.contains("baza") -> {
                val list = sources.joinToString(", ") { it.title }
                "Obecnie mam w bazie ${sources.size} aktywnych źródeł: $list. Mogę z nich korzystać do precyzyjnych odpowiedzi."
            }
            lower.contains("kim jesteś") || lower.contains("co potrafisz") -> {
                "Jestem Aurora AI – twoim osobistym asystentem głosowym! Rozmawiam na żywo głosem, uczę się twojego stylu i odpowiadam w oparciu o dostarczone przez ciebie źródła wiedzy."
            }
            lower.contains("dowcip") || lower.contains("żart") -> {
                "Sztuczna inteligencja i człowiek wchodzą do baru. Barman pyta: 'Co podać?'. Człowiek mówi: 'Piwo', a AI: 'To zależy od promptu i liczby tokenów!' 😄"
            }
            facts.isNotEmpty() && (lower.contains("mnie") || lower.contains("styl") || lower.contains("pamięt")) -> {
                val factsSummary = facts.joinToString("; ") { it.fact }
                "Pamiętam o tobie: $factsSummary. Dostosowuję mój styl do twoich preferencji!"
            }
            else -> {
                "Słyszę cię doskonale! Twoja baza wiedzy zawiera ${sources.size} aktywnych źródeł, a tryb Live Voice działa w czasie rzeczywistym. Aby podłączyć modele Gemini 3.5 Flash/Pro, upewnij się, że klucz GEMINI_API_KEY jest wpisany w Secrets."
            }
        }
    }
}
