package com.example.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.backup.DriveBackupCard
import com.example.ui.components.hideSoftKeyboardNow
import com.example.ui.theme.ThingsIconSize
import com.example.ui.theme.ThingsSpacing
import com.example.ui.theme.ThingsTheme
import com.example.ui.theme.dimens

/** Кнопка «Назад» в верхней строке — как на экранах списков */
private val SETTINGS_BACK_ICON_SIZE = 24.dp

/**
 * Экран «Налаштування»: открывается со строки внизу главного экрана.
 * Здесь синхронизация с Google Tasks и резервная копия / синхронизация через Google Drive.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    googleToken: String,
    syncError: String?,
    isSyncing: Boolean,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    cardSurfaceColor: Color,
    dividerColor: Color,
    backgroundColor: Color,
    onSyncClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        TopAppBar(
            title = {},
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = textPrimaryColor,
                        modifier = Modifier.size(SETTINGS_BACK_ICON_SIZE)
                    )
                }
            },
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaterialTheme.dimens.homeListGutter),
            verticalArrangement = Arrangement.spacedBy(ThingsSpacing.L)
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = ThingsTheme.type.largeTitle.copy(color = textPrimaryColor),
                    modifier = Modifier.padding(horizontal = MaterialTheme.dimens.homeRowInset, vertical = ThingsSpacing.S)
                )
            }
            item {
                GoogleTasksSyncCard(
                    googleToken = googleToken,
                    syncError = syncError,
                    isSyncing = isSyncing,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    cardSurfaceColor = cardSurfaceColor,
                    dividerColor = dividerColor,
                    onSyncClick = onSyncClick
                )
            }
            item {
                DriveBackupCard(
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    cardSurfaceColor = cardSurfaceColor,
                    modifier = Modifier.padding(bottom = ThingsSpacing.XXL)
                )
            }
        }
    }
}

/** Синхронизация с Google Tasks: раскрывается нажатием на заголовок — токен доступа и кнопка синхронизации. */
@Composable
private fun GoogleTasksSyncCard(
    googleToken: String,
    syncError: String?,
    isSyncing: Boolean,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    cardSurfaceColor: Color,
    dividerColor: Color,
    onSyncClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var rawTokenInput by remember { mutableStateOf(googleToken) }
    var isSyncConfigExpanded by remember { mutableStateOf(false) }
    var isTokenFieldFocused by remember { mutableStateOf(false) }
    val view = LocalView.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardSurfaceColor),
        shape = ThingsTheme.shapes.cardShape
    ) {
        Column(modifier = Modifier.padding(ThingsSpacing.L)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Блок с полем токена сворачивается анимацией (~400 мс) — если клавиатура
                        // открыта для этого поля, прячем её сразу (см. hideSoftKeyboardNow)
                        if (isSyncConfigExpanded && isTokenFieldFocused) view.hideSoftKeyboardNow()
                        isSyncConfigExpanded = !isSyncConfigExpanded
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudSync, contentDescription = stringResource(R.string.cd_google_sync), tint = ThingsTheme.colors.inbox, modifier = Modifier.size(ThingsIconSize.L))
                    Spacer(modifier = Modifier.width(MaterialTheme.dimens.homeSyncInnerGap))
                    Text(stringResource(R.string.ui_google_tasks_sync_settings), style = ThingsTheme.type.settingsTitle.copy(color = textPrimaryColor))
                }
                Icon(
                    imageVector = if (isSyncConfigExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = stringResource(R.string.cd_toggle_settings),
                    tint = textSecondaryColor
                )
            }

            AnimatedVisibility(visible = isSyncConfigExpanded) {
                Column(modifier = Modifier.padding(top = ThingsSpacing.M)) {
                    Text(
                        stringResource(R.string.ui_enter_an_oauth_access_token_to),
                        style = ThingsTheme.type.caption.copy(color = textSecondaryColor)
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.homeSyncInnerGap))

                    OutlinedTextField(
                        value = rawTokenInput,
                        onValueChange = { rawTokenInput = it },
                        label = { Text(stringResource(R.string.ui_google_access_token), style = ThingsTheme.type.caption) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isTokenFieldFocused = it.isFocused }
                            .testTag("google_token_input"),
                        shape = ThingsTheme.shapes.rowShape,
                        textStyle = ThingsTheme.type.caption.copy(color = textPrimaryColor),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThingsTheme.colors.accent,
                            unfocusedBorderColor = dividerColor
                        )
                    )

                    Spacer(modifier = Modifier.height(ThingsSpacing.M))

                    if (syncError != null) {
                        Text(
                            stringResource(R.string.ui_sync_error, syncError.ifBlank { stringResource(R.string.sync_error_unknown) }),
                            color = ThingsTheme.colors.danger,
                            style = ThingsTheme.type.caption,
                            modifier = Modifier.padding(bottom = ThingsSpacing.S)
                        )
                    }

                    Button(
                        onClick = { onSyncClick(rawTokenInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("execute_sync_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ThingsTheme.colors.accent),
                        shape = ThingsTheme.shapes.chipShape,
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(ThingsIconSize.S), color = ThingsTheme.colors.onAccent)
                        } else {
                            Text(stringResource(R.string.ui_sync_tasks_now), color = ThingsTheme.colors.onAccent, style = ThingsTheme.type.settingsAction)
                        }
                    }
                }
            }
        }
    }
}
