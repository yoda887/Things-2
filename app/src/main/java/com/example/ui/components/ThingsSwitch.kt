package com.example.ui.components

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.theme.ThingsTheme

/**
 * Переключатель настроек — один вид на всех экранах (спецификация: docs/design-system/components/Switch.md):
 * Material 3, включённый — дорожка `accent` и белый бегунок. Выключенный — как в Material 3: светлая дорожка с серой
 * рамкой и серый бегунок. Его цвета заданы явно: по умолчанию Material берёт их из схемы темы, где рамка и бегунок —
 * цвет разделителя (`outline`), почти как дорожка, и бегунок выключенного был не виден.
 */
@Composable
fun ThingsSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedTrackColor = ThingsTheme.colors.accent,
            checkedThumbColor = ThingsTheme.colors.onAccent,
            uncheckedTrackColor = ThingsTheme.colors.searchField,
            uncheckedBorderColor = ThingsTheme.colors.textSecondary,
            uncheckedThumbColor = ThingsTheme.colors.textSecondary
        )
    )
}
