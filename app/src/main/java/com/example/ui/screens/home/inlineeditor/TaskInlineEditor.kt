package com.example.ui.screens.home.inlineeditor

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
import com.example.ui.theme.ThingsBlue
import com.example.ui.theme.ThingsUpcomingRed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Brightness3
import androidx.compose.material.icons.outlined.Archive
import com.example.ui.theme.ThingsTodayStar
import com.example.ui.theme.ThingsSomedayGrey
import com.example.ui.theme.AppIcons
import com.example.ui.theme.dimens
import com.example.ui.theme.taskEditorDate
import com.example.ui.screens.home.inlineeditor.utils.isTodayDate
import com.example.ui.screens.home.inlineeditor.utils.isTodayDateOrPast
import com.example.ui.screens.home.inlineeditor.components.*
import com.example.ui.screens.home.inlineeditor.dialogs.ThingsWhenDialog
import com.example.ui.screens.home.inlineeditor.dialogs.ThingsTagDialog
import java.util.Calendar
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat
import androidx.compose.foundation.background
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView

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
        priority: Int
    ) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDone: () -> Unit = {},
    allSavedTags: List<String> = emptyList(),
    allSavedTagObjects: List<Tag> = emptyList(),
    onNewTagCreated: (String, String?) -> Unit = { _, _ -> },
    onDeleteTag: (Tag) -> Unit = {},
    onUpdateTag: (Tag) -> Unit = {},
    onUpdateTagsOrder: (List<Tag>) -> Unit = {},
    isDeletedExternally: () -> Boolean = { false },
    isExpanded: Boolean = true,
    expansionProgress: Float = 1f,
    onWhenDialogVisibilityChange: (Boolean) -> Unit = {},
    areas: List<com.example.data.model.Area> = emptyList(),
    onNavigateToProject: ((Item) -> Unit)? = null,
    onNavigateToArea: ((com.example.data.model.Area) -> Unit)? = null,
    screen: com.example.ui.screens.home.ActiveScreen = com.example.ui.screens.home.ActiveScreen.INBOX
) {
    var title by remember(task.item.id) { mutableStateOf<String>(task.item.title) }
    var notes by remember(task.item.id) { mutableStateOf<String>(task.item.notes) }
    var section by remember(task.item.id) { mutableStateOf<TaskSection>(task.item.section) }
    var isTonight by remember(task.item.id) { mutableStateOf<Boolean>(task.item.isTonight) }
    var startDate by remember(task.item.id) { mutableStateOf<Long?>(task.item.startDate) }
    var dueDate by remember(task.item.id) { mutableStateOf<Long?>(task.item.dueDate) }
    var tagInput by remember(task.item.id) { mutableStateOf<String>(task.item.tags.joinToString(", ")) }
    var checklist by remember(task.item.id, task.checklist) { mutableStateOf<List<ChecklistItem>>(task.checklist) }
    var priority by remember(task.item.id) { mutableStateOf<Int>(task.item.priority) }

    // Helpers visibility states
    var showCalendarHelper by remember(task.item.id) { mutableStateOf(false) }
    var showWhenDialog by remember(task.item.id) { mutableStateOf(false) }
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

    val currentTitle by rememberUpdatedState(title)
    val currentNotes by rememberUpdatedState(notes)
    val currentSection by rememberUpdatedState(section)
    val currentIsTonight by rememberUpdatedState(isTonight)
    val currentStartDate by rememberUpdatedState(startDate)
    val currentDueDate by rememberUpdatedState(dueDate)
    val currentTagInput by rememberUpdatedState(tagInput)
    val currentChecklist by rememberUpdatedState(checklist)
    val currentPriority by rememberUpdatedState(priority)
    val currentOnSave by rememberUpdatedState(onSave)
    val currentIsDeletedExternally by rememberUpdatedState(isDeletedExternally)
    val currentProjectId by rememberUpdatedState(task.item.projectId)

    // Synchronize inline editor state with external updates (e.g. from Move Dialog)
    LaunchedEffect(task.item) {
        section = task.item.section
        isTonight = task.item.isTonight
        startDate = task.item.startDate
        dueDate = task.item.dueDate
        tagInput = task.item.tags.joinToString(", ")
        priority = task.item.priority
    }

    LaunchedEffect(task.checklist) {
        checklist = task.checklist
    }

    // Save changes immediately when collapse animation starts so ViewModel & TaskItemRow have updated title before collapse finishes
    LaunchedEffect(isExpanded) {
        if (!isExpanded && !isDeleted && !isDeletedExternally() && !isSavedManually) {
            isSavedManually = true
            if (title.isBlank() && notes.isBlank() && checklist.isEmpty()) {
                onDelete?.invoke()
            } else {
                val tagList = tagInput.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                onSave(
                    title,
                    notes,
                    section,
                    isTonight,
                    startDate,
                    dueDate,
                    tagList,
                    task.item.projectId,
                    checklist,
                    priority
                )
            }
        }
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
                        currentPriority
                    )
                }
            }
        }
    }

    // Force strict light theme for the expanded inline editor (white background & black fonts)
    val editorBackground = Color.White
    val buttonFontSize = MaterialTheme.typography.labelLarge.fontSize

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_inline_editor")
            .clip(RoundedCornerShape(8.dp))
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val fullHeight = placeable.height
                val collapsedHeight = 46.dp.roundToPx()
                val targetHeight = (collapsedHeight + (fullHeight - collapsedHeight) * expansionProgress).toInt()
                layout(placeable.width, targetHeight) {
                    placeable.place(0, 0)
                }
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        // Линейная интерполяция: при p=0 совпадает с TaskItemRow.padding(start),
        // при p=1 — дизайнерский отступ развёрнутого редактора
        val startPadding = androidx.compose.ui.unit.lerp(
            MaterialTheme.dimens.taskRowStartPadding,
            MaterialTheme.dimens.taskEditorExpandedStartPadding,
            expansionProgress
        )
        val endPadding = androidx.compose.ui.unit.lerp(
            MaterialTheme.dimens.taskRowEndPadding,
            MaterialTheme.dimens.taskEditorExpandedEndPadding,
            expansionProgress
        )
        val collapsedTopPadding = MaterialTheme.dimens.taskCollapsedTopPadding
        val expandedTopPadding = MaterialTheme.dimens.taskExpandedTopPadding
        val collapsedBottomPadding = MaterialTheme.dimens.taskCollapsedBottomPadding
        val expandedBottomPadding = MaterialTheme.dimens.taskExpandedBottomPadding

        val topPadding = (collapsedTopPadding.value + (expandedTopPadding.value - collapsedTopPadding.value) * expansionProgress).dp
        val bottomPadding = (collapsedBottomPadding.value + (expandedBottomPadding.value - collapsedBottomPadding.value) * expansionProgress).dp

        val currentProject = remember(task.item.projectId, projects) {
            projects.firstOrNull { it.id == task.item.projectId }
        }
        val currentArea = remember(task.item.areaId, areas) {
            areas.firstOrNull { it.id == task.item.areaId }
        }
        val subtitleText = when {
            currentProject != null && screen != com.example.ui.screens.home.ActiveScreen.PROJECT_DETAIL -> currentProject.name
            currentProject == null && currentArea != null && screen != com.example.ui.screens.home.ActiveScreen.AREA_DETAIL -> currentArea.title
            else -> null
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = startPadding,
                    end = endPadding,
                    top = topPadding,
                    bottom = bottomPadding
                )
        ) {
            // Main Top Content: Checkbox, Title and Notes
                InlineMainInputRow(
                    title = title,
                    onTitleChange = { title = it },
                    notes = notes,
                    onNotesChange = { notes = it },
                    isCompleted = task.item.isCompleted,
                    expansionProgress = expansionProgress,
                    subtitleText = subtitleText,
                    onCheckboxClick = {
                        isSavedManually = true
                        if (title.isBlank() && notes.isBlank() && checklist.isEmpty()) {
                            onDelete?.invoke()
                        } else {
                            val tagList = tagInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            onSave(
                                title,
                                notes,
                                section,
                                isTonight,
                                startDate,
                                dueDate,
                                tagList,
                                task.item.projectId,
                                checklist,
                                priority
                            )
                        }
                        onDone()
                    }
                )

                // Smoothly fade in all secondary items (checklist, dates, tags, buttons) below title row
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = expansionProgress
                        }
                ) {
                    // Checklist Items Panel
                    InlineChecklistPanel(
                    itemId = task.item.id,
                    checklist = checklist,
                    onChecklistChange = { checklist = it },
                    showChecklistHelper = showChecklistHelper,
                    onShowChecklistHelperChange = { showChecklistHelper = it },
                    expansionProgress = expansionProgress
                )

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Actions & Toolbar matching the image closely
            val textPrimaryColor = Color(0xFF1C1C1E)
            val iconInactiveColor = Color(0xFFC7C7CC)
            val bodyFontSize = MaterialTheme.typography.taskEditorDate.fontSize

            val hasActiveDate = startDate != null || section == TaskSection.TODAY || section == TaskSection.SOMEDAY

            val activeDateLabel = if (hasActiveDate) {
                com.example.ui.screens.home.inlineeditor.utils.formatStartDateLabel(startDate, section, isTonight)
            } else ""

            val activeDateIcon = if (hasActiveDate) {
                when {
                    startDate != null && isTodayDateOrPast(startDate) -> {
                        if (isTonight) AppIcons.Evening else AppIcons.Today
                    }
                    startDate == null && section == TaskSection.TODAY -> {
                        if (isTonight) AppIcons.Evening else AppIcons.Today
                    }
                    section == TaskSection.SOMEDAY -> AppIcons.Someday
                    else -> AppIcons.Upcoming
                }
            } else AppIcons.Upcoming

            val activeDateColor = if (hasActiveDate) {
                when {
                    startDate != null && isTodayDateOrPast(startDate) -> {
                        if (isTonight) Color.Unspecified else ThingsTodayStar
                    }
                    startDate == null && section == TaskSection.TODAY -> {
                        if (isTonight) Color.Unspecified else ThingsTodayStar
                    }
                    section == TaskSection.SOMEDAY -> ThingsSomedayGrey
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            activeTags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFD1EAE2)) // light-teal background
                                        .clickable {
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            showTagDialog = true
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        style = TextStyle(
                                            color = Color(0xFF2C7D64), // dark-teal text
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
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
                                contentDescription = "Change date",
                                tint = activeDateColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = activeDateLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(
                                    fontSize = bodyFontSize,
                                    color = textPrimaryColor,
                                    fontWeight = FontWeight.Medium
                                )
                            )
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

                        val lang = remember { java.util.Locale.getDefault().language }
                        val relativeText = when (lang) {
                            "uk" -> when {
                                delta < 0 -> "протерміновано"
                                delta == 0 -> "сьогодні"
                                delta == 1 -> "завтра"
                                else -> "через $delta дн."
                            }
                            "ru" -> when {
                                delta < 0 -> "просрочено"
                                delta == 0 -> "сегодня"
                                delta == 1 -> "завтра"
                                else -> "через $delta дн."
                            }
                            else -> when {
                                delta < 0 -> "overdue"
                                delta == 0 -> "today"
                                delta == 1 -> "tomorrow"
                                else -> "in $delta d."
                            }
                        }

                        val locale = remember(lang) {
                            when (lang) {
                                "uk" -> java.util.Locale("uk")
                                "ru" -> java.util.Locale("ru")
                                else -> java.util.Locale.US
                            }
                        }
                        val sdf = remember(locale) { java.text.SimpleDateFormat("EEE, d MMMM", locale) }
                        val dateText = remember(dueDate) {
                            dueDate?.let { sdf.format(java.util.Date(it)).lowercase() } ?: ""
                        }

                        val isOverdueOrToday = delta <= 0
                        val primaryColor = if (isOverdueOrToday) ThingsUpcomingRed else Color(0xFF1C1C1E)
                        val relativeColor = Color(0xFF8E8E93)

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
                                imageVector = Icons.Outlined.Flag,
                                contentDescription = "Deadline Flag",
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = dateText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(
                                    fontSize = bodyFontSize,
                                    color = primaryColor,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = relativeText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(
                                    fontSize = bodyFontSize,
                                    color = relativeColor,
                                    fontWeight = FontWeight.Normal
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Action Icons Row on the right
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp) // Align slightly with indicator row text/icon padding
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !hasActiveDate,
                        enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
                        exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
                    ) {
                        Box(modifier = Modifier.padding(start = 12.dp)) {
                            Icon(
                                imageVector = AppIcons.Upcoming,
                                contentDescription = "Schedule",
                                tint = iconInactiveColor,
                                modifier = Modifier
                                    .size(22.dp)
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
                        Box(modifier = Modifier.padding(start = 12.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.LocalOffer,
                                contentDescription = "Tags",
                                tint = iconInactiveColor,
                                modifier = Modifier
                                    .size(22.dp)
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
                        Box(modifier = Modifier.padding(start = 12.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.FormatListBulleted,
                                contentDescription = "Checklists",
                                tint = iconInactiveColor,
                                modifier = Modifier
                                    .size(22.dp)
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
                        Box(modifier = Modifier.padding(start = 12.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.Flag,
                                contentDescription = "Set Deadline",
                                tint = iconInactiveColor,
                                modifier = Modifier
                                    .size(22.dp)
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

    if (showWhenDialog) {
        ThingsWhenDialog(
            startDate = startDate,
            onStartDateChange = { startDate = it },
            section = section,
            onSectionChange = { section = it },
            isTonight = isTonight,
            onIsTonightChange = { isTonight = it },
            onShowCalendarHelperChange = { showCalendarHelper = it },
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
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDateMillis ?: System.currentTimeMillis()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onDateSelected(datePickerState.selectedDateMillis)
                    onDismiss()
                }
            ) {
                Text("OK", color = ThingsBlue)
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
                    Text("Clear", color = ThingsUpcomingRed)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
