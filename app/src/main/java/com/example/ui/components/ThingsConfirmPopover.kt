package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.ui.theme.ThingsMotion
import com.example.ui.theme.ThingsSpacing
import com.example.ui.theme.ThingsTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Размеры сняты с эталона (Things 3, удаление области): карточка 240 pt, хвостик 28×12 pt
private val CONFIRM_POPOVER_WIDTH = 240.dp
private val CONFIRM_POPOVER_ARROW_WIDTH = 28.dp
private val CONFIRM_POPOVER_ARROW_HEIGHT = 12.dp

/** Карточка вырастает из хвостика с этой доли размера, */
private const val CONFIRM_POPOVER_ENTER_SCALE = 0.5f

/** а уходя, сжимается к нему до этой доли и гаснет. */
private const val CONFIRM_POPOVER_EXIT_SCALE = 0.2f

/**
 * Подтверждение во всплывающей карточке у кнопки, как в Things при удалении области: светлая карточка
 * с хвостиком к кнопке, пояснение и одна капсула действия (`danger`). Кнопки отмены нет — нажатие мимо
 * карточки (или «Назад») закрывает её.
 *
 * Ставится внутрь кнопки, к которой относится: карточка встаёт под ней по центру, но не ближе
 * `ThingsSpacing.S` к краю экрана, а хвостик всегда смотрит на кнопку.
 *
 * Движение — как в эталоне: карточка появляется, когда меню, из которого её вызвали, уже закрылось, и
 * вырастает из хвостика; закрываясь, сжимается к нему и гаснет. [onConfirm] вызывается после того, как
 * карточка ушла.
 */
@Composable
fun ThingsConfirmPopover(
    expanded: Boolean,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val margin = with(density) { ThingsSpacing.S.roundToPx() }
    // Поле вокруг карточки внутри окна: без него тень обрезается краем окна
    val shadowRoom = ThingsElevation.CARD
    val shadowRoomPx = with(density) { shadowRoom.roundToPx() }
    val gap = with(density) { ThingsSpacing.XXS.roundToPx() }

    // Окно на экране, пока карточка видна или уходит
    var shown by remember { mutableStateOf(false) }
    // Нажата капсула: карточка уходит, затем — onConfirm
    var confirming by remember { mutableStateOf(false) }
    val scale = remember { Animatable(CONFIRM_POPOVER_ENTER_SCALE) }
    val alpha = remember { Animatable(0f) }
    val currentOnConfirm by rememberUpdatedState(onConfirm)

    LaunchedEffect(expanded, confirming) {
        if (expanded && !confirming) {
            if (shown) return@LaunchedEffect
            // Меню, из которого вызвали подтверждение, успевает почти угаснуть
            delay(ThingsMotion.FAST.toLong())
            scale.snapTo(CONFIRM_POPOVER_ENTER_SCALE)
            alpha.snapTo(0f)
            shown = true
            coroutineScope {
                launch { scale.animateTo(1f, tween(ThingsMotion.BASE, easing = LinearOutSlowInEasing)) }
                launch { alpha.animateTo(1f, tween(ThingsMotion.FAST, easing = LinearOutSlowInEasing)) }
            }
        } else if (shown) {
            // В эталоне карточка сжимается резко с первого кадра, а не разгоняясь
            coroutineScope {
                launch { scale.animateTo(CONFIRM_POPOVER_EXIT_SCALE, tween(ThingsMotion.QUICK, easing = LinearOutSlowInEasing)) }
                launch { alpha.animateTo(0f, tween(ThingsMotion.QUICK, easing = LinearOutSlowInEasing)) }
            }
            shown = false
            if (confirming) {
                currentOnConfirm()
                confirming = false
            }
        }
    }
    if (!shown) return

    // Где под карточкой центр кнопки — его знает только расстановка окна, хвостик рисуется по нему
    var arrowCenterX by remember { mutableIntStateOf(0) }
    val positionProvider = remember(margin, shadowRoomPx, gap) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val cardWidth = popupContentSize.width - 2 * shadowRoomPx
                val anchorX = anchorBounds.center.x
                val maxX = (windowSize.width - cardWidth - margin).coerceAtLeast(margin)
                val x = (anchorX - cardWidth / 2).coerceIn(margin, maxX)
                arrowCenterX = anchorX - x
                return IntOffset(x - shadowRoomPx, anchorBounds.bottom + gap - shadowRoomPx)
            }
        }
    }
    val radius = ThingsTheme.shapes.floatingCard
    val shape = remember(arrowCenterX, density, radius) {
        PopoverShape(arrowCenterX.toFloat(), with(density) { radius.toPx() }, density)
    }
    val cardWidthPx = with(density) { CONFIRM_POPOVER_WIDTH.toPx() }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            modifier = Modifier
                .padding(shadowRoom)
                .width(CONFIRM_POPOVER_WIDTH)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                    // Прозрачность — на каждую отрисовку, без промежуточного слоя: иначе тень за краями
                    // карточки обрезается прямоугольником, пока она полупрозрачна
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                    // Тень гаснет вместе с карточкой: полной тени сквозь полупрозрачную карточку она серая
                    shadowElevation = ThingsElevation.CARD.toPx() * alpha.value
                    this.shape = shape
                    // Растёт из вершины хвостика и сжимается к ней
                    transformOrigin = TransformOrigin((arrowCenterX / cardWidthPx).coerceIn(0f, 1f), 0f)
                }
                .background(ThingsTheme.colors.searchCard, shape)
                .padding(top = CONFIRM_POPOVER_ARROW_HEIGHT)
        ) {
            // Текст в эталоне отступает от края карточки почти вдвое дальше капсулы
            Text(
                text = message,
                color = ThingsTheme.colors.textPrimary,
                style = ThingsTheme.type.dialogBody,
                modifier = Modifier.padding(
                    start = ThingsSpacing.XXL,
                    end = ThingsSpacing.XXL,
                    top = ThingsSpacing.L_PLUS
                )
            )
            Spacer(modifier = Modifier.height(ThingsSpacing.L_PLUS))
            Box(
                modifier = Modifier
                    .padding(start = ThingsSpacing.L, end = ThingsSpacing.L, bottom = ThingsSpacing.L)
                    .fillMaxWidth()
                    .clip(ThingsTheme.shapes.capsuleShape)
                    .background(ThingsTheme.colors.searchField)
                    .clickable(enabled = !confirming) { confirming = true }
                    .padding(vertical = ThingsSpacing.M),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = confirmText,
                    color = ThingsTheme.colors.danger,
                    style = ThingsTheme.type.dialogBody
                )
            }
        }
    }
}

/**
 * Карточка со скруглёнными углами и хвостиком сверху, вершиной над [arrowCenterX]. Стороны хвостика — плавные
 * кривые: у основания они сливаются с краем карточки, вершина скруглена, как у всплывающих карточек iOS.
 */
private class PopoverShape(
    private val arrowCenterX: Float,
    private val radius: Float,
    private val density: Density
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val arrowW = with(this.density) { CONFIRM_POPOVER_ARROW_WIDTH.toPx() }
        val arrowH = with(this.density) { CONFIRM_POPOVER_ARROW_HEIGHT.toPx() }
        val half = arrowW / 2
        // Хвостик не заходит на скругление угла
        val c = arrowCenterX.coerceIn(radius + half, (size.width - radius - half).coerceAtLeast(radius + half))
        // Один контур по краю карточки вместе с хвостиком. Хвостик отдельным контуром поверх карточки давал
        // тёмную полоску у основания: где контуры перекрываются, заливка выпадала и просвечивала тень
        val top = arrowH
        val d = radius * 2
        val path = Path().apply {
            moveTo(radius, top)
            lineTo(c - half, top)
            cubicTo(c - half * 0.5f, top, c - half * 0.25f, 0f, c, 0f)
            cubicTo(c + half * 0.25f, 0f, c + half * 0.5f, top, c + half, top)
            lineTo(size.width - radius, top)
            arcTo(Rect(size.width - d, top, size.width, top + d), 270f, 90f, false)
            lineTo(size.width, size.height - radius)
            arcTo(Rect(size.width - d, size.height - d, size.width, size.height), 0f, 90f, false)
            lineTo(radius, size.height)
            arcTo(Rect(0f, size.height - d, d, size.height), 90f, 90f, false)
            lineTo(0f, top + radius)
            arcTo(Rect(0f, top, d, top + d), 180f, 90f, false)
            close()
        }
        return Outline.Generic(path)
    }
}
