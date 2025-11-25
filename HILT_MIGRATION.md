# Hilt DI - ViewModel Migration Guide

## ✅ Bereits erledigt:

1. Hilt Dependencies in `build.gradle.kts` hinzugefügt
2. `TrackerApplication` mit `@HiltAndroidApp` erstellt
3. `MainActivity` mit `@AndroidEntryPoint` annotiert
4. `DatabaseModule` für Database-Injection erstellt

## 📝 TODO: ViewModels migrieren

Aktuell erstellen die Screens ihre ViewModels noch mit `viewModel()` und die ViewModels holen sich die Database manuell.

### Beispiel: HomeViewModel migrieren

**VORHER:**
```kotlin
// HomeScreen.kt
val viewModel: HomeViewModel = viewModel()

// HomeViewModel.kt
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val timeEntryDao = database.timeEntryDao()
    // ...
}
```

**NACHHER:**
```kotlin
// HomeScreen.kt
@Composable
fun HomeScreen() {
    val viewModel: HomeViewModel = hiltViewModel()
    // ...
}

// HomeViewModel.kt
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val timeEntryDao: TimeEntryDao,
    private val settingsDao: UserSettingsDao,
    private val workLocationDao: WorkLocationDao,
    private val sollZeitVorlageDao: SollZeitVorlageDao
) : ViewModel() {  // Nicht mehr AndroidViewModel!
    // DAOs werden automatisch injiziert!
}
```

### Schritt-für-Schritt für JEDEN ViewModel:

#### 1. Screen-Datei anpassen

**Import hinzufügen:**
```kotlin
import androidx.hilt.navigation.compose.hiltViewModel
```

**ViewModel-Creation ändern:**
```kotlin
// Alt:
val viewModel: HomeViewModel = viewModel()

// Neu:
val viewModel: HomeViewModel = hiltViewModel()
```

#### 2. ViewModel-Klasse anpassen

**Imports hinzufügen:**
```kotlin
import androidx.lifecycle.ViewModel  // Statt AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
```

**Klasse annotieren und Konstruktor anpassen:**
```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val timeEntryDao: TimeEntryDao,
    private val settingsDao: UserSettingsDao,
    // Nur die DAOs, die du brauchst!
) : ViewModel() {  // NICHT AndroidViewModel!

    // ENTFERNE diese Zeilen:
    // private val database = AppDatabase.getDatabase(application)
    // private val timeEntryDao = database.timeEntryDao()

    // DAOs sind jetzt Konstruktor-Parameter!
}
```

**Wichtig:**
- `AndroidViewModel(application)` → `ViewModel()`
- Wenn du `getApplication()` nutzt, musst du es anders lösen
- Für Context-Zugriff: `@ApplicationContext context: Context` im Konstruktor

### Liste der zu migrierenden ViewModels:

- [ ] `HomeViewModel`
- [ ] `CalendarViewModel`
- [ ] `UeberstundenViewModel`
- [ ] `ExportViewModel`
- [ ] `GeofencingViewModel`
- [ ] `SettingsViewModel`
- [ ] `TemplateViewModel`
- [ ] `WeekTemplatesViewModel`

### Beispiel mit ApplicationContext:

Wenn ein ViewModel Context braucht (z.B. für Dateizugriff):

```kotlin
@HiltViewModel
class ExportViewModel @Inject constructor(
    private val timeEntryDao: TimeEntryDao,
    private val settingsDao: UserSettingsDao,
    @ApplicationContext private val context: Context  // So!
) : ViewModel() {
    // ...
}
```

### Testen

Nach jeder Migration:
1. App neu builden
2. Entsprechenden Screen öffnen
3. Prüfen: Keine Crashes? ✅
4. Prüfen: Funktionalität funktioniert? ✅

## Warum Hilt?

**Vorteile:**
- ✅ Bessere Testbarkeit (Mock-DAOs einfach injizieren)
- ✅ Weniger Boilerplate (keine manuelle Database-Hol-Logik)
- ✅ Single Source of Truth (Database-Instanz als Singleton)
- ✅ Lifecycle-Aware (ViewModels werden automatisch cleared)
- ✅ Compile-Time Safety (Fehler beim Build, nicht zur Runtime)

**Best Practices:**
- Nur DAOs injizieren, die du wirklich brauchst
- Keine Business-Logik in die Modules
- Repository-Pattern verwenden für komplexe Logik (optional)

## Nächste Schritte (Optional):

1. **Repository-Pattern** einführen
   - `TimeEntryRepository` mit Hilt
   - ViewModels nutzen Repository statt DAO direkt

2. **WorkManager mit Hilt**
   - Workers können auch Hilt nutzen
   - `@HiltWorker` Annotation

3. **Navigation mit Hilt**
   - Bereits vorbereitet mit `hilt-navigation-compose`

## Fragen?

- [Hilt Documentation](https://developer.android.com/training/dependency-injection/hilt-android)
- [Hilt with ViewModels](https://developer.android.com/training/dependency-injection/hilt-jetpack)
