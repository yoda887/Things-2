package com.example.data.local

import android.content.Context

/**
 * Вид списков на этом устройстве — свёрнутые области главного экрана, что уже видели в «Сегодня» и группировать
 * ли дневную часть «Сегодня» по проектам и областям. Это не
 * данные задач и не синхронизируется, поэтому хранится не в базе, а в SharedPreferences.
 */
class HomeLayoutPrefs(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Свёрнутые области (их id). */
    fun collapsedAreaIds(): Set<String> = prefs.getStringSet(KEY_COLLAPSED_AREAS, emptySet()).orEmpty().toSet()

    fun setAreaCollapsed(areaId: String, collapsed: Boolean) {
        val ids = collapsedAreaIds().let { if (collapsed) it + areaId else it - areaId }
        prefs.edit().putStringSet(KEY_COLLAPSED_AREAS, ids).apply()
    }

    /** Что уже видели в «Сегодня» (id, [com.example.domain.lists.TodayNews]); null — ещё ни разу не запоминали */
    fun todaySeenIds(): Set<String>? =
        if (prefs.contains(KEY_TODAY_SEEN)) prefs.getStringSet(KEY_TODAY_SEEN, emptySet()).orEmpty().toSet() else null

    fun setTodaySeenIds(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_TODAY_SEEN, ids).apply()
    }

    /** Дневная часть «Сегодня» — по проектам и областям (настройка; по умолчанию — нет, как в Things) */
    fun todayGroupedByPlace(): Boolean = prefs.getBoolean(KEY_TODAY_GROUPED, false)

    fun setTodayGroupedByPlace(grouped: Boolean) {
        prefs.edit().putBoolean(KEY_TODAY_GROUPED, grouped).apply()
    }

    /** Даты в карточках — месяц словом («9 жовт.»), иначе цифрами («9.10.»); по умолчанию словом */
    fun datesWithMonthName(): Boolean = prefs.getBoolean(KEY_MONTH_NAME, true)

    fun setDatesWithMonthName(monthName: Boolean) {
        prefs.edit().putBoolean(KEY_MONTH_NAME, monthName).apply()
    }

    private companion object {
        const val PREFS_NAME = "home_layout"
        const val KEY_COLLAPSED_AREAS = "collapsed_areas"
        const val KEY_TODAY_SEEN = "today_seen"
        const val KEY_TODAY_GROUPED = "today_grouped_by_place"
        const val KEY_MONTH_NAME = "dates_month_name"
    }
}
