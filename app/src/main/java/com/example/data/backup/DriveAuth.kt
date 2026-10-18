package com.example.data.backup

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Доступ к скрытой папке приложения на Google Drive (Authorization API Google Identity): только `drive.appdata` —
 * другие файлы на Drive приложению не видны. Приложение узнаётся по имени пакета и подписи (клиент Android в Google
 * Cloud), ключей и паролей в коде нет.
 */
object DriveAuth {

    private val SCOPE = Scope("https://www.googleapis.com/auth/drive.appdata")

    private fun request(): AuthorizationRequest =
        AuthorizationRequest.builder().setRequestedScopes(listOf(SCOPE)).build()

    /** Итог запроса доступа: маркер или окно Google, которое нужно показать (выбор аккаунта, согласие) */
    sealed interface Outcome {
        data class Granted(val token: String) : Outcome
        data class NeedsConsent(val intent: PendingIntent) : Outcome
    }

    /** Запросить доступ: если согласие уже дано — сразу маркер, иначе — окно Google для показа */
    suspend fun authorize(context: Context): Outcome {
        val result = Identity.getAuthorizationClient(context).authorize(request()).await()
        val pending = result.pendingIntent
        return if (result.hasResolution() && pending != null) Outcome.NeedsConsent(pending)
        else Outcome.Granted(result.accessToken ?: error("No access token"))
    }

    /** Маркер без окон (фоновая копия); согласия нет — null */
    suspend fun tokenSilently(context: Context): String? = runCatching {
        (authorize(context) as? Outcome.Granted)?.token
    }.getOrNull()

    /** Ответ окна Google — маркер доступа; отказ — null */
    fun tokenFromConsent(context: Context, data: Intent?): String? = runCatching {
        val result: AuthorizationResult = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data)
        result.accessToken
    }.getOrNull()

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }
}
