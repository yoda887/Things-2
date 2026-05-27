package com.example.ui.screens.home.subcomponents

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.example.data.model.Project
import com.example.data.model.Task
import com.example.ui.components.ThingsCheckbox
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * TaskItemRow: Renders the complete, highly responsive checklist item with custom 
 * elevation feedback transitions, note flags, and tag chips..
 */

@Composable
fun TaskItemRow(
    modifier: Modifier = Modifier,
    task: Task,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    dividerColor: Color,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    projects: List<Project>,
    showTodayIndicator: Boolean = false,
    isDragging: Boolean = false,
    dragOffsetY: Float = 0f,
    dragModifier: Modifier = Modifier
) {
    val isDark = false
    val titleFontSize = androidx.compose.material3.MaterialTheme.typography.titleMedium.fontSize
    val subFontSize = androidx.compose.material3.MaterialTheme.typography.bodySmall.fontSize

    val scope = rememberCoroutineScope()
    var localCompleted by remember(task.isCompleted) { mutableStateOf(task.isCompleted) }

    val scale by androidx.compose.animation.core.animateFloatAsState(if (isDragging) 1.04f else 1.0f)
    val elevation by androidx.compose.animation.core.animateDpAsState(if (isDragging) 6.dp else 0.dp)

    val isCalendarTask = task.id.startsWith("cal_")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = dragOffsetY
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation, RoundedCornerShape(8.dp))
            .background(if (isDragging) (if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)) else Color.Transparent, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .then(dragModifier)
            .clickable(enabled = !isCalendarTask) { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (isCalendarTask) {
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = "Calendar Event",
                tint = ThingsBlue,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(22.dp)
            )
        } else {
            ThingsCheckbox(
                checked = localCompleted,
                onCheckedChange = {
                    localCompleted = !localCompleted
                    scope.launch {
                        delay(220)
                        onToggle()
                    }
                },
                modifier = Modifier
                    .padding(top = 2.dp)
                    .testTag("task_checkbox")
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = task.title,
                    style = TextStyle(
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.Normal,
                        color = if (localCompleted) textSecondaryColor else textPrimaryColor,
                        textDecoration = if (localCompleted) TextDecoration.LineThrough else null
                    ),
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Inline note/subtask icons representation
                if (task.notes.isNotBlank() || task.checklist.isNotEmpty() || task.priority > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (task.priority > 0) {
                            val flagColor = when (task.priority) {
                                3 -> ThingsUpcomingRed
                                2 -> ThingsTodayStar
                                1 -> ThingsAnytimeTeal
                                else -> textSecondaryColor.copy(alpha = 0.4f)
                            }
                            Icon(
                                imageVector = Icons.Filled.Flag,
                                contentDescription = "Priority Flag",
                                tint = flagColor,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (task.notes.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = "Has notes",
                                tint = textSecondaryColor.copy(alpha = 0.4f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (task.checklist.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Outlined.FormatListBulleted,
                                contentDescription = "Has checklist",
                                tint = textSecondaryColor.copy(alpha = 0.4f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            // Project link displayed underneath title, matching screenshot
            val project = remember(task.projectId, projects) {
                projects.firstOrNull { it.id == task.projectId }
            }
            if (project != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = project.name,
                    style = TextStyle(
                        fontSize = subFontSize,
                        color = textSecondaryColor.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Normal
                    )
                )
            }
        }

        
        

        
    }
}
