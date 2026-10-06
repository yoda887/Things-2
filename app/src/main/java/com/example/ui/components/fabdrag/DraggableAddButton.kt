package com.example.ui.components.fabdrag

import com.example.ui.theme.ThingsTheme
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material3.Icon
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex


/** Насколько близко к кнопке действия нужно поднести палец, чтобы она сработала */
private const val ACTION_HIT_SLOP_FACTOR = 1.4f

/**
 * Кнопка «+»: нажатие — [onClick], долгое нажатие поднимает её, и её можно тащить в список
 * (см. [FabDragController]). Сброс над списком создаёт объект на месте пальца, над кнопкой
 * отмены — ничего, над кнопкой «во Входящие» — [onDropToInbox].
 *
 * @param canDrag экран сейчас принимает сброс (иначе долгое нажатие ничего не делает)
 */
@Composable
fun DraggableAddButton(
    controller: FabDragController,
    canDrag: () -> Boolean,
    onClick: () -> Unit,
    onDropToInbox: () -> Unit,
    containerColor: Color,
    // Открыт ли редактор задачи: пока он открыт, слот Scaffold убирает кнопку, и она не должна мелькнуть в углу
    isEditorOpen: () -> Boolean = { false },
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val currentIsEditorOpen by rememberUpdatedState(isEditorOpen)
    // Место кнопки без её сдвига за пальцем — от его центра считается сдвиг
    var homeCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // Кнопка со сдвигом — в этих координатах приходят касания
    var layerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnDropToInbox by rememberUpdatedState(onDropToInbox)
    val currentCanDrag by rememberUpdatedState(canDrag)
    // Кнопка «выпрыгивает» из покоя: пружина с отскоком — увеличение и подъём (тень) чуть перелетают цель
    val lift by animateFloatAsState(
        targetValue = if (controller.isDragging || controller.isSettling) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow),
        label = "fabLift"
    )

    // Приземление: кнопка летит в центр промежутка и уменьшается до нуля
    LaunchedEffect(controller.settleToken) {
        if (controller.settleToken == 0) return@LaunchedEffect
        animate(0f, 1f, animationSpec = tween(durationMillis = 180, easing = LinearEasing)) { value, _ ->
            controller.settleProgress = value
        }
        // Снимается, когда редактор закрывается (HomeScreen) или кнопка уходит из композиции
        controller.endSettle(hideUntilEditorCloses = currentIsEditorOpen())
    }

    // Кружок отмены лежит под кнопкой в том же слоте и по центру: пока кнопка стоит на месте, он закрыт ею,
    // а когда она уезжает за пальцем, открывается строго на её месте
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        FabCancelButton(controller)
        FabBody(
            controller = controller,
            homeCoordinates = { homeCoordinates },
            onHome = { homeCoordinates = it },
            onLayer = { layerCoordinates = it },
            layerCoordinates = { layerCoordinates },
            lift = { lift },
            containerColor = containerColor,
            currentOnClick = { currentOnClick() },
            currentOnDropToInbox = { currentOnDropToInbox() },
            currentCanDrag = { currentCanDrag() },
        )
    }
}

@Composable
private fun BoxScope.FabBody(
    controller: FabDragController,
    homeCoordinates: () -> LayoutCoordinates?,
    onHome: (LayoutCoordinates) -> Unit,
    onLayer: (LayoutCoordinates) -> Unit,
    layerCoordinates: () -> LayoutCoordinates?,
    lift: () -> Float,
    containerColor: Color,
    currentOnClick: () -> Unit,
    currentOnDropToInbox: () -> Unit,
    currentCanDrag: () -> Boolean,
) {
    val view = LocalView.current
    // Кнопка ушла из композиции (слот убрал её после сброса) — снова можно показывать
    DisposableEffect(Unit) {
        onDispose { controller.hiddenAfterSettle = false }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f)
            .onGloballyPositioned {
                onHome(it)
                // Центр кнопки в покое — от него считаются сдвиг за пальцем и положение кружка отмены
                controller.homeCenter = it.boundsInRoot().center
            }
            .graphicsLayer {
                val coords = homeCoordinates()
                val l = lift()
                var settleScale = 1f
                var shadowFactor = 1f
                if (controller.isSettling && coords != null) {
                    val t = controller.settleProgress
                    // Полёт — быстрый старт и мягкая посадка; уменьшение ровное по времени, чтобы
                    // кнопка успевала долететь заметной, а не исчезала в первые кадры
                    val p = FastOutSlowInEasing.transform((t * 1.4f).coerceAtMost(1f))
                    val center = coords.boundsInRoot().center
                    val target = controller.settleFrom + (controller.settleTo - controller.settleFrom) * p
                    translationX = target.x - center.x
                    translationY = target.y - center.y
                    settleScale = (1f - t).coerceAtLeast(0f)
                    // Как в Things: кнопка сначала «ложится» — тень уходит в первую треть приземления
                    shadowFactor = (1f - t / 0.35f).coerceIn(0f, 1f)
                    alpha = (1f - t * t * t).coerceIn(0f, 1f)
                } else if (controller.hiddenAfterSettle) {
                    alpha = 0f
                } else if (controller.isDragging && coords != null && controller.pointer != Offset.Unspecified) {
                    // Кнопка держится за ту точку, за которую её взяли, а не прыгает центром под палец
                    val center = coords.boundsInRoot().center
                    translationX = controller.pointer.x - controller.grabOffset.x - center.x
                    translationY = controller.pointer.y - controller.grabOffset.y - center.y
                }
                val scale = (1f + 0.22f * l) * settleScale
                scaleX = scale
                scaleY = scale
                // Подъём: тень растёт вместе с увеличением; на отскоке не уходит в минус
                shadowElevation = ((6f + 12f * l).coerceAtLeast(2f) * shadowFactor).dp.toPx()
                shape = CircleShape
                clip = false
            }
            .onGloballyPositioned { onLayer(it) }
            .background(containerColor, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    currentOnClick()
                })
            }
            .pointerInput(Unit) {
                var dragging = false
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val coords = layerCoordinates() ?: return@detectDragGesturesAfterLongPress
                        if (!currentCanDrag()) return@detectDragGesturesAfterLongPress
                        dragging = true
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        val finger = coords.localToRoot(offset)
                        // Где палец относительно центра кнопки в момент захвата
                        controller.start(finger, finger - controller.homeCenter)
                    },
                    onDrag = { change, _ ->
                        if (!dragging) return@detectDragGesturesAfterLongPress
                        change.consume()
                        val coords = layerCoordinates() ?: return@detectDragGesturesAfterLongPress
                        // Касание приходит в координатах сдвинутой кнопки; localToRoot учитывает её сдвиг
                        val fingerInRoot = coords.localToRoot(change.position)
                        controller.move(fingerInRoot)
                        controller.isOverAction = controller.actionAt(fingerInRoot) != null
                    },
                    onDragEnd = {
                        if (!dragging) return@detectDragGesturesAfterLongPress
                        dragging = false
                        when (controller.actionAt(controller.pointer)) {
                            FabAction.CANCEL -> controller.finish()
                            FabAction.INBOX -> { controller.finish(); currentOnDropToInbox() }
                            null -> controller.dropOnList()
                        }
                    },
                    onDragCancel = {
                        dragging = false
                        controller.finish()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Add, contentDescription = "Create Task", tint = ThingsTheme.colors.onAccent, modifier = Modifier.size(28.dp))
    }
}

/** Кружок отмены: центр совпадает с центром кнопки «+» (оба центрируются в одном слоте). */
@Composable
private fun BoxScope.FabCancelButton(controller: FabDragController) {
    FabActionButton(
        visible = controller.isDragging,
        icon = { Icon(Icons.Default.Close, contentDescription = "Cancel", tint = ThingsTheme.colors.overlayContent, modifier = Modifier.size(20.dp)) },
        onBounds = { controller.cancelBounds = it },
        modifier = Modifier.align(Alignment.Center)
    )
}

/**
 * Кнопка «во Входящие», которая показывается, пока кнопку «+» тянут: слева от экрана, по одной линии
 * с центром кнопки «+» и кружка отмены.
 */
@Composable
fun BoxScope.FabDragActions(controller: FabDragController) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val sizePx = with(density) { 44.dp.roundToPx() }
    val startPx = with(density) { 20.dp.roundToPx() }
    Box(modifier = Modifier.matchParentSize().onGloballyPositioned { origin = it.positionInRoot() }) {
        FabActionButton(
            visible = controller.isDragging,
            icon = { Icon(Icons.Default.MoveToInbox, contentDescription = "Move to Inbox", tint = ThingsTheme.colors.overlayContent, modifier = Modifier.size(22.dp)) },
            onBounds = { controller.inboxBounds = it },
            modifier = Modifier.offset {
                val center = controller.homeCenter
                androidx.compose.ui.unit.IntOffset(startPx, (center.y - origin.y - sizePx / 2f).toInt())
            }
        )
    }
}

@Composable
private fun FabActionButton(
    visible: Boolean,
    icon: @Composable () -> Unit,
    onBounds: (Rect?) -> Unit,
    modifier: Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .onGloballyPositioned { onBounds(it.boundsInRoot()) }
                .background(ThingsTheme.colors.overlaySurface, CircleShape),
            contentAlignment = Alignment.Center
        ) { icon() }
    }
}

internal enum class FabAction { CANCEL, INBOX }

/** Кнопка действия под пальцем (с запасом вокруг неё) */
internal fun FabDragController.actionAt(point: Offset): FabAction? {
    if (point == Offset.Unspecified) return null
    fun Rect?.hit(): Boolean {
        val r = this ?: return false
        val radius = r.width / 2f * ACTION_HIT_SLOP_FACTOR
        return (point - r.center).getDistance() <= radius
    }
    return when {
        cancelBounds.hit() -> FabAction.CANCEL
        inboxBounds.hit() -> FabAction.INBOX
        else -> null
    }
}
