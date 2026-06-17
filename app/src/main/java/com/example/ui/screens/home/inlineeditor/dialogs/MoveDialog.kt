package com.example.ui.screens.home.inlineeditor.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ui.theme.ThingsBlue
import com.example.ui.theme.ThingsLogbookGreen
import androidx.compose.material.icons.outlined.Layers
import com.example.ui.theme.dimens

// Dialog Color Constants to match ThingsTagDialog perfectly
private val DialogBackgroundColor = Color(0xFF22242C)
private val DarkButtonBgColor = Color(0xFF2C2E38)
private val DividerColor = Color(0xFF28292B)
private val TextMutedColor = Color(0xFF8E8E93)

// Custom area icon adapted from vector drawable
private val CustomAreaIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CustomArea",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(2f, 15.5f)
            quadToRelative(0f, 2f, 1.8f, 2.9f)
            lineToRelative(6.4f, 3.2f)
            quadToRelative(1.8f, 0.9f, 3.6f, 0f)
            lineToRelative(6.4f, -3.2f)
            quadToRelative(1.8f, -0.9f, 1.8f, -2.9f)
            lineToRelative(0f, -7f)
            quadToRelative(0f, -2f, -1.8f, -2.9f)
            lineToRelative(-6.4f, -3.2f)
            quadToRelative(-1.8f, -0.9f, -3.6f, 0f)
            lineToRelative(-6.4f, 3.2f)
            quadToRelative(-1.8f, 0.9f, -1.8f, 2.9f)
            close()
        }
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.4f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(3f, 6.5f)
            curveToRelative(-1f, 1.2f, -0.2f, 1.9f, 0.8f, 2.4f)
            lineTo(10.2f, 12.1f)
            quadTo(12f, 13f, 13.8f, 12.1f)
            lineTo(20.2f, 8.9f)
            curveToRelative(1f, -0.5f, 1.8f, -1.2f, 0.8f, -2.4f)
        }
    }.build()
}

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
                shape = RoundedCornerShape(MaterialTheme.dimens.dialogRoundedCornerSize),
                colors = CardDefaults.cardColors(
                    containerColor = DialogBackgroundColor
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
                            text = "Move",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )

                        // Cancel 'X' Button in Circle
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkButtonBgColor)
                                .clickable { onDismissRequest() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
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
                                title = "Inbox",
                                icon = Icons.Default.Inbox,
                                iconColor = ThingsBlue,
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
                                title = "No Project",
                                icon = Icons.Default.Block,
                                iconColor = TextMutedColor,
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
                                color = DividerColor,
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
                                    iconColor = Color.White,
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
                                    color = DividerColor,
                                    thickness = 1.dp
                                )
                            }
                            // Header of Area Group (renders Area as clickable as well)
                            item(key = "area_${area.id}") {
                                val isSelected = currentAreaId == area.id && currentProjectId == null
                                MoveDialogRow(
                                    title = area.title,
                                    icon = CustomAreaIcon,
                                    iconColor = ThingsLogbookGreen,
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
                                    iconColor = Color.White,
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
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF1E2027) else Color.Transparent)
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
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = title,
            color = if (isAreaHeader) Color.White else Color(0xFFECECED),
            fontWeight = if (isAreaHeader) FontWeight.Bold else FontWeight.Normal,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = ThingsBlue,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
