package com.example.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.Project
import com.example.data.model.Task
import com.example.data.model.TaskSection
import com.example.ui.screens.home.components.ThingsHomePanel
import com.example.ui.screens.home.components.ThingsCategoryListPanel
import com.example.ui.screens.ThingsTaskDetailsSheet
import com.example.ui.theme.*
import com.example.ui.viewmodel.ThingsViewModel
import kotlinx.coroutines.launch


/**
 * ThingsHomeScreen: Now serves purely as the top-level orchestration point. 
 * It retains the global navigation state (active screens), Floating Action Button action 
 * triggers, permission request launchers, dialog manager flags, and coordinate cross-fades 
 * between panels.
 */


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
