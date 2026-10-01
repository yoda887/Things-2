package com.example.ui.components.fabdrag

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset

/**
 * Добавление перетаскиванием кнопки «+», как в Things: кнопку тянут в список, на месте пальца
 * в списке раскрывается промежуток, и при отпускании там создаётся задача (на экране проекта
 * у левого края — заголовок, на главном экране — проект).
 *
 * Кнопка живёт в Scaffold, список — внутри экрана, поэтому они связаны этим контроллером:
 * кнопка сообщает положение пальца в координатах корня, экран со списком регистрирует
 * [FabDropTarget], показывает промежуток и создаёт объект при отпускании.
 */
@Stable
class FabDragController {
    /** Кнопку сейчас тянут */
    var isDragging by mutableStateOf(false)
        private set

    /** Положение пальца в координатах корня; [Offset.Unspecified] — кнопку не тянут */
    var pointer by mutableStateOf(Offset.Unspecified)
        private set

    /** Палец над кнопкой отмены или «во Входящие» — список промежуток не показывает */
    var isOverAction by mutableStateOf(false)

    /** Экран, который сейчас принимает сброс; регистрируется, пока он на экране */
    var dropTarget: FabDropTarget? = null

    /** Кнопки отмены и «во Входящие» в координатах корня — пока они показаны */
    var cancelBounds: androidx.compose.ui.geometry.Rect? = null
    var inboxBounds: androidx.compose.ui.geometry.Rect? = null

    fun start(position: Offset) {
        pointer = position
        isDragging = true
    }

    fun move(position: Offset) {
        pointer = position
    }

    /**
     * Палец отпущен над списком.
     * @return true, если экран создал объект на месте пальца
     */
    fun dropOnList(): Boolean {
        val handled = !isOverAction && dropTarget?.onFabDrop() == true
        finish()
        return handled
    }

    fun finish() {
        isDragging = false
        isOverAction = false
        pointer = Offset.Unspecified
    }
}

/** Экран, на который можно сбросить кнопку «+» */
fun interface FabDropTarget {
    /** Создать объект на месте промежутка. @return false — места нет, сброс не принят */
    fun onFabDrop(): Boolean
}

val LocalFabDragController = staticCompositionLocalOf<FabDragController?> { null }
