package com.example.ui.screens.home.inlineeditor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.outlined.Flag
import com.example.data.model.ChecklistItem
import com.example.data.model.Item
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
import com.example.ui.screens.home.inlineeditor.utils.isTodayDate
import com.example.ui.screens.home.inlineeditor.components.*
import com.example.ui.screens.home.inlineeditor.dialogs.ThingsWhenDialog
import java.util.Calendar
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThingsTaskInlineEditor(
    task: Item,
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
    onDone: () -> Unit = {}
) {
    var title by remember(task.id) { mutableStateOf<String>(task.title) }
    var notes by remember(task.id) { mutableStateOf<String>(task.notes) }
    var section by remember(task.id) { mutableStateOf<TaskSection>(task.section) }
    var isTonight by remember(task.id) { mutableStateOf<Boolean>(task.isTonight) }
    var startDate by remember(task.id) { mutableStateOf<Long?>(task.startDate) }
    var dueDate by remember(task.id) { mutableStateOf<Long?>(task.dueDate) }
    var tagInput by remember(task.id) { mutableStateOf<String>(task.tags.joinToString(", ")) }
    var checklist by remember(task.id, task.checklist) { mutableStateOf<List<ChecklistItem>>(task.checklist) }
    var priority by remember(task.id) { mutableStateOf<Int>(task.priority) }

    // Helpers visibility states
    var showCalendarHelper by remember(task.id) { mutableStateOf(false) }
    var showWhenDialog by remember(task.id) { mutableStateOf(false) }
    var showTagHelper by remember(task.id) { mutableStateOf(false) }
    var showChecklistHelper by remember(task.id, task.checklist) { mutableStateOf(task.checklist.isNotEmpty()) }
    var showDatePicker by remember(task.id) { mutableStateOf(false) }

    var isDeleted by remember { mutableStateOf(false) }
    var isSavedManually by remember { mutableStateOf(false) }

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

    // Save changes automatically when focus is cleared or editor is disposed (e.g., clicking outside)
    DisposableEffect(Unit) {
        onDispose {
            if (!isDeleted && !isSavedManually) {
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
                        task.projectId, // preserve original project
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
            .testTag("task_inline_editor"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Main Top Content: Checkbox, Title and Notes
            InlineMainInputRow(
                title = title,
                onTitleChange = { title = it },
                notes = notes,
                onNotesChange = { notes = it },
                isCompleted = task.isCompleted,
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
                            task.projectId,
                            checklist,
                            priority
                        )
                    }
                    onDone()
                }
            )

            // Tag Helper: Sleek input block
            InlineTagField(
                tagInput = tagInput,
                onTagInputChange = { tagInput = it },
                showTagHelper = showTagHelper,
                onShowTagHelperChange = { showTagHelper = it }
            )

            // Checklist Items Panel
            InlineChecklistPanel(
                itemId = task.id,
                checklist = checklist,
                onChecklistChange = { checklist = it },
                showChecklistHelper = showChecklistHelper,
                onShowChecklistHelperChange = { showChecklistHelper = it }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Actions & Toolbar matching the image closely
            val textPrimaryColor = Color(0xFF1C1C1E)
            val iconInactiveColor = Color(0xFFC7C7CC)
            val bodyFontSize = MaterialTheme.typography.bodyLarge.fontSize

            val hasActiveDate = startDate != null || section == TaskSection.TODAY || section == TaskSection.SOMEDAY

            val activeDateLabel = if (hasActiveDate) {
                when {
                    startDate != null && isTodayDate(startDate) -> {
                        if (isTonight) "This Evening" else "Today"
                    }
                    startDate == null && section == TaskSection.TODAY -> {
                        if (isTonight) "This Evening" else "Today"
                    }
                    section == TaskSection.SOMEDAY -> "Someday"
                    else -> {
                        val targetCal = java.util.Calendar.getInstance().apply { timeInMillis = startDate!! }
                        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                        val formatPattern = if (targetCal.get(Calendar.YEAR) == currentYear) {
                            "EEE, MMM d"
                        } else {
                            "EEE, MMM d, yyyy"
                        }
                        java.text.SimpleDateFormat(formatPattern, java.util.Locale.US).format(java.util.Date(startDate!!))
                    }
                }
            } else ""

            val activeDateIcon = if (hasActiveDate) {
                when {
                    startDate != null && isTodayDate(startDate) -> {
                        if (isTonight) Icons.Outlined.Brightness3 else Icons.Default.Star
                    }
                    startDate == null && section == TaskSection.TODAY -> {
                        if (isTonight) Icons.Outlined.Brightness3 else Icons.Default.Star
                    }
                    section == TaskSection.SOMEDAY -> Icons.Outlined.Archive
                    else -> Icons.Outlined.CalendarToday
                }
            } else Icons.Outlined.CalendarToday

            val activeDateColor = if (hasActiveDate) {
                when {
                    startDate != null && isTodayDate(startDate) -> {
                        if (isTonight) ThingsBlue else ThingsTodayStar
                    }
                    startDate == null && section == TaskSection.TODAY -> {
                        if (isTonight) ThingsBlue else ThingsTodayStar
                    }
                    section == TaskSection.SOMEDAY -> ThingsSomedayGrey
                    else -> ThingsBlue
                }
            } else ThingsBlue

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                // Column containing BOTH date and duedate on the left
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    // 1. Date Indicator Row
                    if (hasActiveDate) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { showWhenDialog = true }
                                .padding(vertical = 4.dp)
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
                        Spacer(modifier = Modifier.height(6.dp))
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
                                .clickable { showDatePicker = true }
                                .padding(vertical = 4.dp)
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
                                style = TextStyle(
                                    fontSize = bodyFontSize,
                                    color = primaryColor,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = relativeText,
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp) // Align slightly with indicator row text/icon padding
                ) {
                    if (!hasActiveDate) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = "Schedule",
                            tint = iconInactiveColor,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { showWhenDialog = true }
                        )
                    }

                    // Tag
                    if (!showTagHelper && tagInput.trim().isEmpty()) {
                        Icon(
                            imageVector = Icons.Outlined.LocalOffer,
                            contentDescription = "Tags",
                            tint = iconInactiveColor,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { showTagHelper = true }
                        )
                    }

                    // Checklist toggle
                    if (!showChecklistHelper && checklist.isEmpty()) {
                        Icon(
                            imageVector = Icons.Outlined.FormatListBulleted,
                            contentDescription = "Checklists",
                            tint = iconInactiveColor,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { showChecklistHelper = true }
                        )
                    }

                    // Flag (Deadline)
                    if (dueDate == null) {
                        Icon(
                            imageVector = Icons.Outlined.Flag,
                            contentDescription = "Set Deadline",
                            tint = iconInactiveColor,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { showDatePicker = true }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row for Trash & Done (Below the Smart Icons row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (onDelete != null) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Task",
                        tint = ThingsUpcomingRed.copy(alpha = 0.8f),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                isDeleted = true
                                onDelete()
                            }
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp)) // empty fallback
                }

                // Done / Save Quick Action button
                Text(
                    text = "Done",
                    style = TextStyle(
                        fontSize = buttonFontSize,
                        color = ThingsBlue,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .clickable {
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
                                    task.projectId,
                                    checklist,
                                    priority
                                )
                            }
                            onDone()
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
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
