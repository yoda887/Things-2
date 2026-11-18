package com.example.ui.screens.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.R
import com.example.ui.theme.ThingsIconSize
import com.example.ui.theme.ThingsSpacing
import com.example.ui.theme.ThingsStroke
import com.example.ui.theme.ThingsTheme
import com.example.ui.theme.dimens

/**
 * Карточка «Напоминания»: чтобы напоминания приходили при закрытом приложении и погасшем экране, система должна
 * разрешить ему работу в фоне. Сами разрешения приложение включить не может — кнопки открывают нужные экраны
 * системы: батарея «Без ограничений» (её состояние видно — проверяется при каждом возвращении на экран) и, на
 * Xiaomi, «Автозапуск» (без него HyperOS, смахнув приложение из недавних, останавливает его вместе с будильниками;
 * его состояние системой не сообщается).
 */
@Composable
fun ReminderSettingsCard(
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    cardSurfaceColor: Color,
    dividerColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var unrestricted by remember { mutableStateOf(isBatteryUnrestricted(context)) }
    // Вернулись из настроек системы — состояние могло измениться
    LifecycleResumeEffect(Unit) {
        unrestricted = isBatteryUnrestricted(context)
        onPauseOrDispose { }
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardSurfaceColor),
        shape = ThingsTheme.shapes.cardShape
    ) {
        Column {
            Row(modifier = Modifier.padding(ThingsSpacing.L), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Notifications, contentDescription = null, tint = ThingsTheme.colors.accent, modifier = Modifier.size(ThingsIconSize.L))
                Spacer(modifier = Modifier.width(MaterialTheme.dimens.homeSyncInnerGap))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_reminders), style = ThingsTheme.type.settingsTitle.copy(color = textPrimaryColor))
                    Text(stringResource(R.string.settings_reminders_hint), style = ThingsTheme.type.caption.copy(color = textSecondaryColor))
                }
            }
            HorizontalDivider(color = dividerColor, thickness = ThingsStroke.HAIRLINE)
            ReminderSettingsRow(
                title = stringResource(R.string.settings_reminders_battery),
                status = stringResource(if (unrestricted) R.string.settings_reminders_battery_on else R.string.settings_reminders_battery_off),
                statusColor = if (unrestricted) textSecondaryColor else ThingsTheme.colors.danger,
                textPrimaryColor = textPrimaryColor,
                textSecondaryColor = textSecondaryColor,
                onClick = { openBatterySettings(context) }
            )
            if (isXiaomi()) {
                HorizontalDivider(color = dividerColor, thickness = ThingsStroke.HAIRLINE)
                ReminderSettingsRow(
                    title = stringResource(R.string.settings_reminders_autostart),
                    status = stringResource(R.string.settings_reminders_autostart_hint),
                    statusColor = textSecondaryColor,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    onClick = { openAutostartSettings(context) }
                )
            }
        }
    }
}

/** Строка-кнопка карточки: название, под ним состояние или пояснение, справа стрелка */
@Composable
private fun ReminderSettingsRow(
    title: String,
    status: String,
    statusColor: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = ThingsSpacing.L, vertical = ThingsSpacing.M),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = ThingsTheme.type.settingsAction.copy(color = textPrimaryColor))
            Text(status, style = ThingsTheme.type.caption.copy(color = statusColor))
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = textSecondaryColor,
            modifier = Modifier.size(ThingsIconSize.M)
        )
    }
}

private fun isBatteryUnrestricted(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true

private fun isXiaomi(): Boolean =
    Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) || Build.MANUFACTURER.equals("Redmi", ignoreCase = true) ||
        Build.MANUFACTURER.equals("POCO", ignoreCase = true)

/** Экран батареи приложения: на Xiaomi — сразу выбор «Без ограничений», иначе — сведения о приложении */
private fun openBatterySettings(context: Context) {
    val miui = Intent("miui.intent.action.HIDDEN_APPS_CONFIG_ACTIVITY")
        .putExtra("package_name", context.packageName)
        .putExtra("package_label", context.getString(R.string.app_name))
    if (!isXiaomi() || !tryStart(context, miui)) openAppDetails(context)
}

/** Список автозапуска HyperOS / MIUI; не открылся — сведения о приложении */
private fun openAutostartSettings(context: Context) {
    val autostart = Intent().setComponent(
        ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
    )
    if (!tryStart(context, autostart)) openAppDetails(context)
}

private fun openAppDetails(context: Context) {
    tryStart(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName)))
}

private fun tryStart(context: Context, intent: Intent): Boolean = runCatching {
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}.isSuccess
