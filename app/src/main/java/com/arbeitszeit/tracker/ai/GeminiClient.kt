package com.arbeitszeit.tracker.ai

import android.util.Log
import com.arbeitszeit.tracker.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gemini AI Client für intelligente Urlaubsplanung
 *
 * Verwendet Google's Gemini API um:
 * - Optimale Urlaubstage vorzuschlagen
 * - Brückentage zu identifizieren
 * - Schulferien zu berücksichtigen
 * - Schließtage der Einrichtung einzubeziehen
 *
 * Kosten: KOSTENLOS (1500 Anfragen/Tag)
 * Datenschutz: Nur anonymisierte Daten (Anzahl Tage, Bundesland, Präferenzen)
 */
class GeminiClient {

    private val apiKey: String = BuildConfig.GEMINI_API_KEY

    companion object {
        private const val TAG = "GeminiClient"
        private const val MODEL_NAME = "gemini-2.5-flash"  // Neueste Flash-Version (2025)
    }

    /**
     * Prüft ob API Key vorhanden ist
     */
    fun isConfigured(): Boolean {
        return apiKey.isNotEmpty() && apiKey != "YOUR_API_KEY_HERE"
    }

    /**
     * Erstellt das Gemini-Modell mit optimalen Einstellungen
     */
    private fun createModel(): GenerativeModel {
        val generationConfig = GenerationConfig.builder().apply {
            temperature = 0.7f  // Kreativ aber nicht zu wild
            topK = 40
            topP = 0.95f
            maxOutputTokens = 2048  // Genug für detaillierte Antworten
        }.build()

        // Safety Settings gelockert - Urlaubsplanung ist harmlos
        val safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE),
        )

        return GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = apiKey,
            generationConfig = generationConfig,
            safetySettings = safetySettings
        )
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
                    Exception("Gemini API Key nicht konfiguriert. Bitte in local.properties eintragen.")
                )
            }

            val model = createModel()

            val prompt = buildVacationPrompt(
                availableVacationDays = availableVacationDays,
                bundesland = bundesland,
                closingDays = closingDays,
                preferences = preferences,
                year = year
            )

            Log.d(TAG, "Sende Anfrage an Gemini API...")
            Log.d(TAG, "Model: $MODEL_NAME")

            val response = model.generateContent(prompt)

            Log.d(TAG, "Response erhalten: ${response.candidates.size} candidates")

            val resultText = response.text ?: run {
                Log.e(TAG, "Response.text ist null!")
                Log.e(TAG, "Candidates: ${response.candidates}")
                Log.e(TAG, "Prompt feedback: ${response.promptFeedback}")
                "Keine Antwort erhalten - möglicherweise durch Safety Filter blockiert"
            }

            Log.d(TAG, "Erfolgreich: ${resultText.length} Zeichen")

            Result.success(resultText)

        } catch (e: com.google.ai.client.generativeai.type.SerializationException) {
            Log.e(TAG, "Serialisierungs-Fehler bei Gemini API", e)
            Result.failure(Exception("Die API-Antwort konnte nicht verarbeitet werden. Möglicherweise ist das Modell '$MODEL_NAME' nicht verfügbar oder die Antwort wurde blockiert."))
        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei Gemini API Anfrage", e)
            Result.failure(e)
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

            val model = createModel()

            val prompt = """
Erkläre kurz (max. 2-3 Sätze) den Feiertag "$holidayName" am $date:
- Was wird gefeiert?
- Warum ist es ein Feiertag?
- Gibt es Traditionen?

Antworte auf Deutsch, freundlich und informativ.
            """.trimIndent()

            val response = model.generateContent(prompt)
            val resultText = response.text ?: "Keine Erklärung verfügbar"

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

            val model = createModel()

            val prompt = """
Der gewünschte Urlaubstermin $desiredDate ist nicht verfügbar.
Grund: $reason

Schlage 3 alternative Zeiträume vor:
- Verfügbare Urlaubstage: $availableDays
- Ähnliche Jahreszeit bevorzugt
- Kurze Begründung pro Alternative

Antworte prägnant und hilfreich auf Deutsch.
            """.trimIndent()

            val response = model.generateContent(prompt)
            val resultText = response.text ?: "Keine Alternativen gefunden"

            Result.success(resultText)

        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei Alternative-Suche", e)
            Result.failure(e)
        }
    }
}
