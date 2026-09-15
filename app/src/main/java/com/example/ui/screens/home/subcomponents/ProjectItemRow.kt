package com.example.ui.screens.home.subcomponents

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ui.components.ProjectProgressArc
import com.example.ui.theme.ThingsBlue
import com.example.ui.theme.dimens

/**
 * Строка проекта в списке проектов области.
 * Отображает круглый индикатор прогресса ThingsBlue, название проекта,
 * опциональную иконку заметок и встроенный шеврон перехода > согласно эталону Things 3.
 */
@Composable
fun ProjectItemRow(
    project: Item,
    tasks: List<ItemWithChecklist> = emptyList(),
    projectProgressMap: Map<String, com.example.ui.screens.home.components.ProjectProgress> = emptyMap(),
    textPrimaryColor: Color,
    inlineExpandedTaskId: String?,
    onProjectClick: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = projectProgressMap[project.id]
    val completedCount = progress?.completed ?: tasks.filter { it.item.projectId == project.id }.count { it.item.isCompleted }
    val totalCount = progress?.total ?: tasks.count { it.item.projectId == project.id }
    val shouldDim = inlineExpandedTaskId != null
    val dimAlpha by animateFloatAsState(
        targetValue = if (shouldDim) 0.3f else 1f,
        label = "dimAlpha_proj_${project.id}"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onProjectClick(project) }
            .graphicsLayer { alpha = dimAlpha }
            .padding(
                start = MaterialTheme.dimens.taskRowStartPadding,
                end = MaterialTheme.dimens.taskRowEndPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(MaterialTheme.dimens.projectLeftColumnWidthDefault),
            contentAlignment = Alignment.Center
        ) {
            ProjectProgressArc(
                completed = completedCount,
                total = totalCount,
                color = ThingsBlue,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(MaterialTheme.dimens.taskSpacingToTextDefault))
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = project.title,
                style = MaterialTheme.typography.displaySmall.copy(
                    color = textPrimaryColor,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (project.notes.isNotBlank()) {
                Spacer(modifier = Modifier.width(5.dp))
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = "Has notes",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(3.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
