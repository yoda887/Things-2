package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Project
import com.example.data.model.Task
import com.example.data.model.TaskSection
import com.example.ui.theme.*
import com.example.ui.viewmodel.ThingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

enum class ActiveScreen {
    HOME, INBOX, TODAY, UPCOMING, ANYTIME, SOMEDAY, LOGBOOK, PROJECT_DETAIL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThingsHomeScreen(viewModel: ThingsViewModel) {
    val tasks by viewModel.filteredTasks.collectAsState()
    val allTasksRaw by viewModel.tasks.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTagFilter by viewModel.selectedTagFilter.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val googleToken by viewModel.googleAccessToken.collectAsState()
    val calendarEvents by viewModel.calendarEvents.collectAsState()

    val context = LocalContext.current
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.syncLocalCalendar()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            viewModel.syncLocalCalendar()
        } else {
            calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    var activeScreen by remember { mutableStateOf(ActiveScreen.HOME) }
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddProjectDialog by remember { mutableStateOf(false) }
    var inlineExpandedTaskId by remember { mutableStateOf<String?>(null) }
    
    // Project input fields
    var newProjectName by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    val isDark = false
    
    // Palette assignment
    val backgroundColor = if (isDark) ThingsBackgroundDark else ThingsBackgroundLight
    val cardSurfaceColor = if (isDark) ThingsSurfaceDark else ThingsSurfaceLight
    val textPrimaryColor = if (isDark) ThingsTextPrimaryDark else ThingsTextPrimaryLight
    val textSecondaryColor = if (isDark) ThingsTextSecondaryDark else ThingsTextSecondaryLight
    val dividerColor = if (isDark) ThingsDividerDark else ThingsDividerLight

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    if (activeScreen == ActiveScreen.HOME) {
                        Text(
                            text = "Things",
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimaryColor
                            )
                        )
                    }
                },
                navigationIcon = {
                    if (activeScreen != ActiveScreen.HOME) {
                        IconButton(
                            onClick = {
                                activeScreen = ActiveScreen.HOME
                                selectedProject = null
                                viewModel.selectTag(null)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowLeft,
                                contentDescription = "Back",
                                tint = ThingsBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                },
                actions = {
                    if (activeScreen != ActiveScreen.HOME) {
                        IconButton(
                            onClick = {
                                // Options / batch details shortcut
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .border(1.dp, textSecondaryColor.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Options",
                                    tint = textSecondaryColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    } else {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(20.dp),
                                color = ThingsBlue,
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        if (googleToken.isNotBlank()) {
                                            viewModel.syncWithGoogle()
                                        } else {
                                            viewModel.setAccessToken("demo_token")
                                            viewModel.syncWithGoogle()
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("sync_shortcut_button")
                            ) {
                                Icon(
                                    Icons.Default.Sync,
                                    contentDescription = "Sync",
                                    tint = if (syncError != null) ThingsUpcomingRed else textSecondaryColor
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val targetScreen = if (activeScreen == ActiveScreen.HOME || activeScreen == ActiveScreen.LOGBOOK) {
                        ActiveScreen.INBOX
                    } else {
                        activeScreen
                    }
                    if (activeScreen != targetScreen) {
                        activeScreen = targetScreen
                    }

                    val initialSection = when (targetScreen) {
                        ActiveScreen.TODAY -> TaskSection.TODAY
                        ActiveScreen.UPCOMING -> TaskSection.UPCOMING
                        ActiveScreen.ANYTIME -> TaskSection.ANYTIME
                        ActiveScreen.SOMEDAY -> TaskSection.SOMEDAY
                        else -> TaskSection.INBOX
                    }
                    val initialProjectId = if (targetScreen == ActiveScreen.PROJECT_DETAIL) selectedProject?.id else null
                    val newTaskId = java.util.UUID.randomUUID().toString()

                    val newTask = Task(
                        id = newTaskId,
                        title = "",
                        notes = "",
                        section = initialSection,
                        projectId = initialProjectId,
                        creationDate = System.currentTimeMillis()
                    )

                    viewModel.updateTask(newTask)
                    inlineExpandedTaskId = newTaskId
                },
                containerColor = ThingsBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(16.dp)
                    .size(56.dp)
                    .testTag("add_task_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Task", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = activeScreen, animationSpec = spring()) { screen ->
                when (screen) {
                    ActiveScreen.HOME -> ThingsHomePanel(
                        allTasks = allTasksRaw,
                        projects = projects,
                        searchQuery = searchQuery,
                        googleToken = googleToken,
                        syncError = syncError,
                        isSyncing = isSyncing,
                        textPrimaryColor = textPrimaryColor,
                        textSecondaryColor = textSecondaryColor,
                        cardSurfaceColor = cardSurfaceColor,
                        dividerColor = dividerColor,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onSmartListClick = { listScreen -> activeScreen = listScreen },
                        onProjectClick = { proj ->
                            selectedProject = proj
                            activeScreen = ActiveScreen.PROJECT_DETAIL
                        },
                        onAddProjectClick = { showAddProjectDialog = true },
                        onSyncClick = { token ->
                            viewModel.setAccessToken(token)
                            viewModel.syncWithGoogle()
                        }
                    )
                    else -> ThingsCategoryListPanel(
                        screen = screen,
                        project = selectedProject,
                        tasks = tasks,
                        allTags = allTags,
                        selectedTag = selectedTagFilter,
                        textPrimaryColor = textPrimaryColor,
                        textSecondaryColor = textSecondaryColor,
                        dividerColor = dividerColor,
                        onTagSelect = { viewModel.selectTag(it) },
                        onTaskToggle = { viewModel.toggleTaskCompletion(it) },
                        onTaskClick = { editTask ->
                            inlineExpandedTaskId = editTask.id
                        },
                        projects = projects,
                        viewModel = viewModel,
                        inlineExpandedTaskId = inlineExpandedTaskId,
                        onInlineExpandedTaskIdChange = { inlineExpandedTaskId = it }
                    )
                }
            }
        }
    }

    // Task Create / Edit Sheet
    if (showAddDialog) {
        val initialSection = when (activeScreen) {
            ActiveScreen.TODAY -> TaskSection.TODAY
            ActiveScreen.UPCOMING -> TaskSection.UPCOMING
            ActiveScreen.ANYTIME -> TaskSection.ANYTIME
            ActiveScreen.SOMEDAY -> TaskSection.SOMEDAY
            else -> TaskSection.INBOX
        }
        val initialProjectId = if (activeScreen == ActiveScreen.PROJECT_DETAIL) selectedProject?.id else null

        ThingsTaskDetailsSheet(
            task = taskToEdit,
            initialSection = initialSection,
            initialProjectId = initialProjectId,
            projects = projects,
            onDismiss = { showAddDialog = false },
            onSave = { title, notes, section, isTonight, dueDate, tags, projectId, checklist ->
                if (taskToEdit == null) {
                    // Create Task
                    viewModel.addTask(title, notes, section, isTonight, dueDate, tags, projectId, checklist)
                } else {
                    // Update Task
                    viewModel.updateTask(
                        taskToEdit!!.copy(
                            title = title,
                            notes = notes,
                            section = section,
                            isTonight = isTonight,
                            dueDate = dueDate,
                            tags = tags,
                            projectId = projectId,
                            checklist = checklist
                        )
                    )
                }
                showAddDialog = false
            },
            onDelete = {
                taskToEdit?.let { viewModel.deleteTask(it) }
                showAddDialog = false
            }
        )
    }

    // Project creation Dialog
    if (showAddProjectDialog) {
        AlertDialog(
            onDismissRequest = { showAddProjectDialog = false },
            title = { Text("Create Custom Project", fontWeight = FontWeight.Bold, color = textPrimaryColor) },
            text = {
                Column {
                    Text("Projects group tasks and track completion status with visual progress charts.", color = textSecondaryColor, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newProjectName,
                        onValueChange = { newProjectName = it },
                        label = { Text("Project Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThingsBlue,
                            unfocusedBorderColor = dividerColor,
                            focusedLabelColor = ThingsBlue
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newProjectName.isNotBlank()) {
                            viewModel.addProject(newProjectName.trim())
                            newProjectName = ""
                            showAddProjectDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_project")
                ) {
                    Text("Create", color = ThingsBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProjectDialog = false }) {
                    Text("Cancel", color = textSecondaryColor)
                }
            },
            containerColor = cardSurfaceColor
        )
    }
}

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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmartListRow(
                    title = "Inbox",
                    icon = Icons.Outlined.Inbox,
                    iconColor = ThingsInboxBlue,
                    count = inboxCount,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    onClick = { onSmartListClick(ActiveScreen.INBOX) }
                )
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
                    .padding(top = 12.dp),
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
            items(projects) { project ->
                val projectTasks = allTasks.filter { it.projectId == project.id }
                val completedCount = projectTasks.count { it.isCompleted }
                val totalCount = projectTasks.size
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onProjectClick(project) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small completion arc
                    ProjectProgressArc(
                        completed = completedCount,
                        total = totalCount,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = project.name,
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimaryColor
                        ),
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

@Composable
fun SmartListRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    count: Int,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(title, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textPrimaryColor), modifier = Modifier.weight(1f))
        
        if (count > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    count.toString(),
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = iconColor)
                )
            }
        }
    }
}

@Composable
fun ProjectProgressArc(
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (total > 0) completed.toFloat() / total else 0f
    val isDark = false
    val trackColor = if (isDark) ThingsDividerDark else ThingsDividerLight
    
    Canvas(modifier = modifier) {
        // Track
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 3.dp.toPx())
        )
        // Fill
        drawArc(
            color = ThingsBlue,
            startAngle = -90f,
            sweepAngle = progress * 360f,
            useCenter = false,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

@Composable
fun ThingsCheckbox(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = false
    val borderColor = if (checked) {
        ThingsBlue
    } else {
        if (isDark) Color(0xFF48484A) else Color(0xFFC7C7CC)
    }
    
    val backgroundColor = if (checked) {
        ThingsBlue
    } else {
        if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    }

    Box(
        modifier = modifier
            .size(20.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(backgroundColor)
            .border(1.2.dp, borderColor, RoundedCornerShape(5.dp))
            .clickable { onCheckedChange() },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
fun CalendarEventsWidget(
    events: List<Task>,
    textSecondaryColor: Color,
    isDark: Boolean
) {
    if (events.isEmpty()) return

    val systemDark = isSystemInDarkTheme() || isDark
    val cardBackground = if (systemDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            events.take(5).forEach { event ->
                val hasTime = !event.isAllDay && event.eventStartMillis != null && event.eventStartMillis > 0
                val isPastEvent = event.eventStartMillis != null && event.eventStartMillis < System.currentTimeMillis()

                // Resolve event/calendar color
                val rawColor = event.calendarColor
                val baseColor = if (rawColor != null) {
                    Color(rawColor)
                } else {
                    val hash = (event.calendarDisplayName ?: event.id ?: "Default").hashCode()
                    val presets = listOf(
                        Color(0xFF34A853), // Google Green
                        Color(0xFF4285F4), // Google Blue
                        Color(0xFFFBBC05), // Google Yellow
                        Color(0xFFEA4335), // Google Red
                        Color(0xFF8E24AA), // Purple
                        Color(0xFFF06292), // Pink
                        Color(0xFF00ACC1)  // Teal
                    )
                    presets[Math.abs(hash) % presets.size]
                }

                val markerColor = if (isPastEvent) {
                    if (systemDark) Color(0xFF48484A) else Color(0xFFD1D1D6)
                } else {
                    baseColor
                }

                val timeColor = if (isPastEvent) {
                    if (systemDark) Color(0xFF48484A) else Color(0xFFD1D1D6)
                } else {
                    baseColor
                }

                val titleColor = if (isPastEvent) {
                    if (systemDark) Color(0xFF5A5A5C) else Color(0xFF8E8E93)
                } else {
                    if (systemDark) Color(0xFFD1D1D6) else Color(0xFF2C2C2E)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(13.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(markerColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    if (hasTime) {
                        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        val timeString = sdf.format(java.util.Date(event.eventStartMillis!!))
                        Text(
                            text = timeString,
                            style = TextStyle(
                                fontSize = 13.sp,
                                color = timeColor,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.width(48.dp)
                        )
                    } else {
                        Text(
                            text = "День",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = timeColor.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.width(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = event.title,
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = titleColor,
                            fontWeight = FontWeight.Normal
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ThingsCategoryListPanel(
    screen: ActiveScreen,
    project: Project?,
    tasks: List<Task>,
    allTags: Set<String>,
    selectedTag: String?,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    dividerColor: Color,
    onTagSelect: (String?) -> Unit,
    onTaskToggle: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    projects: List<Project>,
    viewModel: ThingsViewModel,
    inlineExpandedTaskId: String?,
    onInlineExpandedTaskIdChange: (String?) -> Unit
) {
    val listTasks = remember(tasks, screen, project) {
        tasks.filter { task ->
            when (screen) {
                ActiveScreen.INBOX -> task.section == TaskSection.INBOX && !task.isCompleted
                ActiveScreen.TODAY -> task.section == TaskSection.TODAY && !task.isCompleted
                ActiveScreen.UPCOMING -> task.section == TaskSection.UPCOMING && !task.isCompleted
                ActiveScreen.ANYTIME -> task.section == TaskSection.ANYTIME && !task.isCompleted
                ActiveScreen.SOMEDAY -> task.section == TaskSection.SOMEDAY && !task.isCompleted
                ActiveScreen.LOGBOOK -> task.isCompleted
                ActiveScreen.PROJECT_DETAIL -> task.projectId == project?.id && !task.isCompleted
                else -> false
            }
        }.sortedBy { it.creationDate }
    }

    val calendarEvents by viewModel.calendarEvents.collectAsState()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLargeScreen = configuration.screenWidthDp >= 600
    val scaleFactor = if (isLargeScreen) 1.25f else 1.0f

    val headerEmojiFontSize = (30 * scaleFactor).sp
    val headerTitleFontSize = (32 * scaleFactor).sp
    val subHeaderFontSize = (18 * scaleFactor).sp

    val filteredTasks = remember(listTasks, selectedTag) {
        if (selectedTag == null) listTasks else listTasks.filter { it.tags.contains(selectedTag) }
    }

    val lazyListState = rememberLazyListState()
    var draggedTaskId by remember { mutableStateOf<String?>(null) }
    var dragAccumulatedOffset by remember { mutableStateOf(0f) }
    var localTasksList by remember { mutableStateOf(filteredTasks) }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    LaunchedEffect(filteredTasks) {
        localTasksList = filteredTasks
    }

    val makeDragModifier = { task: Task ->
        Modifier.pointerInput(task.id, task.creationDate) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    draggedTaskId = task.id
                    dragAccumulatedOffset = 0f
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragAccumulatedOffset += dragAmount.y

                    // Swap logic
                    val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
                    val draggedItemInfo = visibleItems.firstOrNull { it.key == task.id }
                    if (draggedItemInfo != null) {
                        val dragCenterY = draggedItemInfo.offset + draggedItemInfo.size / 2f + dragAccumulatedOffset
                        val hoveredItem = visibleItems.firstOrNull { item ->
                            val itemKey = item.key as? String
                            itemKey != null && itemKey != task.id &&
                            dragCenterY > item.offset &&
                            dragCenterY < item.offset + item.size
                        }
                        if (hoveredItem != null) {
                            val fromIndex = localTasksList.indexOfFirst { it.id == task.id }
                            
                            if (hoveredItem.key == "evening_header") {
                                if (fromIndex != -1) {
                                    val newList = localTasksList.toMutableList()
                                    var movedItem = newList.removeAt(fromIndex)
                                    if (!movedItem.isTonight) {
                                        movedItem = movedItem.copy(isTonight = true)
                                        val firstEveningIndex = newList.indexOfFirst { it.isTonight }
                                        val toIndex = if (firstEveningIndex != -1) firstEveningIndex else newList.size
                                        newList.add(toIndex, movedItem)
                                        localTasksList = newList
                                        
                                        val distance = (hoveredItem.offset + hoveredItem.size) - draggedItemInfo.offset
                                        dragAccumulatedOffset -= distance
                                    }
                                }
                            } else if (hoveredItem.key == "main_header") {
                                if (fromIndex != -1) {
                                    val newList = localTasksList.toMutableList()
                                    var movedItem = newList.removeAt(fromIndex)
                                    if (movedItem.isTonight) {
                                        movedItem = movedItem.copy(isTonight = false)
                                        val toIndex = 0 // at the very top
                                        newList.add(toIndex, movedItem)
                                        localTasksList = newList
                                        
                                        val distance = (hoveredItem.offset + hoveredItem.size) - draggedItemInfo.offset
                                        dragAccumulatedOffset -= distance
                                    }
                                }
                            } else {
                                val toIndex = localTasksList.indexOfFirst { it.id == hoveredItem.key }
                                if (fromIndex != -1 && toIndex != -1) {
                                    val newList = localTasksList.toMutableList()
                                    var movedItem = newList.removeAt(fromIndex)
                                    
                                    // Adopt isTonight property if different
                                    val hoveredItemTask = localTasksList.firstOrNull { it.id == hoveredItem.key }
                                    if (hoveredItemTask != null && hoveredItemTask.isTonight != movedItem.isTonight) {
                                        movedItem = movedItem.copy(isTonight = hoveredItemTask.isTonight)
                                    }
                                    
                                    newList.add(toIndex, movedItem)
                                    localTasksList = newList

                                    val distance = hoveredItem.offset - draggedItemInfo.offset
                                    dragAccumulatedOffset -= distance
                                }
                            }
                        }
                    }
                },
                onDragEnd = {
                    val baseTime = System.currentTimeMillis() - localTasksList.size * 1000L
                    val updatedList = localTasksList.mapIndexed { index, t ->
                        val newTime = baseTime + index * 1000L
                        t.copy(creationDate = newTime)
                    }
                    val changedTasks = updatedList.filter { t ->
                        t != filteredTasks.firstOrNull { it.id == t.id }
                    }
                    localTasksList = updatedList
                    if (changedTasks.isNotEmpty()) {
                        viewModel.updateTasks(changedTasks)
                    }
                    draggedTaskId = null
                    dragAccumulatedOffset = 0f
                },
                onDragCancel = {
                    draggedTaskId = null
                    dragAccumulatedOffset = 0f
                }
            )
        }
    }

    val standardToday = remember(localTasksList) { localTasksList.filter { !it.isTonight } }
    val eveningToday = remember(localTasksList) { localTasksList.filter { it.isTonight } }

    val anyExpanded = inlineExpandedTaskId != null
    val globalDimAlpha by animateFloatAsState(targetValue = if (anyExpanded) 0.3f else 1f, label = "globalDim")

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    onInlineExpandedTaskIdChange(null)
                    focusManager.clearFocus()
                })
            }
            .padding(horizontal = 20.dp)
            .testTag("tasks_lazy_list"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Large Screen Header inside the scroll container
        item(key = "main_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 14.dp)
                    .graphicsLayer { alpha = globalDimAlpha },
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (screen) {
                    ActiveScreen.TODAY -> {
                        Text(
                            text = "⭐",
                            fontSize = headerEmojiFontSize,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = "Today",
                            style = TextStyle(
                                fontSize = headerTitleFontSize,
                                fontWeight = FontWeight.Bold,
                                color = textPrimaryColor
                            )
                        )
                    }
                    ActiveScreen.INBOX -> {
                        Icon(
                            imageVector = Icons.Outlined.Inbox,
                            contentDescription = null,
                            tint = Color(0xFF1B80FA),
                            modifier = Modifier.size((32 * scaleFactor).dp).padding(end = 10.dp)
                        )
                        Text(
                            text = "Inbox",
                            style = TextStyle(
                                fontSize = headerTitleFontSize,
                                fontWeight = FontWeight.Bold,
                                color = textPrimaryColor
                            )
                        )
                    }
                    ActiveScreen.UPCOMING -> {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = Color(0xFFF35F50),
                            modifier = Modifier.size((32 * scaleFactor).dp).padding(end = 10.dp)
                        )
                        Text(
                            text = "Upcoming",
                            style = TextStyle(
                                fontSize = headerTitleFontSize,
                                fontWeight = FontWeight.Bold,
                                color = textPrimaryColor
                            )
                        )
                    }
                    ActiveScreen.ANYTIME -> {
                        Icon(
                            imageVector = Icons.Outlined.Archive,
                            contentDescription = null,
                            tint = Color(0xFF2EB7CD),
                            modifier = Modifier.size((32 * scaleFactor).dp).padding(end = 10.dp)
                        )
                        Text(
                            text = "Anytime",
                            style = TextStyle(
                                fontSize = headerTitleFontSize,
                                fontWeight = FontWeight.Bold,
                                color = textPrimaryColor
                            )
                        )
                    }
                    ActiveScreen.SOMEDAY -> {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = Color(0xFF8F93A3),
                            modifier = Modifier.size((32 * scaleFactor).dp).padding(end = 10.dp)
                        )
                        Text(
                            text = "Someday",
                            style = TextStyle(
                                fontSize = headerTitleFontSize,
                                fontWeight = FontWeight.Bold,
                                color = textPrimaryColor
                            )
                        )
                    }
                    ActiveScreen.LOGBOOK -> {
                        Icon(
                            imageVector = Icons.Outlined.AssignmentTurnedIn,
                            contentDescription = null,
                            tint = Color(0xFF2EC275),
                            modifier = Modifier.size((32 * scaleFactor).dp).padding(end = 10.dp)
                        )
                        Text(
                            text = "Logbook",
                            style = TextStyle(
                                fontSize = headerTitleFontSize,
                                fontWeight = FontWeight.Bold,
                                color = textPrimaryColor
                            )
                        )
                    }
                    ActiveScreen.PROJECT_DETAIL -> {
                        val completedCount = tasks.count { it.projectId == project?.id && it.isCompleted }
                        val totalCount = tasks.count { it.projectId == project?.id }
                        ProjectProgressArc(
                            completed = completedCount,
                            total = totalCount,
                            modifier = Modifier.size((26 * scaleFactor).dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = project?.name ?: "Project",
                            style = TextStyle(
                                fontSize = headerTitleFontSize,
                                fontWeight = FontWeight.Bold,
                                color = textPrimaryColor
                            )
                        )
                    }
                    else -> {}
                }
            }
        }

        // Calendar Widget on Today
        if (screen == ActiveScreen.TODAY && calendarEvents.isNotEmpty()) {
            item {
                CalendarEventsWidget(
                    events = calendarEvents,
                    textSecondaryColor = textSecondaryColor,
                    isDark = false
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // Tag selectors
        if (allTags.isNotEmpty()) {
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        val isAllSelected = selectedTag == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isAllSelected) ThingsBlue else Color.Transparent)
                                .border(1.dp, if (isAllSelected) ThingsBlue else dividerColor, RoundedCornerShape(12.dp))
                                .clickable { onTagSelect(null) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("All", color = if (isAllSelected) Color.White else textSecondaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    items(allTags.toList()) { tag ->
                        val isSelected = selectedTag == tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) ThingsBlue else Color.Transparent)
                                .border(1.dp, if (isSelected) ThingsBlue else dividerColor, RoundedCornerShape(12.dp))
                                .clickable { onTagSelect(if (isSelected) null else tag) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(tag, color = if (isSelected) Color.White else textSecondaryColor, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        val displayTasks = localTasksList

        val hasTasks = if (screen == ActiveScreen.TODAY) standardToday.isNotEmpty() || eveningToday.isNotEmpty() || draggedTaskId != null else displayTasks.isNotEmpty()

        if (!hasTasks) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 56.dp)
                        .testTag("empty_state_view"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.AssignmentTurnedIn,
                            contentDescription = "Empty",
                            tint = textSecondaryColor.copy(alpha = 0.3f),
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "All clear here! Enjoy your day.",
                            color = textSecondaryColor,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            val flattened = buildList<Any> {
                if (screen == ActiveScreen.TODAY) {
                    addAll(standardToday)
                    if (eveningToday.isNotEmpty() || draggedTaskId != null) {
                        add("evening_header")
                        addAll(eveningToday)
                    }
                } else {
                    addAll(displayTasks)
                }
            }

            items(flattened, key = { it -> 
                if (it is Task) it.id else it.toString()
            }) { item ->
                if (item is Task) {
                    val task = item
                    val isDragTask = draggedTaskId == task.id
                    val isExpanded = inlineExpandedTaskId == task.id
                    val shouldDim = inlineExpandedTaskId != null && !isExpanded
                    
                    val dimAlpha by animateFloatAsState(
                        targetValue = if (shouldDim) 0.3f else 1f,
                        label = "dimAlpha_${task.id}"
                    )
                    val elevation by animateDpAsState(
                        targetValue = if (isExpanded) 8.dp else 0.dp,
                        label = "elev_${task.id}"
                    )
                    val zIndexVal by animateFloatAsState(
                        targetValue = if (isExpanded) 1f else 0f,
                        label = "zIndex_${task.id}"
                    )
                    
                    Column(
                        modifier = if (isDragTask) Modifier else Modifier
                            .animateItem()
                            .zIndex(zIndexVal)
                            .graphicsLayer { alpha = dimAlpha }
                            .shadow(elevation, RoundedCornerShape(8.dp))
                            .animateContentSize()
                            .clipToBounds()
                    ) {
                        Crossfade(targetState = isExpanded, label = "inline_edit_crossfade") { expanded ->
                            if (expanded) {
                                ThingsTaskInlineEditor(
                                    task = task,
                                    projects = projects,
                                    onSave = { title, notes, section, isTonight, dueDate, tags, projectId, checklist, priority ->
                                        val updatedTask = task.copy(
                                            title = title,
                                            notes = notes,
                                            section = section,
                                            isTonight = isTonight,
                                            dueDate = dueDate,
                                            tags = tags,
                                            projectId = projectId,
                                            checklist = checklist,
                                            priority = priority
                                        )
                                        viewModel.updateTask(updatedTask)
                                        onInlineExpandedTaskIdChange(null)
                                    },
                                    onDelete = {
                                        viewModel.deleteTask(task)
                                        onInlineExpandedTaskIdChange(null)
                                    },
                                    onDone = {
                                        onInlineExpandedTaskIdChange(null)
                                    }
                                )
                            } else {
                                TaskItemRow(
                                    modifier = Modifier,
                                    task = task,
                                    textPrimaryColor = textPrimaryColor,
                                    textSecondaryColor = textSecondaryColor,
                                    dividerColor = dividerColor,
                                    onToggle = { onTaskToggle(task) },
                                    onClick = { 
                                        onInlineExpandedTaskIdChange(task.id)
                                    },
                                    projects = projects,
                                    showTodayIndicator = screen == ActiveScreen.TODAY && !task.isTonight,
                                    isDragging = draggedTaskId == task.id,
                                    dragOffsetY = if (draggedTaskId == task.id) dragAccumulatedOffset else 0f,
                                    dragModifier = makeDragModifier(task)
                                )
                            }
                        }
                    }
                } else if (item == "evening_header") {
                    val shouldDim = inlineExpandedTaskId != null
                    val dimAlpha by animateFloatAsState(
                        targetValue = if (shouldDim) 0.3f else 1f,
                        label = "dimAlpha_evening"
                    )

                    Column(modifier = Modifier
                        .animateItem()
                        .graphicsLayer { alpha = dimAlpha }
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🌙",
                                fontSize = 18.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                "This Evening",
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimaryColor
                                )
                            )
                        }
                        // Thin full divider line directly below the section header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(dividerColor)
                                .padding(bottom = 6.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun TaskItemRow(
    modifier: Modifier = Modifier,
    task: Task,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    dividerColor: Color,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    projects: List<Project>,
    showTodayIndicator: Boolean = false,
    isDragging: Boolean = false,
    dragOffsetY: Float = 0f,
    dragModifier: Modifier = Modifier
) {
    val isDark = false
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLargeScreen = configuration.screenWidthDp >= 600
    val scaleFactor = if (isLargeScreen) 1.25f else 1.0f

    val titleFontSize = (15.6 * scaleFactor).sp
    val subFontSize = (12.5 * scaleFactor).sp

    val scope = rememberCoroutineScope()
    var localCompleted by remember(task.isCompleted) { mutableStateOf(task.isCompleted) }

    val scale by androidx.compose.animation.core.animateFloatAsState(if (isDragging) 1.04f else 1.0f)
    val elevation by androidx.compose.animation.core.animateDpAsState(if (isDragging) 6.dp else 0.dp)

    val isCalendarTask = task.id.startsWith("cal_")

    Row(
        modifier = modifier
            .fillMaxWidth()
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
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (isCalendarTask) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Outlined.CalendarToday,
                contentDescription = "Calendar Event",
                tint = ThingsBlue,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(22.dp)
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
                modifier = Modifier
                    .padding(top = 2.dp)
                    .testTag("task_checkbox")
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

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
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Inline note/subtask icons representation
                if (task.notes.isNotBlank() || task.checklist.isNotEmpty() || task.priority > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (task.priority > 0) {
                            val flagColor = when (task.priority) {
                                3 -> ThingsUpcomingRed
                                2 -> ThingsTodayStar
                                1 -> ThingsAnytimeTeal
                                else -> textSecondaryColor.copy(alpha = 0.4f)
                            }
                            Icon(
                                imageVector = Icons.Filled.Flag,
                                contentDescription = "Priority Flag",
                                tint = flagColor,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (task.notes.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = "Has notes",
                                tint = textSecondaryColor.copy(alpha = 0.4f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (task.checklist.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Outlined.FormatListBulleted,
                                contentDescription = "Has checklist",
                                tint = textSecondaryColor.copy(alpha = 0.4f),
                                modifier = Modifier.size(13.dp)
                            )
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

        // Flags
        if (showTodayIndicator && !localCompleted) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = "Today",
                    tint = ThingsUpcomingRed,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "today",
                    style = TextStyle(
                        fontSize = subFontSize,
                        color = ThingsUpcomingRed,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Drag Handle
        Icon(
            imageVector = Icons.Default.Reorder,
            contentDescription = "Reorder",
            tint = textSecondaryColor.copy(alpha = 0.35f),
            modifier = Modifier
                .size(24.dp)
                .padding(top = 2.dp)
        )
    }
}
