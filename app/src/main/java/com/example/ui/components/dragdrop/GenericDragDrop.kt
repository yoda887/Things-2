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
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.PinnableContainer
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.pow

// ─── Constants ───────────────────────────────────────────────────────────────
private const val NANOS_PER_SECOND = 1_000_000_000f

/**
 * Потолок длительности кадра в расчёте автопрокрутки. Если кадр надолго провалился
 * (сборка мусора, тяжёлая перекомпоновка), список не должен телепортироваться.
 */
private const val MAX_SCROLL_FRAME_SECONDS = 0.05f

private const val SCROLL_TOP_ZONE_FRACTION = 0.25f
private const val SCROLL_BOTTOM_ZONE_FRACTION = 0.18f

// Скорость автопрокрутки в пикселях в секунду: 6 и 58 пикселей за кадр при 60 Гц
private const val SCROLL_MIN_PX_PER_SECOND = 360f
private const val SCROLL_MAX_PX_PER_SECOND = 3480f

private const val SCROLL_CURVE_POWER = 1.3f
private const val MOVE_THRESHOLD = 0.7f
private const val DRAG_AUTOSCROLL_SLOP_DP = 12f

/**
 * Универсальное состояние для реализации жеста Drag-and-Drop в списках LazyColumn.
 * Инкапсулирует состояние взаимодействия и предоставляет плавное управление смещением.
 */
class GenericDragDropState(
    val lazyListState: LazyListState
) {
    /** Ключ элемента, который сейчас перетаскивается. Может быть любого типа (String, Int, Long) */
    var draggedItemKey by mutableStateOf<Any?>(null)
    
    /**
     * Ключи элементов, свёрнутых в одну стопку под пальцем; первый — ведущий, его геометрию
     * движок и отслеживает. Остальные визуально отсутствуют в потоке списка.
     *
     * Движок не знает и не решает, из чего собрана стопка — это дело вызывающего слоя.
     * Для него это чисто геометрический факт: перечисленных элементов на своих местах нет,
     * поэтому они не могут быть ни целями обмена, ни частью составного блока.
     */
    var stackedDragKeys by mutableStateOf<List<Any>>(emptyList())

    /** Под пальцем больше одного элемента */
    val hasStackedItems: Boolean get() = stackedDragKeys.size > 1

    /** Флаг указывает, продолжает ли пользователь удерживать палец на экране во время перетаскивания */
    var isInteracting by mutableStateOf(false)

    /**
     * Накопленное смещение перетаскиваемого элемента относительно его места в списке.
     *
     * Это snapshot-состояние, поэтому читать его лучше прямо в лямбде `graphicsLayer`:
     * такое чтение обновляет отрисовку без рекомпозиции элемента.
     */
    var dragAccumulatedY by mutableFloatStateOf(0f)
    var dragAccumulatedX by mutableFloatStateOf(0f)

    /**
     * Экранное положение верхней грани перетаскиваемого элемента, каким оно обязано остаться
     * сразу после последней перестановки.
     *
     * Движок рассчитывает компенсацию прыжка геометрически, но реальное место вставки знает
     * только бизнес-логика: заголовки и составные блоки кладут элемент не туда, куда указывает
     * геометрия. Поэтому на следующем кадре предсказание сверяется с фактическим раскладом
     * и разница добирается — ошибка расчёта больше не уводит карточку из-под пальца.
     *
     * NaN — сверять нечего.
     */
    internal var expectedDragTopAfterMove = Float.NaN

    /**
     * Ключ последнего элемента, с которым был успешно подтверждён обмен
     * (защита от повторного свапа до рекомпозиции).
     *
     * Внутренняя бухгалтерия движка — обёрткам её трогать не нужно.
     */
    internal var lastSwappedTargetKey: Any? = null

    /** Направление последнего подтверждённого обмена (true — вниз, false — вверх) */
    internal var lastSwapMovingDown: Boolean? = null

    /**
     * Смещение, с которым перетаскиваемый элемент [key] нужно РИСОВАТЬ: как [dragAccumulatedY], но верхняя
     * грань элемента не поднимается выше начала контента списка.
     *
     * Верхний contentPadding списка обычно закрыт панелью инструментов, а над ней строка состояния.
     * Палец у самого верхнего края уводил карточку туда, и её не было видно, пока идёт автопрокрутка.
     * Здесь карточка упирается в границу контента и остаётся на виду. Сам жест по-прежнему считается
     * по [dragAccumulatedY] — положению под пальцем: глубина погружения в зону автопрокрутки, а с ней
     * и скорость у края, не меняются.
     *
     * Читать в лямбде graphicsLayer: значение зависит от раскладки списка и обновляет только слой.
     */
    fun visualDragOffsetY(key: Any): Float {
        val offset = dragAccumulatedY
        val layoutInfo = lazyListState.layoutInfo
        val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return offset
        val contentTop = layoutInfo.viewportStartOffset + layoutInfo.beforeContentPadding
        return maxOf(offset, (contentTop - itemInfo.offset).toFloat())
    }

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
 * @param isBlockContinuation Предикат: является ли элемент продолжением блока, начатого элементом выше
 *                          (например, события календаря под заголовком дня). Такие элементы движок
 *                          считает частью целевого блока и приземляет перетаскиваемый элемент за ними.
 *                          Отвечать true нужно ТОЛЬКО для настоящих продолжений: если пометить так
 *                          постороннюю строку — скажем, хвостовую распорку списка, — компенсация
 *                          прыжка раздуется на её высоту и карточка уйдёт из-под пальца.
 *                          По умолчанию продолжений нет.
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
    isBlockContinuation: (targetKey: Any) -> Boolean = { false },
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
     * Видимые элементы списка без тех, что свёрнуты в стопку под пальцем.
     *
     * Свёрнутых элементов фактически нет в потоке списка, поэтому они не могут быть ни целями
     * обмена, ни частью составного блока, ни ориентиром для границ видимой области.
     * Ведущий элемент стопки остаётся — именно его геометрию движок и отслеживает.
     */
    fun activeItems(visibleItems: List<LazyListItemInfo>, draggedKey: Any): List<LazyListItemInfo> {
        if (state.stackedDragKeys.isEmpty()) return visibleItems
        return visibleItems.filter { it.key == draggedKey || !state.stackedDragKeys.contains(it.key) }
    }

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

        val activeVisibleItems = activeItems(visibleItems, draggedKey)
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
                // Находим последний элемент целевого составного блока вместе с его продолжениями
                var lastTarget = nextItem
                val nextIdx = activeVisibleItems.indexOfFirst { it.key == nextItem.key }
                var isBlockFullyVisible = true
                if (nextIdx != -1) {
                    for (i in (nextIdx + 1)..activeVisibleItems.lastIndex) {
                        val item = activeVisibleItems[i]
                        if (isBlockContinuation(item.key)) {
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
                    if (isLastVisibleItem && hasMoreItemsInList && isBlockContinuation(lastTarget.key)) {
                        isBlockFullyVisible = false
                    }
                }

                // Место, куда встанет элемент, должно остаться в видимой области. Иначе у нижнего
                // края (особенно при автопрокрутке) слот уезжает за экран, LazyColumn выгружает
                // строку, жест отменяется и карточка пропадает из-под пальца. Пока цель видна
                // не целиком, ждём: автопрокрутка сама вытянет её на экран.
                val targetInViewport =
                    lastTarget.offset + lastTarget.size <= lazyListState.layoutInfo.viewportEndOffset

                if (isBlockFullyVisible && targetInViewport) {
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

            // То же у верхнего края: слот займёт место цели, и оно должно быть в видимой области
            if (prevItem != null && prevItem.offset >= lazyListState.layoutInfo.viewportStartOffset) {
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
        val rawVisibleItems = lazyListState.layoutInfo.visibleItemsInfo
        val targetItem = checkSwap(key, rawVisibleItems, state.dragAccumulatedY, deltaY) ?: return false

        // Тот же список, с которым работает checkSwap: свёрнутые в стопку элементы
        // не должны попадать в обход составного блока и раздувать компенсацию
        val visibleItems = activeItems(rawVisibleItems, key)
        val draggedItem = visibleItems.firstOrNull { it.key == key } ?: return false

        val isMovingDown = targetItem.index > draggedItem.index

        // Защита движка от повторного свапа с тем же элементом в том же направлении,
        // пока LazyColumn ещё не успел завершить рекомпозицию и обновить visibleItemsInfo
        if (targetItem.key == state.lastSwappedTargetKey && isMovingDown == state.lastSwapMovingDown) {
            return false
        }

        val distanceToShift = if (isMovingDown) {
            // Движение вниз: находим последний элемент целевого блока вместе с его продолжениями
            var lastTarget = targetItem
            val targetIdx = visibleItems.indexOfFirst { it.key == targetItem.key }
            if (targetIdx != -1) {
                for (i in (targetIdx + 1)..visibleItems.lastIndex) {
                    val item = visibleItems[i]
                    if (isBlockContinuation(item.key)) {
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

        // Экранное положение карточки до перестановки — оно обязано остаться прежним
        val dragTopBeforeMove = draggedItem.offset + state.dragAccumulatedY

        // Позиция прокрутки до перестановки: первая видимая строка и её смещение
        val firstVisibleIndex = lazyListState.firstVisibleItemIndex
        val firstVisibleOffset = lazyListState.firstVisibleItemScrollOffset

        // Запрашиваем подтверждение перемещения у внешнего обработчика
        val swapAccepted = onMoveIfNecessary(key, targetItem.key)

        // Если список перестроился, компенсируем прыжок с учётом полного расстояния
        if (swapAccepted) {
            // LazyColumn держит прокрутку за КЛЮЧ первой видимой строки. Если она сама участвует
            // в перестановке (или элемент переезжает через неё), список сдвигается вслед за её
            // ключом: у верхнего края слот перетаскиваемого элемента уходил выше экрана,
            // автопрокрутка без видимого слота вставала до конца жеста, а у ведущего элемента
            // наверху прыгало всё содержимое. Оставляем на месте ту же ПОЗИЦИЮ с тем же смещением —
            // экран стоит, как стоял, и слот остаётся там, где его ждёт расчёт компенсации.
            if (targetItem.index <= firstVisibleIndex || draggedItem.index <= firstVisibleIndex) {
                lazyListState.requestScrollToItem(firstVisibleIndex, firstVisibleOffset)
            }
            state.lastSwappedTargetKey = targetItem.key
            state.lastSwapMovingDown = isMovingDown
            state.adjustOffset(distanceToShift)
            // Расчёт выше — только предсказание. Куда бизнес-логика реально вставила элемент,
            // движок узнает на следующем кадре и доберёт разницу (см. expectedDragTopAfterMove)
            state.expectedDragTopAfterMove = dragTopBeforeMove
        } else {
            if (targetItem.key == state.lastSwappedTargetKey) {
                state.lastSwappedTargetKey = null
                state.lastSwapMovingDown = null
            }
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

    /**
     * Ключ, которым владеет активный жест ЭТОГО узла. Фиксируется при старте перетаскивания
     * и не меняется, даже если LazyColumn переиспользует узел под другой элемент.
     * null — этот узел сейчас ничего не перетаскивает.
     */
    private var dragOwnerKey: Any? = null

    /** Флаг указывает, сместил ли пользователь карточку хотя бы на минимальный порог с момента долгого нажатия */
    private var hasMovedPastSlop = false

    /** Находилась ли карточка изначально (при фиксации долгого нажатия) в верхней зоне автоскролла */
    private var wasInitiallyInTopZone = false

    /** Находилась ли карточка изначально (при фиксации долгого нажатия) в нижней зоне автоскролла */
    private var wasInitiallyInBottomZone = false

    /** Чистое накопленное физическое смещение пальца по оси Y (не зависит от скролла списка scrollBy) */
    private var fingerOffsetY = 0f

    /** Сверка предсказанной компенсации прыжка с фактическим раскладом на следующем кадре */
    private var moveCorrectionJob: Job? = null

    /** Значение [fingerOffsetY] на момент последней перестановки */
    private var fingerOffsetAtMove = 0f

    /**
     * Верхняя грань карточки в координатах списка — там, где она должна быть под пальцем.
     * В начале жеста совпадает со слотом, дальше меняется только от движения пальца: ни прокрутка,
     * ни перестановки её не сдвигают. По ней считаются зоны автопрокрутки — в том числе когда слота
     * элемента нет среди видимых — и с ней сверяется смещение карточки.
     */
    private var cardTop = 0f

    /** Высота перетаскиваемого элемента — для кадров, когда его слота нет среди видимых */
    private var cardSize = 0

    /**
     * Закрепление строки в LazyColumn на время жеста: пока оно держится, список не выгружает
     * элемент, даже если его слот ушёл за экран. Выгрузка отменила бы жест посреди перетаскивания.
     */
    private var pinnedHandle: PinnableContainer.PinnedHandle? = null

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
        dragOwnerKey = key
        state.draggedItemKey = key
        state.isInteracting = true
        state.snapOffsetTo(0f)
        state.lastSwappedTargetKey = null
        state.lastSwapMovingDown = null
        state.expectedDragTopAfterMove = Float.NaN
        hasMovedPastSlop = false
        fingerOffsetY = 0f
        fingerOffsetAtMove = 0f

        val layoutInfo = state.lazyListState.layoutInfo
        val draggedItemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
        val initialTop = draggedItemInfo?.offset?.toFloat() ?: 0f
        val initialBottom = initialTop + (draggedItemInfo?.size ?: 0)
        cardTop = initialTop
        cardSize = draggedItemInfo?.size ?: 0
        val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
        val scrollTopZone = (viewportHeight * topScrollZoneFraction).coerceAtLeast(1f)
        val scrollBottomZone = (viewportHeight * bottomScrollZoneFraction).coerceAtLeast(1f)
        // Границы зон те же, что в autoScrollStep: сверху отсчёт от начала контента,
        // иначе гейт начальной зоны и сам автоскролл сработают на разных рубежах
        val contentStart = layoutInfo.viewportStartOffset + layoutInfo.beforeContentPadding
        wasInitiallyInTopZone = initialTop < contentStart + scrollTopZone
        wasInitiallyInBottomZone = initialBottom > layoutInfo.viewportEndOffset - scrollBottomZone

        pinnedHandle?.release()
        pinnedHandle = currentValueOf(LocalPinnableContainer)?.pin()

        performHaptic(HapticFeedbackConstants.LONG_PRESS)
        onDragStart()
        startAutoScroll(key)
    }

    private fun handleDrag(change: PointerInputChange, dragAmount: Offset) {
        change.consume()
        fingerOffsetY += dragAmount.y
        cardTop += dragAmount.y
        state.adjustOffset(dragAmount.y)
        state.adjustOffsetHorizontal(dragAmount.x)
        if (!hasMovedPastSlop) {
            val slopPx = with(currentValueOf(LocalDensity)) { DRAG_AUTOSCROLL_SLOP_DP.dp.toPx() }
            if (hypot(state.dragAccumulatedX, fingerOffsetY) >= slopPx) {
                hasMovedPastSlop = true
            }
        }
        if (onDrag(dragAmount.y)) {
            performHaptic(HapticFeedbackConstants.CLOCK_TICK)
            dragOwnerKey?.let { scheduleMoveCorrection(it) }
        }
    }

    /**
     * Сверяет предсказанную компенсацию прыжка с тем, куда элемент встал на самом деле.
     *
     * Движок считает компенсацию геометрически, исходя из того, что элемент займёт слот цели.
     * Для заголовков и составных блоков это не так: бизнес-логика вставляет элемент в другое
     * место, и карточка уезжает из-под пальца на величину ошибки. Здесь ошибка добирается
     * по факту, поэтому неточность расчёта больше не видна.
     */
    private fun scheduleMoveCorrection(draggedKey: Any) {
        moveCorrectionJob?.cancel()
        fingerOffsetAtMove = fingerOffsetY

        moveCorrectionJob = coroutineScope.launch {
            // К началу следующего кадра LazyColumn уже разложен с новым порядком элементов
            withFrameNanos { }

            val expectedDragTop = state.expectedDragTopAfterMove
            state.expectedDragTopAfterMove = Float.NaN
            if (expectedDragTop.isNaN() || state.draggedItemKey != draggedKey) return@launch

            val itemInfo = state.lazyListState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.key == draggedKey } ?: return@launch

            // Палец мог сдвинуться между перестановкой и этим кадром — это законное смещение,
            // его вычитать нельзя. Скролл списка на dragTop не влияет: adjustOffset его гасит.
            val fingerShift = fingerOffsetY - fingerOffsetAtMove
            val actualDragTop = itemInfo.offset + state.dragAccumulatedY
            val residual = (expectedDragTop + fingerShift) - actualDragTop

            if (residual != 0f) {
                state.adjustOffset(residual)
            }
        }
    }

    private fun handleDragEnd() {
        val ownerKey = takeOwnedGesture() ?: return
        state.isInteracting = false
        onDragEnd()
        settle(ownerKey)
    }

    private fun handleDragCancel() {
        val ownerKey = takeOwnedGesture() ?: return
        state.isInteracting = false
        settle(ownerKey)
    }

    /**
     * Проверяет, что завершаемый жест действительно принадлежит этому узлу, и освобождает
     * его собственные ресурсы.
     *
     * Ключевой момент: detectDragGesturesAfterLongPress вызывает onDragCancel при отмене своей
     * корутины — в том числе когда узел просто ждал касания, а LazyColumn выбросил элемент
     * за пределы экрана. Во время автопрокрутки это происходит постоянно, и раньше такой
     * «чужой» onDragCancel сбрасывал общее состояние (isInteracting) прямо посреди активного
     * перетаскивания, из-за чего автоскролл вставал до конца жеста.
     *
     * @return ключ, которым владеет жест, либо null — если жест этому узлу не принадлежит
     *         и общее состояние трогать нельзя.
     */
    private fun takeOwnedGesture(): Any? {
        val ownerKey = dragOwnerKey
        releaseOwnResources()
        val owned = ownerKey?.takeIf { state.draggedItemKey == it }
        // Жест этого узла закончился, но общее состояние уже принадлежит другому элементу:
        // анимации возврата не будет, закрепление снимаем сразу
        if (owned == null) releasePin()
        return owned
    }

    private fun releasePin() {
        pinnedHandle?.release()
        pinnedHandle = null
    }

    /** Освобождает только ресурсы самого узла, не касаясь общего состояния перетаскивания */
    private fun releaseOwnResources() {
        dragOwnerKey = null
        hasMovedPastSlop = false
        wasInitiallyInTopZone = false
        wasInitiallyInBottomZone = false
        fingerOffsetY = 0f
        fingerOffsetAtMove = 0f
        autoScrollJob?.cancel()
        autoScrollJob = null
        moveCorrectionJob?.cancel()
        moveCorrectionJob = null
    }

    /** Плавно возвращает элемент на место и очищает общее состояние перетаскивания */
    private fun settle(ownerKey: Any) {
        // Узел уже отсоединён (строку выгрузили посреди жеста): его корутины отменены, и запущенная
        // здесь корутина не выполнилась бы ни разу — карточка так и висела бы в режиме
        // перетаскивания. Без анимации возвращаем всё на место сразу.
        if (!isAttached) {
            if (state.draggedItemKey == ownerKey) state.snapOffsetTo(0f)
            finishSettle(ownerKey)
            return
        }
        // Возврат начинается оттуда, где карточку видно: если палец ушёл выше края контента,
        // рисовалась она прижатой к нему (см. visualDragOffsetY), а не под пальцем
        if (state.draggedItemKey == ownerKey) {
            state.dragAccumulatedY = state.visualDragOffsetY(ownerKey)
        }
        coroutineScope.launch {
            try {
                state.animateOffsetToZero()
            } finally {
                finishSettle(ownerKey)
            }
        }
    }

    private fun finishSettle(ownerKey: Any) {
        // За время анимации перетаскивание мог перехватить другой элемент
        if (state.draggedItemKey == ownerKey) {
            state.draggedItemKey = null
            state.stackedDragKeys = emptyList()
            state.lastSwappedTargetKey = null
            state.lastSwapMovingDown = null
        }
        releasePin()
        onDragSettled()
    }

    // ── Авто-скролл ───────────────────────────────────────────────────────────

    /**
     * Цикл автопрокрутки привязан к ключу собственного жеста, а не к общему флагу isInteracting:
     * общий флаг может сбросить другой элемент списка, и раньше это глушило автоскролл
     * посреди активного перетаскивания.
     */
    private fun startAutoScroll(draggedKey: Any) {
        autoScrollJob?.cancel()
        autoScrollJob = coroutineScope.launch {
            // Первый кадр только задаёт точку отсчёта времени
            var lastFrameNanos = withFrameNanos { it }

            while (isActive && dragOwnerKey == draggedKey && state.draggedItemKey == draggedKey) {
                val frameNanos = withFrameNanos { it }
                val frameSeconds = ((frameNanos - lastFrameNanos) / NANOS_PER_SECOND)
                    .coerceIn(0f, MAX_SCROLL_FRAME_SECONDS)
                lastFrameNanos = frameNanos

                try {
                    autoScrollStep(draggedKey, frameSeconds)
                } catch (cancellation: CancellationException) {
                    // Наш scrollBy отменил чужой скролл через MutatorMutex списка.
                    // Это не повод глушить автопрокрутку до конца жеста: если отменили
                    // сам жест — ensureActive пробросит исключение, иначе идём дальше.
                    coroutineContext.ensureActive()
                }
            }
        }
    }

    /**
     * Один кадр автопрокрутки, когда перетаскиваемый элемент подходит к границе viewport.
     *
     * @param frameSeconds фактическая длительность кадра. Скорость задана в пикселях в секунду,
     *        поэтому неровный кадр даёт пропорционально больший сдвиг, и движение остаётся ровным.
     */
    private suspend fun autoScrollStep(draggedKey: Any, frameSeconds: Float) {
        // До тех пор, пока пользователь явно не сдвинул карточку после долгого нажатия, автоскролл заблокирован
        if (!hasMovedPastSlop) return

        val layoutInfo = state.lazyListState.layoutInfo

        // Слота элемента может не быть среди видимых: бизнес-логика вставляет элемент не туда, куда
        // указывает геометрия (на заголовке месяца — после всех событий месяца), и слот оказывается
        // за краем экрана. Раньше такой кадр пропускался — автопрокрутка вставала и до конца жеста
        // уже не возобновлялась, потому что вернуть слот на экран могла только она сама. Теперь зоны
        // считаются по положению карточки под пальцем (cardTop): прокрутка продолжается и сама
        // вытягивает слот обратно, а перестановки возобновляются, как только он снова виден.
        val draggedItemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.key == draggedKey }
        if (draggedItemInfo != null) {
            cardSize = draggedItemInfo.size
            // Смещение карточки — ровно от слота до места под пальцем: так добирается расхождение,
            // накопившееся, пока слот был вне экрана. Сразу после перестановки раскладка списка
            // здесь ещё старая, и до сверки перестановки (expectedDragTopAfterMove) не трогаем —
            // иначе пересчёт по устаревшему слоту отменил бы компенсацию прыжка
            if (state.expectedDragTopAfterMove.isNaN()) {
                val desiredOffset = cardTop - draggedItemInfo.offset
                if (desiredOffset != state.dragAccumulatedY) state.dragAccumulatedY = desiredOffset
            }
        }
        val dragTop = cardTop
        val dragBottom = dragTop + cardSize

        val viewportStart = layoutInfo.viewportStartOffset.toFloat()
        val viewportEnd = layoutInfo.viewportEndOffset.toFloat()
        val viewportHeight = viewportEnd - viewportStart
        if (viewportHeight <= 0f) return

        val scrollTopZone = (viewportHeight * topScrollZoneFraction).coerceAtLeast(1f)
        val scrollBottomZone = (viewportHeight * bottomScrollZoneFraction).coerceAtLeast(1f)

        // Верхнюю зону отсчитываем от начала контента, а не от viewportStartOffset: последний
        // уходит под верхний contentPadding, эта полоса закрыта тулбаром и карточка туда не
        // попадает. Считая от viewportStartOffset, мы дарили тулбару часть зоны — палец не мог
        // погрузиться в неё глубже середины, и вверх список шёл вдвое медленнее, чем вниз.
        // Снизу такой поправки не нужно: нижний contentPadding — это свободное место на экране,
        // палец достаёт до самого низа viewport.
        val contentStart = viewportStart + layoutInfo.beforeContentPadding

        // Как только карточка физически вышла из начальной зоны автоскролла ИЛИ пользователь сместил её в сторону края, флаг сбрасывается:
        // карточка считается свободно перемещаемой, и при возврате к любому краю автоскролл активируется безусловно
        if (wasInitiallyInTopZone && (dragTop >= contentStart + scrollTopZone || fingerOffsetY < -5f)) {
            wasInitiallyInTopZone = false
        }
        if (wasInitiallyInBottomZone && (dragBottom <= viewportEnd - scrollBottomZone || fingerOffsetY > 5f)) {
            wasInitiallyInBottomZone = false
        }

        val canScrollTop = if (wasInitiallyInTopZone) fingerOffsetY < 0f else true
        val canScrollBottom = if (wasInitiallyInBottomZone) fingerOffsetY > 0f else true

        val scrollAmount = when {
            // Верхний край элемента приблизился к верхней границе, и пользователь ведёт карточку вверх
            dragTop < contentStart + scrollTopZone && canScrollTop -> {
                val distanceIntoZone = (contentStart + scrollTopZone - dragTop).coerceIn(0f, scrollTopZone)
                -scrollDistance(distanceIntoZone / scrollTopZone, frameSeconds)
            }
            // Нижний край элемента приблизился к нижней границе viewport, и пользователь ведёт карточку вниз
            dragBottom > viewportEnd - scrollBottomZone && canScrollBottom -> {
                val distanceIntoZone = (dragBottom - (viewportEnd - scrollBottomZone)).coerceIn(0f, scrollBottomZone)
                scrollDistance(distanceIntoZone / scrollBottomZone, frameSeconds)
            }
            else -> 0f
        }

        if (scrollAmount == 0f) return

        val consumed = state.lazyListState.scrollBy(scrollAmount)
        // Пока слота не видно, смещение карточки не трогаем: её положение на экране задаёт палец,
        // а точное смещение от слота выставится в первом кадре, где слот снова виден
        if (consumed != 0f && draggedItemInfo != null) {
            state.adjustOffset(consumed)
            if (onDragged(consumed)) {
                performHaptic(HapticFeedbackConstants.CLOCK_TICK)
                // Перестановка во время автопрокрутки так же, как от движения пальца, требует
                // сверки: на заголовках бизнес-логика вставляет элемент не туда, куда указывает
                // геометрия, и без сверки карточка отскакивала от пальца
                scheduleMoveCorrection(draggedKey)
            }
        }
    }

    /**
     * Сдвиг за один кадр. Скорость нарастает по мере погружения элемента в зону у края экрана
     * и задана в пикселях в секунду, поэтому длительность кадра входит в расчёт напрямую.
     */
    private fun scrollDistance(ratio: Float, frameSeconds: Float): Float {
        val pixelsPerSecond = SCROLL_MIN_PX_PER_SECOND +
            (SCROLL_MAX_PX_PER_SECOND - SCROLL_MIN_PX_PER_SECOND) * ratio.pow(SCROLL_CURVE_POWER)
        return pixelsPerSecond * frameSeconds
    }

    private fun performHaptic(feedbackConstant: Int) {
        currentValueOf(LocalView).performHapticFeedback(feedbackConstant)
    }
}
