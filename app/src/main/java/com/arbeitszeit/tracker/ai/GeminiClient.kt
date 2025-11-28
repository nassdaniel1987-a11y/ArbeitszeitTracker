package com.arbeitszeit.tracker.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.vertexai.vertexAI
import com.google.firebase.vertexai.type.HarmBlockThreshold
import com.google.firebase.vertexai.type.HarmCategory
import com.google.firebase.vertexai.type.SafetySetting
import com.google.firebase.vertexai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Firebase Vertex AI Client für intelligente Urlaubsplanung
 *
 * Verwendet Google's Gemini 2.5 Flash via Firebase Vertex AI um:
 * - Optimale Urlaubstage vorzuschlagen
 * - Brückentage zu identifizieren
 * - Schulferien zu berücksichtigen
 * - Schließtage der Einrichtung einzubeziehen
 *
 * Vorteile Firebase Vertex AI:
 * - ✅ Offiziell supported von Google (2025+)
 * - ✅ Kein API Key nötig (Firebase Auth)
 * - ✅ Volle Gemini 2.5 Flash Unterstützung
 * - ✅ Automatische Updates & Sicherheit
 *
 * Kosten: KOSTENLOS (1500 Anfragen/Tag)
 * Datenschutz: Nur anonymisierte Daten (Anzahl Tage, Bundesland, Präferenzen)
 */
class GeminiClient {

    companion object {
        private const val TAG = "GeminiClient"
        private const val MODEL_NAME = "gemini-2.5-flash"  // Neueste Flash-Version via Firebase
    }

    /**
     * Prüft ob Firebase Vertex AI verfügbar ist
     */
    fun isConfigured(): Boolean {
        return try {
            Firebase.vertexAI
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Vertex AI nicht verfügbar", e)
            false
        }
    }

    /**
     * Erstellt das Gemini-Modell mit optimalen Einstellungen via Firebase
     */
    private fun createModel() = Firebase.vertexAI.generativeModel(
        modelName = MODEL_NAME,
        generationConfig = generationConfig {
            temperature = 0.7f  // Kreativ aber nicht zu wild
            topK = 40
            topP = 0.95f
            maxOutputTokens = 2048  // Genug für detaillierte Antworten
        },
        safetySettings = listOf(
            // Safety Settings gelockert - Urlaubsplanung ist harmlos
            SafetySetting(HarmCategory.HARASSMENT, HarmBlockThreshold.NONE),
            SafetySetting(HarmCategory.HATE_SPEECH, HarmBlockThreshold.NONE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, HarmBlockThreshold.NONE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, HarmBlockThreshold.NONE),
        )
    )

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
                    Exception("Firebase Vertex AI nicht konfiguriert. Bitte google-services.json im app/ Ordner ablegen.")
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

            Log.d(TAG, "Sende Anfrage an Firebase Vertex AI...")
            Log.d(TAG, "Model: $MODEL_NAME")

            val response = model.generateContent(prompt)

            Log.d(TAG, "Response erhalten")

            val resultText = response.text ?: run {
                Log.e(TAG, "Response.text ist null!")
                Log.e(TAG, "Candidates: ${response.candidates}")
                "Keine Antwort erhalten - möglicherweise durch Safety Filter blockiert"
            }

            Log.d(TAG, "Erfolgreich: ${resultText.length} Zeichen")

            Result.success(resultText)

        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei Firebase Vertex AI Anfrage", e)
            Result.failure(Exception("Fehler bei KI-Anfrage: ${e.message}"))
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
                    Exception("Firebase Vertex AI nicht konfiguriert.")
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
                    Exception("Firebase Vertex AI nicht konfiguriert.")
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
