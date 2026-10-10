package com.example.data.local

import android.content.Context

/**
 * Вид главного экрана на этом устройстве — сейчас это свёрнутые области. Это не данные задач и не
 * синхронизируется, поэтому хранится не в базе, а в SharedPreferences.
 */
class HomeLayoutPrefs(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Свёрнутые области (их id). */
    fun collapsedAreaIds(): Set<String> = prefs.getStringSet(KEY_COLLAPSED_AREAS, emptySet()).orEmpty().toSet()

    fun setAreaCollapsed(areaId: String, collapsed: Boolean) {
        val ids = collapsedAreaIds().let { if (collapsed) it + areaId else it - areaId }
        prefs.edit().putStringSet(KEY_COLLAPSED_AREAS, ids).apply()
    }

    private companion object {
        const val PREFS_NAME = "home_layout"
        const val KEY_COLLAPSED_AREAS = "collapsed_areas"
    }
}
