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
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocalOffer
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.example.data.model.Item
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
    task: Item,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    dividerColor: Color,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    projects: List<Item>,
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
            .height(46.dp)
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
            .padding(start = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCalendarTask) {
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = "Calendar Event",
                tint = ThingsBlue,
                modifier = Modifier
                    .size(16.dp)
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
                size = 16.dp,
                modifier = Modifier
                    .testTag("task_checkbox")
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Inline note/subtask icons representation
                if (task.notes.isNotBlank() || task.checklistItemsCount > 0 || task.cachedTags.isNotBlank() || task.dueDate != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (task.notes.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = "Has notes",
                                tint = textSecondaryColor.copy(alpha = 0.4f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (task.checklistItemsCount > 0) {
                            Icon(
                                imageVector = Icons.Outlined.FormatListBulleted,
                                contentDescription = "Has checklist",
                                tint = textSecondaryColor.copy(alpha = 0.4f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (task.cachedTags.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Outlined.LocalOffer,
                                contentDescription = "Has tags",
                                tint = textSecondaryColor.copy(alpha = 0.4f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (task.dueDate != null) {
                            val delta = remember(task.dueDate) {
                                val today = java.util.Calendar.getInstance().apply {
                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }
                                val due = java.util.Calendar.getInstance().apply {
                                    timeInMillis = task.dueDate
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

                            val isOverdueOrToday = delta <= 0
                            val color = if (isOverdueOrToday) ThingsUpcomingRed else textSecondaryColor.copy(alpha = 0.4f)

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Flag,
                                    contentDescription = "Deadline Flag",
                                    tint = color,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = relativeText,
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        color = color,
                                        fontWeight = FontWeight.Normal
                                    )
                                )
                            }
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
