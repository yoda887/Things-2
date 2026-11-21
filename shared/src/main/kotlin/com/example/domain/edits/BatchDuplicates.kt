package com.example.domain.edits

import com.example.data.model.Item

/**
 * Где встают копии выбранных задач («Дублювати» при множественном выборе), как в Things: одной группой над самой
 * верхней выбранной, в том же порядке, что оригиналы, а не каждая над своим. Закреплено тестами BatchDuplicatesTest.
 *
 * Порядок списков — sortOrder, при равном — выше более новая (в «Планах» внутри дня — ещё dayOrder). Копии берут
 * место верхней выбранной и «создаются» на миг позже неё: первая копия — новее всех.
 */
object BatchDuplicates {

    /** Место копии в списках */
    data class Place(val sortOrder: Int, val creationDate: Long, val dayOrder: Long?)

    /** [selected] — выбранные задачи в порядке экрана; для каждой — место её копии. */
    fun places(selected: List<Item>): List<Pair<Item, Place>> {
        val top = selected.firstOrNull() ?: return emptyList()
        return selected.mapIndexed { index, item ->
            // Порядок дня — только у копий того же дня, что верхняя: у других дней он свой
            val dayOrder = if (item.startDate == top.startDate) top.dayOrder else item.dayOrder
            item to Place(top.sortOrder, top.creationDate + (selected.size - index), dayOrder)
        }
    }
}
