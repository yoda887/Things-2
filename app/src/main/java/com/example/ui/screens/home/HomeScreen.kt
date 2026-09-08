package com.example.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.example.ui.screens.home.inlineeditor.dialogs.QuickAddDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.ThingsViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute


/**
 * ThingsHomeScreen: Now serves purely as the top-level orchestration point. 
 * It retains the global navigation state (active screens), Floating Action Button action 
 * triggers, permission request launchers, dialog manager flags, and coordinate cross-fades 
 * between panels.
 */


@Serializable object HomeRoute
@Serializable object SearchRoute
@Serializable data class ListRoute(val screen: ActiveScreen)

@Serializable
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
    val allSavedTags by viewModel.allSavedTags.collectAsState()
    val allSavedTagObjects by viewModel.allSavedTagObjects.collectAsState()
    
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

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    var activeScreen by remember { mutableStateOf(ActiveScreen.HOME) }

    LaunchedEffect(navBackStackEntry) {
        navBackStackEntry?.let { entry ->
            val route = entry.destination.route ?: ""
            val newScreen = when {
                route.contains("HomeRoute") -> ActiveScreen.HOME
                route.contains("SearchRoute") -> ActiveScreen.SEARCH
                route.contains("ListRoute") -> {
                    try {
                        entry.toRoute<ListRoute>().screen
                    } catch (e: Exception) {
                        ActiveScreen.HOME
                    }
                }
                else -> ActiveScreen.HOME
            }
            if (activeScreen != newScreen) {
                activeScreen = newScreen
            }
        }
    }

    fun navigateTo(screen: ActiveScreen) {
        if (activeScreen == screen) return
        val route: Any = when (screen) {
            ActiveScreen.HOME -> HomeRoute
            ActiveScreen.SEARCH -> SearchRoute
            else -> ListRoute(screen)
        }
        navController.navigate(route) {
            launchSingleTop = true
        }
    }
    var selectedProject by remember { mutableStateOf<Item?>(null) }
    var selectedArea by remember { mutableStateOf<Area?>(null) }
    var taskToEdit by remember { mutableStateOf<ItemWithChecklist?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddProjectDialog by remember { mutableStateOf(false) }
    var showAddAreaDialog by remember { mutableStateOf(false) }
    var inlineExpandedTaskId by remember { mutableStateOf<String?>(null) }
    var editingProjectId by remember { mutableStateOf<String?>(null) }
    var editingAreaId by remember { mutableStateOf<String?>(null) }
    var showFabMenu by remember { mutableStateOf(false) }
    var isSearchOverlayActive by remember { mutableStateOf(false) }
    var newTaskTitlePrefill by remember { mutableStateOf("") }
    var isListDialogActive by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedTaskIds by remember { mutableStateOf(setOf<String>()) }

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
        isSelectionMode = false
        selectedTaskIds = emptySet()
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
            // Скрывать FAB при открытии inline-редактора, FAB-меню, диалогов, окна быстрой задачи (QuickAddDialog) или режима мультивыбора
            AnimatedVisibility(
                visible = inlineExpandedTaskId == null && !showFabMenu && !isListDialogActive && !showAddDialog && !isSelectionMode,
                enter = slideInVertically(
                    initialOffsetY = { it * 2 },
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = 240f
                    )
                ) + fadeIn(animationSpec = tween(durationMillis = 220)),
                exit = slideOutVertically(
                    targetOffsetY = { it * 2 },
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 180))
            ) {
                val view = androidx.compose.ui.platform.LocalView.current
                FloatingActionButton(
                    onClick = {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                        if (activeScreen == ActiveScreen.HOME) {
                            showFabMenu = true
                        } else {
                            val targetScreen = activeScreen
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
                        }
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
            // [ИЗМЕНЕНИЕ]: Анимированный переход между всеми экранами, имитирующий масштабное появление (scale-up) с эффектом отскока (overshoot)
            // аналогично оригинальному проекту Things (makeScaleUpAnimation).
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                enterTransition = {
                    scaleIn(
                        initialScale = 0f,
                        transformOrigin = TransformOrigin.Center,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 200)
                    )
                },
                exitTransition = {
                    scaleOut(
                        targetScale = 0.9f,
                        transformOrigin = TransformOrigin.Center,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 200)
                    )
                },
                popEnterTransition = {
                    scaleIn(
                        initialScale = 0.9f,
                        transformOrigin = TransformOrigin.Center,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 200)
                    )
                },
                popExitTransition = {
                    scaleOut(
                        targetScale = 0f,
                        transformOrigin = TransformOrigin.Center,
                        animationSpec = tween(durationMillis = 300, easing = CubicBezierEasing(0.4f, 0f, 1f, 1f))
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 200)
                    )
                }
            ) {
                composable<HomeRoute> {
                    // Применяем обертку ScreenTransitionWrapper со сплошным фоном и эффектом затемнения для уходящего экрана
                    ScreenTransitionWrapper(isStartDestination = true, backgroundColor = backgroundColor) {
                        ThingsHomePanel(
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
                            onSmartListClick = { listScreen -> navigateTo(listScreen) },
                            onProjectClick = { proj ->
                                selectedProject = proj
                                navigateTo(ActiveScreen.PROJECT_DETAIL)
                            },
                            onAreaClick = { area ->
                                selectedArea = area
                                navigateTo(ActiveScreen.AREA_DETAIL)
                            },
                            onAddProjectClick = {},
                            areas = areas,
                            onAddAreaClick = {},
                            onDeleteArea = { viewModel.deleteArea(it) },
                            onDeleteProject = { viewModel.deleteProject(it) },
                            onSyncClick = { token ->
                                viewModel.setAccessToken(token)
                                viewModel.syncWithGoogle()
                            },
                            onSearchClick = { isSearchOverlayActive = true },
                            isSearchOverlayActive = isSearchOverlayActive,
                            editingProjectId = editingProjectId,
                            onEditingProjectIdChange = { editingProjectId = it },
                            editingAreaId = editingAreaId,
                            onEditingAreaIdChange = { editingAreaId = it },
                            onUpdateProject = { viewModel.updateProject(it) },
                            onUpdateArea = { viewModel.updateArea(it) },
                            onProjectsReordered = { viewModel.updateTasks(it) },
                            onAreasReordered = { viewModel.updateAreas(it) }
                        )
                    }
                }
                composable<SearchRoute> {
                    // Применяем обертку ScreenTransitionWrapper со сплошным фоном и эффектом затемнения
                    ScreenTransitionWrapper(isStartDestination = false, backgroundColor = backgroundColor) {
                        ThingsSearchScreen(
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
                                navController.popBackStack()
                                viewModel.setSearchQuery("")
                            },
                            onFabClick = {
                                taskToEdit = null
                                newTaskTitlePrefill = searchQuery
                                showAddDialog = true
                            }
                        )
                    }
                }
                
                // Helper for the category lists
                val listScreenContent: @Composable (ActiveScreen) -> Unit = { screen ->
                    // Мы запоминаем проект и область именно для данного инстанса экрана на момент его создания/отображения,
                    // чтобы при изменении глобальных selectedProject/selectedArea на других экранах (или при сбросе в null на "Назад")
                    // этот конкретный экран сохранял свое состояние и данные для плавной анимации ухода.
                    val screenProject = remember(screen) { selectedProject }
                    val screenArea = remember(screen) { selectedArea }

                    val screenStateFlow = remember(screen, screenProject, screenArea) {
                        viewModel.getCategoryListStateFlow(screen, screenProject, screenArea)
                    }
                    val screenState by screenStateFlow.collectAsState()

                    ThingsCategoryListPanel(
                        state = screenState.copy(
                            textPrimaryColor = textPrimaryColor,
                            textSecondaryColor = textSecondaryColor,
                            dividerColor = dividerColor,
                            isSelectionMode = isSelectionMode,
                            selectedTaskIds = selectedTaskIds
                        ),
                        onDialogsActiveChange = { isListDialogActive = it },
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
                                    navigateTo(ActiveScreen.PROJECT_DETAIL)
                                }
                                is ThingsCategoryListEvent.ClickArea -> {
                                    selectedArea = event.area
                                    navigateTo(ActiveScreen.AREA_DETAIL)
                                }
                                is ThingsCategoryListEvent.ChangeInlineExpandedTaskId -> {
                                    inlineExpandedTaskId = event.taskId
                                }
                                ThingsCategoryListEvent.ClickSearch -> {
                                    isSearchOverlayActive = true
                                }
                                ThingsCategoryListEvent.ClickBack -> {
                                    navController.popBackStack()
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
                                is ThingsCategoryListEvent.DeleteProject -> {
                                    viewModel.deleteProject(event.project)
                                    navController.popBackStack()
                                    selectedProject = null
                                }
                                is ThingsCategoryListEvent.DeleteArea -> {
                                    viewModel.deleteArea(event.area)
                                    navController.popBackStack()
                                    selectedArea = null
                                }
                                is ThingsCategoryListEvent.SwipeTaskLeft -> {
                                    inlineExpandedTaskId = null
                                    if (isSelectionMode) {
                                        val taskId = event.task.item.id
                                        if (selectedTaskIds.contains(taskId)) {
                                            val newSelected = selectedTaskIds - taskId
                                            if (newSelected.isEmpty()) {
                                                isSelectionMode = false
                                                selectedTaskIds = emptySet()
                                            } else {
                                                selectedTaskIds = newSelected
                                            }
                                        } else {
                                            selectedTaskIds = selectedTaskIds + taskId
                                        }
                                    } else {
                                        isSelectionMode = true
                                        selectedTaskIds = setOf(event.task.item.id)
                                    }
                                }
                                is ThingsCategoryListEvent.SwipeTaskRight -> {
                                    // Заглушка для When / Календаря
                                }
                                is ThingsCategoryListEvent.EnterSelectionMode -> {
                                    inlineExpandedTaskId = null
                                    isSelectionMode = true
                                    selectedTaskIds = if (event.initialTaskId != null) setOf(event.initialTaskId) else emptySet()
                                }
                                is ThingsCategoryListEvent.ToggleTaskSelection -> {
                                    selectedTaskIds = if (selectedTaskIds.contains(event.taskId)) {
                                        selectedTaskIds - event.taskId
                                    } else {
                                        selectedTaskIds + event.taskId
                                    }
                                }
                                is ThingsCategoryListEvent.SetTaskSelected -> {
                                    selectedTaskIds = if (event.selected) {
                                        selectedTaskIds + event.taskId
                                    } else {
                                        selectedTaskIds - event.taskId
                                    }
                                }
                                ThingsCategoryListEvent.SelectAllTasks -> {
                                    selectedTaskIds = screenState.displayTasks.map { it.item.id }.toSet()
                                }
                                ThingsCategoryListEvent.DeselectAllTasks -> {
                                    selectedTaskIds = emptySet()
                                }
                                ThingsCategoryListEvent.ExitSelectionMode -> {
                                    isSelectionMode = false
                                    selectedTaskIds = emptySet()
                                }
                                is ThingsCategoryListEvent.BatchCompleteTasks -> {
                                    val selectedTasks = screenState.allTasks.filter { selectedTaskIds.contains(it.item.id) }
                                    viewModel.batchSetCompleted(selectedTasks, event.completed)
                                    isSelectionMode = false
                                    selectedTaskIds = emptySet()
                                }
                                ThingsCategoryListEvent.BatchDeleteTasks -> {
                                    val selectedTasks = screenState.allTasks.filter { selectedTaskIds.contains(it.item.id) }.map { it.item }
                                    viewModel.deleteTasks(selectedTasks)
                                    isSelectionMode = false
                                    selectedTaskIds = emptySet()
                                }
                                ThingsCategoryListEvent.BatchDuplicateTasks -> {
                                    val selectedTasks = screenState.allTasks.filter { selectedTaskIds.contains(it.item.id) }
                                    viewModel.batchDuplicateTasks(selectedTasks)
                                    isSelectionMode = false
                                    selectedTaskIds = emptySet()
                                }
                                is ThingsCategoryListEvent.BatchScheduleTasks -> {
                                    val selectedTasks = screenState.allTasks.filter { selectedTaskIds.contains(it.item.id) }
                                    viewModel.batchScheduleTasks(selectedTasks, event.startDate, event.isTonight)
                                    isSelectionMode = false
                                    selectedTaskIds = emptySet()
                                }
                                is ThingsCategoryListEvent.BatchMoveTasks -> {
                                    val selectedTasks = screenState.allTasks.filter { selectedTaskIds.contains(it.item.id) }
                                    viewModel.batchMoveTasks(selectedTasks, event.projectId, event.areaId, event.moveToInbox)
                                    isSelectionMode = false
                                    selectedTaskIds = emptySet()
                                }
                                is ThingsCategoryListEvent.BatchSetTags -> {
                                    val selectedTasks = screenState.allTasks.filter { selectedTaskIds.contains(it.item.id) }
                                    viewModel.batchAddTags(selectedTasks, event.tags)
                                    isSelectionMode = false
                                    selectedTaskIds = emptySet()
                                }
                                is ThingsCategoryListEvent.BatchSetDeadline -> {
                                    val selectedTasks = screenState.allTasks.filter { selectedTaskIds.contains(it.item.id) }
                                    viewModel.batchSetDeadline(selectedTasks, event.deadline)
                                    isSelectionMode = false
                                    selectedTaskIds = emptySet()
                                }
                            }
                        }
                    )
                }

                composable<ListRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<ListRoute>()
                    // Применяем обертку ScreenTransitionWrapper со сплошным фоном и эффектом затемнения
                    ScreenTransitionWrapper(isStartDestination = false, backgroundColor = backgroundColor) {
                        listScreenContent(route.screen)
                    }
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
                        
                        // [ИЗМЕНЕНИЕ]: Переход на соответствующий экран через navigateTo вместо прямого присвоения activeScreen
                        val targetScreen = when {
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
                        navigateTo(targetScreen)
                        
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
                        navigateTo(ActiveScreen.PROJECT_DETAIL)
                        viewModel.setSearchQuery("")
                        isSearchOverlayActive = false
                    },
                    onAreaClick = { area ->
                        // [ИЗМЕНЕНИЕ]: Добавление области в недавно искавшиеся объекты
                        addToRecent(SearchResultItem.AreaResult(area))

                        selectedArea = area
                        navigateTo(ActiveScreen.AREA_DETAIL)
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

                        navigateTo(smartScreen)
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
                        navigateTo(ActiveScreen.SEARCH)
                    }
                )
            }

            AnimatedVisibility(
                visible = showFabMenu,
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
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showFabMenu = false
                        }
                ) {
                    AnimatedVisibility(
                        visible = showFabMenu,
                        enter = scaleIn(
                            transformOrigin = TransformOrigin(1f, 1f),
                            animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)
                        ) + fadeIn(
                            animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)
                        ),
                        exit = scaleOut(
                            transformOrigin = TransformOrigin(1f, 1f),
                            animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)
                        ) + fadeOut(
                            animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF23252E), shape = RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                .padding(vertical = 4.dp)
                        ) {
                            // 1. New To-Do
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showFabMenu = false
                                        showAddDialog = true
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(top = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "New To-Do",
                                        style = TextStyle(
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Quickly add a to-do to your inbox.",
                                        style = TextStyle(
                                            fontSize = 13.sp,
                                            color = Color(0xFF8B8C8E)
                                        )
                                    )
                                }
                            }

                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(0.5.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )

                            // 2. New Project
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showFabMenu = false
                                        val newProjectId = java.util.UUID.randomUUID().toString()
                                        val newProject = Item(
                                            id = newProjectId,
                                            type = 1,
                                            title = "",
                                            creationDate = System.currentTimeMillis()
                                        )
                                        viewModel.updateProject(newProject)
                                        editingProjectId = newProjectId
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(top = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = ThingsBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "New Project",
                                        style = TextStyle(
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Define a goal, then work towards it one to-do at a time.",
                                        style = TextStyle(
                                            fontSize = 13.sp,
                                            color = Color(0xFF8B8C8E)
                                        )
                                    )
                                }
                            }

                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(0.5.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )

                            // 3. New Area
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showFabMenu = false
                                        val newAreaId = java.util.UUID.randomUUID().toString()
                                        val newArea = Area(
                                            id = newAreaId,
                                            title = ""
                                        )
                                        viewModel.updateArea(newArea)
                                        editingAreaId = newAreaId
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(top = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = ThingsLogbookGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "New Area",
                                        style = TextStyle(
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Group projects and to-dos based on different responsibilities, such as Family or Work.",
                                        style = TextStyle(
                                            fontSize = 13.sp,
                                            color = Color(0xFF8B8C8E)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Task Create / Edit Sheet
    // Task Create Sheet (QuickAddDialog)
    if (showAddDialog) {
        QuickAddDialog(
            projects = projects,
            areas = areas,
            allSavedTags = allSavedTags,
            allSavedTagObjects = allSavedTagObjects,
            allTasksRaw = allTasksRaw,
            onNewTagCreated = { name, parentId -> viewModel.createTagInGroup(name, parentId) },
            onDeleteTag = { tag -> viewModel.deleteTag(tag) },
            onUpdateTag = { tag -> viewModel.updateTag(tag) },
            onUpdateTagsOrder = { tags -> viewModel.updateTagsOrder(tags) },
            onSave = { title, notes, section, isTonight, startDate, dueDate, tags, projectId, checklistItems, priority ->
                viewModel.addTask(
                    title = title,
                    notes = notes,
                    section = section,
                    isTonight = isTonight,
                    startDate = startDate,
                    tags = tags,
                    projectId = projectId,
                    checklist = checklistItems,
                    priority = priority
                )
                showAddDialog = false
            },
            onDismissRequest = {
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

/**
 * A wrapper for destination screens in [NavHost] that adds solid background and transition dimming.
 *
 * It implements the core feature:
 * - A dark semi-transparent veil over departing or pop-entering screens (the left-most layer)
 *   with a maximum opacity of 0.3f, while maintaining a solid background to avoid transparency artifacts.
 *
 * @param isStartDestination whether the current screen is the root start destination
 * @param backgroundColor the solid background color of the screen to prevent transparent overlap
 * @param content the UI content of the screen
 */
@Composable
private fun AnimatedVisibilityScope.ScreenTransitionWrapper(
    isStartDestination: Boolean,
    backgroundColor: Color,
    content: @Composable () -> Unit
) {
    // Animate the transition progress based on the current state.
    // Visible maps to 0f dimming, PostExit maps to 0.3f dimming, PreEnter/initial states map to 0f dimming.
    val dimmingAlpha by transition.animateFloat(
        transitionSpec = {
            tween(durationMillis = 300, easing = FastOutSlowInEasing)
        },
        label = "DimmingAlpha"
    ) { state ->
        when (state) {
            EnterExitState.Visible -> 0f
            EnterExitState.PostExit -> 0.3f
            EnterExitState.PreEnter -> 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            // Draw a dark semi-transparent dimming veil on top of the screen content when it is exiting/pop-entering
            .drawWithContent {
                drawContent()
                if (dimmingAlpha > 0f) {
                    drawRect(color = Color.Black.copy(alpha = dimmingAlpha))
                }
            }
    ) {
        content()
    }
}

