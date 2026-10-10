package com.example.domain.lists

import com.example.data.model.DayBounds
import com.example.data.model.Item

/**
 * Главный экран: какие проекты в списке. Только те, с которыми можно работать сейчас: открытые, не
 * отложенные в «Когда-нибудь» и без даты старта в будущем. Отложенные и запланированные видны в
 * «Когда-нибудь», «Предстоящих», в своей области и в поиске. Закреплено тестами HomeListTest.
 */
object HomeList {

    fun showsProject(project: Item, bounds: DayBounds = DayBounds.now()): Boolean {
        if (!ListRules.isOpenProject(project)) return false
        if (project.start == Item.START_SOMEDAY) return false
        val startDate = project.startDate ?: return true
        return startDate <= bounds.endOfToday
    }
}
