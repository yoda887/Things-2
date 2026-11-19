package com.example.ui.screens.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlin.math.roundToInt
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ThingsAlpha
import com.example.ui.theme.ThingsIconSize
import com.example.ui.theme.ThingsMotion
import com.example.ui.theme.ThingsSpacing
import com.example.ui.theme.ThingsStroke
import com.example.ui.theme.ThingsTheme

/**
 * Меню кнопки «+»: сама кнопка превращается в карточку. Круг растёт из места кнопки в карточку меню, по пути
 * синий цвет переходит в цвет карточки, «+» гаснет, а пункты проявляются внутри растущей формы. Закрытие касанием
 * мимо или «Назад» — обратно в кнопку; выбор пункта, после которого кнопки нет (окно новой задачи), — угасанием.
 *
 * @param fabCenter центр кнопки «+» в покое, в координатах корня
 * @param closeIntoButton закрытие сворачивает карточку обратно в кнопку (иначе — угасает на месте)
 * @param onOnScreenChange меню на экране — настоящую кнопку надо спрятать: её место занимает форма меню
 */
@Composable
fun FabMenuOverlay(
    visible: Boolean,
    fabCenter: () -> Offset,
    fabSize: Dp,
    closeIntoButton: Boolean,
    onDismiss: () -> Unit,
    onOnScreenChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    // 0 — кнопка, 1 — карточка; пружина на открытии чуть перелетает 1 — карточка слегка «дышит» в конце
    val progress = remember { Animatable(0f) }
    // Угасание всего меню — при выборе пункта, после которого кнопки на экране нет
    val fade = remember { Animatable(1f) }
    var onScreen by remember { mutableStateOf(false) }
    val currentCloseIntoButton by rememberUpdatedState(closeIntoButton)
    val currentOnOnScreenChange by rememberUpdatedState(onOnScreenChange)

    LaunchedEffect(visible) {
        if (visible) {
            fade.snapTo(1f)
            onScreen = true
            currentOnOnScreenChange(true)
            progress.animateTo(1f, spring(dampingRatio = FAB_MENU_OPEN_DAMPING, stiffness = FAB_MENU_OPEN_STIFFNESS))
        } else if (onScreen) {
            if (currentCloseIntoButton) {
                progress.animateTo(0f, tween(ThingsMotion.STANDARD, easing = FastOutSlowInEasing))
            } else {
                fade.animateTo(0f, tween(ThingsMotion.QUICK))
                progress.snapTo(0f)
            }
            onScreen = false
            currentOnOnScreenChange(false)
        }
    }
    if (!onScreen) return

    val density = androidx.compose.ui.platform.LocalDensity.current
    val accent = ThingsTheme.colors.accent
    val surface = ThingsTheme.colors.overlaySurface
    val borderColor = ThingsTheme.colors.overlayContent.copy(alpha = ThingsAlpha.FAINT)
    val scrimColor = ThingsTheme.colors.scrim
    val plusPainter = rememberVectorPainter(Icons.Default.Add)
    val plusFilter = ColorFilter.tint(ThingsTheme.colors.onAccent)
    val fabRadiusPx = with(density) { fabSize.toPx() } / 2f
    val menuRadiusPx = with(density) { ThingsTheme.shapes.menu.toPx() }
    val plusSizePx = with(density) { FAB_MENU_PLUS_SIZE.toPx() }
    val strokePx = with(density) { ThingsStroke.THIN.toPx() }
    // Начало координат меню и место карточки в нём — известны после первой раскладки
    var origin by remember { mutableStateOf(Offset.Zero) }
    var cardRect by remember { mutableStateOf(Rect.Zero) }
    val fabShadowPx = with(density) { FAB_MENU_FAB_SHADOW.toPx() }
    // Форма на доле превращения p: прямоугольник от кнопки к карточке и радиус скругления
    fun morphRect(p: Float): Rect {
        val fabRect = Rect(fabCenter() - origin, fabRadiusPx)
        return Rect(
            left = lerpF(fabRect.left, cardRect.left, p),
            top = lerpF(fabRect.top, cardRect.top, p),
            right = lerpF(fabRect.right, cardRect.right, p),
            bottom = lerpF(fabRect.bottom, cardRect.bottom, p)
        )
    }
    // Первую треть пути форма остаётся круглой (капсулой), потом скругление плавно уходит к углам карточки
    fun morphRadius(rect: Rect, p: Float): Float {
        val round = minOf(rect.width, rect.height) / 2f
        val t = ((p - FAB_MENU_ROUND_HOLD) / (1f - FAB_MENU_ROUND_HOLD)).coerceIn(0f, 1f)
        return lerpF(round, menuRadiusPx, FastOutSlowInEasing.transform(t)).coerceIn(0f, round)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() }
            .graphicsLayer { alpha = fade.value }
    ) {
        // Затемнение — вместе с ростом карточки; касание мимо закрывает меню
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value.coerceIn(0f, 1f) }
                .background(scrimColor.copy(alpha = ThingsAlpha.SUBTLE))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() }
        )
        // Тень кнопки не пропадает разом, а гаснет в начале превращения: отдельный слой по форме меню
        Box(
            modifier = Modifier.layout { measurable, constraints ->
                val p = progress.value
                val rect = if (cardRect == Rect.Zero) Rect.Zero else morphRect(p)
                val placeable = measurable.measure(
                    Constraints.fixed(rect.width.roundToInt().coerceAtLeast(0), rect.height.roundToInt().coerceAtLeast(0))
                )
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.placeWithLayer(rect.left.roundToInt(), rect.top.roundToInt()) {
                        val shadowT = (1f - p / FAB_MENU_SHADOW_FADE_END).coerceIn(0f, 1f)
                        shadowElevation = fabShadowPx * shadowT
                        shape = RoundedCornerShape(morphRadius(rect, p))
                    }
                }
            }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    if (cardRect == Rect.Zero) return@drawWithContent
                    val p = progress.value
                    val rect = morphRect(p)
                    val radius = morphRadius(rect, p)
                    val corner = CornerRadius(radius)
                    val colorT = (p / FAB_MENU_COLOR_END).coerceIn(0f, 1f)
                    drawRoundRect(lerp(accent, surface, colorT), rect.topLeft, rect.size, corner)
                    drawRoundRect(
                        borderColor.copy(alpha = borderColor.alpha * colorT), rect.topLeft, rect.size, corner,
                        style = Stroke(strokePx)
                    )
                    // Пункты — только внутри растущей формы
                    val clip = Path().apply { addRoundRect(RoundRect(rect, corner)) }
                    clipPath(clip) { this@drawWithContent.drawContent() }
                    // «+» кнопки гаснет в начале превращения, оставаясь в центре формы
                    val plusAlpha = (1f - p / FAB_MENU_PLUS_FADE_END).coerceIn(0f, 1f)
                    if (plusAlpha > 0f) {
                        translate(rect.center.x - plusSizePx / 2f, rect.center.y - plusSizePx / 2f) {
                            with(plusPainter) { draw(Size(plusSizePx, plusSizePx), alpha = plusAlpha, colorFilter = plusFilter) }
                        }
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = ThingsSpacing.XXL, start = ThingsSpacing.L, end = ThingsSpacing.L)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .onGloballyPositioned {
                        val pos = it.positionInRoot() - origin
                        cardRect = Rect(pos, Size(it.size.width.toFloat(), it.size.height.toFloat()))
                    }
                    .graphicsLayer {
                        // Пункты проявляются во второй части превращения, когда форма уже похожа на карточку
                        alpha = ((progress.value - FAB_MENU_CONTENT_START) / (1f - FAB_MENU_CONTENT_START)).coerceIn(0f, 1f)
                    }
                    .padding(ThingsSpacing.S),
                content = content
            )
        }
        // Пока меню уходит, пункты не нажимаются: второй выбор создал бы ещё один проект или область
        if (!visible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    }
            )
        }
    }
}

/** Пункт меню «+»: значок, полужирное название и пояснение; нажатие — скруглённая подсветка внутри карточки */
@Composable
fun FabMenuItem(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    // Нажатый пункт, как в Things, — светлая скруглённая плашка (волна на тёмной карточке почти не видна)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Выбранный пункт остаётся подсвеченным, пока меню уходит
    var chosen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ThingsTheme.shapes.chipShape)
            .background(if (pressed || chosen) ThingsTheme.colors.overlayControl else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null) {
                chosen = true
                onClick()
            }
            .padding(horizontal = ThingsSpacing.S, vertical = ThingsSpacing.S_PLUS),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(ThingsIconSize.XL)
                .padding(top = ThingsSpacing.XXS),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(modifier = Modifier.width(ThingsSpacing.M))
        Column {
            Text(
                text = title,
                style = ThingsTheme.type.dialogRowSelected.copy(color = ThingsTheme.colors.overlayContent)
            )
            Spacer(modifier = Modifier.height(ThingsSpacing.XXS))
            Text(
                text = description,
                style = ThingsTheme.type.subhead.copy(color = ThingsTheme.colors.overlayContentSecondary)
            )
        }
    }
}

private fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t

/** Пружина открытия: почти без отскока, чтобы карточка не «прыгала», но и не тянулась */
private const val FAB_MENU_OPEN_DAMPING = 0.82f
private const val FAB_MENU_OPEN_STIFFNESS = 320f
/** К этой доле превращения синий цвет кнопки полностью переходит в цвет карточки */
private const val FAB_MENU_COLOR_END = 0.45f
/** К этой доле гаснет «+» кнопки */
private const val FAB_MENU_PLUS_FADE_END = 0.25f
/** С этой доли проявляются пункты меню */
private const val FAB_MENU_CONTENT_START = 0.55f
/** До этой доли форма остаётся круглой, как кнопка */
private const val FAB_MENU_ROUND_HOLD = 0.33f
/** К этой доле гаснет тень кнопки */
private const val FAB_MENU_SHADOW_FADE_END = 0.3f
/** Тень кнопки в покое — как у DraggableAddButton */
private val FAB_MENU_FAB_SHADOW = 6.dp
/** «+» — того же размера, что на кнопке (DraggableAddButton) */
private val FAB_MENU_PLUS_SIZE = 28.dp
