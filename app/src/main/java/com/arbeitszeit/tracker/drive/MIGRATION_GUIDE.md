# Google Sign-In Migration: Credential Manager API

## Übersicht

Die App wurde von der veralteten Google Sign-In API zur modernen **Credential Manager API** (Android 14+) migriert.

## Was wurde geändert?

### Vorher: GoogleSignInManager (deprecated)
```kotlin
val signInManager = GoogleSignInManager(context)
val signInLauncher = rememberLauncherForActivityResult(...)
signInLauncher.launch(signInManager.getSignInIntent())
```

### Nachher: CredentialAuthManager (neu)
```kotlin
val authManager = CredentialAuthManager(context)
scope.launch {
    val result = authManager.signIn()
    // Handle result
}
```

## Vorteile der neuen API

1. **Modernere API**: Nutzt Kotlin Coroutines statt Intent-Launcher
2. **Bessere UX**: Einheitliche Credential-Auswahl über alle Apps
3. **Zukunftssicher**: Unterstützt Passkeys und weitere Auth-Methoden
4. **Einfacherer Code**: Weniger Boilerplate durch suspend functions

## Setup-Anleitung

### 1. Google Cloud Console Setup

Die neue API benötigt eine **Web Client ID** aus der Google Cloud Console:

1. Gehe zu [Google Cloud Console](https://console.cloud.google.com/)
2. Wähle dein Projekt aus (oder erstelle ein neues)
3. Navigiere zu: **APIs & Services** → **Credentials**
4. Erstelle neue Credentials vom Typ: **OAuth 2.0 Client ID**
5. Wähle Anwendungstyp: **Web application**
6. Kopiere die erstellte Client ID

### 2. Client ID in der App konfigurieren

Füge die Web Client ID zur `app/src/main/res/values/strings.xml` hinzu:

```xml
<resources>
    <string name="default_web_client_id">DEINE_WEB_CLIENT_ID.apps.googleusercontent.com</string>
</resources>
```

**Alternativ**: Verwende `local.properties` (nicht ins Git committen):

```properties
WEB_CLIENT_ID=DEINE_WEB_CLIENT_ID.apps.googleusercontent.com
```

Und lies sie im `build.gradle.kts` aus:

```kotlin
android {
    defaultConfig {
        manifestPlaceholders["webClientId"] = webClientId
    }
}
```

### 3. Dependencies

Die benötigten Dependencies sind bereits im `libs.versions.toml` definiert:

```toml
[versions]
androidx-credentials = "1.3.0"
googleid = "1.1.1"

[bundles]
credentials = [
    "androidx-credentials",
    "androidx-credentials-play-services",
    "googleid"
]
```

Und in `app/build.gradle.kts`:

```kotlin
implementation(libs.bundles.credentials)
```

## Migration bestehender Code

### CloudBackupSection.kt (bereits migriert ✅)

```kotlin
// Vorher
val signInManager = remember { GoogleSignInManager(context) }
val signInLauncher = rememberLauncherForActivityResult(...)

// Nachher
val authManager = remember { CredentialAuthManager(context) }
```

### Sign-In Flow (bereits migriert ✅)

```kotlin
// Vorher
Button(onClick = {
    val intent = signInManager.getSignInIntent()
    signInLauncher.launch(intent)
})

// Nachher
Button(onClick = {
    scope.launch {
        isSigningIn = true
        val result = authManager.signIn()
        when (result) {
            is CredentialAuthManager.SignInResult.Success -> {
                // Success
            }
            is CredentialAuthManager.SignInResult.Error -> {
                // Error
            }
        }
        isSigningIn = false
    }
})
```

### Account State (bereits migriert ✅)

```kotlin
// Vorher
when (accountState) {
    is GoogleSignInManager.AccountState.SignedIn -> {
        val email = state.account.email
    }
}

// Nachher
when (accountState) {
    is CredentialAuthManager.AccountState.SignedIn -> {
        val email = state.account.email
        val displayName = state.account.displayName
    }
}
```

## Wichtige Hinweise

### Android Version Support

- **Credential Manager**: Benötigt Android 14+ (API 34+)
- **Fallback**: Für ältere Versionen funktioniert die Play Services Auth automatisch
- **minSdk**: Die App unterstützt Android 8.0+ (API 26+) - Fallback wird automatisch verwendet

### Berechtigungen

Keine zusätzlichen Manifest-Berechtigungen nötig! Die Credential Manager API funktioniert automatisch.

### Testing

1. **Release Build**: Stelle sicher, dass die SHA-1 deines Release-Keys in der Google Cloud Console registriert ist
2. **Debug Build**: Registriere auch den Debug-Key SHA-1 für lokales Testing

SHA-1 abrufen:
```bash
# Debug Key
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# Release Key
keytool -list -v -keystore /path/to/release.keystore -alias your-alias
```

## Troubleshooting

### "Missing Web Client ID"

**Problem**: Die App kann die Web Client ID nicht finden.

**Lösung**:
- Stelle sicher, dass `default_web_client_id` in `strings.xml` definiert ist
- Oder passe `getWebClientId()` in `CredentialAuthManager.kt` an

### "Sign-in failed: GetCredentialException"

**Problem**: Die Credential Manager API schlägt fehl.

**Mögliche Ursachen**:
1. Web Client ID ist falsch oder nicht konfiguriert
2. SHA-1 Fingerprint nicht in Google Cloud Console registriert
3. Google Play Services nicht installiert oder veraltet

**Lösung**:
- Prüfe die Web Client ID
- Registriere SHA-1 in Google Cloud Console
- Aktualisiere Google Play Services auf dem Gerät

### "Unexpected credential type"

**Problem**: Die App erhält einen unerwarteten Credential-Typ.

**Lösung**:
- Stelle sicher, dass `googleid` Dependency korrekt eingebunden ist
- Prüfe die Build-Konfiguration

## Weiterführende Links

- [Android Credential Manager Guide](https://developer.android.com/training/sign-in/credential-manager)
- [Google Identity Services](https://developers.google.com/identity)
- [Migration Guide (Official)](https://developer.android.com/training/sign-in/credential-manager-migration)

## Status

✅ **Migration abgeschlossen**
- `CredentialAuthManager.kt` erstellt
- `CloudBackupSection.kt` migriert
- `GoogleSignInManager.kt` als deprecated markiert
- Dependencies aktualisiert
