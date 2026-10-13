package com.example.domain.repeat

import java.time.LocalDate

/** Единица повтора: «каждые N дней / недель / месяцев / лет». */
enum class RepeatUnit { DAY, WEEK, MONTH, YEAR }

/** День повтора внутри месяца (или года — вместе с [RepeatOn.month]). */
sealed interface RepeatDay {
    /** Число месяца 1..31 или [LAST] — последний день месяца. 31-е в коротком месяце — его последний день. */
    data class OfMonth(val day: Int) : RepeatDay

    /**
     * День недели [dayOfWeek] (1 — понедельник … 7 — воскресенье). [ordinal]: 0 — каждый такой день
     * (у еженедельных правил), 1..4 — N-й в месяце, [LAST] — последний в месяце.
     */
    data class Weekday(val dayOfWeek: Int, val ordinal: Int = 0) : RepeatDay

    companion object {
        /** «Последний»: последний день месяца или последний такой день недели в месяце */
        const val LAST = -1
    }
}

/** Строка «On / And On» правила: день и, у ежегодных правил, месяц (1..12). */
data class RepeatOn(val day: RepeatDay, val month: Int? = null)

/**
 * Правило повтора, как в Things.
 *
 * - [afterCompletion] — «После выполнения»: новая копия через [interval] [unit] после того, как отметили
 *   предыдущую. Иначе — «Регулярно»: по расписанию, независимо от выполнения.
 * - [on] — дни повтора у регулярных правил (пусто — тот же день, что у [start]).
 * - [start] — отсчёт расписания: от него считаются «каждые 2 недели» и т. п.
 * - Окончание — дата [endDate] (повторы после неё не создаются) или число повторов [endCount].
 * - [deadlineDaysBefore] — у каждой копии срок в день повтора, а сама она появляется на столько дней
 *   раньше (0 — в тот же день); null — без срока.
 *
 * Хранится строкой ([encode] / [decode]) в recurrence_rules.rrule. Закреплено тестами RepeatTest.
 */
data class RepeatRule(
    val afterCompletion: Boolean,
    val interval: Int = 1,
    val unit: RepeatUnit,
    val start: LocalDate,
    val on: List<RepeatOn> = emptyList(),
    val endDate: LocalDate? = null,
    val endCount: Int? = null,
    val deadlineDaysBefore: Int? = null
) {
    init {
        require(interval >= 1) { "interval < 1" }
    }

    fun encode(): String = buildList {
        add(VERSION)
        add("mode=" + if (afterCompletion) "after" else "fixed")
        add("every=$interval")
        add("unit=" + unit.name.lowercase())
        add("start=$start")
        if (on.isNotEmpty()) add("on=" + on.joinToString(",") { encodeOn(it) })
        endDate?.let { add("until=$it") }
        endCount?.let { add("count=$it") }
        deadlineDaysBefore?.let { add("deadline=$it") }
    }.joinToString(";")

    companion object {
        private const val VERSION = "v1"

        fun decode(text: String): RepeatRule? = runCatching {
            val parts = text.split(";")
            if (parts.firstOrNull() != VERSION) return null
            val map = parts.drop(1).associate { it.substringBefore("=") to it.substringAfter("=") }
            RepeatRule(
                afterCompletion = map["mode"] == "after",
                interval = map["every"]?.toInt() ?: 1,
                unit = RepeatUnit.valueOf(map.getValue("unit").uppercase()),
                start = LocalDate.parse(map.getValue("start")),
                on = map["on"]?.split(",")?.filter { it.isNotEmpty() }?.map { decodeOn(it) }.orEmpty(),
                endDate = map["until"]?.let { LocalDate.parse(it) },
                endCount = map["count"]?.toInt(),
                deadlineDaysBefore = map["deadline"]?.toInt()
            )
        }.getOrNull()

        // «M11:D14» — 14-е ноября, «D-1» — последний день, «W1.2» — 2-й понедельник, «W6.0» — каждая суббота
        private fun encodeOn(on: RepeatOn): String {
            val day = when (val d = on.day) {
                is RepeatDay.OfMonth -> "D${d.day}"
                is RepeatDay.Weekday -> "W${d.dayOfWeek}.${d.ordinal}"
            }
            return if (on.month != null) "M${on.month}:$day" else day
        }

        private fun decodeOn(text: String): RepeatOn {
            val month = if (text.startsWith("M")) text.substring(1).substringBefore(":").toInt() else null
            val day = if (month != null) text.substringAfter(":") else text
            val repeatDay = when (day.first()) {
                'D' -> RepeatDay.OfMonth(day.substring(1).toInt())
                'W' -> RepeatDay.Weekday(day.substring(1).substringBefore(".").toInt(), day.substringAfter(".").toInt())
                else -> error("bad day $day")
            }
            return RepeatOn(repeatDay, month)
        }
    }
}
