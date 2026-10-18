package com.example.ui.backup

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.DriveAuth
import com.example.data.backup.DriveBackupManager
import com.example.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Карточка «Резервна копія в Google Drive»: каждое действие сначала получает доступ к Drive — сразу, если согласие
 * уже дано, или через окно Google ([consent]), после которого действие продолжается ([onConsentResult]).
 */
@HiltViewModel
class DriveBackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val manager: DriveBackupManager,
    private val sync: SyncManager
) : ViewModel() {

    val state: StateFlow<DriveBackupManager.State> = manager.state

    /** Синхронизация устройств через тот же доступ к скрытой папке Drive */
    val syncState: StateFlow<SyncManager.State> = sync.state

    private val _consent = MutableSharedFlow<PendingIntent>(extraBufferCapacity = 1)
    /** Окно Google (выбор аккаунта, согласие на доступ) — показать экраном */
    val consent: SharedFlow<PendingIntent> = _consent.asSharedFlow()

    private enum class Action { CONNECT, BACKUP, RESTORE, SYNC }
    private var pendingAction: Action? = null

    fun connect() = request(Action.CONNECT)
    fun backupNow() = request(Action.BACKUP)
    fun restore() = request(Action.RESTORE)
    fun syncNow() = request(Action.SYNC)

    /** Включить синхронизацию — и сразу синхронизироваться; выключить — фоновая больше не идёт */
    fun setSyncEnabled(enabled: Boolean) {
        sync.setEnabled(enabled)
        if (enabled) syncNow()
    }

    fun setAutoBackup(enabled: Boolean) = manager.setAutoBackup(enabled)
    fun disconnect() = manager.disconnect()
    fun clearError() = manager.clearError()

    /** Окно Google закрыто: есть согласие — продолжить отложенное действие */
    fun onConsentResult(data: Intent?) {
        val action = pendingAction ?: Action.CONNECT
        pendingAction = null
        val token = DriveAuth.tokenFromConsent(context, data)
        if (token == null) {
            manager.reportError(DriveBackupManager.Error.ACCESS)
            return
        }
        viewModelScope.launch { perform(action, token) }
    }

    private fun request(action: Action) {
        viewModelScope.launch {
            when (val outcome = runCatching { DriveAuth.authorize(context) }.getOrNull()) {
                is DriveAuth.Outcome.Granted -> perform(action, outcome.token)
                is DriveAuth.Outcome.NeedsConsent -> {
                    pendingAction = action
                    _consent.emit(outcome.intent)
                }
                null -> manager.reportError(DriveBackupManager.Error.ACCESS)
            }
        }
    }

    private suspend fun perform(action: Action, token: String) {
        if (!manager.state.value.connected) manager.onConnected(token)
        when (action) {
            Action.CONNECT -> Unit
            Action.BACKUP -> manager.backupNow(token)
            Action.RESTORE -> if (manager.restore(token) == true) manager.restartApp()
            Action.SYNC -> sync.sync(token)
        }
    }
}
