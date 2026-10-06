package com.example.albionmarketv2

import android.content.Context
import androidx.core.content.edit

/**
 * Enterprise-grade Login Security Manager.
 * Manages rate limiting, multi-tier exponential lockout timers after failed attempts,
 * and high-security validation for username and password credentials.
 */
object LoginSecurityManager {

    private const val PREFS_NAME = "albion_login_security_prefs"
    private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
    private const val KEY_LOCKOUT_UNTIL_MS = "lockout_until_ms"

    private const val MAX_FAILED_ATTEMPTS = 5
    private const val BASE_LOCKOUT_SECONDS = 30L
    private const val MAX_LOCKOUT_SECONDS = 900L // Max 15 minutes lockout

    /**
     * Checks if a login attempt is allowed or if the device/user is temporarily locked out.
     * @return Pair(isAllowed, remainingLockoutSeconds)
     */
    fun canAttemptLogin(context: Context): Pair<Boolean, Long> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL_MS, 0L)
        val now = System.currentTimeMillis()

        if (now < lockoutUntil) {
            val remainingSeconds = ((lockoutUntil - now) / 1000L) + 1L
            return Pair(false, remainingSeconds)
        }
        return Pair(true, 0L)
    }

    /**
     * Records a failed login attempt with exponential backoff scaling.
     * @return remaining lockout seconds if locked out, or 0 if not locked out yet.
     */
    fun recordFailedAttempt(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val now = System.currentTimeMillis()

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            val multiplier = (1L shl (attempts - MAX_FAILED_ATTEMPTS).coerceAtMost(5)) // Exponential scaling: 1x, 2x, 4x, 8x, 16x, 32x
            val lockoutSecs = (BASE_LOCKOUT_SECONDS * multiplier).coerceAtMost(MAX_LOCKOUT_SECONDS)
            val lockoutUntil = now + (lockoutSecs * 1000L)
            prefs.edit {
                putInt(KEY_FAILED_ATTEMPTS, attempts)
                putLong(KEY_LOCKOUT_UNTIL_MS, lockoutUntil)
            }
            return lockoutSecs
        } else {
            prefs.edit {
                putInt(KEY_FAILED_ATTEMPTS, attempts)
            }
            return 0L
        }
    }

    /**
     * Resets failed login attempt counter upon successful enterprise authentication.
     */
    fun resetFailedAttempts(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            remove(KEY_FAILED_ATTEMPTS)
            remove(KEY_LOCKOUT_UNTIL_MS)
        }
    }

    /**
     * Validates username requirements with strict sanitization.
     */
    fun validateUsername(username: String): Pair<Boolean, String> {
        val trimmed = username.trim()
        return when {
            trimmed.isBlank() -> Pair(false, "❌ Benutzername darf nicht leer sein.")
            trimmed.length < 3 -> Pair(false, "❌ Benutzername muss mindestens 3 Zeichen lang sein.")
            trimmed.length > 30 -> Pair(false, "❌ Benutzername darf maximal 30 Zeichen lang sein.")
            !trimmed.matches(Regex("^[a-zA-Z0-9_.-]+$")) -> Pair(false, "❌ Benutzername enthält ungültige Zeichen.")
            else -> Pair(true, "")
        }
    }

    /**
     * Validates password strength requirements for secure login and registration.
     */
    fun validatePassword(password: String, isRegistration: Boolean = false): Pair<Boolean, String> {
        val trimmed = password.trim()
        return when {
            trimmed.isBlank() -> Pair(false, "❌ Passwort darf nicht leer sein.")
            trimmed.length < 6 -> Pair(false, "❌ Passwort muss mindestens 6 Zeichen lang sein.")
            isRegistration && trimmed.length < 8 -> Pair(false, "❌ Registrierung erfordert ein sicheres Passwort (mind. 8 Zeichen).")
            else -> Pair(true, "")
        }
    }
}
