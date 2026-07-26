package com.example.ui.components.swipe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import com.example.ui.theme.AppIcons
import com.example.ui.theme.ThingsBlue
import com.example.ui.theme.ThingsSwipeWhenYellow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView

/**
 * Направление активного свайпа задачи.
 */
private enum class SwipeDirection {
    NONE, LEFT, RIGHT
}

/**
 * Контейнер с жестами свайпа и ограничителями для задач.
 *
 * Оборачивает содержимое (TaskItemRow) и добавляет:
 * - Плавный горизонтальный свайп с гиперболическим демпфированием
 * - Цветной фон с иконкой за сдвигаемой карточкой
 * - Плавный возврат (tween) без пружинного отскока
 * - Плавный старт движения без скачка в момент превышения порога
 *
 * @param modifier Модификатор для внешнего контейнера (перетаскивание taskDragAndDrop)
 * @param onSwipeLeft Callback при успешном свайпе влево (мультиселекция)
 * @param onSwipeRight Callback при успешном свайпе вправо (When/Календарь)
 * @param enabled Включён ли свайп (блокируется при inline editor, drag-and-drop, cal_ задачах)
 * @param content Содержимое контейнера
 */
@Composable
fun SwipeableTaskContainer(
    modifier: Modifier = Modifier,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    // Текущее смещение элемента по горизонтали (в пикселях)
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    
    val currentOnSwipeLeft by rememberUpdatedState(onSwipeLeft)
    val currentOnSwipeRight by rememberUpdatedState(onSwipeRight)

    // Пороги смещения в пикселях
    val density = LocalDensity.current
    val touchSlopPx = with(density) { TOUCH_SLOP_DP.dp.toPx() }
    val activationThresholdPx = with(density) { ACTIVATION_THRESHOLD_DP.dp.toPx() }
    val maxSwipeDistancePx = with(density) { MAX_SWIPE_DISTANCE_DP.dp.toPx() }

    // Зафиксированное направление свайпа
    var lockedDirection by remember { mutableStateOf(SwipeDirection.NONE) }

    /**
     * Плавный возврат карточки в исходное положение с адаптивной длительностью,
     * пропорциональной текущему смещению (чем меньше смещение — тем быстрее возврат).
     */
    val resetSwipe: () -> Unit = {
        val currentAbs = abs(offsetX.value)
        val duration = (currentAbs / maxSwipeDistancePx * RETURN_ANIMATION_MAX_MS)
            .toInt().coerceIn(RETURN_ANIMATION_MIN_MS, RETURN_ANIMATION_MAX_MS)
        scope.launch {
            offsetX.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing)
            )
            lockedDirection = SwipeDirection.NONE
        }
    }

    // Цвет и иконка фона в зависимости от зафиксированного направления
    val backgroundColor = when (lockedDirection) {
        SwipeDirection.LEFT -> ThingsBlue
        SwipeDirection.RIGHT -> ThingsSwipeWhenYellow
        SwipeDirection.NONE -> Color.Transparent
    }

    // Радиус скругления фона
    val cornerRadiusPx = with(density) { BACKGROUND_CORNER_RADIUS_DP.dp.toPx() }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // Слой 1: Цветной фон с иконкой (рисуется за сдвигаемой карточкой)
        if (lockedDirection != SwipeDirection.NONE) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        drawRoundRect(
                            color = backgroundColor,
                            topLeft = Offset.Zero,
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                        )
                    },
                contentAlignment = if (lockedDirection == SwipeDirection.LEFT) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                if (lockedDirection == SwipeDirection.RIGHT) {
                    // Свайп вправо → When/Календарь → AppIcons.Upcoming
                    Icon(
                        imageVector = AppIcons.Upcoming,
                        contentDescription = "When",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(start = ICON_PADDING_DP.dp)
                            .size(ICON_SIZE_DP.dp)
                            .graphicsLayer {
                                val progress = (abs(offsetX.value) / activationThresholdPx).coerceIn(0f, 1f)
                                alpha = progress
                                scaleX = 0.5f + 0.5f * progress
                                scaleY = 0.5f + 0.5f * progress
                            }
                    )
                } else if (lockedDirection == SwipeDirection.LEFT) {
                    // Свайп влево → Мультиселекция → AppIcons.Anytime
                    Icon(
                        imageVector = AppIcons.Anytime,
                        contentDescription = "Select",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(end = ICON_PADDING_DP.dp)
                            .size(ICON_SIZE_DP.dp)
                            .graphicsLayer {
                                val progress = (abs(offsetX.value) / activationThresholdPx).coerceIn(0f, 1f)
                                alpha = progress
                                scaleX = 0.5f + 0.5f * progress
                                scaleY = 0.5f + 0.5f * progress
                            }
                    )
                }
            }
        }

        // Слой 2: Основной контент (TaskItemRow) со смещением
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = offsetX.value
                }
                .then(
                    if (lockedDirection != SwipeDirection.NONE) {
                        Modifier.background(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(BACKGROUND_CORNER_RADIUS_DP.dp)
                        )
                    } else {
                        Modifier
                    }
                )
                .then(
                    if (enabled) {
                        Modifier.pointerInput(enabled) {
                            awaitPointerEventScope {
                                while (true) {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val pointerId = down.id
                                    var rawOffsetX = 0f
                                    var rawOffsetY = 0f
                                    var isHorizontalSwipe = false
                                    var isCancelled = false

                                    // Фаза 1: Проверка порога активации свайпа
                                    while (!isHorizontalSwipe && !isCancelled) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == pointerId }
                                        if (change == null || change.isConsumed) {
                                            isCancelled = true
                                            break
                                        }

                                        val dragAmount = change.positionChange()
                                        rawOffsetX += dragAmount.x
                                        rawOffsetY += dragAmount.y

                                        val absDx = abs(rawOffsetX)
                                        val absDy = abs(rawOffsetY)

                                        if (absDx > touchSlopPx || absDy > touchSlopPx) {
                                            if (absDx > absDy * 1.5f && absDx > touchSlopPx) {
                                                isHorizontalSwipe = true
                                                lockedDirection = if (rawOffsetX < 0f) SwipeDirection.LEFT else SwipeDirection.RIGHT
                                                change.consume()
                                            } else {
                                                isCancelled = true
                                            }
                                        }
                                    }

                                    // Фаза 2: Активный свайп с плавным началом от 0 (без резкого прыжка)
                                    if (isHorizontalSwipe) {
                                        var hasTriggeredHaptic = false
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == pointerId }

                                            if (change == null || !change.pressed) {
                                                val finalOffset = offsetX.value
                                                val shouldActivate = abs(finalOffset) >= activationThresholdPx
                                                val wasLeft = lockedDirection == SwipeDirection.LEFT
                                                val wasRight = lockedDirection == SwipeDirection.RIGHT

                                                resetSwipe()

                                                if (shouldActivate) {
                                                    if (wasLeft) currentOnSwipeLeft()
                                                    if (wasRight) currentOnSwipeRight()
                                                }
                                                break
                                            }

                                            if (change.isConsumed) {
                                                resetSwipe()
                                                break
                                            }

                                            val dragAmount = change.positionChange()
                                            change.consume()
                                            rawOffsetX += dragAmount.x

                                            // Вычитаем touchSlop, чтобы старт движения карточки был строго с 0px без резкого прыжка
                                            val directionSign = if (rawOffsetX >= 0f) 1f else -1f
                                            val adjustedRawOffset = (abs(rawOffsetX) - touchSlopPx).coerceAtLeast(0f) * directionSign

                                            val dampedOffset = applyRubberBandDamping(
                                                rawOffset = adjustedRawOffset,
                                                threshold = activationThresholdPx,
                                                maxDistance = maxSwipeDistancePx
                                            )
                                            scope.launch {
                                                offsetX.snapTo(dampedOffset)
                                            }

                                            // Тактильная обратная связь при пересечении порога активации
                                            val isPastThreshold = abs(dampedOffset) >= activationThresholdPx
                                            if (isPastThreshold != hasTriggeredHaptic) {
                                                hasTriggeredHaptic = isPastThreshold
                                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            content()
        }
    }
}

/**
 * Гиперболическое демпфирование свайпа (rubber band effect).
 */
private fun applyRubberBandDamping(
    rawOffset: Float,
    threshold: Float,
    maxDistance: Float
): Float {
    val absRaw = abs(rawOffset)
    val sign = if (rawOffset >= 0f) 1f else -1f

    if (absRaw <= threshold) {
        return rawOffset
    }

    val overDrag = absRaw - threshold
    val maxOverDrag = maxDistance - threshold
    val resistance = RUBBER_BAND_RESISTANCE

    val dampedOverDrag = overDrag * resistance / (1f + overDrag * resistance / maxOverDrag)

    return sign * (threshold + dampedOverDrag)
}

// ─── Константы ──────────────────────────────────────────────────────────────

/** Порог срабатывания свайпа (в dp) */
private const val TOUCH_SLOP_DP = 8f

/** Порог активации действия свайпа (в dp) */
private const val ACTIVATION_THRESHOLD_DP = 48f

/** Максимальное визуальное смещение элемента (в dp) */
private const val MAX_SWIPE_DISTANCE_DP = 72f

/** Коэффициент сопротивления rubber band (0..1) */
private const val RUBBER_BAND_RESISTANCE = 0.55f

/** Скругление фона за свайпнутым элементом */
private const val BACKGROUND_CORNER_RADIUS_DP = 8f

/** Размер иконки на фоне (в dp) */
private const val ICON_SIZE_DP = 22f

/** Отступ иконки от края (в dp) */
private const val ICON_PADDING_DP = 16f

/** Минимальная длительность анимации возврата (мс) */
private const val RETURN_ANIMATION_MIN_MS = 80

/** Максимальная длительность анимации возврата (мс) */
private const val RETURN_ANIMATION_MAX_MS = 200
