package com.arbeitszeit.tracker.drive

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Google Sign-In Manager
 *
 * @deprecated Diese Klasse verwendet die veraltete Google Sign-In API.
 * Bitte verwende stattdessen [CredentialAuthManager], der die moderne
 * Credential Manager API (Android 14+) nutzt.
 *
 * Migration:
 * - Ersetze `GoogleSignInManager` durch `CredentialAuthManager`
 * - Verwende `signIn()` als suspend function statt Intent-Launcher
 * - `AccountState.SignedIn` enthält jetzt `AccountInfo` statt `GoogleSignInAccount`
 *
 * Verwaltet die Google-Authentifizierung und den Account-Status.
 * Verwendet Google Sign-In API für OAuth 2.0 Flow.
 */
@Deprecated(
    message = "Verwende CredentialAuthManager für moderne Credential Manager API",
    replaceWith = ReplaceWith("CredentialAuthManager", "com.arbeitszeit.tracker.drive.CredentialAuthManager")
)
class GoogleSignInManager(private val context: Context) {

    private val _accountState = MutableStateFlow<AccountState>(AccountState.SignedOut)
    val accountState: StateFlow<AccountState> = _accountState.asStateFlow()

    private var googleSignInClient: GoogleSignInClient

    init {
        // Google Sign-In konfigurieren mit Drive.File Scope
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))  // Nur App-eigene Dateien
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, signInOptions)

        // Check if already signed in
        checkSignInStatus()
    }

    /**
     * Prüft ob bereits ein Google-Account angemeldet ist
     */
    private fun checkSignInStatus() {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && hasRequiredScopes(account)) {
            _accountState.value = AccountState.SignedIn(account)
        } else {
            _accountState.value = AccountState.SignedOut
        }
    }

    /**
     * Prüft ob der Account die erforderlichen Scopes hat
     */
    private fun hasRequiredScopes(account: GoogleSignInAccount): Boolean {
        return GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE))
    }

    /**
     * Gibt den Sign-In Intent zurück
     * Verwende dies mit ActivityResultLauncher
     */
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    /**
     * Verarbeitet das Sign-In Ergebnis
     */
    fun handleSignInResult(data: Intent?): SignInResult {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.result

            if (account != null && hasRequiredScopes(account)) {
                _accountState.value = AccountState.SignedIn(account)
                SignInResult.Success(account)
            } else {
                _accountState.value = AccountState.SignedOut
                SignInResult.Error("Missing required permissions")
            }
        } catch (e: Exception) {
            _accountState.value = AccountState.SignedOut
            SignInResult.Error(e.message ?: "Sign in failed")
        }
    }

    /**
     * Meldet den User ab
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            googleSignInClient.signOut().await()
            _accountState.value = AccountState.SignedOut
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Widerruft den Zugriff (Disconnect)
     */
    suspend fun revokeAccess(): Result<Unit> {
        return try {
            googleSignInClient.revokeAccess().await()
            _accountState.value = AccountState.SignedOut
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gibt das aktuell angemeldete Google-Konto zurück
     */
    fun getCurrentAccount(): GoogleSignInAccount? {
        return when (val state = _accountState.value) {
            is AccountState.SignedIn -> state.account
            is AccountState.SignedOut -> null
        }
    }

    /**
     * Erstellt Credentials für Google Drive API
     */
    fun getCredentials(): GoogleAccountCredential? {
        val account = getCurrentAccount() ?: return null

        return GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE)
        ).apply {
            selectedAccount = account.account
        }
    }

    /**
     * Account State
     */
    sealed class AccountState {
        data class SignedIn(val account: GoogleSignInAccount) : AccountState()
        data object SignedOut : AccountState()
    }

    /**
     * Sign-In Result
     */
    sealed class SignInResult {
        data class Success(val account: GoogleSignInAccount) : SignInResult()
        data class Error(val message: String) : SignInResult()
    }
}
