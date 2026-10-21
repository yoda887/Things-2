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
