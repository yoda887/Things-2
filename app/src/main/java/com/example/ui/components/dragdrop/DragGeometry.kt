package com.example.ui.components.dragdrop

/**
 * Прямоугольник строки списка — всё, что движку перетаскивания нужно знать о раскладке.
 *
 * Отдельный тип вместо LazyListItemInfo нужен, чтобы правила пересечения и компенсации прыжка
 * были обычными функциями над данными: их можно проверять юнит-тестами, не собирая приложение
 * и не воспроизводя жест на устройстве.
 */
data class DragSlot(
    val key: Any,
    val index: Int,
    val offset: Int,
    val size: Int,
) {
    val bottom: Int get() = offset + size
}

/**
 * Элемент, с которым нужно произвести обмен, исходя из геометрии и вектора движения.
 *
 * Направление жёстко задаёт ведущий край (Direction Lock), поэтому взаимного дребезга нет
 * при любом пороге: вниз решает нижний край карточки, вверх — верхний.
 *
 * @param slots видимые строки без свёрнутых в стопку под пальцем, по возрастанию index
 * @param dragTop верхняя грань карточки в координатах списка (там, где палец)
 * @param deltaY направление движения; 0 — решения нет
 * @param thresholdFraction доля высоты цели, которую нужно перекрыть (0.1..0.9)
 * @param totalItemsCount всего строк в списке — по нему видно, что блок оборван краем экрана
 * @param canDropOver можно ли меняться местами с этой строкой
 * @param isBlockContinuation строка — продолжение блока, начатого строкой выше
 */
fun findSwapTarget(
    slots: List<DragSlot>,
    draggedKey: Any,
    dragTop: Float,
    deltaY: Float,
    thresholdFraction: Float,
    viewportStart: Int,
    viewportEnd: Int,
    totalItemsCount: Int,
    canDropOver: (Any) -> Boolean,
    isBlockContinuation: (Any) -> Boolean = { false },
): DragSlot? {
    if (deltaY == 0f) return null

    val draggedItem = slots.firstOrNull { it.key == draggedKey } ?: return null
    val dragBottom = dragTop + draggedItem.size
    val candidates = slots.filter { it.key != draggedKey && canDropOver(it.key) }
    val threshold = thresholdFraction.coerceIn(0.1f, 0.9f)

    if (deltaY > 0f) {
        // Движение строго ВНИЗ: ведущий край — нижний (dragBottom)
        val nextItem = candidates.filter { it.index > draggedItem.index }.minByOrNull { it.index }
            ?: return null

        val lastTarget = lastOfBlock(slots, nextItem, isBlockContinuation)

        // Если хвост блока упирается в нижний край видимых строк, а в списке есть ещё элементы,
        // значит часть блока за экраном и его настоящая высота пока неизвестна
        val blockCutOffByScreen = lastTarget.index == slots.last().index &&
            lastTarget.index < totalItemsCount - 1 &&
            isBlockContinuation(lastTarget.key)
        if (blockCutOffByScreen) return null

        // Место, куда встанет элемент, должно остаться в видимой области. Иначе у нижнего края
        // (особенно при автопрокрутке) слот уезжает за экран, LazyColumn выгружает строку, жест
        // отменяется и карточка пропадает из-под пальца. Пока цель видна не целиком — ждём.
        if (lastTarget.bottom > viewportEnd) return null

        val downThreshold = lastTarget.offset + lastTarget.size * threshold
        return if (dragBottom > downThreshold) nextItem else null
    }

    // Движение строго ВВЕРХ: ведущий край — верхний (dragTop)
    val prevItem = candidates.filter { it.index < draggedItem.index }.maxByOrNull { it.index }
        ?: return null

    // То же у верхнего края: слот займёт место цели, и оно должно быть в видимой области
    if (prevItem.offset < viewportStart) return null

    val upThreshold = prevItem.offset + prevItem.size * (1f - threshold)
    return if (dragTop < upThreshold) prevItem else null
}

/**
 * На сколько сместить карточку, чтобы после перестановки она осталась на прежнем месте экрана.
 *
 * Расчёт геометрический и потому лишь предсказание: куда бизнес-логика реально вставит элемент,
 * движок узнаёт на следующем кадре и добирает разницу.
 *
 * @return величина компенсации либо null, если перетаскиваемой строки или цели нет среди [slots]
 */
fun jumpCompensation(
    slots: List<DragSlot>,
    draggedKey: Any,
    targetKey: Any,
    isBlockContinuation: (Any) -> Boolean = { false },
): Float? {
    val draggedItem = slots.firstOrNull { it.key == draggedKey } ?: return null
    val targetItem = slots.firstOrNull { it.key == targetKey } ?: return null

    return if (targetItem.index > draggedItem.index) {
        // Вниз: элемент встаёт за последней строкой целевого блока
        val targetBottom = lastOfBlock(slots, targetItem, isBlockContinuation).bottom
        (draggedItem.offset - (targetBottom - draggedItem.size)).toFloat()
    } else {
        // Вверх: целевой слот начинается на отступе цели
        (draggedItem.offset - targetItem.offset).toFloat()
    }
}

/** Последняя строка составного блока, начатого строкой [target] */
private fun lastOfBlock(
    slots: List<DragSlot>,
    target: DragSlot,
    isBlockContinuation: (Any) -> Boolean,
): DragSlot {
    val targetIdx = slots.indexOfFirst { it.key == target.key }
    if (targetIdx == -1) return target

    var last = target
    for (i in (targetIdx + 1)..slots.lastIndex) {
        val slot = slots[i]
        if (!isBlockContinuation(slot.key)) break
        last = slot
    }
    return last
}
