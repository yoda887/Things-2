package com.example.ui.screens.home.subcomponents

import com.example.data.model.Area
import com.example.data.model.Item
import com.example.ui.viewmodel.ActiveScreen

/**
 * Подпись под названием задачи — её проект или область; null — где задача и так стоит под их заголовком
 * или на их экране. Общая для строки списка и раскрытого редактора: при раскрытии подпись не появляется
 * там, где её нет у строки, и заголовок не подпрыгивает.
 *
 * @param todayGrouped дневная часть «Сегодня» сгруппирована по проектам и областям (настройка)
 */
internal fun taskSubtitle(task: Item, project: Item?, area: Area?, screen: ActiveScreen, todayGrouped: Boolean): String? {
    // В поиске и «Журнале» закрытые задачи — с подписью, как в Logbook Things
    val logbookStyle = (screen == ActiveScreen.SEARCH || screen == ActiveScreen.LOGBOOK) &&
        (task.isCompleted || task.status == Item.STATUS_CANCELLED)
    return when {
        // В поиске и «В любое время» задачи стоят под заголовком своего проекта или области — подпись повторяла бы его
        screen == ActiveScreen.SEARCH && !logbookStyle -> null
        screen == ActiveScreen.ANYTIME || screen == ActiveScreen.SOMEDAY -> null
        // «Повторяющиеся» сгруппированы под проектом или областью — подпись повторяла бы заголовок
        screen == ActiveScreen.REPEATING -> null
        // Тег — тоже по проектам и областям
        screen == ActiveScreen.TAG_DETAIL -> null
        // Дневная часть «Сегодня» по проектам и областям (настройка); вечер — с подписью, как прежде
        screen == ActiveScreen.TODAY && !task.isTonight && todayGrouped -> null
        project != null && screen != ActiveScreen.PROJECT_DETAIL -> project.name
        project == null && area != null && screen != ActiveScreen.AREA_DETAIL -> area.title
        else -> null
    }
}
