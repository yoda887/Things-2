package com.example.ui.screens.home.subcomponents

import com.example.domain.edits.ProjectEdit
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.example.ui.theme.ThingsStroke
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.R
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.Tag
import com.example.data.model.TaskSection
import com.example.ui.components.ThingsConfirmDialog
import com.example.ui.screens.home.inlineeditor.DeadlineDatePickerDialog
import com.example.ui.screens.home.inlineeditor.dialogs.ThingsMoveDialog
import com.example.ui.screens.home.inlineeditor.dialogs.ThingsTagDialog
import com.example.ui.screens.home.inlineeditor.dialogs.ThingsWhenDialog
import com.example.ui.screens.home.inlineeditor.utils.formatStartDateLabel
import com.example.ui.screens.home.inlineeditor.utils.isTodayDateOrPast
import com.example.ui.screens.home.inlineeditor.utils.relativeDueText
import com.example.ui.theme.AppIcons
import com.example.ui.theme.ThingsIconSize
import com.example.ui.theme.ThingsMotion
import com.example.ui.theme.ThingsSpacing
import com.example.ui.theme.ThingsTheme
import com.example.ui.theme.dimens
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * Выбранное в «Когда» для проекта — по сохранённым полям старта. [Item.section] здесь не годится: это
 * раздел списка, и дедлайн на сегодня переносит туда даже проект «Когда-нибудь».
 */
private val Item.startSection: TaskSection
    get() = when {
        start == Item.START_SOMEDAY -> TaskSection.SOMEDAY
        startDate != null -> TaskSection.UPCOMING
        start == Item.START_TODAY -> TaskSection.TODAY
        else -> TaskSection.ANYTIME
    }

/** Пункты меню опций проекта, которые открывают окно или выполняют действие над проектом. */
enum class ProjectAction { COMPLETE, WHEN, TAGS, DEADLINE, MOVE, DUPLICATE, SHARE }

/** Дней от сегодняшнего до [dueDate] по календарным дням; отрицательное — просрочено. */
private fun deadlineDeltaDays(dueDate: Long): Int {
    fun Calendar.startOfDay() = apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val today = Calendar.getInstance().startOfDay()
    val due = Calendar.getInstance().apply { timeInMillis = dueDate }.startOfDay()
    var days = 0
    val step = if (due.before(today)) -1 else 1
    while (today.get(Calendar.YEAR) != due.get(Calendar.YEAR) || today.get(Calendar.DAY_OF_YEAR) != due.get(Calendar.DAY_OF_YEAR)) {
        today.add(Calendar.DAY_OF_YEAR, step)
        days += step
    }
    return days
}

/**
 * Свойства проекта под его названием, как в Things: теги, «Когда», дедлайн и заметки.
 * Теги, «Когда» и дедлайн видны, только когда заданы; нажатие на них открывает своё окно.
 * Заметки правятся прямо здесь и сохраняются после паузы в наборе и при уходе с экрана.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectDetailsBlock(
    project: Item,
    textPrimaryColor: Color,
    onAction: (ProjectAction) -> Unit,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    // Строки свойств — только заданные; между ними, над первой и под последней — тонкие разделители
    val section = project.startSection
    val startDate = project.startDate
    val hasStart = startDate != null || section == TaskSection.TODAY || section == TaskSection.SOMEDAY
    val dueDate = project.dueDate
    val rows = buildList<@Composable () -> Unit> {
        if (project.tags.isNotEmpty()) add { ProjectTagsRow(project.tags) { onAction(ProjectAction.TAGS) } }
        if (hasStart) add { ProjectStartRow(project, textPrimaryColor) { onAction(ProjectAction.WHEN) } }
        if (dueDate != null) add { ProjectDeadlineRow(dueDate) { onAction(ProjectAction.DEADLINE) } }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        rows.forEach { row ->
            ProjectDetailsDivider()
            row()
        }
        if (rows.isNotEmpty()) ProjectDetailsDivider()
        ProjectNotesField(project = project, onNotesChange = onNotesChange)
        // Заметки отделены от списка задач заметным промежутком
        Spacer(modifier = Modifier.height(ThingsSpacing.XL))
    }
}

@Composable
private fun ProjectDetailsDivider() {
    HorizontalDivider(color = ThingsTheme.colors.divider, thickness = ThingsStroke.DIVIDER)
}

/** Нажатие на строку свойства — с тактильным откликом, как в редакторе задачи. */
@Composable
private fun Modifier.detailsRowClick(onClick: () -> Unit): Modifier {
    val view = LocalView.current
    return clickable {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        onClick()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectTagsRow(tags: List<String>, onClick: () -> Unit) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .detailsRowClick(onClick)
            .padding(vertical = ThingsSpacing.S_PLUS),
        horizontalArrangement = Arrangement.spacedBy(ThingsSpacing.S),
        verticalArrangement = Arrangement.spacedBy(ThingsSpacing.XS_PLUS)
    ) {
        tags.forEach { tag ->
            Box(
                modifier = Modifier
                    .clip(ThingsTheme.shapes.chipShape)
                    .background(ThingsTheme.colors.tagChipBackground)
                    .padding(horizontal = ThingsSpacing.S_PLUS, vertical = ThingsSpacing.XS)
            ) {
                Text(text = tag, style = ThingsTheme.type.tagChip.copy(color = ThingsTheme.colors.tagChipText))
            }
        }
    }
}

/** «Когда»: сегодня, вечер, дата или «Когда-нибудь». */
@Composable
private fun ProjectStartRow(project: Item, textPrimaryColor: Color, onClick: () -> Unit) {
    val section = project.startSection
    val startDate = project.startDate
    val isToday = (startDate != null && isTodayDateOrPast(startDate)) || (startDate == null && section == TaskSection.TODAY)
    val icon = when {
        section == TaskSection.SOMEDAY -> AppIcons.Someday
        isToday -> if (project.isTonight) AppIcons.Evening else AppIcons.Today
        else -> AppIcons.Upcoming
    }
    val tint = when {
        section == TaskSection.SOMEDAY -> ThingsTheme.colors.someday
        isToday && !project.isTonight -> ThingsTheme.colors.today
        else -> Color.Unspecified
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .detailsRowClick(onClick)
            .padding(vertical = ThingsSpacing.S_PLUS)
    ) {
        Icon(imageVector = icon, contentDescription = stringResource(R.string.cd_change_date), tint = tint, modifier = Modifier.size(ThingsIconSize.S))
        Spacer(modifier = Modifier.width(ThingsSpacing.XS_PLUS))
        Text(
            text = formatStartDateLabel(startDate, section, project.isTonight),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = ThingsTheme.type.editorDateStrong.copy(color = textPrimaryColor)
        )
    }
}

/** Дедлайн: дата и сколько осталось; сегодня и просроченный — цветом danger. */
@Composable
private fun ProjectDeadlineRow(dueDate: Long, onClick: () -> Unit) {
    val delta = remember(dueDate) { deadlineDeltaDays(dueDate) }
    val dateText = remember(dueDate) {
        java.text.SimpleDateFormat("EEE, d MMMM", java.util.Locale.getDefault())
            .format(java.util.Date(dueDate)).lowercase()
    }
    val color = if (delta <= 0) ThingsTheme.colors.danger else ThingsTheme.colors.editorText
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .detailsRowClick(onClick)
            .padding(vertical = ThingsSpacing.S_PLUS)
    ) {
        Icon(imageVector = AppIcons.Deadline, contentDescription = stringResource(R.string.cd_deadline_flag), tint = color, modifier = Modifier.size(ThingsIconSize.S))
        Spacer(modifier = Modifier.width(ThingsSpacing.XS_PLUS))
        Text(text = dateText, maxLines = 1, style = ThingsTheme.type.editorDateStrong.copy(color = color))
        Spacer(modifier = Modifier.width(ThingsSpacing.XS))
        Text(
            text = relativeDueText(delta),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = ThingsTheme.type.editorDate.copy(color = ThingsTheme.colors.textSecondary)
        )
    }
}

/** Заметки проекта: правятся на месте, с подсказкой «Нотатки», пока пусто. */
@Composable
private fun ProjectNotesField(project: Item, onNotesChange: (String) -> Unit) {
    var notes by remember(project.id) { mutableStateOf(project.notes) }
    var focused by remember { mutableStateOf(false) }
    val latestNotes by rememberUpdatedState(notes)
    val savedNotes by rememberUpdatedState(project.notes)
    val save by rememberUpdatedState(onNotesChange)

    // Правка извне (синхронизация) подхватывается, пока поле не в работе
    LaunchedEffect(project.notes) { if (!focused) notes = project.notes }
    // Сохранение после паузы в наборе
    LaunchedEffect(notes) {
        if (notes == project.notes) return@LaunchedEffect
        delay(ThingsMotion.LONG.toLong())
        save(notes)
    }
    // …и при уходе с экрана, если пауза не успела закончиться
    DisposableEffect(project.id) {
        onDispose { if (latestNotes != savedNotes) save(latestNotes) }
    }

    val style = ThingsTheme.type.editorNotes.copy(color = ThingsTheme.colors.editorText)
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = ThingsSpacing.S)) {
        if (notes.isEmpty()) {
            Text(text = stringResource(R.string.ui_notes), style = style.copy(color = ThingsTheme.colors.textSecondary))
        }
        BasicTextField(
            value = notes,
            onValueChange = { notes = it },
            textStyle = style,
            cursorBrush = SolidColor(ThingsTheme.colors.accent),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    focused = it.isFocused
                    if (!it.isFocused && notes != project.notes) save(notes)
                }
        )
    }
}

/**
 * Окна пунктов меню проекта: «Когда», теги, дедлайн, перемещение в область и подтверждение
 * завершения. Каждое сообщает правку через [onEditProject] и закрывается через [onClose].
 */
@Composable
fun ProjectDialogs(
    project: Item,
    openDialog: ProjectAction?,
    onClose: () -> Unit,
    allTasks: List<ItemWithChecklist>,
    projects: List<Item>,
    areas: List<Area>,
    allSavedTags: List<String>,
    allSavedTagObjects: List<Tag>,
    onNewTagCreated: (String, String?) -> Unit,
    onDeleteTag: (Tag) -> Unit,
    onUpdateTag: (Tag) -> Unit,
    onUpdateTagsOrder: (List<Tag>) -> Unit,
    onEditProject: (ProjectEdit) -> Unit,
    onCompleteProject: (Item) -> Unit
) {
    when (openDialog) {
        ProjectAction.WHEN -> {
            var startDate by remember(project.id) { mutableStateOf(project.startDate) }
            var section by remember(project.id) { mutableStateOf(project.startSection) }
            var isTonight by remember(project.id) { mutableStateOf(project.isTonight) }
            ThingsWhenDialog(
                startDate = startDate,
                onStartDateChange = { startDate = it },
                section = section,
                onSectionChange = { section = it },
                isTonight = isTonight,
                onIsTonightChange = { isTonight = it },
                onShowCalendarHelperChange = { },
                onDismissRequest = {
                    onEditProject(ProjectEdit.Start(section, startDate, isTonight))
                    onClose()
                }
            )
        }
        ProjectAction.TAGS -> ThingsTagDialog(
            activeTags = project.tags,
            allSavedTags = allSavedTags,
            allSavedTagObjects = allSavedTagObjects,
            onNewTagCreated = onNewTagCreated,
            onDeleteTag = onDeleteTag,
            onUpdateTag = onUpdateTag,
            onUpdateTagsOrder = onUpdateTagsOrder,
            onTagsSelected = { selected -> onEditProject(ProjectEdit.Tags(selected)) },
            onDismissRequest = onClose
        )
        ProjectAction.DEADLINE -> DeadlineDatePickerDialog(
            initialSelectedDateMillis = project.dueDate,
            onDateSelected = { date -> onEditProject(ProjectEdit.Deadline(date)) },
            onDismiss = onClose
        )
        ProjectAction.MOVE -> ThingsMoveDialog(
            currentProjectId = null,
            currentAreaId = project.areaId,
            currentIsInbox = false,
            projects = projects,
            areas = areas.filter { !it.trashed },
            allTasks = allTasks,
            onMove = { _, areaId, _ ->
                onEditProject(ProjectEdit.MoveToArea(areaId))
                onClose()
            },
            onDismissRequest = onClose,
            areasOnly = true
        )
        ProjectAction.COMPLETE -> {
            val openCount = allTasks.count { it.item.projectId == project.id && it.item.status == Item.STATUS_OPEN }
            ThingsConfirmDialog(
                title = stringResource(R.string.project_complete_title),
                message = pluralStringResource(R.plurals.project_complete_message_tasks, openCount, project.title, openCount),
                confirmText = stringResource(R.string.project_complete),
                dismissText = stringResource(R.string.cancel),
                onConfirm = {
                    onCompleteProject(project)
                    onClose()
                },
                onDismiss = onClose
            )
        }
        else -> Unit
    }
}

/** Текст проекта для «Поделиться»: название, заметки и открытые задачи по заголовкам. */
fun projectShareText(project: Item, headings: List<Item>, tasks: List<ItemWithChecklist>): String = buildString {
    append(project.title)
    if (project.notes.isNotBlank()) append("\n\n").append(project.notes.trim())
    val open = tasks.filter { it.item.projectId == project.id && it.item.status == Item.STATUS_OPEN }
    val loose = open.filter { it.item.headingId == null || headings.none { h -> h.id == it.item.headingId } }
    if (loose.isNotEmpty()) {
        append("\n")
        loose.forEach { append("\n• ").append(it.item.title) }
    }
    headings.filter { it.projectId == project.id }.forEach { heading ->
        val under = open.filter { it.item.headingId == heading.id }
        if (under.isNotEmpty()) {
            append("\n\n").append(heading.title)
            under.forEach { append("\n• ").append(it.item.title) }
        }
    }
}
