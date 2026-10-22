package com.example.ui.components

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf

/**
 * Новое в «Сегодня» ([com.example.domain.lists.TodayNews]): [ids] — задачи и проекты с жёлтой точкой,
 * [onDismiss] — «OK» на плашке. Даёт экран; строки списка и плашка берут отсюда.
 */
@Immutable
data class TodayNew(val ids: Set<String> = emptySet(), val onDismiss: () -> Unit = {})

val LocalTodayNew = compositionLocalOf { TodayNew() }

/** Дневная часть «Сегодня» сгруппирована по проектам и областям (настройка): строки задач там без подписи проекта */
val LocalTodayGrouped = compositionLocalOf { false }

/** Даты в карточках («Журнал», «Завершённые проекты», поздние проекты): месяц словом («9 жовт.») или цифрами («9.10.»), настройка */
val LocalDatesWithMonthName = compositionLocalOf { true }
