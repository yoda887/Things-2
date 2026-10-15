package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.model.RecurrenceRule
import com.example.ui.theme.AppIcons
import com.example.ui.theme.ThingsTheme

/** Стрелка занимает эту долю бледного круга значка шаблона */
private const val REPEAT_ICON_ARROW_FRACTION = 0.86f
/**
 * Значок чуть крупнее места чекбокса — соразмерно соседним значкам; на раскладку не влияет. Где края
 * обрезаются (значок в своём слое, как в Quick Find), место задаётся меньше на этот коэффициент.
 */
internal const val REPEAT_ICON_SCALE = 1.15f
/** Плотность бледного круга под стрелкой, как в Things */
private const val REPEAT_ICON_BACKGROUND_ALPHA = 0.2f

/**
 * Правила повтора шаблонов повторяющихся задач и проектов — по id шаблона. Их читают строка шаблона (дата
 * следующего повтора) и редактор шаблона и его копий; задаёт экран списка.
 */
val LocalRepeatRules = compositionLocalOf<Map<String, RecurrenceRule>> { emptyMap() }

/**
 * Плашка шаблона в «Повторяющихся»: день следующего повтора, «Пауза» или «Ждёт» — повтор «после выполнения»
 * ждёт, когда отметят текущую копию, как в Things; без правила и у закончившихся — нет.
 */
@Composable
fun repeatBadge(rule: RecurrenceRule?): String? = when {
    rule == null -> null
    !rule.isActive -> stringResource(R.string.repeat_paused)
    rule.nextGenerationDate <= 0 && com.example.domain.repeat.RepeatRule.decode(rule.rrule)?.afterCompletion == true ->
        stringResource(R.string.repeat_waiting)
    rule.nextGenerationDate > 0 -> com.example.ui.screens.home.subcomponents.startDateBadgeText(rule.nextGenerationDate)
    else -> null
}

/**
 * Значок повтора, как в Things: круговая стрелка на бледном круге её цвета. Шаблон — тёмно-синий, как кольцо
 * проекта; у копий повторяющейся задачи — серый ([color]).
 */
@Composable
fun RepeatTemplateIcon(modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = ThingsTheme.colors.project) {
    val accent = color
    Box(
        modifier = modifier.scale(REPEAT_ICON_SCALE).background(accent.copy(alpha = REPEAT_ICON_BACKGROUND_ALPHA), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = AppIcons.Repeat,
            contentDescription = stringResource(R.string.special_repeating),
            tint = accent,
            modifier = Modifier.fillMaxSize(REPEAT_ICON_ARROW_FRACTION)
        )
    }
}
