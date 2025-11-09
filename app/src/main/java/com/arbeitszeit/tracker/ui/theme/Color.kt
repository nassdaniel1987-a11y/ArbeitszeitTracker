package com.arbeitszeit.tracker.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Colors
val Blue700 = Color(0xFF1976D2)
val Blue500 = Color(0xFF2196F3)
val Blue200 = Color(0xFF90CAF9)

// Secondary Colors
val Green700 = Color(0xFF388E3C)
val Green500 = Color(0xFF4CAF50)
val Green200 = Color(0xFFA5D6A7)

// Error
val Red700 = Color(0xFFC62828)
val Red500 = Color(0xFFF44336)

// Warning/Orange
val Orange500 = Color(0xFFFF9800)
val Orange700 = Color(0xFFF57C00)

// Status Colors
val StatusComplete = Color(0xFF4CAF50)   // Grün
val StatusPartial = Color(0xFFFFC107)     // Gelb
val StatusEmpty = Color(0xFFF44336)       // Rot
val StatusMissing = Color(0xFF9E9E9E)     // Grau
val StatusSpecial = Color(0xFF2196F3)     // Blau

// TimeEntry Type Colors
val TypeNormal = Blue700                  // Arbeit (Blau)
val TypeUrlaub = Green500                 // Urlaub (Grün)
val TypeKrank = Red500                    // Krank (Rot)
val TypeFeiertag = Blue500                // Feiertag (Hellblau)
val TypeAbwesend = Color(0xFF9E9E9E)      // Abwesend (Grau)

// Time Tracking Colors
val OvertimeColor = Green500              // Überstunden (Grün)
val UndertimeColor = Red500               // Fehlstunden (Rot)
val ProgressGood = Green500               // ≥100% (Grün)
val ProgressWarning = Orange500           // 80-99% (Orange)
val ProgressBad = Red500                  // <80% (Rot)

// Location Status Colors
val LocationAtWork = Green500             // Am Arbeitsort (Grün)
val LocationNotAtWork = Orange500         // Nicht am Arbeitsort (Orange)
val LocationInactive = Color(0xFF9E9E9E)  // Inaktiv (Grau)

// Neutral
val Grey50 = Color(0xFFFAFAFA)
val Grey100 = Color(0xFFF5F5F5)
val Grey200 = Color(0xFFEEEEEE)
val Grey800 = Color(0xFF424242)
val Grey900 = Color(0xFF212121)
