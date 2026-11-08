package com.example.ui.screens.home.inlineeditor.utils

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Касания списка вне раскрытого редактора: по ним маркеры курсора прячутся заранее — когда палец
 * только коснулся экрана, а не когда редактор уже начал сворачиваться.
 *
 * Маркер курсора («капля») живёт в отдельном системном окне. Перед сворачиванием его нужно убрать
 * (иначе окно двигается системным вызовом на каждом кадре, см. HideTextSelectionHandles), но само
 * удаление окна на HyperOS — это 10–15 мс ожидания WindowManager на главном потоке. В кадре начала
 * сворачивания оно растягивало первый кадр анимации до 20+ мс. Между касанием и отпусканием пальца
 * проходит 50–100 мс неподвижного экрана — удаление там незаметно.
 *
 * Если касание не свернуло редактор (прокрутка, нажатие на фильтр), маркеры возвращаются, когда палец
 * отпущен. Касания внутри редактора размечает [markTouchesInsideEditor] — они маркеры не прячут.
 */
@Stable
class EditorOutsideTouch {
    private var lastInsideDownId: PointerId? = null
    private var collapseRequested = false

    /** Маркеры курсора спрятаны: палец коснулся списка вне редактора. */
    var hideHandles by mutableStateOf(false)
        private set

    /** Нажатие вне редактора сворачивает его — после отпускания пальца маркеры не возвращаются. */
    fun onCollapseRequested() {
        collapseRequested = true
    }

    /** Редактор раскрыт заново: прошлые касания к нему не относятся. */
    fun reset() {
        collapseRequested = false
        hideHandles = false
    }

    /** Границы раскрытого редактора и списка в координатах окна — чтобы понять, внутри ли касание. */
    internal var editorBounds: Rect? = null
    internal var listCoordinates: LayoutCoordinates? = null

    /** Точка [local] в координатах списка — внутри раскрытого редактора. */
    internal fun isInside(local: Offset): Boolean {
        val bounds = editorBounds ?: return false
        val list = listCoordinates?.takeIf { it.isAttached } ?: return false
        return bounds.contains(list.localToRoot(local))
    }

    internal fun onInsideDown(id: PointerId) {
        lastInsideDownId = id
        if (!collapseRequested) hideHandles = false
    }

    internal fun isInsideDown(id: PointerId): Boolean = id == lastInsideDownId

    internal fun onOutsidePress() {
        collapseRequested = false
        hideHandles = true
    }

    internal fun onOutsideRelease() {
        if (!collapseRequested) hideHandles = false
    }
}

/**
 * Для списка с раскрытым редактором: сообщает [touch] о касаниях вне редактора.
 *
 * Ставится в цепочке раньше обработчика нажатий списка: в Main-проходе события идут от вложенных
 * узлов к внешним, поэтому к отпусканию пальца нажатие на список или на строку уже вызвало
 * [EditorOutsideTouch.onCollapseRequested].
 */
fun Modifier.observeTouchesOutsideEditor(touch: EditorOutsideTouch, isEditorOpen: () -> Boolean): Modifier =
    pointerInput(touch) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
            if (!isEditorOpen() || touch.isInsideDown(down.id)) return@awaitEachGesture
            touch.onOutsidePress()
            try {
                do {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                } while (event.changes.any { it.pressed })
            } finally {
                touch.onOutsideRelease()
            }
        }
    }

/** Для раскрытого редактора: отмечает касания внутри него. Initial-проход доходит сюда раньше, чем Main-проход списка. */
fun Modifier.markTouchesInsideEditor(touch: EditorOutsideTouch): Modifier =
    onGloballyPositioned { touch.editorBounds = it.boundsInRoot() }
        .pointerInput(touch) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                touch.onInsideDown(down.id)
            }
        }

/**
 * Для списка с раскрытым редактором: нажатие вне карточки только сворачивает её ([onCollapse]) и больше
 * ничего — строки, плашки и переключатели под пальцем его не получают. Касание забирается в Initial-проходе,
 * раньше вложенных обработчиков. Если палец повёл дальше порога — это прокрутка: движение отдаётся списку.
 */
fun Modifier.collapseOnTapOutsideEditor(touch: EditorOutsideTouch, isEditorOpen: () -> Boolean, onCollapse: () -> Unit): Modifier =
    onGloballyPositioned { touch.listCoordinates = it }
        .pointerInput(touch) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (!isEditorOpen() || touch.isInside(down.position)) return@awaitEachGesture
                down.consume()
                val slop = viewConfiguration.touchSlop
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                    if (!change.pressed) {
                        change.consume()
                        touch.onCollapseRequested()
                        onCollapse()
                        return@awaitEachGesture
                    }
                    // Второй палец или сдвиг за порог — не нажатие, жест отдаётся списку (прокрутка)
                    if (event.changes.size > 1 || (change.position - down.position).getDistance() > slop) return@awaitEachGesture
                }
            }
        }
