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
import com.example.data.model.ChecklistItem
import com.example.data.model.Item
import com.example.data.model.TaskSection
import com.example.ui.theme.ThingsBlue
import com.example.ui.theme.ThingsUpcomingRed
import com.example.ui.screens.home.inlineeditor.components.*
import com.example.ui.screens.home.inlineeditor.dialogs.ThingsWhenDialog

@Composable
fun ThingsTaskInlineEditor(
    task: Item,
    projects: List<Item>,
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
    var title by remember { mutableStateOf<String>(task.title) }
    var notes by remember { mutableStateOf<String>(task.notes) }
    var section by remember { mutableStateOf<TaskSection>(task.section) }
    var isTonight by remember { mutableStateOf<Boolean>(task.isTonight) }
    var dueDate by remember { mutableStateOf<Long?>(task.dueDate) }
    var tagInput by remember { mutableStateOf<String>(task.tags.joinToString(", ")) }
    var checklist by remember { mutableStateOf<List<ChecklistItem>>(task.checklist) }
    var priority by remember { mutableStateOf<Int>(task.priority) }

    // Helpers visibility states
    var showCalendarHelper by remember { mutableStateOf(false) }
    var showWhenDialog by remember { mutableStateOf(false) }
    var showTagHelper by remember { mutableStateOf(false) }
    var showChecklistHelper by remember { mutableStateOf(task.checklist.isNotEmpty()) }
    var showPriorityHelper by remember { mutableStateOf(false) }

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

            // Priority Selector Helper Panel (Toggled by Flag icon)
            InlinePriorityPanel(
                priority = priority,
                onPriorityChange = { priority = it },
                showPriorityHelper = showPriorityHelper,
                onShowPriorityHelperChange = { showPriorityHelper = it }
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
            InlineEditorToolbar(
                dueDate = dueDate,
                section = section,
                isTonight = isTonight,
                onShowWhenDialogChange = { showWhenDialog = it },
                showTagHelper = showTagHelper,
                onShowTagHelperChange = { showTagHelper = it },
                tagInput = tagInput,
                showChecklistHelper = showChecklistHelper,
                onShowChecklistHelperChange = { showChecklistHelper = it },
                checklist = checklist,
                showPriorityHelper = showPriorityHelper,
                onShowPriorityHelperChange = { showPriorityHelper = it },
                priority = priority
            )

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

    if (showWhenDialog) {
        ThingsWhenDialog(
            dueDate = dueDate,
            onDueDateChange = { dueDate = it },
            section = section,
            onSectionChange = { section = it },
            isTonight = isTonight,
            onIsTonightChange = { isTonight = it },
            onShowCalendarHelperChange = { showCalendarHelper = it },
            onDismissRequest = { showWhenDialog = false }
        )
    }
}
