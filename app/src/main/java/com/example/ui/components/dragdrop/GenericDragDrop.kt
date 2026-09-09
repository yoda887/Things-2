package com.example.ui.components.dragdrop

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow

// ─── Constants ───────────────────────────────────────────────────────────────
private const val SCROLL_FRAME_MS = 16L
private const val SCROLL_TOP_ZONE_FRACTION = 0.25f
private const val SCROLL_BOTTOM_ZONE_FRACTION = 0.18f
private const val SCROLL_MIN_PX = 6f
private const val SCROLL_MAX_PX = 58f
private const val SCROLL_CURVE_POWER = 1.3f
private const val MOVE_THRESHOLD = 0.7f

/**
 * Обертка для предоставления актуального значения смещения через свойство .value.
 */
class DragOffsetHolder(private val getter: () -> Float) {
    val value: Float get() = getter()
}

/**
 * Универсальное состояние для реализации жеста Drag-and-Drop в списках LazyColumn.
 * Инкапсулирует состояние взаимодействия и предоставляет плавное управление смещением.
 */
class GenericDragDropState(
    val lazyListState: LazyListState
) {
    /** Ключ элемента, который сейчас перетаскивается. Может быть любого типа (String, Int, Long) */
    var draggedItemKey by mutableStateOf<Any?>(null)
    
    /** Список ключей элементов, свернутых в пачку при групповом перетаскивании (первый ключ - ведущий) */
    var batchDraggedKeys by mutableStateOf<List<Any>>(emptyList())

    /** Флаг указывает, перетаскивается ли группа из нескольких элементов */
    val isBatchDrag: Boolean get() = batchDraggedKeys.size > 1

    /** Флаг указывает, продолжает ли пользователь удерживать палец на экране во время перетаскивания */
    var isInteracting by mutableStateOf(false)

    /** Актуальные синхронные значения смещения драга без задержек мьютекса */
    var dragAccumulatedY by mutableFloatStateOf(0f)
    var dragAccumulatedX by mutableFloatStateOf(0f)

    /** Накопленный оффсет смещения для совместимости со сторонними читателями .value */
    val dragAccumulatedOffset = DragOffsetHolder { dragAccumulatedY }
    
    /** Накопленный оффсет смещения по горизонтали для свободного перемещения */
    val dragAccumulatedOffsetHorizontal = DragOffsetHolder { dragAccumulatedX }
    
    /** Временная метка начала авто-прокрутки для расчёта ускорения */
    var dragScrollStartMs by mutableStateOf(Long.MIN_VALUE)

    /**
     * Позволяет мгновенно и синхронно скорректировать смещение драга
     * (например, при программном изменении состава или порядка элементов списка в процессе перетаскивания).
     */
    fun adjustOffset(amount: Float) {
        dragAccumulatedY += amount
    }

    /**
     * Позволяет мгновенно и синхронно скорректировать смещение драга внешними силами по горизонтали.
     */
    fun adjustOffsetHorizontal(amount: Float) {
        dragAccumulatedX += amount
    }

    /**
     * Позволяет мгновенно установить точное начальное смещение (например, сбросить в 0f).
     */
    fun snapOffsetTo(amount: Float) {
        dragAccumulatedY = amount
        dragAccumulatedX = 0f
    }

    /**
     * Плавный возврат элемента на своё место с помощью анимации.
     */
    suspend fun animateOffsetToZero() {
        val startY = dragAccumulatedY
        val startX = dragAccumulatedX
        if (startY == 0f && startX == 0f) return

        val animY = Animatable(startY)
        val animX = Animatable(startX)
        coroutineScope {
            launch {
                animY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 200)
                ) {
                    dragAccumulatedY = this.value
                }
            }
            launch {
                animX.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 200)
                ) {
                    dragAccumulatedX = this.value
                }
            }
        }
    }
}


/**
 * Создает и запоминает состояние GenericDragDropState, привязанное к текущему LazyListState.
 */
@Composable
fun rememberGenericDragDropState(lazyListState: LazyListState): GenericDragDropState {
    return remember(lazyListState) { GenericDragDropState(lazyListState) }
}

/**
 * Замедляющийся пятистепенной (quintic) интерполятор из AOSP для плавной автопрокрутки у границ экрана.
 */
fun outOfBoundsScrollCapInterpolator(t: Float): Float {
    val time = t - 1f
    return time * time * time * time * time + 1f
}

/**
 * Вычисляет расстояние (spacing) между элементами списка на основе видимой информации о макете.
 * Метод вынесен в общий движок, чтобы оставаться доступным для всех модификаторов.
 */
fun detectItemSpacing(
    visibleItems: List<LazyListItemInfo>,
): Float {
    for (i in 0 until visibleItems.lastIndex) {
        val cur = visibleItems[i]
        val next = visibleItems[i + 1]
        if (next.index == cur.index + 1) {
            val gap = next.offset - (cur.offset + cur.size)
            if (gap >= 0) return gap.toFloat()
        }
    }
    return 0f
}

/**
 * Вычисляет расстояние (spacing) для целевого элемента списка с учётом его соседей.
 */
fun getItemSpacing(
    targetItem: LazyListItemInfo,
    visibleItems: List<LazyListItemInfo>,
): Float {
    val targetIdx = visibleItems.indexOfFirst { it.key == targetItem.key }
    if (targetIdx != -1) {
        if (targetIdx < visibleItems.lastIndex) {
            val next = visibleItems[targetIdx + 1]
            if (next.index == targetItem.index + 1) {
                val gap = next.offset - (targetItem.offset + targetItem.size)
                if (gap >= 0) return gap.toFloat()
            }
        }
        if (targetIdx > 0) {
            val prev = visibleItems[targetIdx - 1]
            if (prev.index == targetItem.index - 1) {
                val gap = targetItem.offset - (prev.offset + prev.size)
                if (gap >= 0) return gap.toFloat()
            }
        }
    }
    return detectItemSpacing(visibleItems)
}


/**
 * Универсальный модификатор для реализации Drag-and-Drop, абстрагированный от бизнес-логики.
 * Отвечает ТОЛЬКО за жесты, математику пересечений и компенсацию визуального смещения (прыжков).
 *
 * Обычная функция поверх [reorderableItem]: раньше здесь был Modifier.composed, из-за которого
 * Compose не мог переиспользовать модификатор и пересобирал его подкомпозицию для каждого
 * элемента списка на каждой рекомпозиции.
 *
 * @param state Общее состояние перетаскивания списка [GenericDragDropState]
 * @param key Уникальный ключ текущего перетаскиваемого элемента
 * @param canDropOver Предикат, определяющий возможность пересечения/свапа с целевым элементом по его ключу
 * @param thresholdFraction Порог смещения центра (от 0.1f до 0.9f) с направленным гистерезисом (Direction Lock).
 *                          По умолчанию 0.5f (симметричное пересечение середин слотов).
 * @param onMoveIfNecessary Функция обратного вызова, принимающая ключ тащимого элемента и ключ цели,
 *                          с которой произошло геометрическое пересечение. Возвращает true, если
 *                          внешний обработчик подтвердил перемещение и переупорядочил коллекцию.
 * @param onDragStarted Функция обратного вызова при начале жеста перетаскивания (после долгого нажатия).
 *                      Тактильный отклик воспроизводит сам движок, дублировать его не нужно.
 * @param onDragEnd Функция обратного вызова при успешном завершении жеста, до анимации возврата.
 * @param onDragSettled Функция обратного вызова после завершения анимации возврата, в том числе
 *                      при отмене жеста. Здесь удобно восстанавливать состояние, которое было
 *                      временно изменено на время перетаскивания.
 */
fun Modifier.universalDragAndDrop(
    state: GenericDragDropState,
    key: Any,
    canDropOver: (targetKey: Any) -> Boolean = { true },
    thresholdFraction: Float = MOVE_THRESHOLD,
    topScrollZoneFraction: Float = SCROLL_TOP_ZONE_FRACTION,
    bottomScrollZoneFraction: Float = SCROLL_BOTTOM_ZONE_FRACTION,
    onMoveIfNecessary: (draggedKey: Any, targetKey: Any) -> Boolean,
    onDragStarted: () -> Unit = {},
    onDragEnd: () -> Unit,
    onDragSettled: () -> Unit = {}
): Modifier {
    val lazyListState = state.lazyListState

    /**
     * Поиск элемента списка, с которым необходимо произвести обмен на основании геометрии и вектора движения (Direction Lock).
     * Исключает взаимный дребезг при любых порогах thresholdFraction (0.1f .. 0.9f).
     */
    fun checkSwap(
        draggedKey: Any,
        visibleItems: List<LazyListItemInfo>,
        currentOffset: Float,
        deltaY: Float
    ): LazyListItemInfo? {
        if (deltaY == 0f) return null

        // Исключаем вторичные элементы пачки (они свернуты в стопку под пальцем и не должны служить препятствиями)
        val activeVisibleItems = visibleItems.filter {
            it.key == draggedKey || !state.batchDraggedKeys.contains(it.key)
        }
        val draggedItem = activeVisibleItems.firstOrNull { it.key == draggedKey } ?: return null

        val dragTop = draggedItem.offset + currentOffset
        val dragBottom = dragTop + draggedItem.size

        // Фильтруем элементы согласно предикату canDropOver, исключая неподходящие цели для свапа
        val candidates = activeVisibleItems.filter {
            it.key != draggedKey && canDropOver(it.key)
        }

        val threshold = thresholdFraction.coerceIn(0.1f, 0.9f)

        if (deltaY > 0f) {
            // Движение строго ВНИЗ: ведущий край — нижний (dragBottom)
            val nextItem = candidates
                .filter { it.index > draggedItem.index }
                .minByOrNull { it.index }

            if (nextItem != null) {
                // Находим последний элемент целевого составного блока (включая связанные элементы !canDropOver)
                var lastTarget = nextItem
                val nextIdx = activeVisibleItems.indexOfFirst { it.key == nextItem.key }
                var isBlockFullyVisible = true
                if (nextIdx != -1) {
                    for (i in (nextIdx + 1)..activeVisibleItems.lastIndex) {
                        val item = activeVisibleItems[i]
                        if (!canDropOver(item.key)) {
                            lastTarget = item
                        } else {
                            break
                        }
                    }
                    // Если дочерние элементы блока упираются в нижний край видимых элементов списка
                    // и в общем списке ещё есть элементы, значит часть составного блока находится за пределами экрана
                    val totalItemsCount = lazyListState.layoutInfo.totalItemsCount
                    val isLastVisibleItem = lastTarget.index == activeVisibleItems.last().index
                    val hasMoreItemsInList = lastTarget.index < totalItemsCount - 1
                    if (isLastVisibleItem && hasMoreItemsInList && !canDropOver(lastTarget.key)) {
                        isBlockFullyVisible = false
                    }
                }

                if (isBlockFullyVisible) {
                    val downThreshold = lastTarget.offset + lastTarget.size * threshold
                    if (dragBottom > downThreshold) {
                        return nextItem
                    }
                }
            }
        } else if (deltaY < 0f) {
            // Движение строго ВВЕРХ: ведущий край — верхний (dragTop)
            val prevItem = candidates
                .filter { it.index < draggedItem.index }
                .maxByOrNull { it.index }

            if (prevItem != null) {
                val upThreshold = prevItem.offset + prevItem.size * (1f - threshold)
                if (dragTop < upThreshold) {
                    return prevItem
                }
            }
        }

        return null
    }

    /**
     * Выполняет геометрическую проверку пересечения элементов и обращение к внешнему обработчику перемещения.
     * Компенсирует скачок смещения перетаскиваемого элемента на величину шага целевого слота.
     *
     * @return true, если внешний обработчик подтвердил перемещение — движок воспроизведёт тактильный отклик.
     */
    fun performIntersectionCheck(deltaY: Float): Boolean {
        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        val targetItem = checkSwap(key, visibleItems, state.dragAccumulatedOffset.value, deltaY) ?: return false
        val draggedItem = visibleItems.firstOrNull { it.key == key } ?: return false

        val distanceToShift = if (targetItem.index > draggedItem.index) {
            // Движение вниз: находим последний непропускаемый элемент в целевом блоке
            var lastTarget = targetItem
            val targetIdx = visibleItems.indexOfFirst { it.key == targetItem.key }
            if (targetIdx != -1) {
                for (i in (targetIdx + 1)..visibleItems.lastIndex) {
                    val item = visibleItems[i]
                    if (!canDropOver(item.key)) {
                        lastTarget = item
                    } else {
                        break
                    }
                }
            }
            val targetBottom = lastTarget.offset + lastTarget.size
            (draggedItem.offset - (targetBottom - draggedItem.size)).toFloat()
        } else {
            // Движение вверх: целевой слот начинается на отступе targetItem
            (draggedItem.offset - targetItem.offset).toFloat()
        }

        // Запрашиваем подтверждение перемещения у внешнего обработчика
        val swapAccepted = onMoveIfNecessary(key, targetItem.key)

        // Если список перестроился, компенсируем прыжок с учётом полного расстояния
        if (swapAccepted) {
            state.adjustOffset(distanceToShift)
        }
        return swapAccepted
    }

    return this.reorderableItem(
        state = state,
        key = key,
        topScrollZoneFraction = topScrollZoneFraction,
        bottomScrollZoneFraction = bottomScrollZoneFraction,
        onDragStart = onDragStarted,
        onDrag = { dragDeltaY -> performIntersectionCheck(dragDeltaY) },
        onDragged = { scrollDeltaY -> performIntersectionCheck(scrollDeltaY) },
        onDragEnd = onDragEnd,
        onDragSettled = onDragSettled
    )
}

/**
 * Общий модификатор для перетаскивания элементов списка.
 * Управляет жестами долгого нажатия, физического драга по оси Y, умного авто-скролла
 * и тактильного отклика.
 *
 * Реализован на Modifier.Node вместо Modifier.composed. Это даёт две вещи:
 * модификатор переиспользуется между рекомпозициями вместо пересборки подкомпозиции на каждый
 * элемент списка, и корутина автопрокрутки заводится одна на жест — раньше LaunchedEffect
 * с ключом draggedItemKey перезапускался у каждого элемента списка при каждом старте драга.
 *
 * Обратите внимание: содержимое элементов (AnimatedTaskItem и подобные) по-прежнему читает
 * draggedItemKey в композиции, чтобы применить стили перетаскивания, поэтому старт и конец
 * жеста всё ещё один раз рекомпозируют видимые элементы.
 *
 * @param onDrag Вызывается на каждое движение пальца. Должен вернуть true, если перемещение
 *               элементов состоялось — движок воспроизведёт тактильный отклик.
 * @param onDragged То же самое для смещения, вызванного автопрокруткой.
 * @param onDragEnd Вызывается при завершении жеста, до анимации возврата. При отмене не вызывается.
 * @param onDragSettled Вызывается после анимации возврата, в том числе при отмене жеста.
 */
fun Modifier.reorderableItem(
    state: GenericDragDropState,
    key: Any,
    topScrollZoneFraction: Float = SCROLL_TOP_ZONE_FRACTION,
    bottomScrollZoneFraction: Float = SCROLL_BOTTOM_ZONE_FRACTION,
    onDragStart: () -> Unit = {},
    onDrag: (dragAmount: Float) -> Boolean = { false },
    onDragged: (scrollDelta: Float) -> Boolean = { false },
    onDragEnd: () -> Unit = {},
    onDragSettled: () -> Unit = {},
): Modifier = this then ReorderableItemElement(
    state = state,
    key = key,
    topScrollZoneFraction = topScrollZoneFraction,
    bottomScrollZoneFraction = bottomScrollZoneFraction,
    onDragStart = onDragStart,
    onDrag = onDrag,
    onDragged = onDragged,
    onDragEnd = onDragEnd,
    onDragSettled = onDragSettled
)

private data class ReorderableItemElement(
    val state: GenericDragDropState,
    val key: Any,
    val topScrollZoneFraction: Float,
    val bottomScrollZoneFraction: Float,
    val onDragStart: () -> Unit,
    val onDrag: (Float) -> Boolean,
    val onDragged: (Float) -> Boolean,
    val onDragEnd: () -> Unit,
    val onDragSettled: () -> Unit
) : ModifierNodeElement<ReorderableItemNode>() {

    override fun create() = ReorderableItemNode(
        state = state,
        key = key,
        topScrollZoneFraction = topScrollZoneFraction,
        bottomScrollZoneFraction = bottomScrollZoneFraction,
        onDragStart = onDragStart,
        onDrag = onDrag,
        onDragged = onDragged,
        onDragEnd = onDragEnd,
        onDragSettled = onDragSettled
    )

    override fun update(node: ReorderableItemNode) {
        node.update(
            state = state,
            key = key,
            topScrollZoneFraction = topScrollZoneFraction,
            bottomScrollZoneFraction = bottomScrollZoneFraction,
            onDragStart = onDragStart,
            onDrag = onDrag,
            onDragged = onDragged,
            onDragEnd = onDragEnd,
            onDragSettled = onDragSettled
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "reorderableItem"
        properties["key"] = key
    }
}

/**
 * Узел жеста перетаскивания: долгое нажатие, перемещение пальца, автопрокрутка у границ
 * списка и тактильный отклик.
 */
private class ReorderableItemNode(
    private var state: GenericDragDropState,
    private var key: Any,
    private var topScrollZoneFraction: Float,
    private var bottomScrollZoneFraction: Float,
    private var onDragStart: () -> Unit,
    private var onDrag: (Float) -> Boolean,
    private var onDragged: (Float) -> Boolean,
    private var onDragEnd: () -> Unit,
    private var onDragSettled: () -> Unit
) : DelegatingNode(), CompositionLocalConsumerModifierNode {

    /** Корутина автопрокрутки живёт ровно столько, сколько пользователь удерживает элемент */
    private var autoScrollJob: Job? = null

    private val pointerInputNode = delegate(
        SuspendingPointerInputModifierNode {
            detectDragGesturesAfterLongPress(
                onDragStart = { handleDragStart() },
                onDrag = { change, dragAmount -> handleDrag(change, dragAmount) },
                onDragEnd = { handleDragEnd() },
                onDragCancel = { handleDragCancel() }
            )
        }
    )

    fun update(
        state: GenericDragDropState,
        key: Any,
        topScrollZoneFraction: Float,
        bottomScrollZoneFraction: Float,
        onDragStart: () -> Unit,
        onDrag: (Float) -> Boolean,
        onDragged: (Float) -> Boolean,
        onDragEnd: () -> Unit,
        onDragSettled: () -> Unit
    ) {
        // Аналог pointerInput(key): при смене ключа или общего состояния жест начинается заново
        val gestureRestartNeeded = this.key != key || this.state !== state

        this.state = state
        this.key = key
        this.topScrollZoneFraction = topScrollZoneFraction
        this.bottomScrollZoneFraction = bottomScrollZoneFraction
        this.onDragStart = onDragStart
        this.onDrag = onDrag
        this.onDragged = onDragged
        this.onDragEnd = onDragEnd
        this.onDragSettled = onDragSettled

        if (gestureRestartNeeded) {
            pointerInputNode.resetPointerInputHandler()
        }
    }

    // ── Обработка жеста ───────────────────────────────────────────────────────

    private fun handleDragStart() {
        state.draggedItemKey = key
        state.isInteracting = true
        state.snapOffsetTo(0f)
        performHaptic(HapticFeedbackConstants.LONG_PRESS)
        onDragStart()
        startAutoScroll()
    }

    private fun handleDrag(change: PointerInputChange, dragAmount: Offset) {
        change.consume()
        state.adjustOffset(dragAmount.y)
        state.adjustOffsetHorizontal(dragAmount.x)
        if (onDrag(dragAmount.y)) {
            performHaptic(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    private fun handleDragEnd() {
        stopInteraction()
        onDragEnd()
        settle()
    }

    private fun handleDragCancel() {
        stopInteraction()
        settle()
    }

    private fun stopInteraction() {
        state.isInteracting = false
        autoScrollJob?.cancel()
        autoScrollJob = null
    }

    /** Плавно возвращает элемент на место и очищает общее состояние перетаскивания */
    private fun settle() {
        coroutineScope.launch {
            try {
                state.animateOffsetToZero()
            } finally {
                state.draggedItemKey = null
                state.batchDraggedKeys = emptyList()
                state.dragScrollStartMs = Long.MIN_VALUE
                onDragSettled()
            }
        }
    }

    // ── Авто-скролл ───────────────────────────────────────────────────────────

    private fun startAutoScroll() {
        autoScrollJob?.cancel()
        autoScrollJob = coroutineScope.launch {
            while (isActive && state.isInteracting && state.draggedItemKey == key) {
                autoScrollStep()
                delay(SCROLL_FRAME_MS)
            }
        }
    }

    /** Один кадр автопрокрутки, когда перетаскиваемый элемент подходит к границе viewport */
    private suspend fun autoScrollStep() {
        val layoutInfo = state.lazyListState.layoutInfo
        val draggedItemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return

        val viewportStart = layoutInfo.viewportStartOffset.toFloat()
        val viewportEnd = layoutInfo.viewportEndOffset.toFloat()
        val viewportHeight = viewportEnd - viewportStart
        if (viewportHeight <= 0f) return

        val scrollTopZone = (viewportHeight * topScrollZoneFraction).coerceAtLeast(1f)
        val scrollBottomZone = (viewportHeight * bottomScrollZoneFraction).coerceAtLeast(1f)
        val dragTop = draggedItemInfo.offset + state.dragAccumulatedOffset.value
        val dragBottom = dragTop + draggedItemInfo.size

        val scrollAmount = when {
            // Верхний край элемента приблизился к верхней границе с учётом TopBar
            dragTop < viewportStart + scrollTopZone -> {
                val distanceIntoZone = (viewportStart + scrollTopZone - dragTop).coerceIn(0f, scrollTopZone)
                -scrollSpeed(distanceIntoZone / scrollTopZone)
            }
            // Нижний край элемента приблизился к нижней границе viewport
            dragBottom > viewportEnd - scrollBottomZone -> {
                val distanceIntoZone = (dragBottom - (viewportEnd - scrollBottomZone)).coerceIn(0f, scrollBottomZone)
                scrollSpeed(distanceIntoZone / scrollBottomZone)
            }
            else -> 0f
        }
        if (scrollAmount == 0f) return

        val consumed = state.lazyListState.scrollBy(scrollAmount)
        if (consumed != 0f) {
            state.adjustOffset(consumed)
            if (onDragged(consumed)) {
                performHaptic(HapticFeedbackConstants.CLOCK_TICK)
            }
        }
    }

    /** Скорость автопрокрутки нарастает по мере погружения элемента в зону у края экрана */
    private fun scrollSpeed(ratio: Float): Float =
        SCROLL_MIN_PX + (SCROLL_MAX_PX - SCROLL_MIN_PX) * ratio.pow(SCROLL_CURVE_POWER)

    private fun performHaptic(feedbackConstant: Int) {
        currentValueOf(LocalView).performHapticFeedback(feedbackConstant)
    }
}
