package com.example.ui.screens.home.inlineeditor.dialogs

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.R
import com.example.data.model.Item
import com.example.domain.repeat.RepeatDates
import com.example.domain.repeat.RepeatDay
import com.example.domain.repeat.RepeatOn
import com.example.domain.repeat.RepeatRule
import com.example.domain.repeat.RepeatUnit
import com.example.ui.components.WHEEL_HEIGHT
import com.example.ui.components.WHEEL_ROW_HEIGHT
import com.example.ui.components.Wheel
import com.example.ui.components.WheelState
import com.example.ui.components.rememberWheelState
import com.example.ui.screens.home.inlineeditor.utils.formatReminderTime
import com.example.ui.theme.AppIcons
import com.example.ui.theme.ThingsAlpha
import com.example.ui.theme.ThingsIconSize
import com.example.ui.theme.ThingsMotion
import com.example.ui.theme.ThingsSpacing
import com.example.ui.theme.ThingsStroke
import com.example.ui.theme.ThingsTheme
import com.example.ui.theme.dimens
import kotlinx.coroutines.flow.drop
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Окно «Повторять» — ширина как у окна «Когда», высота — доля экрана, как в эталоне */
private val REPEAT_DIALOG_WIDTH = 336.dp
private const val REPEAT_DIALOG_HEIGHT_FRACTION = 0.72f

/** Появление окна: вырастает из этого масштаба */
private const val REPEAT_ENTER_SCALE = 0.9f

/** Колонки барабанов окна */
private val REPEAT_WHEEL_NUMBER_WIDTH = 72.dp
private val REPEAT_WHEEL_WORD_WIDTH = 150.dp
private val REPEAT_WHEEL_MONTH_WIDTH = 80.dp
/** Колонка «день / день недели» у ежегодных — уже, рядом ещё колонка месяца */
private val REPEAT_WHEEL_KIND_YEARLY_WIDTH = 110.dp

/** Наибольшие значения на барабанах: «каждые N», «после N повторов», «за N дней» */
private const val REPEAT_MAX_INTERVAL = 30
private const val REPEAT_MAX_COUNT = 100
private const val REPEAT_MAX_DEADLINE_DAYS = 30

/** Ячейка календаря «Следующая» / «Заканчивается» */
private val REPEAT_CALENDAR_CELL_HEIGHT = 44.dp
/** Календарь листается на 100 лет вперёд — для дальних дат ограничения по сути нет (недели строятся по мере прокрутки) */
private const val REPEAT_CALENDAR_WEEKS = 52 * 100

/** Порядковых номеров на барабане «В»: 1..31 и «последний»; у дня недели — только 1..4 и «последний» */
private const val REPEAT_ORDINALS = 32
private const val REPEAT_LAST_INDEX = 31
private const val REPEAT_MAX_WEEKDAY_ORDINAL = 4

/** Шаг минут на барабане напоминания */
private const val REPEAT_MINUTE_STEP = 5

/** Страницы окна: главная и вложенные, на которые переходят со строк главной */
private enum class RepeatPage(val depth: Int) { MAIN(0), EVERY(1), ON(1), NEXT(1), ENDS(1), DEADLINE(1), REMINDER(1) }

/** Окончание повторов */
private enum class RepeatEnd { NEVER, DATE, COUNT }

/** Изменяемый черновик правила, пока окно открыто */
private class RepeatDraft(task: Item, today: LocalDate) {
    private val zone = ZoneId.systemDefault()
    val firstDay: LocalDate = task.startDate
        ?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        ?.takeIf { !it.isBefore(today) } ?: today

    var afterCompletion by mutableStateOf(true)
    var afterInterval by mutableIntStateOf(1)
    var afterUnit by mutableStateOf(RepeatUnit.WEEK)
    var interval by mutableIntStateOf(1)
    var unit by mutableStateOf(RepeatUnit.WEEK)
    var start by mutableStateOf(firstDay)
    val weekdays = mutableStateListOf(firstDay.dayOfWeek.value)
    val entries = mutableStateListOf(RepeatOn(RepeatDay.OfMonth(firstDay.dayOfMonth), firstDay.monthValue))
    var selectedEntry by mutableIntStateOf(0)
    var end by mutableStateOf(RepeatEnd.NEVER)
    var endDate by mutableStateOf(firstDay.plusMonths(1))
    var endCount by mutableIntStateOf(5)
    var deadlineOn by mutableStateOf(false)
    var deadlineDays by mutableIntStateOf(0)
    var reminderTime by mutableStateOf(task.reminderTime)

    /** Окно открыто с правилом шаблона — черновик берёт его значения */
    fun load(rule: RepeatRule) {
        afterCompletion = rule.afterCompletion
        if (rule.afterCompletion) {
            afterInterval = rule.interval
            afterUnit = rule.unit
        } else {
            interval = rule.interval
            unit = rule.unit
            start = rule.start
        }
        val days = rule.on.mapNotNull { (it.day as? RepeatDay.Weekday)?.takeIf { d -> d.ordinal == 0 }?.dayOfWeek }
        if (rule.unit == RepeatUnit.WEEK && days.isNotEmpty()) { weekdays.clear(); weekdays.addAll(days) }
        if ((rule.unit == RepeatUnit.MONTH || rule.unit == RepeatUnit.YEAR) && rule.on.isNotEmpty()) {
            entries.clear(); entries.addAll(rule.on.map { if (it.month == null) it.copy(month = rule.start.monthValue) else it })
        }
        when {
            rule.endDate != null -> { end = RepeatEnd.DATE; endDate = rule.endDate }
            rule.endCount != null -> { end = RepeatEnd.COUNT; endCount = rule.endCount }
            else -> end = RepeatEnd.NEVER
        }
        deadlineOn = rule.deadlineDaysBefore != null
        deadlineDays = rule.deadlineDaysBefore ?: 0
    }

    fun rule(today: LocalDate): RepeatRule {
        val deadline = if (deadlineOn) deadlineDays else null
        if (afterCompletion) {
            return RepeatRule(afterCompletion = true, interval = afterInterval, unit = afterUnit, start = today, deadlineDaysBefore = deadline)
        }
        val on = when (unit) {
            RepeatUnit.DAY -> emptyList()
            RepeatUnit.WEEK -> weekdays.sorted().map { RepeatOn(RepeatDay.Weekday(it)) }
            RepeatUnit.MONTH -> entries.map { RepeatOn(it.day) }
            RepeatUnit.YEAR -> entries.toList()
        }
        return RepeatRule(
            afterCompletion = false, interval = interval, unit = unit, start = start, on = on,
            endDate = if (end == RepeatEnd.DATE) endDate else null,
            endCount = if (end == RepeatEnd.COUNT) endCount else null,
            deadlineDaysBefore = deadline
        )
    }
}

/**
 * Окно «Повторять», как в Things: «После выполнения» или «Регулярно» — с интервалом, днями, следующей
 * датой и окончанием; срок и напоминание для каждой копии. Вложенные страницы въезжают сбоку, «Назад» —
 * возвращает. «Сохранить» отдаёт правило и время напоминания.
 */
@Composable
fun ThingsRepeatDialog(
    task: Item,
    // Правило шаблона: окно открывается с ним («Изменить правило»)
    initialRule: RepeatRule? = null,
    onDismissRequest: () -> Unit,
    onSave: (rule: RepeatRule, reminderTime: Int?) -> Unit
) {
    val today = remember { LocalDate.now() }
    val draft = remember(task.id) { RepeatDraft(task, today).also { d -> initialRule?.let { d.load(it) } } }
    var page by remember { mutableStateOf(RepeatPage.MAIN) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val appear by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(ThingsMotion.BASE, easing = FastOutSlowInEasing),
        label = "repeatAppear"
    )
    val view = LocalView.current

    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // Закрытие — штатной анимацией окна, как у окна «Когда»
        (LocalView.current.parent as? DialogWindowProvider)?.window?.setWindowAnimations(R.style.DialogWindowAnimationGrowOpen)
        BackHandler { if (page != RepeatPage.MAIN) page = RepeatPage.MAIN else onDismissRequest() }
        val height = LocalConfiguration.current.screenHeightDp.dp * REPEAT_DIALOG_HEIGHT_FRACTION
        Card(
            shape = ThingsTheme.shapes.dialogShape,
            colors = CardDefaults.cardColors(containerColor = ThingsTheme.colors.overlaySurface),
            modifier = Modifier
                .width(REPEAT_DIALOG_WIDTH)
                .height(height)
                .graphicsLayer {
                    val s = REPEAT_ENTER_SCALE + (1f - REPEAT_ENTER_SCALE) * appear
                    scaleX = s
                    scaleY = s
                    alpha = appear
                }
        ) {
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    val forward = targetState.depth > initialState.depth
                    val enter = slideInHorizontally(tween(ThingsMotion.MEDIUM)) { if (forward) it else -it / 3 } + fadeIn(tween(ThingsMotion.MEDIUM))
                    val exit = slideOutHorizontally(tween(ThingsMotion.MEDIUM)) { if (forward) -it / 3 else it } + fadeOut(tween(ThingsMotion.QUICK))
                    enter togetherWith exit
                },
                label = "repeatPage"
            ) { p ->
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = ThingsSpacing.L, vertical = ThingsSpacing.L)) {
                    val back = { page = RepeatPage.MAIN }
                    when (p) {
                        RepeatPage.MAIN -> MainPage(
                            draft = draft,
                            today = today,
                            onOpen = { page = it },
                            onCancel = onDismissRequest,
                            onSave = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                onSave(draft.rule(today), draft.reminderTime)
                                onDismissRequest()
                            }
                        )
                        RepeatPage.EVERY -> SubPage(stringResource(R.string.repeat_title), back) { IntervalWheels(draft.interval, draft.unit) { n, u -> draft.interval = n; draft.unit = u } }
                        RepeatPage.ON -> SubPage(stringResource(R.string.repeat_title), back) { OnPage(draft) }
                        RepeatPage.NEXT -> SubPage(stringResource(R.string.repeat_next), back) {
                            RepeatCalendar(selected = draft.start, from = today) { draft.start = it; back() }
                        }
                        RepeatPage.ENDS -> SubPage(stringResource(R.string.repeat_ends), back) { EndsPage(draft, today) }
                        RepeatPage.DEADLINE -> SubPage(stringResource(R.string.repeat_deadline), back) { DeadlinePage(draft) }
                        RepeatPage.REMINDER -> SubPage(stringResource(R.string.reminder_title), back) { ReminderPage(draft) }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- главная страница

@Composable
private fun MainPage(draft: RepeatDraft, today: LocalDate, onOpen: (RepeatPage) -> Unit, onCancel: () -> Unit, onSave: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HeaderText(stringResource(R.string.cancel), Modifier.weight(1f), TextAlign.Start, onCancel)
        Text(
            text = stringResource(R.string.repeat_title),
            style = ThingsTheme.type.dialogTitle.copy(color = ThingsTheme.colors.overlayContent),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1.4f)
        )
        HeaderText(stringResource(R.string.tag_dialog_save), Modifier.weight(1f), TextAlign.End, onSave, accent = true)
    }
    Spacer(Modifier.height(ThingsSpacing.L))
    Segmented(
        options = listOf(stringResource(R.string.repeat_mode_after), stringResource(R.string.repeat_mode_regular)),
        selected = if (draft.afterCompletion) 0 else 1,
        onSelect = { draft.afterCompletion = it == 0 }
    )
    Spacer(Modifier.height(ThingsSpacing.M))
    AnimatedContent(
        targetState = draft.afterCompletion,
        transitionSpec = { fadeIn(tween(ThingsMotion.QUICK)) togetherWith fadeOut(tween(ThingsMotion.QUICK)) },
        label = "repeatMode"
    ) { after ->
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            if (after) {
                val unitText = unitText(draft.afterUnit, draft.afterInterval)
                Text(
                    text = buildAnnotatedString {
                        append(stringResource(R.string.repeat_hint_after_before)); append(" ")
                        withStyle(SpanStyle(color = ThingsTheme.colors.accent)) { append(unitText) }
                        append(" "); append(stringResource(R.string.repeat_hint_after_after))
                    },
                    style = ThingsTheme.type.bodyMedium.copy(color = ThingsTheme.colors.overlayContentSecondary)
                )
                Spacer(Modifier.height(ThingsSpacing.S))
                IntervalWheels(draft.afterInterval, draft.afterUnit) { n, u -> draft.afterInterval = n; draft.afterUnit = u }
            } else {
                Text(
                    text = stringResource(R.string.repeat_hint_regular),
                    style = ThingsTheme.type.bodyMedium.copy(color = ThingsTheme.colors.overlayContentSecondary)
                )
                Spacer(Modifier.height(ThingsSpacing.M))
                ValueRow(stringResource(R.string.repeat_every), unitText(draft.unit, draft.interval)) { onOpen(RepeatPage.EVERY) }
                if (draft.unit != RepeatUnit.DAY) {
                    ValueRow(stringResource(R.string.repeat_on), onText(draft)) { onOpen(RepeatPage.ON) }
                }
                RepeatDivider()
                val next = RepeatDates.nextOccurrence(draft.rule(today), today)
                ValueRow(stringResource(R.string.repeat_next), next?.let { dateText(it) } ?: "—") { onOpen(RepeatPage.NEXT) }
                ValueRow(stringResource(R.string.repeat_ends), endsText(draft)) { onOpen(RepeatPage.ENDS) }
            }
            RepeatDivider()
            // Поставленные срок и напоминание — фразой акцентом, как в Things: «Кожна копія матиме термін.
            // Вона з’явиться в «Сьогодні» за 2 дні.», «Кожна копія матиме нагадування о 12:00.»
            if (draft.deadlineOn) {
                SetIconRow(
                    icon = Icons.Outlined.Flag,
                    text = stringResource(R.string.repeat_deadline_each, deadlineText(draft.deadlineDays))
                ) { onOpen(RepeatPage.DEADLINE) }
            } else {
                IconRow(
                    icon = { Icon(Icons.Outlined.Flag, null, tint = ThingsTheme.colors.overlayContentSecondary, modifier = Modifier.size(ThingsIconSize.S)) },
                    label = stringResource(R.string.repeat_add_deadline),
                    value = null
                ) { onOpen(RepeatPage.DEADLINE) }
            }
            val reminder = draft.reminderTime
            if (reminder != null) {
                SetIconRow(
                    icon = AppIcons.Bell,
                    text = stringResource(R.string.repeat_reminder_each, formatReminderTime(LocalContext.current, reminder))
                ) { onOpen(RepeatPage.REMINDER) }
            } else {
                IconRow(
                    icon = { Icon(AppIcons.Bell, null, tint = ThingsTheme.colors.overlayContentSecondary, modifier = Modifier.size(ThingsIconSize.S)) },
                    label = stringResource(R.string.ui_add_reminder),
                    value = null
                ) { onOpen(RepeatPage.REMINDER) }
            }
        }
    }
}

// ---------------------------------------------------------------- вложенные страницы

@Composable
internal fun ColumnScope.SubPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onBack() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ChevronLeft, null, tint = ThingsTheme.colors.overlayContent, modifier = Modifier.size(ThingsIconSize.M))
            Text(stringResource(R.string.cd_back), style = ThingsTheme.type.dialogRow.copy(color = ThingsTheme.colors.overlayContent))
        }
        Text(
            text = title,
            style = ThingsTheme.type.dialogTitle.copy(color = ThingsTheme.colors.overlayContent),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1.4f)
        )
        Spacer(Modifier.weight(1f))
    }
    Spacer(Modifier.height(ThingsSpacing.L))
    content()
}

/** «Каждые N дней / недель / месяцев / лет»: число и единица; единица склоняется по числу */
@Composable
private fun IntervalWheels(interval: Int, unit: RepeatUnit, onChange: (Int, RepeatUnit) -> Unit) {
    val numbers = remember { (1..REPEAT_MAX_INTERVAL).map { it.toString() } }
    val numberState = rememberWheelState(interval - 1, numbers.size, cyclic = false)
    val unitState = rememberWheelState(unit.ordinal, RepeatUnit.entries.size, cyclic = false)
    val n = numberState.selected + 1
    val units = RepeatUnit.entries.map { unitName(it, n) }
    LaunchedEffect(numberState, unitState) {
        snapshotFlow { numberState.selected to unitState.selected }.drop(1)
            .collect { (ni, ui) -> onChange(ni + 1, RepeatUnit.entries[ui]) }
    }
    WheelBox(listOf(Triple(numbers, numberState, REPEAT_WHEEL_NUMBER_WIDTH), Triple(units, unitState, REPEAT_WHEEL_WORD_WIDTH)))
}

/**
 * «В»: у еженедельных — дни недели с галочками; у ежемесячных и ежегодных — дни списком и барабан,
 * «Добавить ещё» — внизу окна, как в Things
 */
@Composable
private fun ColumnScope.OnPage(draft: RepeatDraft) {
    if (draft.unit == RepeatUnit.WEEK) {
        val view = LocalView.current
        DayOfWeek.entries.forEach { dow ->
            val checked = dow.value in draft.weekdays
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MaterialTheme.dimens.rowHeight)
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        if (checked) {
                            if (draft.weekdays.size > 1) draft.weekdays.remove(dow.value)
                        } else draft.weekdays.add(dow.value)
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dow.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()).replaceFirstChar { it.titlecase() },
                    style = ThingsTheme.type.dialogRow.copy(color = ThingsTheme.colors.overlayContent),
                    modifier = Modifier.weight(1f)
                )
                if (checked) Icon(Icons.Default.Check, null, tint = ThingsTheme.colors.accent, modifier = Modifier.size(ThingsIconSize.S))
            }
            RepeatDivider()
        }
        return
    }
    val yearly = draft.unit == RepeatUnit.YEAR
    draft.entries.forEachIndexed { index, entry ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MaterialTheme.dimens.rowHeight)
                .clickable { draft.selectedEntry = index },
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .size(ThingsIconSize.M)
                        .clip(CircleShape)
                        .background(ThingsTheme.colors.danger)
                        .clickable {
                            draft.entries.removeAt(index)
                            draft.selectedEntry = draft.selectedEntry.coerceAtMost(draft.entries.lastIndex)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Remove, stringResource(R.string.cd_repeat_remove_day), tint = ThingsTheme.colors.overlayContent, modifier = Modifier.size(ThingsIconSize.XS))
                }
                Spacer(Modifier.width(ThingsSpacing.S))
            }
            Text(
                text = stringResource(if (index == 0) R.string.repeat_on else R.string.repeat_and_on),
                style = ThingsTheme.type.dialogRow.copy(color = ThingsTheme.colors.overlayContent),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = entryText(entry, yearly),
                style = ThingsTheme.type.dialogRow.copy(
                    color = ThingsTheme.colors.accent
                )
            )
        }
    }
    // Барабан правит выбранную строку: порядковый номер, «день» или день недели, месяц у ежегодных
    val entry = draft.entries.getOrNull(draft.selectedEntry) ?: return
    key(draft.selectedEntry) {
        val (ordinalIndex, kindIndex) = wheelIndices(entry.day)
        val ordinalState = rememberWheelState(ordinalIndex, REPEAT_ORDINALS, cyclic = false)
        val kindState = rememberWheelState(kindIndex, 1 + DayOfWeek.entries.size, cyclic = false)
        val monthState = rememberWheelState((entry.month ?: 1) - 1, Month.entries.size, cyclic = false)
        val ordinals = (1..REPEAT_ORDINALS - 1).map { ordinalText(it) } + stringResource(R.string.repeat_last)
        val kinds = listOf(stringResource(R.string.repeat_day_word)) +
            DayOfWeek.entries.map { it.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()) }
        val months = Month.entries.map { it.getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault()) }
        LaunchedEffect(ordinalState, kindState, monthState) {
            snapshotFlow { Triple(ordinalState.selected, kindState.selected, monthState.selected) }.drop(1).collect { (o, k, m) ->
                // У дня недели номер — только 1..4 или «последний»
                if (k > 0 && o in REPEAT_MAX_WEEKDAY_ORDINAL until REPEAT_LAST_INDEX) {
                    ordinalState.animateTo(REPEAT_MAX_WEEKDAY_ORDINAL - 1)
                    return@collect
                }
                val day = if (k == 0) {
                    RepeatDay.OfMonth(if (o == REPEAT_LAST_INDEX) RepeatDay.LAST else o + 1)
                } else {
                    RepeatDay.Weekday(k, if (o == REPEAT_LAST_INDEX) RepeatDay.LAST else o + 1)
                }
                val i = draft.selectedEntry
                if (i in draft.entries.indices) draft.entries[i] = RepeatOn(day, if (yearly) m + 1 else entry.month)
            }
        }
        val columns = buildList {
            add(Triple(ordinals, ordinalState, REPEAT_WHEEL_NUMBER_WIDTH))
            add(Triple(kinds, kindState, if (yearly) REPEAT_WHEEL_KIND_YEARLY_WIDTH else REPEAT_WHEEL_WORD_WIDTH))
            if (yearly) add(Triple(months, monthState, REPEAT_WHEEL_MONTH_WIDTH))
        }
        WheelBox(columns)
    }
    Spacer(Modifier.height(ThingsSpacing.M))
    Spacer(Modifier.weight(1f))
    PillButton(stringResource(R.string.repeat_add_more)) {
        draft.entries.add(draft.entries[draft.selectedEntry])
        draft.selectedEntry = draft.entries.lastIndex
    }
}

/** Окончание: «Никогда», «Дата» — календарь, «После» — барабан числа повторов */
@Composable
private fun EndsPage(draft: RepeatDraft, today: LocalDate) {
    Segmented(
        options = listOf(stringResource(R.string.repeat_never), stringResource(R.string.repeat_on_date), stringResource(R.string.repeat_after)),
        selected = draft.end.ordinal,
        onSelect = { draft.end = RepeatEnd.entries[it] }
    )
    Spacer(Modifier.height(ThingsSpacing.M))
    when (draft.end) {
        RepeatEnd.NEVER -> Unit
        RepeatEnd.DATE -> RepeatCalendar(selected = draft.endDate, from = maxOf(today, draft.start)) { draft.endDate = it }
        RepeatEnd.COUNT -> {
            val counts = (1..REPEAT_MAX_COUNT).map { pluralStringResource(R.plurals.repeat_repetitions, it, it) }
            val state = rememberWheelState(draft.endCount - 1, counts.size, cyclic = false)
            LaunchedEffect(state) { snapshotFlow { state.selected }.drop(1).collect { draft.endCount = it + 1 } }
            WheelBox(listOf(Triple(counts, state, REPEAT_WHEEL_WORD_WIDTH + REPEAT_WHEEL_NUMBER_WIDTH)))
        }
    }
}

/** Срок у каждой копии: в день повтора, а сама копия появляется за выбранное число дней */
@Composable
private fun DeadlinePage(draft: RepeatDraft) {
    ToggleRow(stringResource(R.string.repeat_add_deadline), draft.deadlineOn) { draft.deadlineOn = it }
    Spacer(Modifier.height(ThingsSpacing.S))
    Text(
        text = buildAnnotatedString {
            append(stringResource(R.string.repeat_deadline_hint)); append(" ")
            withStyle(SpanStyle(color = ThingsTheme.colors.accent)) { append(deadlineText(draft.deadlineDays)) }
            append(".")
        },
        style = ThingsTheme.type.bodyMedium.copy(color = ThingsTheme.colors.overlayContentSecondary)
    )
    if (draft.deadlineOn) {
        Spacer(Modifier.height(ThingsSpacing.S))
        val options = (0..REPEAT_MAX_DEADLINE_DAYS).map { deadlineText(it) }
        val state = rememberWheelState(draft.deadlineDays, options.size, cyclic = false)
        LaunchedEffect(state) { snapshotFlow { state.selected }.drop(1).collect { draft.deadlineDays = it } }
        WheelBox(listOf(Triple(options, state, REPEAT_WHEEL_WORD_WIDTH + REPEAT_WHEEL_NUMBER_WIDTH)))
    }
}

/** Напоминание у каждой копии — время дня */
@Composable
private fun ReminderPage(draft: RepeatDraft) {
    val context = LocalContext.current
    val is24 = remember { android.text.format.DateFormat.is24HourFormat(context) }
    val initial = draft.reminderTime ?: com.example.domain.reminders.Reminders.defaultTime()
    ToggleRow(stringResource(R.string.ui_add_reminder), draft.reminderTime != null) { on ->
        draft.reminderTime = if (on) initial else null
    }
    if (draft.reminderTime != null) {
        Spacer(Modifier.height(ThingsSpacing.M))
        val time = draft.reminderTime ?: initial
        val rounded = (time / REPEAT_MINUTE_STEP) * REPEAT_MINUTE_STEP
        val hours = if (is24) (0..23).map { "%02d".format(it) } else (1..12).map { it.toString() }
        val minutes = (0 until 60 step REPEAT_MINUTE_STEP).map { "%02d".format(it) }
        val hourState = rememberWheelState(if (is24) rounded / 60 else (rounded / 60 + 11) % 12, hours.size, cyclic = true)
        val minuteState = rememberWheelState((rounded % 60) / REPEAT_MINUTE_STEP, minutes.size, cyclic = true)
        val periodState = rememberWheelState(if (rounded / 60 >= 12) 1 else 0, 2, cyclic = false)
        LaunchedEffect(hourState, minuteState, periodState) {
            snapshotFlow { Triple(hourState.selected, minuteState.selected, periodState.selected) }.drop(1).collect { (h, m, pm) ->
                val h24 = if (is24) h else (h + 1) % 12 + if (pm == 1) 12 else 0
                draft.reminderTime = h24 * 60 + m * REPEAT_MINUTE_STEP
            }
        }
        val columns = buildList {
            add(Triple(hours, hourState, REPEAT_WHEEL_NUMBER_WIDTH))
            add(Triple(minutes, minuteState, REPEAT_WHEEL_NUMBER_WIDTH))
            if (!is24) add(Triple(listOf("AM", "PM"), periodState, REPEAT_WHEEL_NUMBER_WIDTH))
        }
        WheelBox(columns)
    }
}

// ---------------------------------------------------------------- календарь

/**
 * Календарь в стиле окна «Когда», как в Things: недели подряд с понедельника; дни до [from] пустые,
 * у первого дня и у первых чисел — подпись месяца, месяцы разделены ступенчатой линией; выбранный день обведён.
 */
@Composable
internal fun RepeatCalendar(selected: LocalDate?, from: LocalDate, modifier: Modifier = Modifier, onPick: (LocalDate) -> Unit) {
    val view = LocalView.current
    val firstMonday = from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val selectedWeek = selected?.let { java.time.temporal.ChronoUnit.WEEKS.between(firstMonday, it.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))).toInt() } ?: 0
    val lineColor = ThingsTheme.colors.overlayDivider
    Row(modifier = Modifier.fillMaxWidth()) {
        DayOfWeek.entries.forEach {
            Text(
                text = it.getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault()).replaceFirstChar { c -> c.titlecase() },
                style = ThingsTheme.type.subhead.copy(color = ThingsTheme.colors.overlayContentSecondary),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
    LazyColumn(modifier = modifier, state = rememberLazyListState(initialFirstVisibleItemIndex = maxOf(0, selectedWeek - 1))) {
        items(REPEAT_CALENDAR_WEEKS) { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    val date = firstMonday.plusWeeks(week.toLong()).plusDays(i.toLong())
                    if (date.isBefore(from)) {
                        Spacer(Modifier.weight(1f).height(REPEAT_CALENDAR_CELL_HEIGHT))
                        continue
                    }
                    val isSelected = date == selected
                    // Граница месяца: линия сверху, если над днём — другой месяц, и слева, если слева — другой
                    val above = date.minusWeeks(1)
                    val lineTop = !above.isBefore(from) && above.month != date.month
                    val lineStart = i > 0 && !date.minusDays(1).isBefore(from) && date.minusDays(1).month != date.month
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(REPEAT_CALENDAR_CELL_HEIGHT)
                            .drawBehind {
                                val w = ThingsStroke.THIN.toPx()
                                if (lineTop) drawLine(lineColor, Offset(0f, w / 2), Offset(size.width, w / 2), w)
                                if (lineStart) drawLine(lineColor, Offset(w / 2, 0f), Offset(w / 2, size.height), w)
                            }
                            .clip(ThingsTheme.shapes.smallShape)
                            .then(if (isSelected) Modifier.border(ThingsStroke.FOCUS, ThingsTheme.colors.accent, ThingsTheme.shapes.smallShape) else Modifier)
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onPick(date)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val color = ThingsTheme.colors.overlayContent
                        if (date.dayOfMonth == 1 || date == from) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(date.month.getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault()), style = ThingsTheme.type.bodySmall.copy(color = color))
                                Text(date.dayOfMonth.toString(), style = ThingsTheme.type.bodySmall.copy(color = color))
                            }
                        } else {
                            Text(date.dayOfMonth.toString(), style = ThingsTheme.type.dialogRow.copy(color = color))
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- общие части окна

@Composable
private fun HeaderText(text: String, modifier: Modifier, align: TextAlign, onClick: () -> Unit, accent: Boolean = false) {
    Text(
        text = text,
        style = ThingsTheme.type.dialogRow.copy(color = if (accent) ThingsTheme.colors.accent else ThingsTheme.colors.overlayContent),
        textAlign = align,
        modifier = modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
    )
}

/** Переключатель вариантов, как в Things: плашка с выделенным сегментом */
@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val view = LocalView.current
    val selectedColor = ThingsTheme.colors.overlayContent.copy(alpha = ThingsAlpha.SUBTLE).compositeOver(ThingsTheme.colors.overlayControl)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ThingsTheme.shapes.rowShape)
            .background(ThingsTheme.colors.overlayControl)
            .padding(ThingsSpacing.XXS),
        horizontalArrangement = Arrangement.spacedBy(ThingsSpacing.XXS)
    ) {
        options.forEachIndexed { index, label ->
            val bg by animateColorAsState(
                if (index == selected) selectedColor else ThingsTheme.colors.overlayControl,
                tween(ThingsMotion.QUICK), label = "segment"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(ThingsTheme.shapes.rowShape)
                    .background(bg)
                    .clickable {
                        if (index != selected) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onSelect(index)
                    }
                    .padding(vertical = ThingsSpacing.S),
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = ThingsTheme.type.bodyMedium.copy(color = ThingsTheme.colors.overlayContent))
            }
        }
    }
}

/** Строка «подпись — значение ›»: значение акцентным цветом, нажатие открывает страницу */
@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MaterialTheme.dimens.rowHeight)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = ThingsTheme.type.dialogRow.copy(color = ThingsTheme.colors.overlayContent))
        Spacer(Modifier.width(ThingsSpacing.M))
        Text(
            text = value,
            style = ThingsTheme.type.dialogRow.copy(color = ThingsTheme.colors.accent),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Default.ChevronRight, null, tint = ThingsTheme.colors.accent, modifier = Modifier.size(ThingsIconSize.S))
    }
}

/** Строка с иконкой внизу окна: «Добавить срок», «Добавить напоминание» или поставленное значение */
@Composable
private fun IconRow(icon: @Composable () -> Unit, label: String, value: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MaterialTheme.dimens.rowHeight)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(ThingsSpacing.S))
        Text(
            text = label,
            style = ThingsTheme.type.dialogRow.copy(color = if (value != null) ThingsTheme.colors.overlayContent else ThingsTheme.colors.overlayContentSecondary),
            modifier = Modifier.weight(1f)
        )
        if (value != null) Text(value, style = ThingsTheme.type.dialogRow.copy(color = ThingsTheme.colors.accent))
        Icon(
            Icons.Default.ChevronRight, null,
            tint = if (value != null) ThingsTheme.colors.accent else ThingsTheme.colors.overlayContentSecondary,
            modifier = Modifier.size(ThingsIconSize.S)
        )
    }
}

/**
 * Поставленный срок или напоминание: значок и фраза акцентом, с переносом строк, чтобы фраза помещалась
 * целиком; значок — по первой строке, «›» — по центру, тоже акцентом
 */
@Composable
private fun SetIconRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.dimens.rowHeight)
            .clickable { onClick() }
            .padding(vertical = ThingsSpacing.S),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null,
            tint = ThingsTheme.colors.accent,
            modifier = Modifier
                .align(Alignment.Top)
                .padding(top = ThingsSpacing.XXS)
                .size(ThingsIconSize.S)
        )
        Spacer(Modifier.width(ThingsSpacing.S))
        Text(
            text = text,
            style = ThingsTheme.type.bodyLarge.copy(color = ThingsTheme.colors.accent),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(ThingsSpacing.S))
        Icon(Icons.Default.ChevronRight, null, tint = ThingsTheme.colors.accent, modifier = Modifier.size(ThingsIconSize.S))
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(MaterialTheme.dimens.rowHeight), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = ThingsTheme.type.dialogRow.copy(color = ThingsTheme.colors.overlayContent), modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = ThingsTheme.colors.accent,
                checkedThumbColor = ThingsTheme.colors.onAccent,
                uncheckedTrackColor = ThingsTheme.colors.overlayControl,
                uncheckedThumbColor = ThingsTheme.colors.overlayContentSecondary,
                uncheckedBorderColor = ThingsTheme.colors.overlayControl
            )
        )
    }
}

@Composable
private fun PillButton(text: String, onClick: () -> Unit) {
    val view = LocalView.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MaterialTheme.dimens.rowHeight)
            .clip(ThingsTheme.shapes.rowShape)
            .background(ThingsTheme.colors.overlayControl)
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = ThingsTheme.type.dialogButton.copy(color = ThingsTheme.colors.overlayContent))
    }
}

@Composable
private fun RepeatDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = ThingsSpacing.XS), color = ThingsTheme.colors.overlayDivider, thickness = ThingsStroke.THIN)
}

/**
 * Барабаны рядом на карточке, как у напоминания в окне «Когда»: те же скругления, поля и полоса выбора —
 * две линии во всю ширину карточки
 */
@Composable
private fun WheelBox(columns: List<Triple<List<String>, WheelState, androidx.compose.ui.unit.Dp>>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ThingsTheme.shapes.dialogShape)
            .background(ThingsTheme.colors.overlayPopover)
            .padding(ThingsSpacing.M)
            .height(WHEEL_HEIGHT),
        contentAlignment = Alignment.Center
    ) {
        com.example.ui.components.WheelSelectionLines()
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            columns.forEach { (items, state, width) -> Wheel(items, state, width) }
        }
    }
}

// ---------------------------------------------------------------- тексты значений

@Composable
private fun unitName(unit: RepeatUnit, n: Int): String = pluralStringResource(
    when (unit) {
        RepeatUnit.DAY -> R.plurals.repeat_unit_day
        RepeatUnit.WEEK -> R.plurals.repeat_unit_week
        RepeatUnit.MONTH -> R.plurals.repeat_unit_month
        RepeatUnit.YEAR -> R.plurals.repeat_unit_year
    },
    n
)

/** «неделя», «2 недели» */
@Composable
private fun unitText(unit: RepeatUnit, n: Int): String = if (n == 1) unitName(unit, 1) else "$n ${unitName(unit, n)}"

/** Порядковый номер: «1-й» или по-английски «1st» */
private fun ordinalText(n: Int): String {
    if (Locale.getDefault().language != "en") return "$n-й"
    val suffix = if (n % 100 in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }
    return "$n$suffix"
}

@Composable
private fun entryText(entry: RepeatOn, yearly: Boolean): String {
    val day = when (val d = entry.day) {
        is RepeatDay.OfMonth ->
            if (d.day == RepeatDay.LAST) stringResource(R.string.repeat_last_day) else stringResource(R.string.repeat_nth_day, ordinalText(d.day))
        is RepeatDay.Weekday -> {
            val name = DayOfWeek.of(d.dayOfWeek).getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())
            val ordinal = if (d.ordinal == RepeatDay.LAST) stringResource(R.string.repeat_last) else ordinalText(d.ordinal)
            "$ordinal $name"
        }
    }
    val month = entry.month?.takeIf { yearly }?.let { Month.of(it).getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault()) }
    return if (month != null) "$day, $month" else day
}

@Composable
private fun onText(draft: RepeatDraft): String = when (draft.unit) {
    RepeatUnit.WEEK -> draft.weekdays.sorted().joinToString(", ") { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault()) }
    else -> draft.entries.map { entryText(it, draft.unit == RepeatUnit.YEAR) }.joinToString("; ")
}

private fun dateText(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault()))

@Composable
private fun endsText(draft: RepeatDraft): String = when (draft.end) {
    RepeatEnd.NEVER -> stringResource(R.string.repeat_never)
    RepeatEnd.DATE -> dateText(draft.endDate)
    RepeatEnd.COUNT -> pluralStringResource(R.plurals.repeat_ends_after, draft.endCount, draft.endCount)
}

@Composable
private fun deadlineText(days: Int): String =
    if (days == 0) stringResource(R.string.repeat_same_day) else pluralStringResource(R.plurals.repeat_days_earlier, days, days)

/** Положение барабана «В» для дня: номер (0..30, 31 — «последний») и вид (0 — число месяца, 1..7 — день недели) */
private fun wheelIndices(day: RepeatDay): Pair<Int, Int> = when (day) {
    is RepeatDay.OfMonth -> (if (day.day == RepeatDay.LAST) REPEAT_LAST_INDEX else day.day - 1) to 0
    is RepeatDay.Weekday -> (if (day.ordinal == RepeatDay.LAST) REPEAT_LAST_INDEX else (day.ordinal.coerceAtLeast(1) - 1)) to day.dayOfWeek
}

@Composable
private fun key(value: Any, content: @Composable () -> Unit) = androidx.compose.runtime.key(value) { content() }

/**
 * Краткое описание правила для редактора шаблона: «Щотижня: ср», «Кожні 2 місяці: 1-й пн»,
 * «Через тиждень після виконання».
 */
@Composable
fun repeatRuleSummary(rule: RepeatRule): String {
    if (rule.afterCompletion) return stringResource(R.string.repeat_summary_after, unitText(rule.unit, rule.interval))
    val every = if (rule.interval == 1) {
        stringResource(
            when (rule.unit) {
                RepeatUnit.DAY -> R.string.repeat_every_one_day
                RepeatUnit.WEEK -> R.string.repeat_every_one_week
                RepeatUnit.MONTH -> R.string.repeat_every_one_month
                RepeatUnit.YEAR -> R.string.repeat_every_one_year
            }
        )
    } else "${stringResource(R.string.repeat_every)} ${rule.interval} ${unitName(rule.unit, rule.interval)}"
    val on = when (rule.unit) {
        RepeatUnit.DAY -> ""
        RepeatUnit.WEEK -> rule.on.mapNotNull { (it.day as? RepeatDay.Weekday)?.dayOfWeek }
            .joinToString(", ") { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault()) }
        else -> rule.on.map { entryText(it, rule.unit == RepeatUnit.YEAR) }.joinToString("; ")
    }
    return if (on.isEmpty()) every else "$every: $on"
}
