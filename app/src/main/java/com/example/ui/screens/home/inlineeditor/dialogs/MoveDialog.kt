package com.example.ui.screens.home.inlineeditor.dialogs

import com.example.ui.theme.ThingsIconSize
import com.example.R
import androidx.compose.ui.res.stringResource
import com.example.ui.theme.ThingsTheme
import com.example.ui.theme.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.ui.components.ProjectProgressArc
import androidx.compose.material.icons.outlined.Layers
import com.example.ui.theme.dimens



/**
 * [ИЗМЕНЕНИЕ]: Диалог выбора назначения перемещения ("Move") для задачи.
 * Стиль полностью унифицирован с ThingsTagDialog, включая цвета, размеры, круглый крестик.
 * Список содержит "Inbox", "No project", далее разделитель, проекты без области и проекты, разбитые по областям.
 */
@Composable
fun ThingsMoveDialog(
    currentProjectId: String?,
    currentAreaId: String?,
    currentIsInbox: Boolean,
    projects: List<Item>,
    areas: List<Area>,
    allTasks: List<ItemWithChecklist>,
    onMove: (projectId: String?, areaId: String?, moveToInbox: Boolean) -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = ThingsTheme.shapes.dialogShape,
                colors = CardDefaults.cardColors(
                    containerColor = ThingsTheme.colors.overlaySurface
                ),
                modifier = Modifier
                    .width(MaterialTheme.dimens.dialogWidth)
                    .height(MaterialTheme.dimens.dialogHeight)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume taps inside card
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = MaterialTheme.dimens.dialogInnerContentPadding,
                            bottom = MaterialTheme.dimens.dialogInnerContentPadding
                        )
                ) {
                    // Header Row with Cancel Button alignment matching Tag Dialog
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = MaterialTheme.dimens.dialogInnerContentPadding,
                                end = MaterialTheme.dimens.dialogInnerContentPadding,
                                bottom = 12.dp
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Invisible placeholder for centering
                        Box(modifier = Modifier.size(36.dp))

                        Text(
                            text = stringResource(R.string.batch_action_move),
                            color = ThingsTheme.colors.overlayContent,
                            style = ThingsTheme.type.dialogTitle,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )

                        // Cancel 'X' Button in Circle
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ThingsTheme.colors.overlayControl)
                                .clickable { onDismissRequest() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.tag_dialog_cancel),
                                tint = ThingsTheme.colors.overlayContent,
                                modifier = Modifier.size(ThingsIconSize.S)
                            )
                        }
                    }

                    // Content List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // 1. Inbox Option
                        item {
                            val isSelected = currentProjectId == null && currentAreaId == null && currentIsInbox
                            MoveDialogRow(
                                title = stringResource(R.string.category_inbox),
                                icon = Icons.Default.Inbox,
                                iconColor = ThingsTheme.colors.accent,
                                isSelected = isSelected,
                                onClick = {
                                    onMove(null, null, true)
                                }
                            )
                        }

                        // 2. Clear/No project Option
                        item {
                            val isSelected = currentProjectId == null && currentAreaId == null && !currentIsInbox
                            MoveDialogRow(
                                title = stringResource(R.string.move_no_project),
                                icon = Icons.Default.Block,
                                iconColor = ThingsTheme.colors.overlayContentSecondary,
                                isSelected = isSelected,
                                onClick = {
                                    onMove(null, null, false)
                                }
                            )
                        }

                        // Horizontal Divider
                        item {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = ThingsTheme.colors.overlayDivider,
                                thickness = 1.dp
                            )
                        }

                        // Filters for Projects
                        val projectsWithNoArea = projects.filter { it.areaId.isNullOrBlank() }

                        // 3. Projects without Area
                        if (projectsWithNoArea.isNotEmpty()) {
                            items(projectsWithNoArea, key = { "proj_no_area_${it.id}" }) { project ->
                                val isSelected = currentProjectId == project.id
                                val projectTasks = allTasks.filter { it.item.projectId == project.id }
                                val completedCount = projectTasks.count { it.item.isCompleted }
                                val totalCount = projectTasks.size
                                MoveDialogRow(
                                    title = project.title,
                                    icon = null,
                                    iconColor = ThingsTheme.colors.overlayContent,
                                    isSelected = isSelected,
                                    isProject = true,
                                    completed = completedCount,
                                    total = totalCount,
                                    indentation = 0.dp,
                                    onClick = {
                                        onMove(project.id, null, false)
                                    }
                                )
                            }
                        }

                        // 4. Areas and Projects grouped under Areas
                        areas.forEach { area ->
                            // Horizontal divider before each area
                            item(key = "divider_${area.id}") {
                                HorizontalDivider(
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 12.dp, end = 12.dp),
                                    color = ThingsTheme.colors.overlayDivider,
                                    thickness = 1.dp
                                )
                            }
                            // Header of Area Group (renders Area as clickable as well)
                            item(key = "area_${area.id}") {
                                val isSelected = currentAreaId == area.id && currentProjectId == null
                                MoveDialogRow(
                                    title = area.title,
                                    icon = AppIcons.Area,
                                    iconColor = ThingsTheme.colors.area,
                                    isSelected = isSelected,
                                    isAreaHeader = true,
                                    onClick = {
                                        onMove(null, area.id, false)
                                    }
                                )
                            }

                            // Projects belonging to this Area
                            val areaProjects = projects.filter { it.areaId == area.id }
                            items(areaProjects, key = { "proj_${area.id}_${it.id}" }) { project ->
                                val isSelected = currentProjectId == project.id
                                val projectTasks = allTasks.filter { it.item.projectId == project.id }
                                val completedCount = projectTasks.count { it.item.isCompleted }
                                val totalCount = projectTasks.size
                                MoveDialogRow(
                                    title = project.title,
                                    icon = null,
                                    iconColor = ThingsTheme.colors.overlayContent,
                                    isSelected = isSelected,
                                    isProject = true,
                                    completed = completedCount,
                                    total = totalCount,
                                    indentation = 0.dp,
                                    onClick = {
                                        onMove(project.id, area.id, false)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * [ИЗМЕНЕНИЕ]: Отдельная строка списка выбора перемещения для обеспечения чистоты и переиспользования.
 */
@Composable
private fun MoveDialogRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    iconColor: Color,
    isSelected: Boolean,
    indentation: androidx.compose.ui.unit.Dp = 0.dp,
    isAreaHeader: Boolean = false,
    isProject: Boolean = false,
    completed: Int = 0,
    total: Int = 0,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indentation)
            .clip(ThingsTheme.shapes.rowShape)
            .background(if (isSelected) ThingsTheme.colors.overlayRowSelected else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isProject) {
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                ProjectProgressArc(
                    completed = completed,
                    total = total,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(ThingsIconSize.M)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = title,
            color = ThingsTheme.colors.overlayContent,
            style = ThingsTheme.type.dialogRow,
            fontWeight = if (isAreaHeader) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.cd_selected),
                tint = ThingsTheme.colors.accent,
                modifier = Modifier.size(ThingsIconSize.XS)
            )
        }
    }
}
