package com.example.ui.screens.home.inlineeditor.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.R

/** Срок относительно сегодня на языке системы: «просрочено», «сегодня», «завтра», «через N дней». */
@Composable
fun relativeDueText(deltaDays: Int): String = when {
    deltaDays < 0 -> stringResource(R.string.due_overdue)
    deltaDays == 0 -> stringResource(R.string.due_today)
    deltaDays == 1 -> stringResource(R.string.due_tomorrow)
    else -> pluralStringResource(R.plurals.due_in_days, deltaDays, deltaDays)
}
