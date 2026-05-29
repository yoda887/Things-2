package com.example.ui.screens.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import com.example.data.model.Project
import com.example.data.model.Task
import com.example.data.model.TaskSection
import com.example.ui.components.ProjectProgressArc
import com.example.ui.screens.home.ActiveScreen
import com.example.ui.screens.home.subcomponents.SmartListRow
import com.example.ui.theme.*

/**
 * ThingsHomePanel: Houses the main grid dashboard logic, search interface inputs, 
 * smart-list counters (Inbox, Today, Logbook), list index selections, and the expandable 
 * Google Tasks synchronization panel..
 */

@Composable
fun ThingsHomePanel(
    allTasks: List<Task>,
    projects: List<Project>,
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
    onProjectClick: (Project) -> Unit,
    onAddProjectClick: () -> Unit,
    onSyncClick: (String) -> Unit
) {
    var rawTokenInput by remember { mutableStateOf(googleToken) }
    var isSyncConfigExpanded by remember { mutableStateOf(false) }

    val inboxCount = allTasks.count { it.section == TaskSection.INBOX && !it.isCompleted }
    val todayCount = allTasks.count { it.section == TaskSection.TODAY && !it.isCompleted }
    val upcomingCount = allTasks.count { it.section == TaskSection.UPCOMING && !it.isCompleted }
    val anytimeCount = allTasks.count { it.section == TaskSection.ANYTIME && !it.isCompleted }
    val somedayCount = allTasks.count { it.section == TaskSection.SOMEDAY && !it.isCompleted }

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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    count = upcomingCount,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    onClick = { onSmartListClick(ActiveScreen.UPCOMING) }
                )
                SmartListRow(
                    title = "Anytime",
                    icon = Icons.Outlined.Archive,
                    iconColor = ThingsAnytimeTeal,
                    count = anytimeCount,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    onClick = { onSmartListClick(ActiveScreen.ANYTIME) }
                )
                SmartListRow(
                    title = "Someday",
                    icon = Icons.Outlined.Folder,
                    iconColor = ThingsSomedayGrey,
                    count = somedayCount,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    onClick = { onSmartListClick(ActiveScreen.SOMEDAY) }
                )
                SmartListRow(
                    title = "Logbook",
                    icon = Icons.Outlined.AssignmentTurnedIn,
                    iconColor = ThingsLogbookGreen,
                    count = 0, // logbook shows historical completed tasks, count is optional
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    onClick = { onSmartListClick(ActiveScreen.LOGBOOK) }
                )
            }
        }

        // Projects Section List
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PROJECTS",
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondaryColor,
                        letterSpacing = 1.sp
                    )
                )

                IconButton(
                    onClick = onAddProjectClick,
                    modifier = Modifier.size(24.dp).testTag("add_project_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Project", tint = ThingsBlue, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (projects.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No custom projects created.", color = textSecondaryColor, fontSize = 13.sp)
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    projects.forEach { project ->
                        val projectTasks = allTasks.filter { it.projectId == project.id }
                        val completedCount = projectTasks.count { it.isCompleted }
                        val totalCount = projectTasks.size
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onProjectClick(project) }
                                .padding(vertical = 6.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Small completion arc
                            ProjectProgressArc(
                                completed = completedCount,
                                total = totalCount,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(9.dp))

                            Text(
                                text = project.name,
                                style = MaterialTheme.typography.displaySmall.copy(color = textPrimaryColor),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (totalCount > 0) {
                                Text(
                                    text = "$completedCount/$totalCount",
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        color = textSecondaryColor
                                    )
                                )
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
