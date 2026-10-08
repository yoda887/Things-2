package com.example.ui.screens.home.inlineeditor.components

import com.example.ui.theme.ThingsSpacing
import com.example.ui.theme.ThingsIconSize
import com.example.R
import androidx.compose.ui.res.stringResource
import com.example.ui.theme.ThingsTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ChecklistItem
import com.example.data.model.TaskSection
import com.example.ui.theme.AppIcons
import com.example.ui.theme.dimens
import com.example.ui.screens.home.inlineeditor.utils.isTodayDateOrPast
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView

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
    dueDate: Long?,
    onShowDatePickerChange: (Boolean) -> Unit
) {
    val textPrimaryColor = ThingsTheme.colors.editorText
    val iconInactiveColor = ThingsTheme.colors.checkboxBorder
    val view = LocalView.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val hasActiveDate = startDate != null || section == TaskSection.TODAY || section == TaskSection.SOMEDAY
        if (hasActiveDate) {
            val activeDateLabel = com.example.ui.screens.home.inlineeditor.utils.formatStartDateLabel(startDate, section, isTonight)

            val activeDateIcon = when {
                startDate != null && isTodayDateOrPast(startDate) -> {
                    if (isTonight) AppIcons.Evening else AppIcons.Today
                }
                startDate == null && section == TaskSection.TODAY -> {
                    if (isTonight) AppIcons.Evening else AppIcons.Today
                }
                section == TaskSection.SOMEDAY -> AppIcons.Someday
                else -> AppIcons.Upcoming
            }

            val activeDateColor = when {
                startDate != null && isTodayDateOrPast(startDate) -> {
                    if (isTonight) Color.Unspecified else ThingsTheme.colors.today
                }
                startDate == null && section == TaskSection.TODAY -> {
                    if (isTonight) Color.Unspecified else ThingsTheme.colors.today
                }
                section == TaskSection.SOMEDAY -> ThingsTheme.colors.someday
                else -> Color.Unspecified
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(ThingsTheme.shapes.smallShape)
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onShowWhenDialogChange(true)
                    }
                    .padding(vertical = ThingsSpacing.XS)
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
            }
        } else {
            // Show Calendar symbol on the left
            //Icon(
           //     imageVector = AppIcons.Upcoming,
            //    contentDescription = stringResource(R.string.cd_schedule),
            //    tint = iconInactiveColor,
            //    modifier = Modifier
            //        .size(ThingsIconSize.L)
               //     .clickable { onShowWhenDialogChange(true) }
            //)
        }

        Spacer(modifier = Modifier.weight(1f))

        // Right side: Tag, Checklist, Priority/Flag
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.taskEditorActionIconsSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {

             if (!hasActiveDate) {
             Icon(
                imageVector = AppIcons.Upcoming,
                contentDescription = stringResource(R.string.cd_schedule),
                tint = iconInactiveColor,
                modifier = Modifier
                    .size(ThingsIconSize.L)
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onShowWhenDialogChange(true)
                    }
            )
             
             }
            // Tag
            if (!showTagHelper && tagInput.trim().isEmpty()) {
                Icon(
                    imageVector = AppIcons.Tag,
                    contentDescription = stringResource(R.string.tag_dialog_title),
                    tint = iconInactiveColor,
                    modifier = Modifier
                        .size(ThingsIconSize.M)
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onShowTagHelperChange(true)
                        }
                )
            }

            // Checklist toggle
            if (!showChecklistHelper && checklist.isEmpty()) {
                Icon(
                    imageVector = AppIcons.BulletList,
                    contentDescription = stringResource(R.string.cd_checklists),
                    tint = iconInactiveColor,
                    modifier = Modifier
                        .size(ThingsIconSize.L)
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onShowChecklistHelperChange(true)
                        }
                )
            }

            // Flag (Deadline)
            if (dueDate == null) {
                Icon(
                    imageVector = AppIcons.Deadline,
                    contentDescription = stringResource(R.string.batch_action_set_deadline),
                    tint = iconInactiveColor,
                    modifier = Modifier
                        .size(ThingsIconSize.L)
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onShowDatePickerChange(true)
                        }
                )
            }
        }
    }
}
