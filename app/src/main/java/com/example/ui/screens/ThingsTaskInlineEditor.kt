package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChecklistItem
import com.example.data.model.Project
import com.example.data.model.Task
import com.example.data.model.TaskSection
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
// Renders a high-fidelity inline task editor matching the user's requirements & visual layout sketch.
// FORCE white background & black fonts as requested by user ("белый фон, а шрифты черные")
fun ThingsTaskInlineEditor(
    task: Task,
    projects: List<Project>,
    onSave: (
        title: String,
        notes: String,
        section: TaskSection,
        isTonight: Boolean,
        dueDate: Long?,
        tags: List<String>,
        projectId: String?,
        checklist: List<ChecklistItem>,
        priority: Int
    ) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDone: () -> Unit = {}
) {
    var title by remember { mutableStateOf(task.title) }
    var notes by remember { mutableStateOf(task.notes) }
    var section by remember { mutableStateOf(task.section) }
    var isTonight by remember { mutableStateOf(task.isTonight) }
    var dueDate by remember { mutableStateOf(task.dueDate) }
    var tagInput by remember { mutableStateOf(task.tags.joinToString(", ")) }
    var checklist by remember { mutableStateOf(task.checklist) }
    var priority by remember { mutableStateOf(task.priority) }

    // Helpers visibility states
    var showCalendarHelper by remember { mutableStateOf(false) }
    var showWhenDialog by remember { mutableStateOf(false) }
    var calendarWeekOffset by remember { mutableStateOf(0) }
    var showTagHelper by remember { mutableStateOf(false) }
    var showChecklistHelper by remember { mutableStateOf(task.checklist.isNotEmpty()) }
    var showPriorityHelper by remember { mutableStateOf(false) }

    // Checklist interactive state
    var newChecklistItemTitle by remember { mutableStateOf("") }

    var isDeleted by remember { mutableStateOf(false) }
    var isSavedManually by remember { mutableStateOf(false) }

    val currentTitle by rememberUpdatedState(title)
    val currentNotes by rememberUpdatedState(notes)
    val currentSection by rememberUpdatedState(section)
    val currentIsTonight by rememberUpdatedState(isTonight)
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
    val textPrimaryColor = Color(0xFF1C1C1E) // blackish font
    val textSecondaryColor = Color(0xFF747679) // dark grey font
    val helperBgColor = Color(0xFFF2F2F7) // light grey panel background
    val helperHintColor = Color(0xFF8E8E93)

    // Proportional font sizing from Material 3 Typography slots
    val titleFontSize = androidx.compose.material3.MaterialTheme.typography.headlineSmall.fontSize
    val notesFontSize = androidx.compose.material3.MaterialTheme.typography.titleSmall.fontSize
    val bodyFontSize = androidx.compose.material3.MaterialTheme.typography.bodyLarge.fontSize
    val tagFontSize = androidx.compose.material3.MaterialTheme.typography.bodyMedium.fontSize
    val buttonFontSize = androidx.compose.material3.MaterialTheme.typography.labelLarge.fontSize
    val smallFontSize = androidx.compose.material3.MaterialTheme.typography.labelMedium.fontSize
    val tinyFontSize = androidx.compose.material3.MaterialTheme.typography.labelSmall.fontSize

    // Determine priority color & name representation
    val priorityName = when (priority) {
        3 -> "High Priority"
        2 -> "Medium Priority"
        1 -> "Low Priority"
        else -> "No Priority"
    }

    val priorityColor = when (priority) {
        3 -> ThingsUpcomingRed
        2 -> ThingsTodayStar
        1 -> ThingsAnytimeTeal
        else -> Color(0xFF8E8E93)
    }

    val iconActiveColor = ThingsBlue
    val iconInactiveColor = Color(0xFFC7C7CC)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_inline_editor"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = editorBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Main Top Content: Checkbox, Title and Notes
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Checkbox matching the custom square outline in the image
                val checkboxBorderColor = if (task.isCompleted) ThingsBlue else Color(0xFFC7C7CC)
                val checkboxBgColor = if (task.isCompleted) ThingsBlue else Color.Transparent

                Box(
                    modifier = Modifier
                        .padding(top = 2.dp, end = 12.dp)
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(checkboxBgColor)
                        .border(1.5.dp, checkboxBorderColor, RoundedCornerShape(4.dp))
                        .clickable {
                            isSavedManually = true
                            if (title.isBlank() && notes.isBlank() && checklist.isEmpty()) {
                                onDelete?.invoke()
                            } else {
                                onSave(
                                    title,
                                    notes,
                                    section,
                                    isTonight,
                                    dueDate,
                                    tagInput.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                    task.projectId,
                                    checklist,
                                    priority
                                )
                            }
                            onDone()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Title and Notes Fields (Always black text on white background)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        textStyle = TextStyle(
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.Normal,
                            color = textPrimaryColor
                        ),
                        cursorBrush = SolidColor(ThingsBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_title_input"),
                        decorationBox = { innerTextField ->
                            if (title.isEmpty()) {
                                Text(
                                    "New To-Do",
                                    style = TextStyle(
                                        fontSize = titleFontSize,
                                        fontWeight = FontWeight.Normal,
                                        color = textSecondaryColor.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    BasicTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        textStyle = TextStyle(
                            fontSize = notesFontSize,
                            fontWeight = FontWeight.Normal,
                            color = textSecondaryColor
                        ),
                        cursorBrush = SolidColor(ThingsBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 24.dp)
                            .testTag("task_notes_input"),
                        decorationBox = { innerTextField ->
                            if (notes.isEmpty()) {
                                Text(
                                    "Notes",
                                    style = TextStyle(
                                        fontSize = notesFontSize,
                                        fontWeight = FontWeight.Normal,
                                        color = textSecondaryColor.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            // Tag Helper: Sleek input block
            AnimatedVisibility(
                visible = showTagHelper || tagInput.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, top = 10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(helperBgColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocalOffer,
                        contentDescription = "Tags",
                        tint = ThingsAnytimeTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        textStyle = TextStyle(fontSize = tagFontSize, color = textPrimaryColor),
                        cursorBrush = SolidColor(ThingsBlue),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (tagInput.isEmpty()) {
                                Text(
                                    "Tags (e.g., Work, Home)",
                                    style = TextStyle(fontSize = tagFontSize, color = helperHintColor)
                                )
                            }
                            innerTextField()
                        }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Tags",
                        tint = textSecondaryColor.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(14.dp)
                            .clickable {
                                tagInput = ""
                                showTagHelper = false
                            }
                    )
                }
            }

            // Priority Selector Helper Panel (Toggled by Flag icon)
            AnimatedVisibility(
                visible = showPriorityHelper || priority > 0,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, top = 10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(helperBgColor)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (priority > 0) Icons.Filled.Flag else Icons.Outlined.Flag,
                        contentDescription = "Priority",
                        tint = priorityColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = priorityName,
                        style = TextStyle(fontSize = smallFontSize, color = textPrimaryColor),
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Quick Action Buttons to choose priority level
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "No",
                            style = TextStyle(
                                fontSize = tinyFontSize,
                                color = if (priority == 0) ThingsBlue else ThingsSomedayGrey,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .clickable { priority = 0 }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        Text(
                            text = "Low",
                            style = TextStyle(
                                fontSize = tinyFontSize,
                                color = if (priority == 1) ThingsAnytimeTeal else ThingsSomedayGrey,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .clickable { priority = 1 }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        Text(
                            text = "Med",
                            style = TextStyle(
                                fontSize = tinyFontSize,
                                color = if (priority == 2) ThingsTodayStar else ThingsSomedayGrey,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .clickable { priority = 2 }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        Text(
                            text = "High",
                            style = TextStyle(
                                fontSize = tinyFontSize,
                                color = if (priority == 3) ThingsUpcomingRed else ThingsSomedayGrey,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .clickable { priority = 3 }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Priority",
                        tint = textSecondaryColor.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(14.dp)
                            .clickable {
                                showPriorityHelper = false
                            }
                    )
                }
            }

            // Checklist Items Panel
            AnimatedVisibility(
                visible = showChecklistHelper || checklist.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, top = 10.dp)
                ) {
                    checklist.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val itemBorderColor = if (item.isCompleted) ThingsBlue else Color(0xFFD1D1D6)
                            val itemBgColor = if (item.isCompleted) ThingsBlue else Color.Transparent

                            Box(
                                modifier = Modifier
                                    .size(15.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(itemBgColor)
                                    .border(1.2.dp, itemBorderColor, RoundedCornerShape(3.dp))
                                    .clickable {
                                        checklist = checklist.mapIndexed { idx, checklistItem ->
                                            if (idx == index) checklistItem.copy(isCompleted = !checklistItem.isCompleted)
                                            else checklistItem
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.isCompleted) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Editable checklist item title inline
                            BasicTextField(
                                value = item.title,
                                onValueChange = { updatedTitle ->
                                    checklist = checklist.mapIndexed { idx, checklistItem ->
                                        if (idx == index) checklistItem.copy(title = updatedTitle)
                                        else checklistItem
                                    }
                                },
                                textStyle = TextStyle(
                                    fontSize = bodyFontSize,
                                    color = if (item.isCompleted) textSecondaryColor else textPrimaryColor,
                                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                cursorBrush = SolidColor(ThingsBlue),
                                modifier = Modifier.weight(1f)
                            )

                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove Item",
                                tint = textSecondaryColor.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable {
                                        checklist = checklist.filterIndexed { idx, _ -> idx != index }
                                    }
                            )
                        }
                    }

                    // Add inline checklist item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Checklist",
                            tint = ThingsBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = newChecklistItemTitle,
                            onValueChange = { newChecklistItemTitle = it },
                            textStyle = TextStyle(fontSize = bodyFontSize, color = textPrimaryColor),
                            cursorBrush = SolidColor(ThingsBlue),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (newChecklistItemTitle.isNotBlank()) {
                                    checklist = checklist + ChecklistItem(title = newChecklistItemTitle.trim())
                                    newChecklistItemTitle = ""
                                }
                            }),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (newChecklistItemTitle.isEmpty()) {
                                    Text(
                                        "Add Checklist Item...",
                                        style = TextStyle(fontSize = bodyFontSize, color = textSecondaryColor.copy(alpha = 0.4f))
                                    )
                                }
                                innerTextField()
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Checklist Helper",
                            tint = textSecondaryColor.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable {
                                    showChecklistHelper = false
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Actions & Toolbar matching the image closely
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hasActiveDate = dueDate != null || section == TaskSection.TODAY || section == TaskSection.SOMEDAY
                if (hasActiveDate) {
                    val activeDateLabel = when {
                        dueDate != null && isTodayDate(dueDate) -> {
                            if (isTonight) "This Evening" else "Today"
                        }
                        dueDate == null && section == TaskSection.TODAY -> {
                            if (isTonight) "This Evening" else "Today"
                        }
                        section == TaskSection.SOMEDAY -> "Someday"
                        else -> {
                            val targetCal = Calendar.getInstance().apply { timeInMillis = dueDate!! }
                            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                            val formatPattern = if (targetCal.get(Calendar.YEAR) == currentYear) {
                                "EEE, MMM d"
                            } else {
                                "EEE, MMM d, yyyy"
                            }
                            SimpleDateFormat(formatPattern, Locale.US).format(Date(dueDate!!))
                        }
                    }

                    val activeDateIcon = when {
                        dueDate != null && isTodayDate(dueDate) -> {
                            if (isTonight) Icons.Outlined.Brightness3 else Icons.Default.Star
                        }
                        dueDate == null && section == TaskSection.TODAY -> {
                            if (isTonight) Icons.Outlined.Brightness3 else Icons.Default.Star
                        }
                        section == TaskSection.SOMEDAY -> Icons.Outlined.Archive
                        else -> Icons.Outlined.CalendarToday
                    }

                    val activeDateColor = when {
                        dueDate != null && isTodayDate(dueDate) -> {
                            if (isTonight) ThingsBlue else ThingsTodayStar
                        }
                        dueDate == null && section == TaskSection.TODAY -> {
                            if (isTonight) ThingsBlue else ThingsTodayStar
                        }
                        section == TaskSection.SOMEDAY -> ThingsSomedayGrey
                        else -> ThingsBlue
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
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
                } else {
                    // Show Calendar symbol on the left
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = "Schedule",
                        tint = iconInactiveColor,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { showWhenDialog = true }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Right side: Tag, Checklist, Priority/Flag
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                    // Flag (Priority)
                    if (!showPriorityHelper && priority == 0) {
                        Icon(
                            imageVector = Icons.Outlined.Flag,
                            contentDescription = "Priority",
                            tint = iconInactiveColor,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { showPriorityHelper = true }
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

    androidx.compose.runtime.LaunchedEffect(showWhenDialog) {
        if (showWhenDialog) {
            val todayStartSunday = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (dueDate != null && section == TaskSection.UPCOMING) {
                val targetStartSunday = Calendar.getInstance().apply {
                    timeInMillis = dueDate!!
                    set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                
                var diffWeeks = 0
                val tempCal = todayStartSunday.clone() as Calendar
                if (tempCal.before(targetStartSunday)) {
                    while (tempCal.before(targetStartSunday)) {
                        tempCal.add(Calendar.WEEK_OF_YEAR, 1)
                        diffWeeks++
                    }
                }
                calendarWeekOffset = maxOf(0, diffWeeks)
            } else {
                calendarWeekOffset = 0
            }
        }
    }

    if (showWhenDialog) {
        val isTodayActive = dueDate != null && isTodayDate(dueDate) && section == TaskSection.TODAY && !isTonight
        val isThisEveningActive = dueDate != null && isTodayDate(dueDate) && section == TaskSection.TODAY && isTonight
        val isSomedayActive = dueDate == null && section == TaskSection.SOMEDAY

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showWhenDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF22242C)
                ),
                modifier = Modifier
                    .width(320.dp)
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top bar: "When?" and "Cancel"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "When?",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = titleFontSize,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Cancel",
                            style = TextStyle(
                                color = Color(0xFF8E8E93),
                                fontSize = notesFontSize,
                                fontWeight = FontWeight.Normal
                            ),
                            modifier = Modifier.clickable { showWhenDialog = false }
                        )
                    }

                    // 1. Today Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (isTodayActive) {
                                    dueDate = null
                                    section = TaskSection.ANYTIME
                                    isTonight = false
                                    showCalendarHelper = false
                                } else {
                                    dueDate = System.currentTimeMillis()
                                    section = TaskSection.TODAY
                                    isTonight = false
                                    showCalendarHelper = true
                                }
                                showWhenDialog = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⭐",
                            fontSize = titleFontSize,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = "Today",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = androidx.compose.material3.MaterialTheme.typography.titleLarge.fontSize,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        if (isTodayActive) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active Today",
                                tint = ThingsBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // 2. This Evening Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (isThisEveningActive) {
                                    dueDate = null
                                    section = TaskSection.ANYTIME
                                    isTonight = false
                                    showCalendarHelper = false
                                } else {
                                    dueDate = System.currentTimeMillis()
                                    section = TaskSection.TODAY
                                    isTonight = true
                                    showCalendarHelper = true
                                }
                                showWhenDialog = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🌙",
                            fontSize = titleFontSize,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = "This Evening",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = androidx.compose.material3.MaterialTheme.typography.titleLarge.fontSize,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        if (isThisEveningActive) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active This Evening",
                                tint = ThingsBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Weekday headers: "Sun Mon Tue Wed Thu Fri Sat"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                        weekdays.forEach { dayName ->
                            Text(
                                text = dayName,
                                style = TextStyle(
                                    color = Color(0xFF5F6368),
                                    fontSize = smallFontSize,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                             )
                        }
                    }

                    // Calendar cells
                    val todayCal = Calendar.getInstance()
                    val todayStartSunday = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val cells = remember(calendarWeekOffset) {
                        val list = mutableListOf<CalendarCell>()
                        val tempCal = (todayStartSunday.clone() as Calendar).apply {
                            add(Calendar.WEEK_OF_YEAR, calendarWeekOffset)
                        }
                        
                        for (i in 0 until 28) {
                            val dNum = tempCal.get(Calendar.DAY_OF_MONTH)
                            val cMonth = tempCal.get(Calendar.MONTH)
                            val cYear = tempCal.get(Calendar.YEAR)
                            
                            val isTod = (cYear == todayCal.get(Calendar.YEAR) &&
                                         cMonth == todayCal.get(Calendar.MONTH) &&
                                         dNum == todayCal.get(Calendar.DAY_OF_MONTH))
                                         
                            val mLabel = if (dNum == 1 || (i == 0 && calendarWeekOffset == 0) || (i == 1 && calendarWeekOffset > 0)) {
                                SimpleDateFormat("MMM", Locale.US).format(tempCal.time)
                            } else null
                            
                            val cTime = tempCal.timeInMillis
                            val isPast = isPastDate(cYear, cMonth, dNum, todayCal)
                            
                            if (i == 0 && calendarWeekOffset > 0) {
                                list.add(CalendarCell.PrevMonth)
                            } else if (i == 27) {
                                list.add(CalendarCell.NextMonth)
                            } else if (isPast) {
                                list.add(CalendarCell.Empty)
                            } else {
                                list.add(
                                    CalendarCell.Day(
                                        day = dNum,
                                        isToday = isTod,
                                        monthLabel = mLabel,
                                        timestamp = cTime
                                    )
                                )
                            }
                            tempCal.add(Calendar.DATE, 1)
                        }
                        list
                    }

                    // Display standard 4 rows of 7 items
                    for (rowIdx in 0 until 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (colIdx in 0 until 7) {
                                val cellIdx = rowIdx * 7 + colIdx
                                val cell = cells.getOrNull(cellIdx) ?: CalendarCell.Empty
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (cell) {
                                        is CalendarCell.Empty -> {
                                            // Empty past day
                                        }
                                        is CalendarCell.PrevMonth -> {
                                            Icon(
                                                imageVector = Icons.Default.ChevronLeft,
                                                contentDescription = "Previous Month",
                                                tint = ThingsBlue,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clickable {
                                                        calendarWeekOffset = maxOf(0, calendarWeekOffset - 3)
                                                    }
                                            )
                                        }
                                        is CalendarCell.NextMonth -> {
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = "Next Month",
                                                tint = ThingsBlue,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clickable {
                                                        calendarWeekOffset += 3
                                                    }
                                            )
                                        }
                                        is CalendarCell.Day -> {
                                            val isSelected = isSameDay(dueDate, cell.timestamp) && section == TaskSection.UPCOMING
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .then(
                                                        if (isSelected) {
                                                            Modifier.border(1.5.dp, ThingsBlue, RoundedCornerShape(6.dp))
                                                        } else {
                                                            Modifier
                                                        }
                                                    )
                                                    .clickable {
                                                        if (isSelected) {
                                                            // Toggle off -> Next state (ANYTIME)
                                                            dueDate = null
                                                            section = TaskSection.ANYTIME
                                                            isTonight = false
                                                            showCalendarHelper = false
                                                        } else {
                                                            dueDate = cell.timestamp
                                                            section = if (isTodayDate(cell.timestamp)) TaskSection.TODAY else TaskSection.UPCOMING
                                                            isTonight = false
                                                            showCalendarHelper = true
                                                        }
                                                        showWhenDialog = false
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        val monthLabel = SimpleDateFormat("MMM", Locale.US).format(Date(cell.timestamp))
                                                        Text(
                                                            text = monthLabel,
                                                            style = TextStyle(
                                                                color = ThingsBlue,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        )
                                                        Text(
                                                            text = cell.day.toString(),
                                                            style = TextStyle(
                                                                color = Color.White,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        )
                                                    }
                                                } else if (cell.isToday) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Star,
                                                        contentDescription = "Today",
                                                        tint = Color(0xFF7E8494),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                } else {
                                                    if (cell.monthLabel != null) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center
                                                        ) {
                                                            Text(
                                                                text = cell.monthLabel,
                                                                style = TextStyle(
                                                                    color = ThingsBlue,
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            )
                                                            Text(
                                                                text = cell.day.toString(),
                                                                style = TextStyle(
                                                                    color = Color.White,
                                                                    fontSize = 13.sp,
                                                                    fontWeight = FontWeight.Medium
                                                                )
                                                            )
                                                        }
                                                    } else {
                                                        Text(
                                                            text = cell.day.toString(),
                                                            style = TextStyle(
                                                                color = Color.White,
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.Normal
                                                            )
                                                        )
                                                     }
                                                 }
                                             }
                                         }
                                     }
                                 }
                             }
                        }
                    }

                    // 3. Someday Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (isSomedayActive) {
                                    dueDate = null
                                    section = TaskSection.ANYTIME
                                    isTonight = false
                                    showCalendarHelper = false
                                } else {
                                    dueDate = null
                                    section = TaskSection.SOMEDAY
                                    isTonight = false
                                    showCalendarHelper = true
                                }
                                showWhenDialog = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📦",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = "Someday",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        if (isSomedayActive) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active Someday",
                                tint = ThingsBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // 4. Add Reminder
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFF5F6368),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Add Reminder",
                            style = TextStyle(
                                color = Color(0xFF5F6368),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                }
            }
        }
    }
}

sealed class CalendarCell {
    object Empty : CalendarCell()
    object PrevMonth : CalendarCell()
    object NextMonth : CalendarCell()
    data class Day(
        val day: Int,
        val isToday: Boolean,
        val monthLabel: String?,
        val timestamp: Long
    ) : CalendarCell()
}

fun isPastDate(year: Int, month: Int, day: Int, today: Calendar): Boolean {
    val todayYear = today.get(Calendar.YEAR)
    val todayMonth = today.get(Calendar.MONTH)
    val todayDay = today.get(Calendar.DAY_OF_MONTH)
    
    if (year < todayYear) return true
    if (year > todayYear) return false
    
    if (month < todayMonth) return true
    if (month > todayMonth) return false
    
    return day < todayDay
}

fun isTodayDate(timestamp: Long?): Boolean {
    if (timestamp == null) return false
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    return cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
           cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
}

fun isSameDay(t1: Long?, t2: Long): Boolean {
    if (t1 == null) return false
    val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
