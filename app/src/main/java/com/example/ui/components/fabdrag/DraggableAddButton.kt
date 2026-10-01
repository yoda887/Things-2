package com.example.ui.components.fabdrag

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

private val ActionButtonColor = Color(0xFF3A3B40)

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
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    // Место кнопки без её сдвига за пальцем — от его центра считается сдвиг
    var homeCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // Кнопка со сдвигом — в этих координатах приходят касания
    var layerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnDropToInbox by rememberUpdatedState(onDropToInbox)
    val currentCanDrag by rememberUpdatedState(canDrag)
    val lift by animateFloatAsState(if (controller.isDragging) 1f else 0f, label = "fabLift")

    Box(
        modifier = modifier
            .zIndex(10f)
            .onGloballyPositioned { homeCoordinates = it }
            .graphicsLayer {
                val coords = homeCoordinates
                if (controller.isDragging && coords != null && controller.pointer != Offset.Unspecified) {
                    val center = coords.boundsInRoot().center
                    translationX = controller.pointer.x - center.x
                    translationY = controller.pointer.y - center.y
                }
                val scale = 1f + 0.12f * lift
                scaleX = scale
                scaleY = scale
            }
            .onGloballyPositioned { layerCoordinates = it }
            .shadow(6.dp + 6.dp * lift, CircleShape)
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
                        val coords = layerCoordinates ?: return@detectDragGesturesAfterLongPress
                        if (!currentCanDrag()) return@detectDragGesturesAfterLongPress
                        dragging = true
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        controller.start(coords.localToRoot(offset))
                    },
                    onDrag = { change, _ ->
                        if (!dragging) return@detectDragGesturesAfterLongPress
                        change.consume()
                        val coords = layerCoordinates ?: return@detectDragGesturesAfterLongPress
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
        Icon(Icons.Default.Add, contentDescription = "Create Task", tint = Color.White, modifier = Modifier.size(28.dp))
    }
}

/** Кнопки отмены и «во Входящие», которые показываются, пока кнопку «+» тянут. */
@Composable
fun BoxScope.FabDragActions(controller: FabDragController, bottomPadding: androidx.compose.ui.unit.Dp) {
    FabActionButton(
        visible = controller.isDragging,
        icon = { Icon(Icons.Default.MoveToInbox, contentDescription = "Move to Inbox", tint = Color.White, modifier = Modifier.size(22.dp)) },
        onBounds = { controller.inboxBounds = it },
        modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = bottomPadding)
    )
    FabActionButton(
        visible = controller.isDragging,
        icon = { Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White, modifier = Modifier.size(20.dp)) },
        onBounds = { controller.cancelBounds = it },
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 26.dp, bottom = bottomPadding)
    )
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
                .background(ActionButtonColor, CircleShape),
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
