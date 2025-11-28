package com.arbeitszeit.tracker.ai

import android.util.Log
import com.arbeitszeit.tracker.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Gemini AI Client mit direkten HTTP-Requests
 *
 * Verwendet Google's Gemini REST API (v1beta) für KI-gestützte Urlaubsplanung:
 * - Optimale Urlaubstage vorschlagen
 * - Brückentage identifizieren
 * - Schulferien berücksichtigen
 * - Schließtage der Einrichtung einbeziehen
 *
 * Vorteile direkte REST API:
 * - ✅ KOSTENLOS mit API Key (1500 Anfragen/Tag)
 * - ✅ Kein Firebase/Blaze-Plan nötig
 * - ✅ Keine deprecated SDK
 * - ✅ Volle Kontrolle über Requests
 * - ✅ Gemini 2.5 Flash vollständig unterstützt
 *
 * API Key Setup:
 * 1. Gehe zu https://aistudio.google.com/apikey
 * 2. Erstelle einen API Key
 * 3. Füge in local.properties hinzu: GEMINI_API_KEY=dein_key_hier
 *
 * Datenschutz: Nur anonymisierte Daten (Anzahl Tage, Bundesland, Präferenzen)
 */
class GeminiClient {

    private val apiKey: String = BuildConfig.GEMINI_API_KEY

    companion object {
        private const val TAG = "GeminiClient"
        private const val MODEL_NAME = "gemini-2.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    // Ktor HTTP Client mit JSON Support
    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d(TAG, "HTTP: $message")
                }
            }
            level = LogLevel.INFO
        }
    }

    /**
     * Prüft ob API Key konfiguriert ist
     */
    fun isConfigured(): Boolean {
        return apiKey.isNotEmpty() && apiKey != "YOUR_API_KEY_HERE"
    }

    /**
     * Optimiert Urlaubsplanung basierend auf Präferenzen
     *
     * @param availableVacationDays Verfügbare Urlaubstage (z.B. 30)
     * @param bundesland Bundesland-Code für Schulferien (z.B. "BW")
     * @param closingDays Liste der Schließtage (z.B. "24.12.2025 - 26.12.2025: Weihnachtsferien")
     * @param preferences Benutzer-Präferenzen (z.B. "Lange am Stück", "Viele kurze Auszeiten", "Brückentage nutzen")
     * @param year Jahr für Planung (z.B. 2025)
     *
     * @return Optimierungsvorschläge als formatierter Text
     */
    suspend fun optimizeVacation(
        availableVacationDays: Int,
        bundesland: String,
        closingDays: List<String>,
        preferences: String,
        year: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured()) {
                return@withContext Result.failure(
                    Exception("Gemini API Key nicht konfiguriert.\n\nBitte:\n1. Gehe zu https://aistudio.google.com/apikey\n2. Erstelle einen API Key\n3. Füge in local.properties hinzu:\n   GEMINI_API_KEY=dein_key_hier")
                )
            }

            val prompt = buildVacationPrompt(
                availableVacationDays = availableVacationDays,
                bundesland = bundesland,
                closingDays = closingDays,
                preferences = preferences,
                year = year
            )

            Log.d(TAG, "Sende Anfrage an Gemini REST API...")
            Log.d(TAG, "Model: $MODEL_NAME")

            val requestBody = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(Part(text = prompt))
                    )
                ),
                generationConfig = GenerationConfig(
                    temperature = 0.7f,
                    topK = 40,
                    topP = 0.95f,
                    maxOutputTokens = 2048
                ),
                safetySettings = listOf(
                    SafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_NONE"),
                    SafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_NONE"),
                    SafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_NONE"),
                    SafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_NONE")
                )
            )

            val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"

            val response: GeminiResponse = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body()

            Log.d(TAG, "Response erhalten: ${response.candidates.size} candidates")

            val candidate = response.candidates.firstOrNull()
            if (candidate == null) {
                Log.w(TAG, "Keine Candidates in Response!")
                Log.w(TAG, "PromptFeedback: ${response.promptFeedback}")
                return@withContext Result.failure(Exception("API hat keine Antwort generiert. Möglicherweise durch Safety Filter blockiert."))
            }

            val parts = candidate.content.parts
            if (parts == null || parts.isEmpty()) {
                Log.w(TAG, "Content.parts ist leer oder null!")
                Log.w(TAG, "FinishReason: ${candidate.finishReason}")
                Log.w(TAG, "SafetyRatings: ${candidate.safetyRatings}")
                return@withContext Result.failure(Exception("API-Antwort wurde blockiert.\n\nGrund: ${candidate.finishReason ?: "Unknown"}\n\nDie Anfrage wurde möglicherweise als unsicher eingestuft. Versuche eine andere Formulierung."))
            }

            val resultText = parts.firstOrNull()?.text ?: "Keine Antwort erhalten"

            Log.d(TAG, "Erfolgreich: ${resultText.length} Zeichen")

            Result.success(resultText)

        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei Gemini API Anfrage", e)
            Result.failure(Exception("KI-Anfrage fehlgeschlagen: ${e.message}"))
        }
    }

    /**
     * Erstellt den Prompt für die Urlaubsoptimierung
     */
    private fun buildVacationPrompt(
        availableVacationDays: Int,
        bundesland: String,
        closingDays: List<String>,
        preferences: String,
        year: Int
    ): String {
        return """
Du bist ein intelligenter Urlaubsplaner für Lehrer an Ganztagsschulen in Deutschland.

**KONTEXT:**
- Jahr: $year
- Verfügbare Urlaubstage: $availableVacationDays
- Bundesland: $bundesland (für Schulferien)
- Präferenz: $preferences

**SCHLIESTAGE DER EINRICHTUNG:**
${if (closingDays.isEmpty()) "Keine" else closingDays.joinToString("\n")}

**AUFGABE:**
Erstelle eine optimale Urlaubsplanung für das Jahr $year.

**BEACHTE:**
1. Schulferien in $bundesland (recherchiere aktuelle Termine für $year)
2. Gesetzliche Feiertage in $bundesland
3. Brückentage (Feiertage + 1 Urlaubstag = langes Wochenende)
4. Schließtage der Einrichtung (diese MÜSSEN als Urlaub genommen werden)
5. Benutzer-Präferenz: $preferences

**FORMAT DER ANTWORT:**

📅 **URLAUBSPLANUNG $year**

🎯 **EMPFOHLENE ZEITRÄUME:**

1. **[Name des Zeitraums]** (z.B. Osterferien)
   - Datum: [Startdatum] - [Enddatum]
   - Urlaubstage: [X Tage]
   - Freie Tage gesamt: [Y Tage]
   - Vorteil: [Warum diese Zeit gut ist]

2. **[Nächster Zeitraum]**
   ...

💡 **BRÜCKENTAGE-CHANCEN:**
- [Datum]: [Feiertag] → +1 Urlaubstag = [X] Tage frei
- ...

⚠️ **PFLICHT-URLAUB (Schließtage):**
${if (closingDays.isEmpty()) "Keine" else "- " + closingDays.joinToString("\n- ")}

📊 **ZUSAMMENFASSUNG:**
- Empfohlene Urlaubstage: [X] von $availableVacationDays
- Freie Tage gesamt: [Y] Tage
- Effizienz: [Z]% (Verhältnis Urlaub zu freien Tagen)

✅ **RESTLICHE URLAUBSTAGE:**
Du hast noch [X] Urlaubstage übrig für spontane Auszeiten.

---

Sei konkret, präzise und optimiere für maximale Erholung bei minimalen Urlaubstagen!
        """.trimIndent()
    }

    /**
     * Erklärt einen Feiertag oder besonderen Tag
     * (Für zukünftige Features)
     */
    suspend fun explainHoliday(
        holidayName: String,
        date: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured()) {
                return@withContext Result.failure(
                    Exception("Gemini API Key nicht konfiguriert.")
                )
            }

            val prompt = """
Erkläre kurz (max. 2-3 Sätze) den Feiertag "$holidayName" am $date:
- Was wird gefeiert?
- Warum ist es ein Feiertag?
- Gibt es Traditionen?

Antworte auf Deutsch, freundlich und informativ.
            """.trimIndent()

            val requestBody = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt))))
            )

            val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"

            val response: GeminiResponse = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body()

            val resultText = response.candidates.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text
                ?: "Keine Erklärung verfügbar"

            Result.success(resultText)

        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei Holiday-Erklärung", e)
            Result.failure(e)
        }
    }

    /**
     * Schlägt alternative Urlaubszeiten vor, wenn Wunschtermin nicht verfügbar
     */
    suspend fun suggestAlternatives(
        desiredDate: String,
        reason: String,
        availableDays: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured()) {
                return@withContext Result.failure(
                    Exception("Gemini API Key nicht konfiguriert.")
                )
            }

            val prompt = """
Der gewünschte Urlaubstermin $desiredDate ist nicht verfügbar.
Grund: $reason

Schlage 3 alternative Zeiträume vor:
- Verfügbare Urlaubstage: $availableDays
- Ähnliche Jahreszeit bevorzugt
- Kurze Begründung pro Alternative

Antworte prägnant und hilfreich auf Deutsch.
            """.trimIndent()

            val requestBody = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt))))
            )

            val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"

            val response: GeminiResponse = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body()

            val resultText = response.candidates.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text
                ?: "Keine Alternativen gefunden"

            Result.success(resultText)

        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei Alternative-Suche", e)
            Result.failure(e)
        }
    }

    // JSON Data Classes für Gemini REST API

    @Serializable
    data class GeminiRequest(
        val contents: List<Content>,
        val generationConfig: GenerationConfig? = null,
        val safetySettings: List<SafetySetting>? = null
    )

    @Serializable
    data class Content(
        val parts: List<Part>? = null,  // Optional - kann bei blockierten Antworten leer sein
        val role: String = "user"
    )

    @Serializable
    data class Part(
        val text: String
    )

    @Serializable
    data class GenerationConfig(
        val temperature: Float,
        val topK: Int,
        val topP: Float,
        val maxOutputTokens: Int
    )

    @Serializable
    data class SafetySetting(
        val category: String,
        val threshold: String
    )

    @Serializable
    data class GeminiResponse(
        val candidates: List<Candidate> = emptyList(),
        @SerialName("promptFeedback") val promptFeedback: PromptFeedback? = null
    )

    @Serializable
    data class Candidate(
        val content: Content,
        val finishReason: String? = null,
        val safetyRatings: List<SafetyRating>? = null
    )

    @Serializable
    data class SafetyRating(
        val category: String,
        val probability: String
    )

    @Serializable
    data class PromptFeedback(
        val safetyRatings: List<SafetyRating>? = null
    )
}
