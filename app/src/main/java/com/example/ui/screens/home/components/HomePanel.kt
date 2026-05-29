package com.example.ui.screens.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.TaskSection
import com.example.ui.components.ProjectProgressArc
import com.example.ui.screens.home.ActiveScreen
import com.example.ui.screens.home.subcomponents.SmartListRow
import com.example.ui.theme.*

@Composable
fun ThingsHomePanel(
    allTasks: List<Item>,
    projects: List<Item>,
    searchQuery: String,
    googleToken: String,
    syncError: String?,
    isSyncing: Boolean,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    cardSurfaceColor: Color,
    dividerColor: Color,
    onSearchChange: (String) -> Unit,
    onSmartListClick: (ActiveScreen) -> Unit,
    onProjectClick: (Item) -> Unit,
    onAddProjectClick: () -> Unit,
    onSyncClick: (String) -> Unit,
    // Two-level Areas management additions
    areas: List<Area> = emptyList(),
    onAddAreaClick: () -> Unit = {},
    onDeleteArea: (Area) -> Unit = {},
    onDeleteProject: (Item) -> Unit = {},
    onAreaClick: (Area) -> Unit = {}
) {
    var rawTokenInput by remember { mutableStateOf(googleToken) }
    var isSyncConfigExpanded by remember { mutableStateOf(false) }

    val inboxCount = allTasks.count { it.type == 0 && it.start == 0 && !it.isCompleted }
    val todayCount = allTasks.count { it.type == 0 && it.start == 1 && !it.isCompleted }

    val tasksByProject = remember(allTasks) {
        allTasks.groupBy { it.projectId }
    }

    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }
    
    LaunchedEffect(areas) {
        areas.forEach { area ->
            if (expandedStates[area.id] == null) {
                expandedStates[area.id] = true // expanded by default
            }
        }
        if (expandedStates["no_area"] == null) {
            expandedStates["no_area"] = true
        }
    }

    // Confirmation dialog for project deletion (CASCADE warning)
    var projectToDelete by remember { mutableStateOf<Item?>(null) }

    if (projectToDelete != null) {
        val proj = projectToDelete!!
        val taskCountInProj = allTasks.count { it.projectId == proj.id }
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Удалить проект?", fontWeight = FontWeight.Bold, color = textPrimaryColor) },
            text = {
                Text(
                    "Вы действительно хотите удалить проект \"${proj.title}\"? Проект удалится вместе с $taskCountInProj задачами.",
                    color = textSecondaryColor,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteProject(proj)
                        projectToDelete = null
                    }
                ) {
                    Text("Удалить", color = ThingsUpcomingRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Отмена", color = textSecondaryColor)
                }
            },
            containerColor = cardSurfaceColor
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search Filter row
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search list contents...", color = textSecondaryColor.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = textSecondaryColor) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("home_search_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThingsBlue,
                    unfocusedBorderColor = dividerColor
                )
            )
        }

        // Smart Lists Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SmartListRow(
                    title = "Inbox",
                    icon = Icons.Outlined.Inbox,
                    iconColor = ThingsInboxBlue,
                    count = inboxCount,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    onClick = { onSmartListClick(ActiveScreen.INBOX) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                SmartListRow(
                    title = "Today",
                    icon = Icons.Default.Star,
                    iconColor = ThingsTodayStar,
                    count = todayCount,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    onClick = { onSmartListClick(ActiveScreen.TODAY) }
                )
                SmartListRow(
                    title = "Upcoming",
                    icon = Icons.Outlined.CalendarToday,
                    iconColor = ThingsUpcomingRed,
                    count = 0,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    onClick = { onSmartListClick(ActiveScreen.UPCOMING) }
                )
                SmartListRow(
                    title = "Anytime",
                    icon = Icons.Outlined.Archive,
                    iconColor = ThingsAnytimeTeal,
                    count = 0,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    onClick = { onSmartListClick(ActiveScreen.ANYTIME) }
                )
                SmartListRow(
                    title = "Someday",
                    icon = Icons.Outlined.Folder,
                    iconColor = ThingsSomedayGrey,
                    count = 0,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    onClick = { onSmartListClick(ActiveScreen.SOMEDAY) }
                )
                SmartListRow(
                    title = "Logbook",
                    icon = Icons.Outlined.AssignmentTurnedIn,
                    iconColor = ThingsLogbookGreen,
                    count = 0,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    onClick = { onSmartListClick(ActiveScreen.LOGBOOK) }
                )
            }
        }

        // AREAS & PROJECTS Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AREAS & PROJECTS",
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondaryColor,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Add Area button
                TextButton(
                    onClick = onAddAreaClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("add_area_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Area", tint = ThingsBlue, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Area", color = ThingsBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Add Project button
                TextButton(
                    onClick = onAddProjectClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("add_project_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Project", tint = ThingsBlue, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Project", color = ThingsBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Two-level Areas & Projects tree
        val noAreaProjects = projects.filter { it.areaId == null }

        // Render projects without area first, as standalone items
        noAreaProjects.forEach { project ->
            item {
                val projectTasks = tasksByProject[project.id] ?: emptyList()
                val completedCount = projectTasks.count { it.isCompleted }
                val totalCount = projectTasks.size

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onProjectClick(project) }
                        .padding(vertical = 6.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProjectProgressArc(
                        completed = completedCount,
                        total = totalCount,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.displaySmall.copy(
                            color = textPrimaryColor,
                            fontWeight = FontWeight.Normal
                        ),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Add a visual divider between standalone projects and subsequent area headers if both exist
        if (noAreaProjects.isNotEmpty() && areas.isNotEmpty()) {
            item {
                HorizontalDivider(
                    color = dividerColor,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        // Render Areas
        areas.forEach { area ->
            val areaProjects = projects.filter { it.areaId == area.id }
            val hasProjects = areaProjects.isNotEmpty()

            item {
                val isExpanded = expandedStates[area.id] ?: true
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Title block for Area
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onAreaClick(area) }
                            .padding(vertical = 2.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Layers,
                            contentDescription = area.title,
                            tint = textSecondaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        // [ИЗМЕНЕНИЕ]: Убрано переопределение fontWeight = FontWeight.Bold, чтобы начертание автоматически унаследовалось из базовой темы (FontWeight.Medium)
                        Text(
                            text = area.title,
                            style = MaterialTheme.typography.displaySmall.copy(
                                color = textPrimaryColor
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        // [ИЗМЕНЕНИЕ]: Если в области есть проекты, показываем кнопку раскрытия. Иначе отображаем Spacer(44.dp) для сохранения выравнивания.
                        if (hasProjects) {
                            IconButton(
                                onClick = { expandedStates[area.id] = !isExpanded },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                    contentDescription = "Toggle Area",
                                    tint = textSecondaryColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(44.dp))
                        }
                    }

                    // [ИЗМЕНЕНИЕ]: Дочерние проекты отображаются только если область раскрыта и в ней действительно есть проекты (без заглушек вроде "No projects")
                    if (isExpanded && hasProjects) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 0.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            areaProjects.forEach { project ->
                                val projectTasks = tasksByProject[project.id] ?: emptyList()
                                val completedCount = projectTasks.count { it.isCompleted }
                                val totalCount = projectTasks.size

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onProjectClick(project) }
                                        .padding(vertical = 6.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ProjectProgressArc(
                                        completed = completedCount,
                                        total = totalCount,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(7.dp))
                                    Text(
                                        text = project.title,
                                        style = MaterialTheme.typography.displaySmall.copy(
                                            color = textPrimaryColor,
                                            fontWeight = FontWeight.Normal
                                        ),
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Google Sync Integration card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, bottom = 32.dp),
                colors = CardDefaults.cardColors(containerColor = cardSurfaceColor),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSyncConfigExpanded = !isSyncConfigExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = "Google Sync", tint = ThingsInboxBlue, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Google Tasks Sync Settings", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textPrimaryColor)
                        }
                        Icon(
                            imageVector = if (isSyncConfigExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Settings",
                            tint = textSecondaryColor
                        )
                    }

                    AnimatedVisibility(visible = isSyncConfigExpanded) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                "Enter an OAuth access token to bidirectionally synchronize local tasks and projects directly with Google Tasks.",
                                fontSize = 12.sp,
                                color = textSecondaryColor,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = rawTokenInput,
                                onValueChange = { rawTokenInput = it },
                                label = { Text("Google Access Token", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("google_token_input"),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = TextStyle(fontSize = 12.sp, color = textPrimaryColor),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ThingsBlue,
                                    unfocusedBorderColor = dividerColor
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            if (syncError != null) {
                                Text(
                                    "Error: $syncError",
                                    color = ThingsUpcomingRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            Button(
                                onClick = { onSyncClick(rawTokenInput) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("execute_sync_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ThingsBlue),
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isSyncing
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                } else {
                                    Text("Sync Tasks Now", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
