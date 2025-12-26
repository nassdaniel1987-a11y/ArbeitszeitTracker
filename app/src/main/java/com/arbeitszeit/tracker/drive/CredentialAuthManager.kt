package com.arbeitszeit.tracker.drive

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Credential Manager Auth Manager
 *
 * Moderne Authentifizierung mit Credential Manager API (Android 14+).
 * Ersetzt die deprecated GoogleSignIn API.
 */
class CredentialAuthManager(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    private val _accountState = MutableStateFlow<AccountState>(AccountState.SignedOut)
    val accountState: StateFlow<AccountState> = _accountState.asStateFlow()

    // Cached credentials
    private var cachedEmail: String? = null
    private var cachedIdToken: String? = null

    /**
     * Startet den Sign-In Flow mit Credential Manager
     */
    suspend fun signIn(): SignInResult {
        return try {
            // Google ID Option konfigurieren
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)  // Alle Google Accounts anzeigen
                .setServerClientId(getWebClientId())  // OAuth 2.0 Client ID
                .setAutoSelectEnabled(false)  // User muss manuell auswählen
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            handleSignInResult(result)
        } catch (e: GetCredentialException) {
            _accountState.value = AccountState.SignedOut
            SignInResult.Error("Sign-in failed: ${e.message}")
        } catch (e: Exception) {
            _accountState.value = AccountState.SignedOut
            SignInResult.Error("Unexpected error: ${e.message}")
        }
    }

    /**
     * Verarbeitet das Sign-In Ergebnis
     */
    private fun handleSignInResult(result: GetCredentialResponse): SignInResult {
        return try {
            when (val credential = result.credential) {
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

                        // Speichere Account-Daten
                        cachedEmail = googleIdTokenCredential.id
                        cachedIdToken = googleIdTokenCredential.idToken

                        val accountInfo = AccountInfo(
                            email = googleIdTokenCredential.id,
                            displayName = googleIdTokenCredential.displayName,
                            profilePictureUri = googleIdTokenCredential.profilePictureUri?.toString()
                        )

                        _accountState.value = AccountState.SignedIn(accountInfo)
                        SignInResult.Success(accountInfo)
                    } else {
                        _accountState.value = AccountState.SignedOut
                        SignInResult.Error("Unexpected credential type")
                    }
                }
                else -> {
                    _accountState.value = AccountState.SignedOut
                    SignInResult.Error("Unexpected credential type")
                }
            }
        } catch (e: GoogleIdTokenParsingException) {
            _accountState.value = AccountState.SignedOut
            SignInResult.Error("Invalid ID token: ${e.message}")
        } catch (e: Exception) {
            _accountState.value = AccountState.SignedOut
            SignInResult.Error("Error processing credential: ${e.message}")
        }
    }

    /**
     * Meldet den User ab
     */
    fun signOut() {
        cachedEmail = null
        cachedIdToken = null
        _accountState.value = AccountState.SignedOut
    }

    /**
     * Gibt das aktuell angemeldete Konto zurück
     */
    fun getCurrentAccount(): AccountInfo? {
        return when (val state = _accountState.value) {
            is AccountState.SignedIn -> state.account
            is AccountState.SignedOut -> null
        }
    }

    /**
     * Erstellt Credentials für Google Drive API
     *
     * WICHTIG: Diese Methode erstellt OAuth 2.0 Credentials für die Drive API.
     * Der User muss beim ersten Zugriff die Drive-Berechtigung erteilen.
     */
    fun getCredentials(): GoogleAccountCredential? {
        val email = cachedEmail ?: return null

        // Erstelle Credentials mit OAuth 2.0
        return GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE)  // Nur App-eigene Dateien
        ).apply {
            // Verwende die E-Mail des angemeldeten Users
            selectedAccountName = email
        }
    }

    /**
     * Gibt die Web Client ID für OAuth 2.0 zurück
     *
     * HINWEIS: Diese muss in der Google Cloud Console konfiguriert werden.
     * Die Client ID muss vom Typ "Web application" sein.
     */
    private fun getWebClientId(): String {
        // TODO: Diese Client ID sollte aus einer sicheren Konfigurationsdatei kommen
        // Für jetzt verwenden wir einen Platzhalter - User muss eigene Client ID eintragen
        val clientId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )

        return if (clientId != 0) {
            context.getString(clientId)
        } else {
            // Fallback: Lese aus BuildConfig oder verwende Platzhalter
            "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
        }
    }

    /**
     * Account State
     */
    sealed class AccountState {
        data class SignedIn(val account: AccountInfo) : AccountState()
        data object SignedOut : AccountState()
    }

    /**
     * Account Info - Moderne Alternative zu GoogleSignInAccount
     */
    data class AccountInfo(
        val email: String,
        val displayName: String?,
        val profilePictureUri: String?
    )

    /**
     * Sign-In Result
     */
    sealed class SignInResult {
        data class Success(val account: AccountInfo) : SignInResult()
        data class Error(val message: String) : SignInResult()
    }
}
