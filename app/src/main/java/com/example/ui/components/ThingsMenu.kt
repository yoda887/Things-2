package com.example.ui.components

import com.example.ui.theme.ThingsIconSize
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ThingsTheme

/**
 * Единое выпадающее меню опций: скруглённые углы `menuShape`, тёмный фон `overlaySurface`
 * в обеих темах. Пункты — только [ThingsMenuItem].
 *
 * Material 3 (1.2) берёт форму меню из `shapes.extraSmall`, а фон — из `colorScheme.surface`
 * с тональной подсветкой `surfaceTint`, поэтому они подменяются только внутри меню.
 */
@Composable
fun ThingsDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
    content: @Composable ColumnScope.() -> Unit
) {
    val surface = ThingsTheme.colors.overlaySurface
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(surface = surface, surfaceTint = Color.Transparent),
        shapes = MaterialTheme.shapes.copy(extraSmall = ThingsTheme.shapes.menuShape)
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            offset = offset,
            content = content
        )
    }
}

/**
 * Пункт меню: иконка цвета `project` перед надписью, надпись `menuItem` обычным начертанием
 * цветом `overlayContent`. Удаление ([destructive]) — иконка и надпись цвета `danger`.
 * Недоступный пункт приглушён целиком.
 */
@Composable
fun ThingsMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    destructive: Boolean = false
) {
    val alpha = if (enabled) 1f else 0.4f
    val textColor = if (destructive) ThingsTheme.colors.danger else ThingsTheme.colors.overlayContent
    val iconColor = if (destructive) ThingsTheme.colors.danger else ThingsTheme.colors.project
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = ThingsTheme.type.menuItem,
                color = textColor.copy(alpha = alpha)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor.copy(alpha = alpha),
                modifier = Modifier.size(ThingsIconSize.L)
            )
        },
        onClick = onClick,
        enabled = enabled
    )
}

/** Тонкий разделитель групп пунктов меню, как в меню опций проекта Things. */
@Composable
fun ThingsMenuDivider() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(vertical = com.example.ui.theme.ThingsSpacing.XS),
        color = ThingsTheme.colors.overlayDivider,
        thickness = com.example.ui.theme.ThingsStroke.HAIRLINE
    )
}
