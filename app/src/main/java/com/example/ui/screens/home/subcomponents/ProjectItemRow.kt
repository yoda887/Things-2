package com.example.ui.screens.home.subcomponents

import com.example.ui.viewmodel.ProjectProgress
import com.example.ui.theme.ThingsIconSize
import com.example.ui.theme.ThingsAlpha
import com.example.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import com.example.ui.theme.ThingsTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.ui.components.ClosedProjectIcon
import com.example.ui.components.ProjectProgressArc
import com.example.ui.theme.dimens

// Название → значок заметок → шеврон
private val NOTES_ICON_GAP = 5.dp
private val CHEVRON_GAP = 3.dp

private val NOTES_ICON_SIZE = 14.dp

/**
 * Строка проекта в списке проектов области.
 * Отображает круглый индикатор прогресса ThingsTheme.colors.accent, название проекта,
 * опциональную иконку заметок и встроенный шеврон перехода > согласно эталону Things 3.
 */
@Composable
fun ProjectItemRow(
    project: Item,
    tasks: List<ItemWithChecklist> = emptyList(),
    projectProgressMap: Map<String, ProjectProgress> = emptyMap(),
    textPrimaryColor: Color,
    inlineExpandedTaskId: String?,
    onProjectClick: (Item) -> Unit,
    // Дата закрытия перед названием — в «Журнале», в той же колонке, что у закрытых задач
    closedAt: Long? = null,
    modifier: Modifier = Modifier
) {
    val progress = projectProgressMap[project.id]
    val completedCount = progress?.completed ?: tasks.filter { it.item.projectId == project.id }.count { it.item.isClosed }
    val totalCount = progress?.total ?: tasks.count { it.item.projectId == project.id }
    // Выполненный или отменённый проект («Журнал», Logbook поиска) — кольцо с галочкой или крестиком
    val isDone = project.isCompleted || project.status == Item.STATUS_CANCELLED
    val shouldDim = inlineExpandedTaskId != null
    val dimAlpha by animateFloatAsState(
        targetValue = if (shouldDim) 0.3f else 1f,
        label = "dimAlpha_proj_${project.id}"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MaterialTheme.dimens.rowHeight)
            .clip(ThingsTheme.shapes.rowShape)
            .clickable { onProjectClick(project) }
            .graphicsLayer { alpha = dimAlpha }
            .padding(
                start = MaterialTheme.dimens.taskRowStartPadding,
                end = MaterialTheme.dimens.taskRowEndPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Колонка той же ширины, что под чекбоксом задачи: кольцо центрируется по оси чекбоксов,
        // а название начинается с того же края, что и названия задач. Кольцо крупнее колонки
        // и выступает из неё поровну с обеих сторон
        Box(
            modifier = Modifier.size(MaterialTheme.dimens.taskLeftColumnWidthDefault),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                ClosedProjectIcon(
                    cancelled = project.status == Item.STATUS_CANCELLED,
                    modifier = Modifier.requiredSize(ThingsIconSize.M)
                )
            } else {
                ProjectProgressArc(
                    completed = completedCount,
                    total = totalCount,
                    color = ThingsTheme.colors.project,
                    dashed = project.start == Item.START_SOMEDAY,
                    modifier = Modifier.requiredSize(ThingsIconSize.M)
                )
            }
        }
        Spacer(modifier = Modifier.width(MaterialTheme.dimens.taskSpacingToTextDefault))
        if (closedAt != null) {
            ClosedDateLabel(closedAt = closedAt, inLogbook = true)
            Spacer(modifier = Modifier.width(com.example.ui.theme.ThingsSpacing.S))
        }
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = project.title,
                style = ThingsTheme.type.listTitle.copy(
                    color = textPrimaryColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (project.notes.isNotBlank()) {
                Spacer(modifier = Modifier.width(NOTES_ICON_GAP))
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = stringResource(R.string.cd_has_notes),
                    tint = ThingsTheme.colors.textSecondary.copy(alpha = ThingsAlpha.MUTED),
                    modifier = Modifier.size(NOTES_ICON_SIZE)
                )
            }
            Spacer(modifier = Modifier.width(CHEVRON_GAP))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = ThingsTheme.colors.textSecondary.copy(alpha = ThingsAlpha.MUTED),
                modifier = Modifier.size(ThingsIconSize.XS)
            )
        }
    }
}
