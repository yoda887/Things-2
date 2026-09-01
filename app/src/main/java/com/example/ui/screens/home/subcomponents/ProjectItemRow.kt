package com.example.ui.screens.home.subcomponents

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * Строка проекта в списке проектов области.
 */
@Composable
fun ProjectItemRow(
    project: Item,
    tasks: List<ItemWithChecklist>,
    textPrimaryColor: Color,
    inlineExpandedTaskId: String?,
    onProjectClick: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
    val projectTasks = tasks.filter { it.item.projectId == project.id }
    val completedCount = projectTasks.count { it.item.isCompleted }
    val totalCount = projectTasks.size
    val shouldDim = inlineExpandedTaskId != null
    val dimAlpha by animateFloatAsState(
        targetValue = if (shouldDim) 0.3f else 1f,
        label = "dimAlpha_proj_${project.id}"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onProjectClick(project) }
            .graphicsLayer { alpha = dimAlpha }
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center
        ) {
            ProjectProgressArc(
                completed = completedCount,
                total = totalCount,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = project.title,
            style = MaterialTheme.typography.displaySmall.copy(
                color = textPrimaryColor,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
