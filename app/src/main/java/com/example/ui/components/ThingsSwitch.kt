package com.example.ui.components

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.theme.ThingsTheme

/**
 * Переключатель настроек — один вид на всех экранах (спецификация: docs/design-system/components/Switch.md):
 * стандартный Material 3, включённый — дорожка `accent`.
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
        colors = SwitchDefaults.colors(checkedTrackColor = ThingsTheme.colors.accent)
    )
}
