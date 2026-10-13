# Modernizacja i Diagnostyka Integracji Gemini API

Kompleksowy audyt i aktualizacja integracji Google Gemini API w Aurora AI: eliminacja zablokowanych modeli archiwalnych, wdrożenie rekomendowanych modeli `gemini-3.5-flash` i `gemini-3.1-pro-preview` oraz implementacja interaktywnego panelu diagnostyki połączenia w Ustawieniach.

## User Review & Critical Decisions

> [!IMPORTANT]
> Kluczowe decyzje potwierdzone przez użytkownika:

- **Główny model AI**: Jako domyślny silnik ustawiamy **Gemini 3.5 Flash** (`gemini-3.5-flash`) z możliwością wyboru **Gemini 3.1 Pro Preview** (`gemini-3.1-pro-preview`) dla głębokiego wnioskowania oraz **Gemini 3.1 Flash Lite** (`gemini-3.1-flash-lite-preview`) dla natychmiastowych odpowiedzi głosowych.
- **Wycofanie zabronionych wersji**: Usunięcie z kodu i listy wyboru modeli wycofanych `gemini-1.5-flash` i `gemini-2.0-flash`, które powodują błędy odrzucenia zapytań przez serwery Google.
- **Interaktywna diagnostyka**: Wdrożenie w ekranie Ustawień (`SettingsScreen`) przycisku "Przetestuj połączenie z API", który wykonuje próbne zapytanie sieciowe, mierzy opóźnienie (ping ms), weryfikuje status klucza i wyświetla dokładne komunikaty diagnostyczne (HTTP 200, 401, 403, 429).

---

## 1. Overview & Core Concept

Aurora AI korzysta z Google Gemini API jako głównego silnika konwersacyjnego, łącząc go z lokalną bazą wiedzy Room oraz trybem głosu na żywo (Live Voice). 

W ramach audytu API:
1. **Zgodność z najnowszym standardem Google**: Przejście na oficjalne modele nowej generacji (`gemini-3.5-flash` jako domyślny model tekstowo-głosowy).
2. **Niezawodność i odporność**: W przypadku braku klucza API aplikacja nadal zapewnia płynny fallback bazujący na lokalnych źródłach wiedzy Room, a przy obecnym kluczu wykorzystuje inteligentną listę zapasowych modeli nowej generacji.
3. **Pełna widoczność stanu (Observability)**: Użytkownik może w każdej chwili sprawdzić w aplikacji, czy jego klucz `GEMINI_API_KEY` skonfigurowany w panelu Secrets działa poprawnie, jakie ma czasy odpowiedzi i czy serwery Google zwracają poprawny status.

---

## 2. User Experience & Visual Design

### Ścieżka użytkownika (User Flow)
1. **Wybór modelu w Ustawieniach**:
   - Sekcja "Model Gemini" prezentuje 3 nowoczesne kafelki `FilterChip`:
     - `3.5 Flash (Domyślny)` – zbalansowana szybkość i inteligencja, idealny do Live Voice.
     - `3.1 Pro (Zaawansowany)` – głęboka logika i analiza źródeł wiedzy.
     - `3.1 Flash Lite (Błyskawiczny)` – minimalne opóźnienia.
2. **Karta diagnostyczna w Ustawieniach**:
   - Bezpośrednio w karcie statusu API pojawia się przycisk `Przetestuj połączenie`.
   - Kliknięcie uruchamia wskaźnik ładowania i wysyła testowy pakiet ping do Gemini API.
   - Po teście wyświetla się czytelny badge z czasem odpowiedzi (np. `142 ms`) oraz szczegółowym statusem:
     - Zielony: *Połączenie aktywne, model odpowiedział pomyślnie.*
     - Bursztynowy / Czerwony: Dokładny komunikat błędu (np. niepoprawny klucz, brak uprawnień lub wyczerpany limit zapytań).

---

## 3. Key Product Decisions & Trade-Offs

- **Decyzja 1: Zastąpienie modeli 1.5 i 2.0 przez serię Gemini 3**:
  - *Podejście*: Pełna eliminacja `gemini-1.5-flash` i `gemini-2.0-flash` na rzecz `gemini-3.5-flash`, `gemini-3.1-pro-preview` oraz `gemini-3.1-flash-lite-preview`.
  - *Uzasadnienie*: Google Gemini API wycofuje starsze wersje i odrzuca zapytania do nich. Nowa seria 3.x oferuje wyższą jakość odpowiedzi po polsku i niższe opóźnienia.
- **Decyzja 2: Diagnostyka przez dedykowaną metodę w Repository**:
  - *Podejście*: Dodanie w `GeminiRepository` metody `testApiConnection(): ApiTestResult`, która wysyła minimalistyczny prompt testowy (np. "ping") i mierzy rzeczywisty czas `System.currentTimeMillis()`.
  - *Uzasadnienie*: Nie obciąża limitu tokenów, a daje użytkownikowi 100% pewności, że wprowadzony klucz API działa.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                      UI Layer                          │
│   SettingsScreen  ◄──►  ChatViewModel  ◄──► LiveVoice   │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                    GeminiRepository                    │
│   • generateResponse(messages, settings)               │
│   • testApiConnection(): ApiTestResult                 │
│   • buildSystemInstruction(sources, facts, persona)    │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│            Network Layer (Retrofit + OkHttp)           │
│   • Base URL: https://generativelanguage.googleapis... │
│   • Timeouts: 60s connect / 60s read / 60s write       │
│   • Model: gemini-3.5-flash / gemini-3.1-pro-preview   │
│   • GeminiErrorHandler (Moshi, HTTP 400/401/429/500)   │
└────────────────────────────────────────────────────────┘
```

### Planowane zmiany w kodzie:
1. **`PersonaSettings` & `GeminiRepository.kt`**:
   - Domyślny `selectedModel` = `"gemini-3.5-flash"`.
   - Zaktualizowanie listy modeli zapasowych: `listOf(settings.selectedModel, "gemini-3.5-flash", "gemini-3.1-flash-lite-preview", "gemini-3.1-pro-preview")`.
   - Dodanie metody testującej `testApiConnection()`.
2. **`ChatViewModel.kt`**:
   - Dodanie stanu testu `apiTestState: StateFlow<ApiTestStatus>` (`Idle`, `Testing`, `Success(latencyMs)`, `Error(msg)`).
   - Metoda `runApiConnectionTest()`.
3. **`SettingsScreen.kt`**:
   - Aktualizacja kafelków wyboru modelu do `gemini-3.5-flash`, `gemini-3.1-pro-preview`, `gemini-3.1-flash-lite-preview`.
   - Dodanie przycisku i wyników testu diagnostycznego w karcie API Key.
4. **Weryfikacja**:
   - Sprawdzenie kompilacji za pomocą `compile_applet`.
   - Uruchomienie testów jednostkowych.
