package com.example.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.R
import com.example.data.backup.DriveBackupManager
import com.example.ui.components.ThingsConfirmDialog
import com.example.ui.theme.ThingsIconSize
import com.example.ui.theme.ThingsSpacing
import com.example.ui.theme.ThingsTheme
import com.example.ui.theme.dimens

/**
 * Карточка на главном экране: резервная копия базы в скрытой папке приложения на Google Drive — подключить,
 * «Зберегти зараз», «Відновити з копії» (с подтверждением: нынешние данные заменятся) и автоматическая копия раз в день.
 */
@Composable
fun DriveBackupCard(
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    cardSurfaceColor: Color,
    modifier: Modifier = Modifier,
    viewModel: DriveBackupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    var confirmRestore by remember { mutableStateOf(false) }

    // Окно Google (аккаунт и согласие на доступ к скрытой папке Drive) — его ответ продолжает действие
    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        viewModel.onConsentResult(result.data)
    }
    LaunchedEffect(viewModel) {
        viewModel.consent.collect { pending ->
            consentLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardSurfaceColor),
        shape = ThingsTheme.shapes.cardShape
    ) {
        Column(modifier = Modifier.padding(ThingsSpacing.L)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Backup, contentDescription = null, tint = ThingsTheme.colors.inbox, modifier = Modifier.size(ThingsIconSize.L))
                Spacer(modifier = Modifier.width(MaterialTheme.dimens.homeSyncInnerGap))
                Text(stringResource(R.string.backup_title), style = ThingsTheme.type.settingsTitle.copy(color = textPrimaryColor))
            }
            Spacer(modifier = Modifier.height(ThingsSpacing.M))

            val remoteAt = state.remoteBackupAt
            Text(
                text = when {
                    !state.connected -> stringResource(R.string.backup_hint)
                    remoteAt != null -> stringResource(R.string.backup_last, formatBackupTime(remoteAt))
                    else -> stringResource(R.string.backup_none)
                },
                style = ThingsTheme.type.caption.copy(color = textSecondaryColor)
            )

            state.error?.let { error ->
                Spacer(modifier = Modifier.height(ThingsSpacing.S))
                Text(
                    text = stringResource(
                        when (error) {
                            DriveBackupManager.Error.NETWORK -> R.string.backup_error_network
                            DriveBackupManager.Error.ACCESS -> R.string.backup_error_access
                            DriveBackupManager.Error.NO_BACKUP -> R.string.backup_error_none
                            DriveBackupManager.Error.BAD_BACKUP -> R.string.backup_error_bad
                        }
                    ),
                    style = ThingsTheme.type.caption.copy(color = ThingsTheme.colors.danger)
                )
            }
            Spacer(modifier = Modifier.height(ThingsSpacing.M))

            if (!state.connected) {
                ActionButton(stringResource(R.string.backup_connect), busy = state.busy, onClick = viewModel::connect)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.backup_auto), style = ThingsTheme.type.bodyMedium.copy(color = textPrimaryColor))
                    Switch(
                        checked = state.autoBackup,
                        onCheckedChange = viewModel::setAutoBackup,
                        colors = SwitchDefaults.colors(checkedTrackColor = ThingsTheme.colors.accent)
                    )
                }
                Spacer(modifier = Modifier.height(ThingsSpacing.S))
                ActionButton(stringResource(R.string.backup_now), busy = state.busy, onClick = viewModel::backupNow)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { confirmRestore = true }, enabled = !state.busy && remoteAt != null) {
                        Text(stringResource(R.string.backup_restore), style = ThingsTheme.type.bodyMedium)
                    }
                    TextButton(onClick = viewModel::disconnect, enabled = !state.busy) {
                        Text(stringResource(R.string.backup_disconnect), style = ThingsTheme.type.bodyMedium.copy(color = textSecondaryColor))
                    }
                }

                // Синхронизация между устройствами — через ту же скрытую папку Drive
                Spacer(modifier = Modifier.height(ThingsSpacing.M))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.sync_devices), style = ThingsTheme.type.bodyMedium.copy(color = textPrimaryColor))
                        val lastSync = syncState.lastSyncAt
                        Text(
                            text = when {
                                !syncState.enabled -> stringResource(R.string.sync_devices_hint)
                                lastSync != null -> stringResource(R.string.sync_last, formatBackupTime(lastSync))
                                else -> stringResource(R.string.sync_never)
                            },
                            style = ThingsTheme.type.caption.copy(color = textSecondaryColor)
                        )
                    }
                    Switch(
                        checked = syncState.enabled,
                        onCheckedChange = viewModel::setSyncEnabled,
                        colors = SwitchDefaults.colors(checkedTrackColor = ThingsTheme.colors.accent)
                    )
                }
                syncState.error?.let { error ->
                    Spacer(modifier = Modifier.height(ThingsSpacing.XS))
                    Text(
                        text = stringResource(
                            when (error) {
                                com.example.data.sync.SyncManager.Error.NETWORK -> R.string.backup_error_network
                                com.example.data.sync.SyncManager.Error.ACCESS -> R.string.backup_error_access
                                com.example.data.sync.SyncManager.Error.BAD_FILE -> R.string.sync_error_bad
                            }
                        ),
                        style = ThingsTheme.type.caption.copy(color = ThingsTheme.colors.danger)
                    )
                }
                if (syncState.enabled) {
                    Spacer(modifier = Modifier.height(ThingsSpacing.S))
                    ActionButton(stringResource(R.string.sync_now), busy = syncState.busy, onClick = viewModel::syncNow)
                }
            }
        }
    }

    if (confirmRestore) {
        ThingsConfirmDialog(
            title = stringResource(R.string.backup_restore_title),
            message = stringResource(R.string.backup_restore_message, state.remoteBackupAt?.let { formatBackupTime(it) }.orEmpty()),
            confirmText = stringResource(R.string.backup_restore_confirm),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                confirmRestore = false
                viewModel.restore()
            },
            onDismiss = { confirmRestore = false }
        )
    }
}

@Composable
private fun ActionButton(label: String, busy: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = ThingsTheme.colors.accent),
        shape = ThingsTheme.shapes.chipShape,
        enabled = !busy
    ) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(ThingsIconSize.S), color = ThingsTheme.colors.onAccent)
        } else {
            Text(label, color = ThingsTheme.colors.onAccent, style = ThingsTheme.type.settingsAction)
        }
    }
}

/** Дата и время копии на языке телефона: «18 жовт. 2026 р., 09:15» */
private fun formatBackupTime(millis: Long): String =
    java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT).format(java.util.Date(millis))
