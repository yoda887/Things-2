package com.example.domain.repeat

import com.example.data.model.Item
import java.time.Instant
import java.time.ZoneId

/**
 * Что сохранить после перетаскивания в «Планах», где стоят шаблоны повтора: шаблон показан на дне своей
 * следующей копии, с подставленной датой. Закреплено тестами RepeatDropsTest.
 */
object RepeatDrops {

    /**
     * [save] — сохранить как обычную перестановку; [moved] — id шаблона и день, куда перенесли его ближайший
     * повтор (его переносят отдельно, как в Things: «Исключение» или «Обновить правило»), или null.
     */
    data class Result(val save: List<Item>, val moved: Pair<String, Long>?)

    /**
     * [items] — записи, которые перетаскивание хочет сохранить; [stored] — запись в базе по id; [shown] — строки
     * экрана до переноса.
     *
     * - Не шаблон — сохраняется как есть.
     * - Шаблон со своей датой из базы (перестановка внутри дня — UpcomingDragController) — сохраняется только его
     *   порядок дня.
     * - Шаблон с другой, подставленной датой сам не сохраняется. Перенесён он, только если этого дня нет среди
     *   дней, где он стоял на экране: шаблон стоит в «Планах» на каждом дне своих повторов (один id), а в
     *   сохраняемые попадают и соседи, у которых сменились лишь место или порядок дня.
     */
    fun afterDrag(
        items: List<Item>,
        stored: (String) -> Item?,
        shown: List<Item>,
        zone: ZoneId = ZoneId.systemDefault()
    ): Result {
        val (substituted, save) = items.partition { it.isRepeatTemplate && it.startDate != stored(it.id)?.startDate }
        fun dayOf(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        val moved = substituted.firstOrNull { template ->
            val day = template.startDate ?: return@firstOrNull false
            shown.none { it.id == template.id && it.startDate?.let { d -> dayOf(d) } == dayOf(day) }
        }
        return Result(save, moved?.let { m -> m.startDate?.let { m.id to it } })
    }
}
