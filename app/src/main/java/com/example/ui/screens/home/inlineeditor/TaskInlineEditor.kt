package com.example.ui.screens.home.inlineeditor

import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import com.example.ui.viewmodel.ActiveScreen
import com.example.ui.theme.ThingsElevation
import com.example.ui.theme.ThingsSpacing
import com.example.ui.theme.ThingsIconSize
import com.example.R
import androidx.compose.ui.res.stringResource
import com.example.ui.theme.ThingsTheme
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.outlined.Flag
import com.example.data.model.ChecklistItem
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.Tag
import com.example.data.model.TaskSection
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Brightness3
import androidx.compose.material.icons.outlined.Archive
import com.example.ui.theme.AppIcons
import com.example.ui.theme.dimens
import com.example.ui.screens.home.inlineeditor.utils.isTodayDate
import com.example.ui.screens.home.inlineeditor.utils.isTodayDateOrPast
import com.example.ui.screens.home.inlineeditor.utils.hasUnsavedChanges
import com.example.ui.screens.home.inlineeditor.utils.EditorOutsideTouch
import com.example.ui.screens.home.inlineeditor.utils.markTouchesInsideEditor
import com.example.ui.screens.home.inlineeditor.components.*
import com.example.ui.screens.home.inlineeditor.dialogs.ThingsWhenDialog
import com.example.ui.screens.home.inlineeditor.dialogs.ThingsTagDialog
import java.util.Calendar
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat
import androidx.compose.foundation.background
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import com.example.ui.components.hideSoftKeyboardNow
import com.example.ui.components.progressPadding
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.domain.edits.TaskEditorFields
import com.example.domain.edits.TaskEdits

/** Высота редактора в начале раскрытия — совпадает с высотой свёрнутой строки задачи */
internal val EDITOR_COLLAPSED_HEIGHT = 46.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

@Composable
fun ThingsTaskInlineEditor(
    task: ItemWithChecklist,
    projects: List<Item>,
    onSave: (
        title: String,
        notes: String,
        section: TaskSection,
        isTonight: Boolean,
        startDate: Long?,
        dueDate: Long?,
        tags: List<String>,
        projectId: String?,
        checklist: List<ChecklistItem>,
        priority: Int,
        reminderTime: Int?
    ) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDone: () -> Unit = {},
    // Запись черновика при уходе приложения в фон: только сохраняет задачу, редактор остаётся открытым
    onSaveDraft: (fields: TaskEditorFields, checklist: List<ChecklistItem>) -> Unit = { _, _ -> },
    onToggle: (
        title: String,
        notes: String,
        section: TaskSection,
        isTonight: Boolean,
        startDate: Long?,
        dueDate: Long?,
        tags: List<String>,
        projectId: String?,
        checklist: List<ChecklistItem>,
        priority: Int,
        reminderTime: Int?
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _ -> },
    allSavedTags: List<String> = emptyList(),
    allSavedTagObjects: List<Tag> = emptyList(),
    onNewTagCreated: (String, String?) -> Unit = { _, _ -> },
    onDeleteTag: (Tag) -> Unit = {},
    onUpdateTag: (Tag) -> Unit = {},
    onUpdateTagsOrder: (List<Tag>) -> Unit = {},
    isDeletedExternally: () -> Boolean = { false },
    isExpanded: Boolean = true,
    // Новая задача, созданная перетаскиванием «+»: фокус в названии и клавиатура сразу
    autoFocusTitle: Boolean = false,
    // Касания списка вне редактора: по ним маркеры курсора прячутся ещё до начала сворачивания
    outsideTouch: EditorOutsideTouch? = null,
    // Прогресс раскрытия (0..1) — функцией, а не числом: его читают только лямбды раскладки
    // и graphicsLayer, поэтому анимация не пересобирает редактор на каждом кадре.
    expansionProgress: () -> Float = { 1f },
    // Полная высота содержимого редактора, измеренная в раскладке этого кадра (видимая часть — её доля
    // по прогрессу раскрытия). Нужна, чтобы подтянуть список, если редактор не помещается над нижним краем
    onFullHeightMeasured: (Int) -> Unit = {},
    onWhenDialogVisibilityChange: (Boolean) -> Unit = {},
    onTagDialogVisibilityChange: (Boolean) -> Unit = {},
    areas: List<com.example.data.model.Area> = emptyList(),
    // Шаблон повтора: действия с ним; копия повтора: перейти к шаблону
    onRepeatAction: (com.example.ui.screens.home.components.RepeatAction) -> Unit = {},
    onOpenRepeatTemplate: () -> Unit = {},
    // Шаблон повтора: в «Когда» выбрали новый день ближайшего повтора (начало дня, мс)
    onRepeatMove: (Long) -> Unit = {},
    onNavigateToProject: ((Item) -> Unit)? = null,
    onNavigateToArea: ((com.example.data.model.Area) -> Unit)? = null,
    screen: ActiveScreen = ActiveScreen.INBOX
) {
    var title by remember(task.item.id) { mutableStateOf<String>(task.item.title) }
    var notes by remember(task.item.id) { mutableStateOf<String>(task.item.notes) }
    var section by remember(task.item.id) { mutableStateOf<TaskSection>(task.item.section) }
    var isTonight by remember(task.item.id) { mutableStateOf<Boolean>(task.item.isTonight) }
    var startDate by remember(task.item.id) { mutableStateOf<Long?>(task.item.startDate) }
    // Отметка повторяющейся задачи заранее: чекбокс пустеет после выполнения копии
    val repeatCheckScope = androidx.compose.runtime.rememberCoroutineScope()
    var dueDate by remember(task.item.id) { mutableStateOf<Long?>(task.item.dueDate) }
    var tagInput by remember(task.item.id) { mutableStateOf<String>(task.item.tags.joinToString(", ")) }
    var checklist by remember(task.item.id, task.checklist) { mutableStateOf<List<ChecklistItem>>(task.checklist) }
    var priority by remember(task.item.id) { mutableStateOf<Int>(task.item.priority) }
    // Напоминание — время дня в минутах; задаётся в окне «Когда»
    var reminderTime by remember(task.item.id) { mutableStateOf<Int?>(task.item.reminderTime) }
    var isCompleted by remember(task.item.id) { mutableStateOf<Boolean>(task.item.isCompleted) }

    LaunchedEffect(task.item.isCompleted) {
        isCompleted = task.item.isCompleted
    }

    // Helpers visibility states
    var showCalendarHelper by remember(task.item.id) { mutableStateOf(false) }
    var showWhenDialog by remember(task.item.id) { mutableStateOf(false) }
    // Шаблон повтора: «Когда» выбирает день ближайшего повтора, а не дату самого шаблона
    var repeatDay by remember(task.item.id, showWhenDialog) { mutableStateOf(task.item.startDate) }
    var repeatSection by remember(task.item.id, showWhenDialog) { mutableStateOf(task.item.section) }
    //var showTagHelper by remember(task.item.id) { mutableStateOf(false) }
    var showTagDialog by remember(task.item.id) { mutableStateOf(false) }
    var showChecklistHelper by remember(task.item.id, task.checklist) { mutableStateOf(task.checklist.isNotEmpty()) }
    var showDatePicker by remember(task.item.id) { mutableStateOf(false) }

    var isDeleted by remember { mutableStateOf(false) }
    var isSavedManually by remember { mutableStateOf(false) }
    val view = LocalView.current

    LaunchedEffect(showWhenDialog) {
        onWhenDialogVisibilityChange(showWhenDialog)
    }

    LaunchedEffect(showTagDialog) {
        onTagDialogVisibilityChange(showTagDialog)
    }

    val currentTitle by rememberUpdatedState(title)
    val currentNotes by rememberUpdatedState(notes)
    val currentSection by rememberUpdatedState(section)
    val currentIsTonight by rememberUpdatedState(isTonight)
    val currentStartDate by rememberUpdatedState(startDate)
    val currentDueDate by rememberUpdatedState(dueDate)
    val currentTagInput by rememberUpdatedState(tagInput)
    val currentChecklist by rememberUpdatedState(checklist.filter { it.title.isNotBlank() })
    val currentPriority by rememberUpdatedState(priority)
    val currentReminderTime by rememberUpdatedState(reminderTime)
    val currentOnSave by rememberUpdatedState(onSave)
    val currentIsDeletedExternally by rememberUpdatedState(isDeletedExternally)
    val currentProjectId by rememberUpdatedState(task.item.projectId)

    // Внешние изменения задачи (например, перенос в проект кнопкой Move, пока редактор открыт)
    // принимаются только в тех полях, которые в редакторе ещё не правили. Перенос записывает в базу
    // задачу в прежнем виде с новым проектом, и раньше редактор подхватывал её целиком — несохранённые
    // дата, раздел, теги и приоритет пропадали.
    var lastSeenItem by remember(task.item.id) { mutableStateOf(task.item) }
    LaunchedEffect(task.item) {
        val prev = lastSeenItem
        val now = task.item
        if (now.section != prev.section && section == prev.section) section = now.section
        if (now.isTonight != prev.isTonight && isTonight == prev.isTonight) isTonight = now.isTonight
        if (now.startDate != prev.startDate && startDate == prev.startDate) startDate = now.startDate
        if (now.dueDate != prev.dueDate && dueDate == prev.dueDate) dueDate = now.dueDate
        val prevTags = prev.tags.joinToString(", ")
        if (now.tags != prev.tags && tagInput == prevTags) tagInput = now.tags.joinToString(", ")
        if (now.priority != prev.priority && priority == prev.priority) priority = now.priority
        if (now.reminderTime != prev.reminderTime && reminderTime == prev.reminderTime) reminderTime = now.reminderTime
        lastSeenItem = now
    }

    LaunchedEffect(task.checklist) {
        checklist = task.checklist
    }

    // Клавиатуру убираем в момент начала сворачивания, а не когда редактор покинет композицию:
    // иначе она висела бы всю анимацию сворачивания (300 мс). Это страховка для путей закрытия,
    // которые не прячут клавиатуру сами: чекбокс, переход в проект.
    // Фокус здесь НЕ снимаем и поля не выключаем (enabled = false действует так же): Compose сразу
    // вызвал бы restartInput, и ещё видимая Gboard перерисовалась бы в раскладку с рядом цифр
    // прямо во время скрытия. Сессия ввода закончится сама, когда редактор покинет композицию, —
    // к этому моменту hideSoftKeyboardNow уже полностью скроет клавиатуру.
    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            view.hideSoftKeyboardNow()
        }
    }

    // Save changes immediately when collapse animation starts so ViewModel & TaskItemRow have updated title before collapse finishes.
    // Без изменений не сохраняем — см. hasUnsavedChanges.
    LaunchedEffect(isExpanded) {
        if (!isExpanded && !isDeleted && !isDeletedExternally() && !isSavedManually) {
            isSavedManually = true
            // Пустые пункты чек-листа (новый пункт, в который так ничего и не ввели) не сохраняются
            val savedChecklist = checklist.filter { it.title.isNotBlank() }
            if (title.isBlank() && notes.isBlank() && savedChecklist.isEmpty()) {
                onDelete?.invoke()
            } else {
                val tagList = tagInput.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                if (hasUnsavedChanges(task, title, notes, section, isTonight, startDate, dueDate, tagList, savedChecklist, priority, reminderTime)) {
                    onSave(
                        title,
                        notes,
                        section,
                        isTonight,
                        startDate,
                        dueDate,
                        tagList,
                        task.item.projectId,
                        savedChecklist,
                        priority,
                        reminderTime
                    )
                }
            }
        }
    }

    val currentTask by rememberUpdatedState(task)
    val currentOnDone by rememberUpdatedState(onDone)
    val currentOnSaveDraft by rememberUpdatedState(onSaveDraft)

    // Приложение ушло в фон (ON_STOP), а редактор открыт: записываем черновик, не сворачивая редактор.
    // Иначе набранное пропадало, если система выгружала процесс в фоне (или приложение переустанавливали):
    // правки писались только при сворачивании и в onDispose, а новая задача оставалась во «Входящих»
    // без названия. Пустую новую задачу здесь не удаляем — к ней ещё вернутся. После записи задача
    // из базы совпадает с редактором, и сворачивание (hasUnsavedChanges) не перезапишет её ещё раз.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_STOP) return@LifecycleEventObserver
            if (isDeleted || currentIsDeletedExternally() || isSavedManually) return@LifecycleEventObserver
            if (currentTitle.isBlank() && currentNotes.isBlank() && currentChecklist.isEmpty()) return@LifecycleEventObserver
            val tagList = currentTagInput.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            val saved = currentTask.item.let {
                TaskEditorFields(
                    it.title, it.notes, it.section, it.isTonight, it.startDate,
                    it.dueDate, it.tags, it.projectId, it.priority, it.reminderTime
                )
            }
            val draft = TaskEdits.backgroundDraft(
                saved,
                TaskEditorFields(
                    currentTitle, currentNotes, currentSection, currentIsTonight, currentStartDate,
                    currentDueDate, tagList, currentProjectId, currentPriority, currentReminderTime
                )
            )
            val changed = hasUnsavedChanges(
                currentTask, draft.title, draft.notes, draft.section, draft.isTonight,
                draft.startDate, draft.dueDate, draft.tags, currentChecklist, draft.priority, draft.reminderTime
            )
            if (changed) currentOnSaveDraft(draft, currentChecklist)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Save changes automatically when focus is cleared or editor is disposed (e.g., clicking outside)
    DisposableEffect(Unit) {
        onDispose {
            if (!isDeleted && !currentIsDeletedExternally() && !isSavedManually) {
                if (currentTitle.isBlank() && currentNotes.isBlank() && currentChecklist.isEmpty()) {
                    onDelete?.invoke()
                } else {
                    val tagList = currentTagInput.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    val changed = hasUnsavedChanges(
                        currentTask, currentTitle, currentNotes, currentSection, currentIsTonight,
                        currentStartDate, currentDueDate, tagList, currentChecklist, currentPriority, currentReminderTime
                    )
                    if (changed) {
                        currentOnSave(
                            currentTitle,
                            currentNotes,
                            currentSection,
                            currentIsTonight,
                            currentStartDate,
                            currentDueDate,
                            tagList,
                            currentProjectId, // preserve original project safely
                            currentChecklist,
                            currentPriority,
                            currentReminderTime
                        )
                    } else {
                        // Сохранение заодно сбрасывало раскрытую задачу; без изменений делаем только это
                        currentOnDone()
                    }
                }
            }
        }
    }

    // Force strict light theme for the expanded inline editor (white background & black fonts)
    val buttonFontSize = ThingsTheme.type.dialogButton.fontSize
    val focusManager = LocalFocusManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_inline_editor")
            .then(if (outsideTouch != null) Modifier.markTouchesInsideEditor(outsideTouch) else Modifier)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        view.hideSoftKeyboardNow()
                        focusManager.clearFocus()
                    }
                )
            }
            .clip(ThingsTheme.shapes.rowShape)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val fullHeight = placeable.height
                onFullHeightMeasured(fullHeight)
                val collapsedHeight = EDITOR_COLLAPSED_HEIGHT.roundToPx()
                val targetHeight = (collapsedHeight + (fullHeight - collapsedHeight) * expansionProgress()).toInt()
                layout(placeable.width, targetHeight) {
                    placeable.place(0, 0)
                }
            },
        shape = ThingsTheme.shapes.rowShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = ThingsElevation.NONE)
    ) {
        // Линейная интерполяция отступов: при p=0 совпадают с TaskItemRow.padding,
        // при p=1 — дизайнерские отступы развёрнутого редактора
        val collapsedContentPadding = PaddingValues(
            start = MaterialTheme.dimens.taskRowStartPadding,
            end = MaterialTheme.dimens.taskEditorCollapsedEndPadding,
            top = MaterialTheme.dimens.taskCollapsedTopPadding,
            bottom = MaterialTheme.dimens.taskCollapsedBottomPadding
        )
        val expandedContentPadding = PaddingValues(
            start = MaterialTheme.dimens.taskEditorExpandedStartPadding,
            end = MaterialTheme.dimens.taskEditorExpandedEndPadding,
            top = MaterialTheme.dimens.taskExpandedTopPadding,
            bottom = MaterialTheme.dimens.taskExpandedBottomPadding
        )

        val currentProject = remember(task.item.projectId, projects) {
            projects.firstOrNull { it.id == task.item.projectId }
        }
        val currentArea = remember(task.item.areaId, areas) {
            areas.firstOrNull { it.id == task.item.areaId }
        }
        val subtitleText = when {
            currentProject != null && screen != ActiveScreen.PROJECT_DETAIL -> currentProject.name
            currentProject == null && currentArea != null && screen != ActiveScreen.AREA_DETAIL -> currentArea.title
            else -> null
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .progressPadding(expansionProgress, collapsedContentPadding, expandedContentPadding)
        ) {
            // Main Top Content: Checkbox, Title and Notes
                InlineMainInputRow(
                    title = title,
                    onTitleChange = { title = it },
                    notes = notes,
                    onNotesChange = { notes = it },
                    isCompleted = isCompleted,
                    expansionProgress = expansionProgress,
                    subtitleText = subtitleText,
                    showCursor = isExpanded && outsideTouch?.hideHandles != true,
                    checklistBelow = showChecklistHelper || checklist.isNotEmpty(),
                    autoFocusTitle = autoFocusTitle,
                    // «Done» на клавиатуре у новой задачи — как тап вне редактора: клавиатура уходит в этом же
                    // кадре, редактор сворачивается, задача сохраняется (или удаляется, если пустая)
                    onTitleDone = if (autoFocusTitle) {
                        {
                            outsideTouch?.onCollapseRequested()
                            view.hideSoftKeyboardNow()
                            currentOnDone()
                        }
                    } else null,
                    onCheckboxClick = onCheckboxClick@{
                        // Задачу шаблона проекта не отмечают — отмечают её копии
                        if (task.item.isTemplatePart) return@onCheckboxClick
                        // Повторяющуюся задачу отмечают заранее: выполняется копия ближайшего повтора, сама она
                        // остаётся со следующим повтором — чекбокс отмечается и снова пустеет
                        if (task.item.isRepeatTemplate) {
                            isCompleted = true
                            onRepeatAction(com.example.ui.screens.home.components.RepeatAction.COMPLETE_EARLY)
                            repeatCheckScope.launch {
                                kotlinx.coroutines.delay(com.example.ui.theme.ThingsMotion.LONG.toLong() * 2)
                                isCompleted = false
                            }
                            return@onCheckboxClick
                        }
                        val newCompleted = !isCompleted
                        isCompleted = newCompleted
                        val savedChecklist = checklist.filter { it.title.isNotBlank() }
                        val tagList = tagInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        onToggle(
                            title,
                            notes,
                            section,
                            isTonight,
                            startDate,
                            dueDate,
                            tagList,
                            task.item.projectId,
                            savedChecklist,
                            priority,
                            reminderTime
                        )
                    }
                )

                // Smoothly fade in all secondary items (checklist, dates, tags, buttons) below title row
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = expansionProgress()
                        }
                ) {
                    // Checklist Items Panel
                    InlineChecklistPanel(
                    itemId = task.item.id,
                    checklist = checklist,
                    onChecklistChange = { checklist = it },
                    showChecklistHelper = showChecklistHelper,
                    onShowChecklistHelperChange = { showChecklistHelper = it }
                )

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.taskEditorChecklistToToolbarSpacer))

            // Bottom Actions & Toolbar matching the image closely
            val textPrimaryColor = ThingsTheme.colors.editorText
            // Серые иконки нижней строки (теги, флажок и т. п.) — цвет как в Things 3
            val iconInactiveColor = ThingsTheme.colors.editorIconInactive

            // У шаблона повтора своей даты нет — показывается день, когда появится его ближайшая копия по правилу;
            // у ждущего «после выполнения» (копию ещё не отметили) даты нет. Нажатие — «Когда»: перенос повтора
            val repeatStored = if (task.item.isRepeatTemplate) com.example.ui.components.LocalRepeatRules.current[task.item.id] else null
            val templateDay: Long? = repeatStored?.takeIf { it.nextGenerationDate > 0 }?.let { stored ->
                com.example.domain.repeat.RepeatRule.decode(stored.rrule)?.let { rule ->
                    val zone = java.time.ZoneId.systemDefault()
                    val occurrence = java.time.Instant.ofEpochMilli(stored.nextGenerationDate).atZone(zone).toLocalDate()
                    com.example.domain.repeat.RepeatPlan.appearsOn(rule, occurrence).atStartOfDay(zone).toInstant().toEpochMilli()
                }
            }
            val isTemplateItem = task.item.type == Item.TYPE_TEMPLATE
            val shownStart = if (isTemplateItem) templateDay else startDate
            val shownSection = if (isTemplateItem) TaskSection.UPCOMING else section
            val hasActiveDate = if (isTemplateItem) templateDay != null
                else (startDate != null || section == TaskSection.TODAY || section == TaskSection.SOMEDAY)

            val activeDateLabel = if (hasActiveDate) {
                com.example.ui.screens.home.inlineeditor.utils.formatStartDateLabel(shownStart, shownSection, isTonight)
            } else ""

            val activeDateIcon = if (hasActiveDate) {
                when {
                    shownStart != null && isTodayDateOrPast(shownStart) -> {
                        if (isTonight) AppIcons.Evening else AppIcons.Today
                    }
                    shownStart == null && shownSection == TaskSection.TODAY -> {
                        if (isTonight) AppIcons.Evening else AppIcons.Today
                    }
                    shownSection == TaskSection.SOMEDAY -> AppIcons.Someday
                    else -> AppIcons.Upcoming
                }
            } else AppIcons.Upcoming

            val activeDateColor = if (hasActiveDate) {
                when {
                    shownStart != null && isTodayDateOrPast(shownStart) -> {
                        if (isTonight) Color.Unspecified else ThingsTheme.colors.today
                    }
                    shownStart == null && shownSection == TaskSection.TODAY -> {
                        if (isTonight) Color.Unspecified else ThingsTheme.colors.today
                    }
                    shownSection == TaskSection.SOMEDAY -> ThingsTheme.colors.someday
                    else -> Color.Unspecified
                }
            } else Color.Unspecified

            val startRelativePadding = MaterialTheme.dimens.taskEditorStartRelativePadding
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                // Column containing BOTH date and duedate on the left
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = startRelativePadding),
                    verticalArrangement = Arrangement.Center
                ) {
                    // 0. Active Tags Chips
                    val activeTags = remember(tagInput) {
                        tagInput.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                    }
                    if (activeTags.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = MaterialTheme.dimens.taskEditorTagsVerticalPadding),
                            horizontalArrangement = Arrangement.spacedBy(ThingsSpacing.S),
                            verticalArrangement = Arrangement.spacedBy(ThingsSpacing.XS_PLUS)
                        ) {
                            activeTags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(ThingsTheme.shapes.chipShape)
                                        .background(ThingsTheme.colors.tagChipBackground) // light-teal background
                                        .clickable {
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            showTagDialog = true
                                        }
                                        .padding(horizontal = ThingsSpacing.S_PLUS, vertical = ThingsSpacing.XS)
                                ) {
                                    Text(
                                        text = tag,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        style = ThingsTheme.type.tagChip.copy(
                                            color = ThingsTheme.colors.tagChipText
                                        )
                                    )
                                }
                            }
                        }

                        if (hasActiveDate || dueDate != null) {
                            Spacer(modifier = Modifier.height(MaterialTheme.dimens.taskEditorTagsToDateSpacer))
                        }
                    }

                    // 1. Date Indicator Row
                    if (hasActiveDate) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    showWhenDialog = true
                                }
                                .padding(vertical = MaterialTheme.dimens.taskEditorIndicatorVerticalPadding)
                        ) {
                            Icon(
                                imageVector = activeDateIcon,
                                contentDescription = stringResource(R.string.cd_change_date),
                                tint = activeDateColor,
                                modifier = Modifier.size(ThingsIconSize.S)
                            )
                            Spacer(modifier = Modifier.width(ThingsSpacing.XS_PLUS))
                            Text(
                                text = activeDateLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = ThingsTheme.type.editorDateStrong.copy(
                                    color = textPrimaryColor
                                )
                            )
                            // Напоминание — колокольчик и время рядом с днём, как в Things
                            reminderTime?.let { minutes ->
                                Spacer(modifier = Modifier.width(ThingsSpacing.S))
                                Icon(
                                    imageVector = AppIcons.Bell,
                                    contentDescription = stringResource(R.string.reminder_title),
                                    tint = textPrimaryColor,
                                    modifier = Modifier.size(ThingsIconSize.XS)
                                )
                                Spacer(modifier = Modifier.width(ThingsSpacing.XXS))
                                Text(
                                    text = com.example.ui.screens.home.inlineeditor.utils.formatReminderTime(view.context, minutes),
                                    maxLines = 1,
                                    style = ThingsTheme.type.editorDateStrong.copy(color = textPrimaryColor)
                                )
                            }
                        }
                    }

                    // Spacer between date indicator and duedate indicator
                    if (hasActiveDate && dueDate != null) {
                        Spacer(modifier = Modifier.height(MaterialTheme.dimens.taskEditorDateToDeadlineSpacer))
                    }

                    // 2. Due Date (Deadline) Indicator Row
                    if (dueDate != null) {
                        val delta = remember(dueDate) {
                            val today = java.util.Calendar.getInstance().apply {
                                set(java.util.Calendar.HOUR_OF_DAY, 0)
                                set(java.util.Calendar.MINUTE, 0)
                                set(java.util.Calendar.SECOND, 0)
                                set(java.util.Calendar.MILLISECOND, 0)
                            }
                            val due = java.util.Calendar.getInstance().apply {
                                timeInMillis = dueDate!!
                                set(java.util.Calendar.HOUR_OF_DAY, 0)
                                set(java.util.Calendar.MINUTE, 0)
                                set(java.util.Calendar.SECOND, 0)
                                set(java.util.Calendar.MILLISECOND, 0)
                            }
                            val diffMillis = due.timeInMillis - today.timeInMillis
                            if (diffMillis >= 0) {
                                (diffMillis / (24 * 60 * 60 * 1000L)).toInt()
                            } else {
                                ((diffMillis - (24 * 60 * 60 * 1000L - 1)) / (24 * 60 * 60 * 1000L)).toInt()
                            }
                        }

                        val relativeText = com.example.ui.screens.home.inlineeditor.utils.relativeDueText(delta)

                        val locale = remember { java.util.Locale.getDefault() }
                        val sdf = remember(locale) { java.text.SimpleDateFormat("EEE, d MMMM", locale) }
                        val dateText = remember(dueDate) {
                            dueDate?.let { sdf.format(java.util.Date(it)).lowercase() } ?: ""
                        }

                        val isOverdueOrToday = delta <= 0
                        val primaryColor = if (isOverdueOrToday) ThingsTheme.colors.danger else ThingsTheme.colors.editorText
                        val relativeColor = ThingsTheme.colors.textSecondary

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    showDatePicker = true
                                }
                                .padding(vertical = MaterialTheme.dimens.taskEditorIndicatorVerticalPadding)
                        ) {
                            Icon(
                                imageVector = AppIcons.Deadline,
                                contentDescription = stringResource(R.string.cd_deadline_flag),
                                tint = primaryColor,
                                modifier = Modifier.size(ThingsIconSize.S)
                            )
                            Spacer(modifier = Modifier.width(ThingsSpacing.XS_PLUS))
                            Text(
                                text = dateText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = ThingsTheme.type.editorDateStrong.copy(
                                    color = primaryColor
                                )
                            )
                            Spacer(modifier = Modifier.width(ThingsSpacing.XS))
                            Text(
                                text = relativeText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = ThingsTheme.type.editorDate.copy(
                                    color = relativeColor
                                )
                            )
                        }
                    }

                    // 3. Повтор — под датой и дедлайном: у шаблона — правило и действия, у копии — ссылка на шаблон
                    if (hasActiveDate || dueDate != null) {
                        Spacer(modifier = Modifier.height(MaterialTheme.dimens.taskEditorDateToDeadlineSpacer))
                    }
                    RepeatEditorRow(task.item, onRepeatAction, onOpenRepeatTemplate)
                }

                Spacer(modifier = Modifier.width(ThingsSpacing.L))

                // Action Icons Row on the right
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = ThingsSpacing.XS) // Align slightly with indicator row text/icon padding
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        // У шаблона повтора «Когда» задаёт правило — кнопки нет
                        visible = !hasActiveDate && !task.item.isTemplatePart,
                        enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
                        exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
                    ) {
                        Box(modifier = Modifier.padding(start = MaterialTheme.dimens.taskEditorActionIconsSpacing)) {
                            Icon(
                                imageVector = AppIcons.Upcoming,
                                contentDescription = stringResource(R.string.cd_schedule),
                                tint = iconInactiveColor,
                                modifier = Modifier
                                    .size(ThingsIconSize.L)
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        showWhenDialog = true
                                    }
                            )
                        }
                    }

                    // Tag
                    androidx.compose.animation.AnimatedVisibility(
                        visible = tagInput.trim().isEmpty(),
                        enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
                        exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
                    ) {
                        Box(modifier = Modifier.padding(start = MaterialTheme.dimens.taskEditorActionIconsSpacing)) {
                            Icon(
                                imageVector = AppIcons.Tag,
                                contentDescription = stringResource(R.string.tag_dialog_title),
                                tint = iconInactiveColor,
                                modifier = Modifier
                                    .size(ThingsIconSize.M)
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        showTagDialog = true
                                    }
                            )
                        }
                    }

                    // Checklist toggle
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !showChecklistHelper && checklist.isEmpty(),
                        enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
                        exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
                    ) {
                        Box(modifier = Modifier.padding(start = MaterialTheme.dimens.taskEditorActionIconsSpacing)) {
                            Icon(
                                imageVector = AppIcons.BulletList,
                                contentDescription = stringResource(R.string.cd_checklists),
                                tint = iconInactiveColor,
                                modifier = Modifier
                                    .size(ThingsIconSize.L)
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        showChecklistHelper = true
                                    }
                            )
                        }
                    }

                    // Flag (Deadline)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = dueDate == null,
                        enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
                        exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
                    ) {
                        Box(modifier = Modifier.padding(start = MaterialTheme.dimens.taskEditorActionIconsSpacing)) {
                            Icon(
                                imageVector = AppIcons.Deadline,
                                contentDescription = stringResource(R.string.batch_action_set_deadline),
                                tint = iconInactiveColor,
                                modifier = Modifier
                                    .size(ThingsIconSize.L)
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        showDatePicker = true
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

    if (showWhenDialog && task.item.isRepeatTemplate) {
        ThingsWhenDialog(
            startDate = repeatDay,
            onStartDateChange = { repeatDay = it },
            section = repeatSection,
            onSectionChange = { repeatSection = it },
            isTonight = false,
            onIsTonightChange = { },
            onShowCalendarHelperChange = { },
            reminderTime = reminderTime,
            onReminderTimeChange = { reminderTime = it },
            onDismissRequest = {
                val day = if (repeatSection == com.example.data.model.TaskSection.TODAY) {
                    com.example.data.model.DayBounds.now().todayStart
                } else repeatDay
                if (day != null && day != task.item.startDate) onRepeatMove(day)
                showWhenDialog = false
            }
        )
    } else if (showWhenDialog) {
        ThingsWhenDialog(
            startDate = startDate,
            onStartDateChange = { startDate = it },
            section = section,
            onSectionChange = { section = it },
            isTonight = isTonight,
            onIsTonightChange = { isTonight = it },
            onShowCalendarHelperChange = { showCalendarHelper = it },
            reminderTime = reminderTime,
            onReminderTimeChange = { reminderTime = it },
            onDismissRequest = { showWhenDialog = false }
        )
    }

    if (showDatePicker) {
        DeadlineDatePickerDialog(
            initialSelectedDateMillis = dueDate,
            onDateSelected = { dueDate = it },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showTagDialog) {
        ThingsTagDialog(
            activeTags = remember(tagInput) {
                tagInput.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            },
            allSavedTags = allSavedTags,
            allSavedTagObjects = allSavedTagObjects,
            onNewTagCreated = onNewTagCreated,
            onDeleteTag = onDeleteTag,
            onUpdateTag = onUpdateTag,
            onUpdateTagsOrder = onUpdateTagsOrder,
            onTagsSelected = { selected ->
                tagInput = selected.joinToString(", ")
            },
            onDismissRequest = { showTagDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeadlineDatePickerDialog(
    initialSelectedDateMillis: Long?,
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    // Календарь работает в UTC — переводим местный день туда и обратно (см. localDayToUtcMidnight)
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = com.example.ui.screens.home.inlineeditor.utils.localDayToUtcMidnight(
            initialSelectedDateMillis ?: System.currentTimeMillis()
        )
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onDateSelected(
                        datePickerState.selectedDateMillis?.let {
                            com.example.ui.screens.home.inlineeditor.utils.utcMidnightToLocalDay(it)
                        }
                    )
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.ui_ok), color = ThingsTheme.colors.accent)
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        onDateSelected(null) // Clear option
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.cd_clear), color = ThingsTheme.colors.danger)
                }
                Spacer(modifier = Modifier.width(ThingsSpacing.S))
                TextButton(
                    onClick = onDismiss
                ) {
                    Text(stringResource(R.string.tag_dialog_cancel), color = ThingsTheme.colors.textSecondary)
                }
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

/**
 * Строка повтора в редакторе, как в Things. У шаблона — синяя стрелка, правило и следующий повтор
 * («Щотижня: ср · наступна 21 жовт.»), нажатие — меню действий; у копии — «Это копия повторяющейся
 * задачи ›», ведёт к шаблону. У обычной задачи строки нет.
 */
@Composable
internal fun RepeatEditorRow(
    item: Item,
    onAction: (com.example.ui.screens.home.components.RepeatAction) -> Unit,
    onOpenTemplate: () -> Unit
) {
    // Шаблон задачи или проекта — правило и меню действий; копия — ссылка на шаблон; часть шаблона проекта — ничего
    val isTemplate = item.isRepeatTemplate
    if (item.isTemplatePart || (!isTemplate && item.repeatTemplateId == null)) return
    val view = LocalView.current
    var menu by remember { mutableStateOf(false) }
    val stored = com.example.ui.components.LocalRepeatRules.current[item.id]
    val rule = stored?.let { com.example.domain.repeat.RepeatRule.decode(it.rrule) }
    val text = when {
        !isTemplate -> stringResource(if (item.type == Item.TYPE_PROJECT) R.string.repeat_project_copy_link else R.string.repeat_copy_link)
        rule == null -> stringResource(R.string.special_repeating)
        else -> {
            // Правило и ближайший повтор («наступна 22 жовт.») или пауза — вместе со строкой даты выше
            val summary = com.example.ui.screens.home.inlineeditor.dialogs.repeatRuleSummary(rule)
            val next = when {
                !stored.isActive -> stringResource(R.string.repeat_paused)
                stored.nextGenerationDate > 0 -> stringResource(
                    R.string.repeat_next_short,
                    java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault()).format(java.util.Date(stored.nextGenerationDate))
                )
                else -> null
            }
            if (next != null) "$summary · $next" else summary
        }
    }
    Box {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .clickable {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    if (isTemplate) menu = true else onOpenTemplate()
                }
                .padding(vertical = MaterialTheme.dimens.taskEditorIndicatorVerticalPadding)
        ) {
            if (isTemplate) {
                // Обычный значок повтора — синяя стрелка в голубом круге, как у строки шаблона в списке
                com.example.ui.components.RepeatTemplateIcon(modifier = Modifier.size(ThingsIconSize.S))
            } else {
                // Копия — серая круговая стрелка, как в строке списка
                Icon(AppIcons.RepeatCopy, contentDescription = null, tint = ThingsTheme.colors.textSecondary, modifier = Modifier.size(ThingsIconSize.S))
            }
            Spacer(modifier = Modifier.width(ThingsSpacing.XS_PLUS))
            // Чёрным, мельче даты и обычной толщины; переносится, чтобы правило помещалось целиком
            Text(
                text = text,
                style = ThingsTheme.type.subhead.copy(color = ThingsTheme.colors.editorText),
                modifier = Modifier.weight(1f, fill = false)
            )
            if (!isTemplate) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = ThingsTheme.colors.textSecondary, modifier = Modifier.size(ThingsIconSize.S))
            }
        }
        if (isTemplate) {
            com.example.ui.components.ThingsDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                fun pick(action: com.example.ui.screens.home.components.RepeatAction) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    menu = false
                    onAction(action)
                }
                com.example.ui.components.ThingsMenuItem(stringResource(R.string.repeat_action_edit), AppIcons.Repeat, onClick = { pick(com.example.ui.screens.home.components.RepeatAction.EDIT) })
                com.example.ui.components.ThingsMenuItem(stringResource(R.string.repeat_action_next_copy), Icons.Default.Add, onClick = { pick(com.example.ui.screens.home.components.RepeatAction.NEXT_COPY) })
                com.example.ui.components.ThingsMenuItem(stringResource(R.string.repeat_action_previous), Icons.Default.History, onClick = { pick(com.example.ui.screens.home.components.RepeatAction.PREVIOUS_COPY) })
                if (stored?.isActive == false) {
                    com.example.ui.components.ThingsMenuItem(stringResource(R.string.repeat_action_resume), Icons.Default.PlayArrow, onClick = { pick(com.example.ui.screens.home.components.RepeatAction.RESUME) })
                } else {
                    com.example.ui.components.ThingsMenuItem(stringResource(R.string.repeat_action_pause), Icons.Default.Pause, onClick = { pick(com.example.ui.screens.home.components.RepeatAction.PAUSE) })
                }
                com.example.ui.components.ThingsMenuItem(stringResource(R.string.repeat_action_stop), Icons.Default.Stop, onClick = { pick(com.example.ui.screens.home.components.RepeatAction.STOP) }, destructive = true)
            }
        }
    }
}
