package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.example.ui.theme.ThingsElevation
import com.example.ui.theme.ThingsSpacing
import com.example.ui.theme.ThingsTheme

private val CONFIRM_POPOVER_WIDTH = 260.dp
private val CONFIRM_POPOVER_ARROW_WIDTH = 22.dp
private val CONFIRM_POPOVER_ARROW_HEIGHT = 10.dp

/**
 * Подтверждение во всплывающей карточке у кнопки, как в Things при удалении области: светлая карточка
 * с «хвостиком» к кнопке, пояснение и одна капсула действия (`danger`). Кнопки отмены нет — нажатие мимо
 * карточки (или «Назад») закрывает её.
 *
 * Ставится внутрь кнопки, к которой относится: карточка встаёт под ней по центру, но не ближе
 * `ThingsSpacing.L` к краю экрана, а хвостик всегда смотрит на кнопку.
 */
@Composable
fun ThingsConfirmPopover(
    expanded: Boolean,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!expanded) return
    val density = LocalDensity.current
    val margin = with(density) { ThingsSpacing.L.roundToPx() }
    // Где под карточкой центр кнопки — его знает только расстановка окна, хвостик рисуется по нему
    var arrowCenterX by remember { mutableIntStateOf(0) }
    val positionProvider = remember(margin) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val anchorX = anchorBounds.center.x
                val maxX = (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)
                val x = (anchorX - popupContentSize.width / 2).coerceIn(margin, maxX)
                arrowCenterX = anchorX - x
                return IntOffset(x, anchorBounds.bottom)
            }
        }
    }
    // Скругление — как у капсулы поиска (22 dp): в Things карточка подтверждения скруглена так же
    val radius = ThingsTheme.shapes.capsule
    val shape = remember(arrowCenterX, density, radius) {
        PopoverShape(arrowCenterX.toFloat(), with(density) { radius.toPx() }, density)
    }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            modifier = Modifier
                .width(CONFIRM_POPOVER_WIDTH)
                .shadow(ThingsElevation.CARD, shape)
                .background(ThingsTheme.colors.searchCard, shape)
                .padding(top = CONFIRM_POPOVER_ARROW_HEIGHT)
                .padding(ThingsSpacing.L)
        ) {
            Text(
                text = message,
                color = ThingsTheme.colors.textPrimary,
                style = ThingsTheme.type.dialogBody
            )
            Spacer(modifier = Modifier.height(ThingsSpacing.L))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ThingsTheme.shapes.capsuleShape)
                    .background(ThingsTheme.colors.searchField)
                    .clickable(onClick = onConfirm)
                    .padding(vertical = ThingsSpacing.M),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = confirmText,
                    color = ThingsTheme.colors.danger,
                    style = ThingsTheme.type.dialogButton
                )
            }
        }
    }
}

/** Карточка со скруглёнными углами и треугольным хвостиком сверху, вершиной над [arrowCenterX]. */
private class PopoverShape(
    private val arrowCenterX: Float,
    private val radius: Float,
    private val density: Density
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val arrowW = with(this.density) { CONFIRM_POPOVER_ARROW_WIDTH.toPx() }
        val arrowH = with(this.density) { CONFIRM_POPOVER_ARROW_HEIGHT.toPx() }
        // Хвостик не заходит на скругление угла
        val center = arrowCenterX.coerceIn(radius + arrowW / 2, size.width - radius - arrowW / 2)
        val path = Path().apply {
            addRoundRect(RoundRect(0f, arrowH, size.width, size.height, CornerRadius(radius)))
            moveTo(center - arrowW / 2, arrowH + 1f)
            lineTo(center, 0f)
            lineTo(center + arrowW / 2, arrowH + 1f)
            close()
        }
        return Outline.Generic(path)
    }
}
