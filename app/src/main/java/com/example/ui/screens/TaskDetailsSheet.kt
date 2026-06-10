package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.TaskSection
import com.example.ui.theme.*
import com.example.ui.components.ThingsCheckbox
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThingsTaskDetailsSheet(
    task: ItemWithChecklist?, // If null, we are in "Create Mode"
    initialSection: TaskSection = TaskSection.INBOX,
    initialProjectId: String? = null,
    projects: List<Item>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        notes: String,
        section: TaskSection,
        isTonight: Boolean,
        startDate: Long?,
        tags: List<String>,
        projectId: String?,
        checklist: List<ChecklistItem>
    ) -> Unit,
    onDelete: (() -> Unit)? = null,
    initialTitle: String = ""
) {
    var title by remember(task?.item?.id) { mutableStateOf(task?.item?.title ?: initialTitle) }
    var notes by remember(task?.item?.id) { mutableStateOf(task?.item?.notes ?: "") }
    var section by remember(task?.item?.id) { mutableStateOf(task?.item?.section ?: initialSection) }
    var isTonight by remember(task?.item?.id) { mutableStateOf(task?.item?.isTonight ?: false) }
    var startDate by remember(task?.item?.id) { mutableStateOf(task?.item?.startDate) }
    var tagInput by remember(task?.item?.id) { mutableStateOf(task?.item?.tags?.joinToString(", ") ?: "") }
    var selectedProjectId by remember(task?.item?.id) { mutableStateOf(task?.item?.projectId ?: initialProjectId) }
    var checklist by remember(task?.item?.id, task?.checklist) { mutableStateOf<List<ChecklistItem>>(task?.checklist ?: emptyList()) }
    
    // Checklist state
    var newChecklistItemTitle by remember(task?.item?.id) { mutableStateOf("") }
    
    var showProjectDropdown by remember(task?.item?.id) { mutableStateOf(false) }

    val formattedDate = remember(startDate) {
        startDate?.let {
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it))
        } ?: "No Date"
    }

    val isDark = false
    val sheetBackground = if (isDark) ThingsSurfaceDark else Color.White
    val textPrimaryColor = if (isDark) ThingsTextPrimaryDark else ThingsTextPrimaryLight
    val textSecondaryColor = if (isDark) ThingsTextSecondaryDark else ThingsTextSecondaryLight
    val dividerColor = if (isDark) ThingsDividerDark else ThingsDividerLight

    val sheetHeaderStyle = androidx.compose.material3.MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = textSecondaryColor)
    val sheetTitleStyle = androidx.compose.material3.MaterialTheme.typography.headlineMedium.copy(color = textPrimaryColor)
    val sheetTitlePlaceholderStyle = androidx.compose.material3.MaterialTheme.typography.headlineMedium.copy(color = textSecondaryColor.copy(alpha = 0.5f))
    val sheetNotesStyle = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(color = textPrimaryColor)
    val sheetNotesPlaceholderStyle = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(color = textSecondaryColor.copy(alpha = 0.5f))
    val sheetBodyStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(color = textPrimaryColor)
    val sheetBodyPlaceholderStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(color = textSecondaryColor.copy(alpha = 0.5f))
    val sheetSmallStyle = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(color = textPrimaryColor)
    val sheetTinyStyle = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(color = textSecondaryColor)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = sheetBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(dividerColor, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .testTag("task_details_sheet")
        ) {
            // Header Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (task == null) "New To-Do" else "Edit To-Do",
                    style = sheetHeaderStyle
                )

                Row {
                    if (onDelete != null && task != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.testTag("delete_task_button")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = ThingsUpcomingRed
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val tagList = tagInput.split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                            onSave(
                                title,
                                notes,
                                section,
                                isTonight,
                                startDate,
                                tagList,
                                selectedProjectId,
                                checklist
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThingsBlue),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.testTag("save_task_button")
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Text Inputs (Index-card style - Borderless and clean)
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = sheetTitleStyle,
                cursorBrush = SolidColor(ThingsBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input"),
                decorationBox = { innerTextField ->
                    if (title.isEmpty()) {
                        Text(
                            "New To-Do",
                            style = sheetTitlePlaceholderStyle
                        )
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            BasicTextField(
                value = notes,
                onValueChange = { notes = it },
                textStyle = sheetNotesStyle,
                cursorBrush = SolidColor(ThingsBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .testTag("task_notes_input"),
                decorationBox = { innerTextField ->
                    if (notes.isEmpty()) {
                        Text(
                            "Notes",
                            style = sheetNotesPlaceholderStyle
                        )
                    }
                    innerTextField()
                }
            )

            HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 12.dp))

            // Subtasks checklist builder
            Text(
                "CHECKLIST",
                style = sheetTinyStyle.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtask rows
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(checklist) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Custom ThingsCheckbox for beautifully styled subtasks
                        ThingsCheckbox(
                            checked = item.isCompleted,
                            onCheckedChange = {
                                checklist = checklist.map {
                                    if (it.id == item.id) it.copy(isCompleted = !it.isCompleted) else it
                                }
                            },
                            size = 18.dp,
                            uncheckedColor = if (isDark) Color(0xFF48484A) else Color(0xFFC7C7CC)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = item.title,
                            style = sheetHeaderStyle.copy(
                                fontWeight = FontWeight.Normal,
                                color = if (item.isCompleted) textSecondaryColor else textPrimaryColor,
                                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete Subtask",
                            tint = textSecondaryColor.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    checklist = checklist.filter { it.id != item.id }
                                }
                        )
                    }
                }

                item {
                    // Inline input to add subtask instantly
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Checklist",
                            tint = ThingsBlue,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        BasicTextField(
                            value = newChecklistItemTitle,
                            onValueChange = { newChecklistItemTitle = it },
                            textStyle = sheetHeaderStyle.copy(fontWeight = FontWeight.Normal, color = textPrimaryColor),
                            cursorBrush = SolidColor(ThingsBlue),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (newChecklistItemTitle.isNotBlank()) {
                                    checklist = checklist + ChecklistItem(itemId = task?.item?.id ?: "", title = newChecklistItemTitle.trim())
                                    newChecklistItemTitle = ""
                                }
                            }),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("subtask_input"),
                            decorationBox = { innerTextField ->
                                if (newChecklistItemTitle.isEmpty()) {
                                    Text(
                                        "Add Checklist Item...",
                                        style = sheetHeaderStyle.copy(fontWeight = FontWeight.Normal, color = textSecondaryColor.copy(alpha = 0.5f))
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 12.dp))

            // Task Meta selectors (Section / Project / Date / Tags)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Section Selector (Inbox, Today, etc)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "ORGANIZATION",
                        style = sheetTinyStyle.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Horizontal Section Pill choice
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TaskSection.values().forEach { sec ->
                            val isSelected = section == sec
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) ThingsBlue.copy(alpha = 0.15f) else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isSelected) ThingsBlue else dividerColor,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { section = sec }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = sec.name.lowercase().capitalize(),
                                    style = sheetTinyStyle.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) ThingsBlue else textPrimaryColor
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Date & Tonight Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Quick date pick
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.CalendarToday,
                        contentDescription = "Due Date",
                        tint = ThingsUpcomingRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "When: $formattedDate",
                        style = sheetBodyStyle,
                        modifier = Modifier.clickable {
                            // Automatically select Today or flip date
                            startDate = if (startDate == null) System.currentTimeMillis() else null
                        }
                    )
                }

                // Evening toggle (Things iconic Tonight section)
                if (section == TaskSection.TODAY) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isTonight) ThingsSomedayGrey.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { isTonight = !isTonight }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Brightness3,
                            contentDescription = "Tonight",
                            tint = ThingsSomedayGrey,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "This Evening",
                            style = sheetSmallStyle.copy(
                                fontWeight = FontWeight.Medium,
                                color = if (isTonight) ThingsBlue else textPrimaryColor
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Project Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.FolderOpen,
                        contentDescription = "Project",
                        tint = ThingsInboxBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    val projName = projects.firstOrNull { it.id == selectedProjectId }?.name ?: "No Project"
                    Text(
                        text = "Project: $projName",
                        style = sheetBodyStyle,
                        modifier = Modifier.clickable { showProjectDropdown = true }
                    )
                }

                Box {
                    DropdownMenu(
                        expanded = showProjectDropdown,
                        onDismissRequest = { showProjectDropdown = false },
                        containerColor = sheetBackground
                    ) {
                        DropdownMenuItem(
                            text = { Text("No Project", color = textPrimaryColor) },
                            onClick = {
                                selectedProjectId = null
                                showProjectDropdown = false
                            }
                        )
                        projects.forEach { proj ->
                            DropdownMenuItem(
                                text = { Text(proj.name, color = textPrimaryColor) },
                                onClick = {
                                    selectedProjectId = proj.id
                                    if (section == TaskSection.INBOX) {
                                        section = TaskSection.ANYTIME
                                    }
                                    showProjectDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tags edit row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.LocalOffer,
                    contentDescription = "Tags",
                    tint = ThingsAnytimeTeal,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    textStyle = sheetBodyStyle,
                    cursorBrush = SolidColor(ThingsBlue),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (tagInput.isEmpty()) {
                            Text(
                                "Tags (comma-separated, e.g. Work, Urgent)",
                                style = sheetBodyPlaceholderStyle
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}
