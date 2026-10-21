package com.example.data.local

import android.content.Context

/**
 * Вид списков на этом устройстве — свёрнутые области главного экрана и что уже видели в «Сегодня». Это не
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

    private companion object {
        const val PREFS_NAME = "home_layout"
        const val KEY_COLLAPSED_AREAS = "collapsed_areas"
        const val KEY_TODAY_SEEN = "today_seen"
    }
}
