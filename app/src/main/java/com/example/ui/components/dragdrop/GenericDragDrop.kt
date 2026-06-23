package com.example.ui.components.dragdrop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// ─── Constants ───────────────────────────────────────────────────────────────
private const val SCROLL_FRAME_MS = 16L
private const val SCROLL_ZONE_FRACTION = 0.15f
private const val SCROLL_MAX_PX = 20f
private const val DRAG_SCROLL_ACCELERATION_LIMIT_MS = 2_000f

/**
 * Универсальное состояние для реализации жеста Drag-and-Drop в списках LazyColumn.
 * Инкапсулирует состояние взаимодействия и предоставляет плавное управление смещением.
 */
class GenericDragDropState(
    val lazyListState: LazyListState
) {
    /** Ключ элемента, который сейчас перетаскивается. Может быть любого типа (String, Int, Long) */
    var draggedItemKey by mutableStateOf<Any?>(null)
    
    /** Флаг указывает, продолжает ли пользователь удерживать палец на экране во время перетаскивания */
    var isInteracting by mutableStateOf(false)
    
    /** Накопленный оффсет смещения с использованием Animatable для плавного перемещения и возврата */
    val dragAccumulatedOffset = Animatable(0f)
    
    /** Временная метка начала авто-прокрутки для расчёта ускорения */
    var dragScrollStartMs by mutableStateOf(Long.MIN_VALUE)

    /**
     * Позволяет скорректировать смещение накопленного драга внешними силами
     * (например, при смене структуры/даты в процессе перетаскивания).
     */
    suspend fun adjustOffset(amount: Float) {
        dragAccumulatedOffset.snapTo(dragAccumulatedOffset.value + amount)
    }

    /**
     * Позволяет установить точное начальное смещение (например, сбросить в 0f).
     */
    suspend fun snapOffsetTo(amount: Float) {
        dragAccumulatedOffset.snapTo(amount)
    }

    /**
     * Плавный возврат элемента на своё место с помощью анимации.
     */
    suspend fun animateOffsetToZero() {
        dragAccumulatedOffset.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 200)
        )
    }
}

/**
 * Хелпер-расширение для приведения draggedItemKey к типу String? 
 * Это позволяет внешнему коду (например, списку задач) лаконично работать со строковыми ID задач.
 */
val GenericDragDropState.draggedTaskId: String?
    get() = draggedItemKey as? String

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

private const val MOVE_THRESHOLD = 0.5f

/**
 * Универсальный модификатор для реализации Drag-and-Drop, абстрагированный от бизнес-логики.
 * Отвечает ТОЛЬКО за жесты, математику пересечений и компенсацию визуального смещения (прыжков).
 * 
 * @param state Общее состояние перетаскивания списка [GenericDragDropState]
 * @param key Уникальный ключ текущего перетаскиваемого элемента
 * @param onMoveIfNecessary Функция обратного вызова, принимающая ключ тащимого элемента и ключ цели,
 *                          на которую он наехал более чем на 50%. Возвращает true, если бизнес-логика
 *                          разрешила перемещение и перестроила список.
 * @param onDragEnd Функция обратного вызова при успешном завершении жеста.
 */
fun Modifier.universalDragAndDrop(
    state: GenericDragDropState,
    key: Any,
    onMoveIfNecessary: (draggedKey: Any, targetKey: Any) -> Boolean,
    onDragEnd: () -> Unit
): Modifier = composed {
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = state.lazyListState

    val currentOnMoveIfNecessary by rememberUpdatedState(onMoveIfNecessary)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    /**
     * Поиск элемента списка, с которым необходимо произвести обмен на основании геометрии.
     * Реализует строгое пересечение по центру для заголовков и 50% наложение для других элементов.
     */
    fun checkSwap(
        draggedKey: Any,
        visibleItems: List<LazyListItemInfo>,
        currentOffset: Float
    ): LazyListItemInfo? {
        val draggedItem = visibleItems.firstOrNull { it.key == draggedKey } ?: return null
        
        val dragTop = draggedItem.offset + currentOffset
        val dragBottom = dragTop + draggedItem.size
        

        return visibleItems.firstOrNull { target ->
            if (target.key == draggedKey) return@firstOrNull false

            












            
                val overlapTop = maxOf(dragTop, target.offset.toFloat())
                val overlapBottom = minOf(dragBottom, (target.offset + target.size).toFloat())
                val overlapAmount = overlapBottom - overlapTop
                
                
                overlapAmount > (target.size * MOVE_THRESHOLD) // Универсальное правило 50%
        }
    }

    /**
     * Инициализирует проверку пересечения и последующий вызов бизнес-логики.


     */
    fun performIntersectionCheck() {
        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        val targetItem = checkSwap(key, visibleItems, state.dragAccumulatedOffset.value)

        if (targetItem != null) {
             val draggedItem = visibleItems.firstOrNull { it.key == key } ?: return
            
            val spacing = detectItemSpacing(visibleItems)
            val distanceToShift = if (targetItem.index > draggedItem.index) {
                -(targetItem.size + spacing)
            } else {
                (targetItem.size + spacing)
            }

            // Запрашиваем бизнес-логику внешнего уровня
            val swapAccepted = currentOnMoveIfNecessary(key, targetItem.key)

            // Если список перестроился, компенсируем прыжок
            if (swapAccepted) {
                coroutineScope.launch {
                    state.adjustOffset(distanceToShift)
                    
                }
            }
        }
    }

    this.reorderableItem(
        state = state,
        key = key,
        onDragStart = {
            // Резерв для обработки старта перетаскивания
        },
        onDrag = { _ ->
            performIntersectionCheck()
        },
        onDragged = {
            performIntersectionCheck()
        },
        onDragEnd = { runWithAnimation ->
            currentOnDragEnd()
            runWithAnimation()
        },
        onDragCancel = { runWithAnimation ->
            runWithAnimation()
        }
    )
}

/**
 * Общий модификатор для перетаскивания элементов списка.
 * Управляет жестами долгого нажатия, физического драга по оси Y и умного авто-скролла.
 */
fun Modifier.reorderableItem(
    state: GenericDragDropState,
    key: Any,
    onDragStart: () -> Unit = {},
    onDrag: (dragAmount: Float) -> Unit = {},
    onDragged: () -> Unit = {},
    onDragEnd: (afterAnimation: () -> Unit) -> Unit = {},
    onDragCancel: (afterAnimation: () -> Unit) -> Unit = {},
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragged by rememberUpdatedState(onDragged)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)

    // ── Auto-scroll ───────────────────────────────────────────────────────────
    val isDraggedKey = state.draggedItemKey
    LaunchedEffect(isDraggedKey) {
        while (isActive && isDraggedKey == key && state.isInteracting) {
            val draggedItemInfo = state.lazyListState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.key == key }

            if (draggedItemInfo != null) {
                val dragCenterY =
                    draggedItemInfo.offset + draggedItemInfo.size / 2f + state.dragAccumulatedOffset.value
                val viewportHeight = state.lazyListState.layoutInfo.viewportSize.height.toFloat()
                val margin = viewportHeight * SCROLL_ZONE_FRACTION

                val scrollAmount = when {
                    dragCenterY < margin -> {
                        val ratio = (1f - dragCenterY / margin).coerceIn(0f, 1f)
                        val interpolatedRatio = outOfBoundsScrollCapInterpolator(ratio)
                        -interpolatedRatio * SCROLL_MAX_PX
                    }
                    dragCenterY > viewportHeight - margin -> {
                        val ratio = ((dragCenterY - (viewportHeight - margin)) / margin).coerceIn(0f, 1f)
                        val interpolatedRatio = outOfBoundsScrollCapInterpolator(ratio)
                        interpolatedRatio * SCROLL_MAX_PX
                    }
                    else -> 0f
                }

                if (scrollAmount != 0f) {
                    val now = System.currentTimeMillis()
                    if (state.dragScrollStartMs == Long.MIN_VALUE) {
                        state.dragScrollStartMs = now
                    }
                    val elapsed = (now - state.dragScrollStartMs)
                        .coerceAtMost(DRAG_SCROLL_ACCELERATION_LIMIT_MS.toLong())
                    val timeRatio = elapsed / DRAG_SCROLL_ACCELERATION_LIMIT_MS
                    val interpolated = timeRatio * timeRatio * timeRatio * timeRatio * timeRatio
                    val finalScroll = scrollAmount * interpolated.coerceAtLeast(0.05f)
                    
                    val consumed = state.lazyListState.scrollBy(finalScroll)
                    state.adjustOffset(consumed)
                    currentOnDragged()
                } else {
                    state.dragScrollStartMs = Long.MIN_VALUE
                }
            }
            delay(SCROLL_FRAME_MS)
        }
    }

    // ── Pointer Input ─────────────────────────────────────────────────────────
    pointerInput(key) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                state.draggedItemKey = key
                state.isInteracting = true
                coroutineScope.launch {
                    state.snapOffsetTo(0f)
                }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                currentOnDragStart()
            },
            onDrag = { change, dragAmount ->
                change.consume()
                coroutineScope.launch {
                    state.adjustOffset(dragAmount.y)
                    currentOnDrag(dragAmount.y)
                }
            },
            onDragEnd = {
                state.isInteracting = false
                currentOnDragEnd {
                    coroutineScope.launch {
                        try {
                            state.animateOffsetToZero()
                        } finally {
                            state.draggedItemKey = null
                            state.dragScrollStartMs = Long.MIN_VALUE
                        }
                    }
                }
            },
            onDragCancel = {
                state.isInteracting = false
                currentOnDragCancel {
                    coroutineScope.launch {
                        try {
                            state.animateOffsetToZero()
                        } finally {
                            state.draggedItemKey = null
                            state.dragScrollStartMs = Long.MIN_VALUE
                        }
                    }
                }
            }
        )
    }
}
