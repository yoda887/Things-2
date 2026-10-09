package com.example.ui.components.fabdrag

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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

    /** Центр кнопки «+» в покое в координатах корня; кружок отмены встаёт ровно на него */
    var homeCenter by mutableStateOf(Offset.Zero)

    /** Где палец был относительно центра кнопки в момент захвата — кнопка держится за эту точку */
    var grabOffset by mutableStateOf(Offset.Zero)
        private set

    /** Кнопка летит в центр промежутка и уменьшается до нуля (после успешного сброса на список) */
    var isSettling by mutableStateOf(false)
        private set

    /** Откуда (центр кнопки в момент отпускания) и куда (центр промежутка) летит кнопка, в координатах корня */
    var settleFrom by mutableStateOf(Offset.Zero)
        private set
    var settleTo by mutableStateOf(Offset.Zero)
        private set

    /** Время приземления от 0 до 1 (линейное); положение и размер выводятся из него отдельно */
    var settleProgress by mutableFloatStateOf(0f)

    /**
     * Кнопка уже исчезла при приземлении. Пока слот Scaffold ещё убирает её (экран открывает редактор,
     * и кнопка уезжает вниз), она не должна вернуться в угол на эти кадры. Снимается, когда кнопка
     * уходит из композиции, и на всякий случай — через секунду.
     */
    var hiddenAfterSettle by mutableStateOf(false)

    /**
     * Высота строки раскрытой задачи в списке, px, — последняя замеренная. Новая задача после сброса
     * раскрывается сразу на всю высоту, и промежуток заранее раздвигается ровно на неё.
     */
    var expandedRowHeightPx by mutableFloatStateOf(DEFAULT_EXPANDED_ROW_HEIGHT_PX)

    /** Задача, только что созданная сбросом кнопки: её строка раскрывается из центра промежутка. */
    var freshTaskId by mutableStateOf<String?>(null)

    /** Задача, только что созданная тапом по кнопке: у неё сразу фокус в названии и клавиатура. */
    var focusTaskId by mutableStateOf<String?>(null)

    /** Растёт на каждое приземление — по нему анимация запускается заново */
    var settleToken by mutableIntStateOf(0)
        private set

    /**
     * Центр промежутка, на который кнопка приземлится. Экран выставляет его в [FabDropTarget.onFabDrop],
     * пока промежуток ещё на месте; null — приземляться некуда, кнопка просто возвращается в угол.
     */
    var dropAnchor: Offset? = null

    fun start(position: Offset, grab: Offset = Offset.Zero) {
        isSettling = false
        hiddenAfterSettle = false
        pointer = position
        grabOffset = grab
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
        dropAnchor = null
        val handled = !isOverAction && dropTarget?.onFabDrop() == true
        val anchor = dropAnchor
        dropAnchor = null
        if (handled && anchor != null && pointer != Offset.Unspecified) {
            // Кнопка не возвращается в угол, а влетает в место новой строки и исчезает
            settleFrom = pointer - grabOffset
            settleTo = anchor
            settleProgress = 0f
            settleToken++
            isSettling = true
            isDragging = false
            isOverAction = false
        } else {
            finish()
        }
        return handled
    }

    /** Приземление закончено: кнопка исчезла, подготовка к следующему жесту. */
    fun endSettle(hideUntilEditorCloses: Boolean) {
        hiddenAfterSettle = hideUntilEditorCloses
        isSettling = false
        pointer = Offset.Unspecified
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

/** Высота раскрытой строки до первого замера, px (по ней первый раз раздвигается промежуток) */
private const val DEFAULT_EXPANDED_ROW_HEIGHT_PX = 560f
