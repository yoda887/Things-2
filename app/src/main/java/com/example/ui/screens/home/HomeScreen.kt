package com.example.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.TaskSection
import com.example.data.model.Area
import com.example.ui.screens.home.components.ThingsHomePanel
import com.example.ui.screens.home.components.ThingsCategoryListPanel
import com.example.ui.screens.home.components.ThingsCategoryListState
import com.example.ui.screens.home.components.ThingsCategoryListEvent
import com.example.ui.screens.home.components.ThingsSearchOverlay
import com.example.ui.screens.home.components.ThingsSearchScreen
import com.example.ui.screens.home.components.SearchResultItem
import com.example.ui.screens.ThingsTaskDetailsSheet
import com.example.ui.theme.*
import com.example.ui.viewmodel.ThingsViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch


/**
 * ThingsHomeScreen: Now serves purely as the top-level orchestration point. 
 * It retains the global navigation state (active screens), Floating Action Button action 
 * triggers, permission request launchers, dialog manager flags, and coordinate cross-fades 
 * between panels.
 */


enum class ActiveScreen {
    HOME, INBOX, TODAY, UPCOMING, ANYTIME, SOMEDAY, LOGBOOK, PROJECT_DETAIL, AREA_DETAIL, SEARCH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThingsHomeScreen(viewModel: ThingsViewModel = hiltViewModel()) {
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
    var selectedProject by remember { mutableStateOf<Item?>(null) }
    var selectedArea by remember { mutableStateOf<Area?>(null) }
    var taskToEdit by remember { mutableStateOf<ItemWithChecklist?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddProjectDialog by remember { mutableStateOf(false) }
    var showAddAreaDialog by remember { mutableStateOf(false) }
    var inlineExpandedTaskId by remember { mutableStateOf<String?>(null) }
    var isSearchOverlayActive by remember { mutableStateOf(false) }
    var newTaskTitlePrefill by remember { mutableStateOf("") }
    
    // [ИЗМЕНЕНИЕ]: Состояния для недавно искавшихся объектов и подсветки конкретной задачи
    var recentSearchItems by remember { mutableStateOf<List<SearchResultItem>>(emptyList()) }
    var highlightedTaskId by remember { mutableStateOf<String?>(null) }
    
    // [ИЗМЕНЕНИЕ]: Функция для сохранения недавно искавшихся и нажатых объектов в поиске
    val addToRecent: (SearchResultItem) -> Unit = remember(recentSearchItems) {
        { item ->
            val current = recentSearchItems.toMutableList()
            val existingIndex = current.indexOfFirst { existing ->
                when {
                    existing is SearchResultItem.TaskResult && item is SearchResultItem.TaskResult -> 
                        existing.taskWrapper.item.id == item.taskWrapper.item.id
                    existing is SearchResultItem.ProjectResult && item is SearchResultItem.ProjectResult -> 
                        existing.project.id == item.project.id
                    existing is SearchResultItem.AreaResult && item is SearchResultItem.AreaResult -> 
                        existing.area.id == item.area.id
                    existing is SearchResultItem.SmartListResult && item is SearchResultItem.SmartListResult -> 
                        existing.screen == item.screen
                    else -> false
                }
            }
            if (existingIndex != -1) {
                current.removeAt(existingIndex)
            }
            current.add(0, item)
            recentSearchItems = current.take(10)
        }
    }
    
    // Project input fields
    var newProjectName by remember { mutableStateOf("") }
    val areas by viewModel.areas.collectAsState()
    var selectedAreaIdForNewProject by remember { mutableStateOf<String?>(null) }
    var showAreaDropdownInNewProject by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val isDark = false
    
    // Palette assignment
    val backgroundColor = if (isDark) ThingsBackgroundDark else ThingsBackgroundLight
    val cardSurfaceColor = if (isDark) ThingsSurfaceDark else ThingsSurfaceLight
    val textPrimaryColor = if (isDark) ThingsTextPrimaryDark else ThingsTextPrimaryLight
    val textSecondaryColor = if (isDark) ThingsTextSecondaryDark else ThingsTextSecondaryLight
    val dividerColor = if (isDark) ThingsDividerDark else ThingsDividerLight

    val categoryListState by viewModel.categoryListState.collectAsState()

    LaunchedEffect(activeScreen) {
        viewModel.setScreen(activeScreen)
    }
    LaunchedEffect(selectedProject) {
        viewModel.setProject(selectedProject)
    }
    LaunchedEffect(selectedArea) {
        viewModel.setArea(selectedArea)
    }
    LaunchedEffect(inlineExpandedTaskId) {
        viewModel.setInlineExpandedTaskId(inlineExpandedTaskId)
    }
    LaunchedEffect(highlightedTaskId) {
        viewModel.setHighlightedTaskId(highlightedTaskId)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        containerColor = backgroundColor,
        // [ИЗМЕНЕНИЕ]: Тулбары перенесены внутрь панелей (ThingsCategoryListPanel), 
        // чтобы индикатор поиска выезжал поверх них (Z-index/layering). 
        // Поэтому на уровне главного Scaffold тулбар больше не отображается.
        topBar = {},
        floatingActionButton = {
            // [ИЗМЕНЕНИЕ]: Скрывать глобально на уровне HomeScreen, если открыт встроенный редактор (inlineExpandedTaskId != null)
            if (inlineExpandedTaskId == null) {
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

                        val startValue = when (initialSection) {
                            TaskSection.INBOX -> 0
                            TaskSection.TODAY -> 1
                            TaskSection.ANYTIME -> 2
                            TaskSection.SOMEDAY -> 3
                            TaskSection.UPCOMING -> 2
                        }
                        val computedStartDate = when (targetScreen) {
                            ActiveScreen.TODAY -> System.currentTimeMillis()
                            ActiveScreen.UPCOMING -> {
                                val earliestUpcomingTask = allTasksRaw
                                    .filter { it.item.isUpcoming }
                                    .minByOrNull { it.item.startDate ?: Long.MAX_VALUE }
                                
                                earliestUpcomingTask?.item?.startDate ?: java.util.Calendar.getInstance().apply {
                                    add(java.util.Calendar.DAY_OF_YEAR, 1)
                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }.timeInMillis
                            }
                            else -> null
                        }

                        val newTask = Item(
                            id = newTaskId,
                            type = 0,
                            title = "",
                            notes = "",
                            start = startValue,
                            projectId = initialProjectId,
                            startDate = computedStartDate,
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
                        onAreaClick = { area ->
                            selectedArea = area
                            activeScreen = ActiveScreen.AREA_DETAIL
                        },
                        onAddProjectClick = { showAddProjectDialog = true },
                        areas = areas,
                        onAddAreaClick = { showAddAreaDialog = true },
                        onDeleteArea = { viewModel.deleteArea(it) },
                        onDeleteProject = { viewModel.deleteProject(it) },
                        onSyncClick = { token ->
                            viewModel.setAccessToken(token)
                            viewModel.syncWithGoogle()
                        },
                        onSearchClick = { isSearchOverlayActive = true },
                        // [ИЗМЕНЕНИЕ]: Передается флаг активности оверлея поиска
                        isSearchOverlayActive = isSearchOverlayActive
                    )
                    ActiveScreen.SEARCH -> ThingsSearchScreen(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        allTasks = allTasksRaw,
                        projects = projects,
                        areas = areas,
                        textPrimaryColor = textPrimaryColor,
                        textSecondaryColor = textSecondaryColor,
                        dividerColor = dividerColor,
                        onTaskClick = { clickedTask ->
                            taskToEdit = clickedTask
                            showAddDialog = true
                        },
                        onTaskToggle = { toggledTask ->
                            viewModel.toggleTaskCompletion(toggledTask)
                        },
                        onBack = {
                            activeScreen = ActiveScreen.HOME
                            viewModel.setSearchQuery("")
                        },
                        onFabClick = {
                            taskToEdit = null
                            newTaskTitlePrefill = searchQuery
                            showAddDialog = true
                        }
                    )
                    else -> ThingsCategoryListPanel(
                        state = categoryListState.copy(
                            textPrimaryColor = textPrimaryColor,
                            textSecondaryColor = textSecondaryColor,
                            dividerColor = dividerColor
                        ),
                        onEvent = { event ->
                            when (event) {
                                is ThingsCategoryListEvent.SelectTag -> {
                                    viewModel.selectTag(event.tag)
                                }
                                is ThingsCategoryListEvent.ToggleTask -> {
                                    viewModel.toggleTaskCompletion(event.task)
                                }
                                is ThingsCategoryListEvent.ClickTask -> {
                                    inlineExpandedTaskId = event.task.item.id
                                }
                                is ThingsCategoryListEvent.ClickProject -> {
                                    selectedProject = event.project
                                    activeScreen = ActiveScreen.PROJECT_DETAIL
                                }
                                is ThingsCategoryListEvent.ChangeInlineExpandedTaskId -> {
                                    inlineExpandedTaskId = event.taskId
                                }
                                ThingsCategoryListEvent.ClickSearch -> {
                                    isSearchOverlayActive = true
                                }
                                ThingsCategoryListEvent.ClickBack -> {
                                    activeScreen = ActiveScreen.HOME
                                    selectedProject = null
                                    selectedArea = null
                                    viewModel.selectTag(null)
                                }
                                is ThingsCategoryListEvent.CreateTag -> {
                                    viewModel.insertTag(event.title, event.parentId)
                                }
                                is ThingsCategoryListEvent.DeleteTag -> {
                                    viewModel.deleteTag(event.tag)
                                }
                                is ThingsCategoryListEvent.UpdateTag -> {
                                    viewModel.updateTag(event.tag)
                                }
                                is ThingsCategoryListEvent.UpdateTagsOrder -> {
                                    viewModel.updateTagsOrder(event.tags)
                                }
                                is ThingsCategoryListEvent.SaveTask -> {
                                    viewModel.updateTask(
                                        task = event.taskWrapper.item,
                                        checklist = event.checklist,
                                        section = event.section,
                                        title = event.title,
                                        notes = event.notes,
                                        isTonight = event.isTonight,
                                        startDate = event.startDate,
                                        dueDate = event.dueDate,
                                        tags = event.tags,
                                        projectId = event.projectId,
                                        priority = event.priority
                                    )
                                }
                                is ThingsCategoryListEvent.DeleteTask -> {
                                    viewModel.deleteTask(event.taskWrapper)
                                }
                                is ThingsCategoryListEvent.DuplicateTask -> {
                                    viewModel.duplicateTask(event.taskWrapper)
                                }
                                is ThingsCategoryListEvent.MoveTask -> {
                                    val updatedTask = if (event.moveToInbox) {
                                        event.taskWrapper.item.copy(
                                            projectId = null,
                                            areaId = null,
                                            start = 0,
                                            startDate = null,
                                            dueDate = null,
                                            modificationDate = System.currentTimeMillis()
                                        )
                                    } else {
                                        val newStart = if (event.taskWrapper.item.start == 0 && event.projectId != null) 2 else event.taskWrapper.item.start
                                        event.taskWrapper.item.copy(
                                            projectId = event.projectId,
                                            areaId = event.areaId,
                                            start = newStart,
                                            modificationDate = System.currentTimeMillis()
                                        )
                                    }
                                    viewModel.updateTask(updatedTask, event.taskWrapper.checklist)
                                }
                                is ThingsCategoryListEvent.ReorderTasks -> {
                                    viewModel.updateTasks(event.items)
                                }
                            }
                        }
                    )
                }
            }

            // [ИЗМЕНЕНИЕ]: Отдельное затемняющее поле для заднего плана оверлея поиска, которое только меняет прозрачность независимо от масштаба карточки поиска
            AnimatedVisibility(
                visible = isSearchOverlayActive,
                enter = fadeIn(
                    animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)
                ),
                exit = fadeOut(
                    animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            isSearchOverlayActive = false
                            viewModel.setSearchQuery("")
                        }
                )
            }

            // [ИЗМЕНЕНИЕ]: Полноэкранный оверлей поиска отображается с пружинной анимацией входа (масштаб + прозрачность), раскрываясь из области поля поиска (сверху по центру)
            AnimatedVisibility(
                visible = isSearchOverlayActive,
                enter = fadeIn(
                    animationSpec = spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                    )
                ) + scaleIn(
                    animationSpec = spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                    ),
                    initialScale = 0.6f,
                    transformOrigin = TransformOrigin(0.5f, 0.05f)
                ),
                exit = fadeOut(
                    animationSpec = spring(
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                    )
                ) + scaleOut(
                    animationSpec = spring(
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                    ),
                    targetScale = 0.6f,
                    transformOrigin = TransformOrigin(0.5f, 0.05f)
                )
            ) {
                ThingsSearchOverlay(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    allTasks = allTasksRaw,
                    projects = projects,
                    areas = areas,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    cardSurfaceColor = cardSurfaceColor,
                    dividerColor = dividerColor,
                    onTaskToggle = { viewModel.toggleTaskCompletion(it) },
                    onTaskClick = { task ->
                        // [ИЗМЕНЕНИЕ]: Добавление задачи в недавно искавшиеся объекты
                        addToRecent(SearchResultItem.TaskResult(task))

                        // Найти расположение задачи
                        val proj = projects.find { it.id == task.item.projectId }
                        val area = areas.find { it.id == task.item.areaId ?: proj?.areaId }
                        
                        selectedProject = proj
                        selectedArea = area
                        
                        // Определить активный экран
                        activeScreen = when {
                            task.item.projectId != null -> ActiveScreen.PROJECT_DETAIL
                            task.item.areaId != null -> ActiveScreen.AREA_DETAIL
                            task.item.isCompleted -> ActiveScreen.LOGBOOK
                            task.item.isInbox -> ActiveScreen.INBOX
                            task.item.isToday -> ActiveScreen.TODAY
                            task.item.isUpcoming -> ActiveScreen.UPCOMING
                            task.item.isAnytime -> ActiveScreen.ANYTIME
                            task.item.isSomeday -> ActiveScreen.SOMEDAY
                            else -> ActiveScreen.INBOX
                        }
                        
                        // [ИЗМЕНЕНИЕ]: Кликнутая задача более не разворачивается для редактирования, а кратковременно подсвечивается
                        highlightedTaskId = task.item.id
                        scope.launch {
                            kotlinx.coroutines.delay(1500)
                            if (highlightedTaskId == task.item.id) {
                                highlightedTaskId = null
                            }
                        }
                        
                        viewModel.setSearchQuery("")
                        isSearchOverlayActive = false
                    },
                    onProjectClick = { proj ->
                        // [ИЗМЕНЕНИЕ]: Добавление проекта в недавно искавшиеся объекты
                        addToRecent(SearchResultItem.ProjectResult(proj))

                        selectedProject = proj
                        activeScreen = ActiveScreen.PROJECT_DETAIL
                        viewModel.setSearchQuery("")
                        isSearchOverlayActive = false
                    },
                    onAreaClick = { area ->
                        // [ИЗМЕНЕНИЕ]: Добавление области в недавно искавшиеся объекты
                        addToRecent(SearchResultItem.AreaResult(area))

                        selectedArea = area
                        activeScreen = ActiveScreen.AREA_DETAIL
                        viewModel.setSearchQuery("")
                        isSearchOverlayActive = false
                    },
                    onSmartListClick = { smartScreen ->
                        // [ИЗМЕНЕНИЕ]: Добавление смарт-списка в недавно искавшиеся объекты
                        val title = when (smartScreen) {
                            ActiveScreen.TODAY -> "Today"
                            ActiveScreen.INBOX -> "Inbox"
                            ActiveScreen.UPCOMING -> "Upcoming"
                            ActiveScreen.ANYTIME -> "Anytime"
                            ActiveScreen.SOMEDAY -> "Someday"
                            ActiveScreen.LOGBOOK -> "Logbook"
                            else -> "List"
                        }
                        addToRecent(SearchResultItem.SmartListResult(title, smartScreen))

                        activeScreen = smartScreen
                        viewModel.setSearchQuery("")
                        isSearchOverlayActive = false
                    },
                    onClose = {
                        isSearchOverlayActive = false
                        viewModel.setSearchQuery("")
                    },
                    // [ИЗМЕНЕНИЕ]: Передача списка недавно найденных/искавшихся объектов
                    recentSearchItems = recentSearchItems,
                    onContinueSearchClick = {
                        isSearchOverlayActive = false
                        activeScreen = ActiveScreen.SEARCH
                    }
                )
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
        val currentTaskToEdit = taskToEdit

        ThingsTaskDetailsSheet(
            task = currentTaskToEdit,
            initialSection = initialSection,
            initialProjectId = initialProjectId,
            projects = projects,
            onDismiss = { 
                showAddDialog = false
                newTaskTitlePrefill = ""
            },
            onSave = { title, notes, section, isTonight, startDate, tags, projectId, checklistItems ->
                if (currentTaskToEdit == null) {
                    // Create Task
                    viewModel.addTask(title, notes, section, isTonight, startDate, tags, projectId, checklistItems)
                } else {
                    // Update Task
                    val startVal = when (section) {
                        TaskSection.INBOX -> 0
                        TaskSection.TODAY -> 1
                        TaskSection.ANYTIME -> 2
                        TaskSection.SOMEDAY -> 3
                        TaskSection.UPCOMING -> 2
                    }
                    val updatedTask = currentTaskToEdit.item.copy(
                         title = title,
                         notes = notes,
                         start = startVal,
                         isTonight = isTonight,
                         startDate = startDate,
                         cachedTags = tags.joinToString(", "),
                         projectId = projectId
                    )
                    viewModel.updateTask(updatedTask, checklistItems)
                    
                }
                showAddDialog = false
                newTaskTitlePrefill = ""
            },
            onDelete = {
                currentTaskToEdit?.let { viewModel.deleteTask(it) }
                showAddDialog = false
                newTaskTitlePrefill = ""
            },
            initialTitle = newTaskTitlePrefill
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
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Area of Responsibility:", color = textPrimaryColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, dividerColor, RoundedCornerShape(4.dp))
                            .clickable { showAreaDropdownInNewProject = true }
                            .padding(12.dp)
                    ) {
                        val activeAreaName = if (selectedAreaIdForNewProject == null) "Без области" else {
                            areas.firstOrNull { it.id == selectedAreaIdForNewProject }?.title ?: "Без области"
                        }
                        Text(activeAreaName, color = textPrimaryColor)
                        
                        DropdownMenu(
                            expanded = showAreaDropdownInNewProject,
                            onDismissRequest = { showAreaDropdownInNewProject = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Без области", color = textPrimaryColor) },
                                onClick = {
                                    selectedAreaIdForNewProject = null
                                    showAreaDropdownInNewProject = false
                                }
                            )
                            areas.forEach { area ->
                                DropdownMenuItem(
                                    text = { Text(area.title, color = textPrimaryColor) },
                                    onClick = {
                                        selectedAreaIdForNewProject = area.id
                                        showAreaDropdownInNewProject = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newProjectName.isNotBlank()) {
                            viewModel.addProject(newProjectName.trim(), areaId = selectedAreaIdForNewProject)
                            newProjectName = ""
                            selectedAreaIdForNewProject = null
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

    // Area creation Dialog
    if (showAddAreaDialog) {
        var newAreaName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddAreaDialog = false },
            title = { Text("Create Responsibility Area", fontWeight = FontWeight.Bold, color = textPrimaryColor) },
            text = {
                Column {
                    Text("Areas (Области) organize related activities like Work, Personal Life, or Health, and do not have deadlines.", color = textSecondaryColor, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newAreaName,
                        onValueChange = { newAreaName = it },
                        label = { Text("Area Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("area_title_input"),
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
                        if (newAreaName.isNotBlank()) {
                            viewModel.addArea(newAreaName.trim())
                            showAddAreaDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_area")
                ) {
                    Text("Create", color = ThingsBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAreaDialog = false }) {
                    Text("Cancel", color = textSecondaryColor)
                }
            },
            containerColor = cardSurfaceColor
        )
    }
}
