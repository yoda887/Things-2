package com.example.ui.screens.home.inlineeditor.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.data.model.ChecklistItem
import com.example.data.model.TaskSection
import com.example.ui.theme.ThingsBlue
import com.example.ui.theme.ThingsTodayStar
import com.example.ui.theme.ThingsSomedayGrey
import com.example.ui.screens.home.inlineeditor.utils.isTodayDate
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InlineEditorToolbar(
    startDate: Long?,
    section: TaskSection,
    isTonight: Boolean,
    onShowWhenDialogChange: (Boolean) -> Unit,
    showTagHelper: Boolean,
    onShowTagHelperChange: (Boolean) -> Unit,
    tagInput: String,
    showChecklistHelper: Boolean,
    onShowChecklistHelperChange: (Boolean) -> Unit,
    checklist: List<ChecklistItem>,
    showPriorityHelper: Boolean,
    onShowPriorityHelperChange: (Boolean) -> Unit,
    priority: Int
) {
    val textPrimaryColor = Color(0xFF1C1C1E)
    val iconInactiveColor = Color(0xFFC7C7CC)
    val bodyFontSize = MaterialTheme.typography.bodyLarge.fontSize

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val hasActiveDate = startDate != null || section == TaskSection.TODAY || section == TaskSection.SOMEDAY
        if (hasActiveDate) {
            val activeDateLabel = when {
                startDate != null && isTodayDate(startDate) -> {
                    if (isTonight) "This Evening" else "Today"
                }
                startDate == null && section == TaskSection.TODAY -> {
                    if (isTonight) "This Evening" else "Today"
                }
                section == TaskSection.SOMEDAY -> "Someday"
                else -> {
                    val targetCal = Calendar.getInstance().apply { timeInMillis = startDate!! }
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    val formatPattern = if (targetCal.get(Calendar.YEAR) == currentYear) {
                        "EEE, MMM d"
                    } else {
                        "EEE, MMM d, yyyy"
                    }
                    SimpleDateFormat(formatPattern, Locale.US).format(Date(startDate!!))
                }
            }

            val activeDateIcon = when {
                startDate != null && isTodayDate(startDate) -> {
                    if (isTonight) Icons.Outlined.Brightness3 else Icons.Default.Star
                }
                startDate == null && section == TaskSection.TODAY -> {
                    if (isTonight) Icons.Outlined.Brightness3 else Icons.Default.Star
                }
                section == TaskSection.SOMEDAY -> Icons.Outlined.Archive
                else -> Icons.Outlined.CalendarToday
            }

            val activeDateColor = when {
                startDate != null && isTodayDate(startDate) -> {
                    if (isTonight) ThingsBlue else ThingsTodayStar
                }
                startDate == null && section == TaskSection.TODAY -> {
                    if (isTonight) ThingsBlue else ThingsTodayStar
                }
                section == TaskSection.SOMEDAY -> ThingsSomedayGrey
                else -> ThingsBlue
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onShowWhenDialogChange(true) }
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
            //Icon(
           //     imageVector = Icons.Outlined.CalendarToday,
            //    contentDescription = "Schedule",
            //    tint = iconInactiveColor,
            //    modifier = Modifier
            //        .size(22.dp)
               //     .clickable { onShowWhenDialogChange(true) }
            //)
        }

        Spacer(modifier = Modifier.weight(1f))

        // Right side: Tag, Checklist, Priority/Flag
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

             if (!hasActiveDate) {
             Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = "Schedule",
                tint = iconInactiveColor,
                modifier = Modifier
                    .size(22.dp)
                    .clickable { onShowWhenDialogChange(true) }
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
                        .clickable { onShowTagHelperChange(true) }
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
                        .clickable { onShowChecklistHelperChange(true) }
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
                        .clickable { onShowPriorityHelperChange(true) }
                )
            }
        }
    }
}
