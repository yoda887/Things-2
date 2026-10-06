package com.example.ui.screens.home

import com.example.ui.components.ThingsDropdownMenu
import com.example.ui.components.ThingsMenuItem
import com.example.ui.theme.ThingsTheme
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import com.example.ui.components.fabdrag.DraggableAddButton
import com.example.ui.components.fabdrag.FabDragActions
import com.example.ui.components.fabdrag.FabDragController
import com.example.ui.components.fabdrag.LocalFabDragController
import com.example.ui.components.holdForSoftKeyboardHide
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import androidx.core.content.ContextCompat
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.TaskSection
import com.example.data.model.Area
import com.example.data.model.Tag
import com.example.ui.screens.home.components.ThingsHomePanel
import com.example.ui.screens.home.components.ThingsCategoryListPanel
import com.example.ui.screens.home.components.ThingsCategoryListState
import com.example.ui.screens.home.components.ThingsCategoryListEvent
import com.example.ui.screens.home.components.ThingsSearchOverlay
import com.example.ui.screens.home.components.SearchResultItem
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
@Serializable data class ListRoute(val screen: ActiveScreen, val entityId: String? = null)

@Serializable
enum class ActiveScreen {
    HOME, INBOX, TODAY, UPCOMING, ANYTIME, SOMEDAY, LOGBOOK, PROJECT_DETAIL, AREA_DETAIL, SEARCH, TAG_DETAIL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThingsHomeScreen(viewModel: ThingsViewModel = hiltViewModel()) {
    val tasks by viewModel.filteredTasks.collectAsState()
    val allTasksRaw by viewModel.tasks.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val areas by viewModel.areas.collectAsState()
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
    var selectedProject by remember { mutableStateOf<Item?>(null) }
    var selectedArea by remember { mutableStateOf<Area?>(null) }
    var selectedTagDetail by remember { mutableStateOf<Tag?>(null) }

    LaunchedEffect(navBackStackEntry, projects, areas, allSavedTagObjects) {
        navBackStackEntry?.let { entry ->
            val route = entry.destination.route ?: ""
            val newScreen = when {
                route.contains("HomeRoute") -> {
                    selectedProject = null
                    selectedArea = null
                    selectedTagDetail = null
                    ActiveScreen.HOME
                }
                route.contains("SearchRoute") -> ActiveScreen.SEARCH
                route.contains("ListRoute") -> {
                    try {
                        val listRoute = entry.toRoute<ListRoute>()
                        if (listRoute.screen == ActiveScreen.PROJECT_DETAIL) {
                            selectedProject = projects.firstOrNull { it.id == listRoute.entityId } ?: selectedProject
                            selectedArea = null
                            selectedTagDetail = null
                        } else if (listRoute.screen == ActiveScreen.AREA_DETAIL) {
                            selectedArea = areas.firstOrNull { it.id == listRoute.entityId } ?: selectedArea
                            selectedProject = null
                            selectedTagDetail = null
                        } else if (listRoute.screen == ActiveScreen.TAG_DETAIL) {
                            selectedTagDetail = allSavedTagObjects.firstOrNull { it.id == listRoute.entityId || it.title == listRoute.entityId } ?: selectedTagDetail
                            selectedProject = null
                            selectedArea = null
                        } else {
                            selectedProject = null
                            selectedArea = null
                            selectedTagDetail = null
                        }
                        listRoute.screen
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

    fun navigateTo(screen: ActiveScreen, entityId: String? = null) {
        val currentListRoute = try {
            navBackStackEntry?.toRoute<ListRoute>()
        } catch (e: Exception) {
            null
        }
        if (activeScreen == screen) {
            if (screen == ActiveScreen.PROJECT_DETAIL || screen == ActiveScreen.AREA_DETAIL || screen == ActiveScreen.TAG_DETAIL) {
                if (currentListRoute?.entityId == entityId && entityId != null) return
            } else {
                return
            }
        }
        val route: Any = when (screen) {
            ActiveScreen.HOME -> HomeRoute
            ActiveScreen.SEARCH -> SearchRoute
            else -> ListRoute(screen, entityId)
        }
        navController.navigate(route) {
            launchSingleTop = true
        }
    }
    var taskToEdit by remember { mutableStateOf<ItemWithChecklist?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddProjectDialog by remember { mutableStateOf(false) }
    var showAddAreaDialog by remember { mutableStateOf(false) }
    var editingProjectId by remember { mutableStateOf<String?>(null) }
    var editingAreaId by remember { mutableStateOf<String?>(null) }
    var showFabMenu by remember { mutableStateOf(false) }
    var isSearchOverlayActive by remember { mutableStateOf(false) }
    // Окно поиска готово рисоваться на месте поля: поле стартового экрана прячется только после этого
    var isSearchMorphReady by remember { mutableStateOf(false) }
    // [ИЗМЕНЕНИЕ]: Плавная прозрачность капсулы поиска для бесшовного cross-fade при закрытии оверлея
    var searchCapsuleAlpha by remember { mutableFloatStateOf(1f) }
    // Прямоугольник, из которого разворачивается Quick Find (поле поиска или круг оттяжки)
    var searchMorphSource by remember { mutableStateOf<Rect?>(null) }
    var searchWasPulled by remember { mutableStateOf(false) }
    // Источник морфинга — поле поиска стартового экрана (иначе круг оттяжки на экранах списков)
    var searchFromWideField by remember { mutableStateOf(true) }
    var newTaskTitlePrefill by remember { mutableStateOf("") }
    var isListDialogActive by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedTaskIds by remember { mutableStateOf(setOf<String>()) }

    // [ИЗМЕНЕНИЕ]: Состояния для недавно искавшихся объектов и подсветки конкретной задачи
    var recentSearchItems by remember { mutableStateOf<List<SearchResultItem>>(emptyList()) }

    // Развёрнутая и подсвеченная задачи живут только во ViewModel: экран их читает, но не хранит копию,
    // поэтому состояние переживает поворот экрана и не может разойтись с тем, что видит список
    val inlineExpandedTaskId by viewModel.inlineExpandedTaskId.collectAsState()
    val highlightedTaskId by viewModel.highlightedTaskId.collectAsState()


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
                    existing is SearchResultItem.TagResult && item is SearchResultItem.TagResult -> 
                        existing.tag.id == item.tag.id
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
    var selectedAreaIdForNewProject by remember { mutableStateOf<String?>(null) }
    var showAreaDropdownInNewProject by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    // Palette assignment from ThingsTheme
    val backgroundColor = ThingsTheme.colors.background
    val cardSurfaceColor = ThingsTheme.colors.surface
    val textPrimaryColor = ThingsTheme.colors.textPrimary
    val textSecondaryColor = ThingsTheme.colors.textSecondary
    val dividerColor = ThingsTheme.colors.divider

    LaunchedEffect(activeScreen) {
        isSelectionMode = false
        selectedTaskIds = emptySet()
    }

    // Добавление перетаскиванием кнопки «+» (см. FabDragController)
    val fabDragController = remember { FabDragController() }
    // Редактор закрылся — кнопка снова показывается (она была спрятана после приземления в место новой задачи)
    LaunchedEffect(inlineExpandedTaskId) {
        if (inlineExpandedTaskId == null) fabDragController.hiddenAfterSettle = false
    }

    CompositionLocalProvider(LocalFabDragController provides fabDragController) {
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
                visible = fabDragController.isDragging || fabDragController.isSettling ||
                    (inlineExpandedTaskId == null && !showFabMenu && !isListDialogActive && !showAddDialog && !isSelectionMode),
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
                val onFabClick: () -> Unit = {
                                                if (activeScreen == ActiveScreen.HOME) {
                            showFabMenu = true
                        } else if (activeScreen == ActiveScreen.SEARCH) {
                            // На экране поиска новая задача создаётся с текстом запроса
                            taskToEdit = null
                            newTaskTitlePrefill = searchQuery
                            showAddDialog = true
                        } else {
                            val targetScreen = activeScreen
                            val initialSection = when (targetScreen) {
                                ActiveScreen.TODAY -> TaskSection.TODAY
                                ActiveScreen.UPCOMING -> TaskSection.UPCOMING
                                ActiveScreen.ANYTIME -> TaskSection.ANYTIME
                                ActiveScreen.SOMEDAY -> TaskSection.SOMEDAY
                                ActiveScreen.AREA_DETAIL -> TaskSection.ANYTIME
                                ActiveScreen.TAG_DETAIL -> TaskSection.ANYTIME
                                else -> TaskSection.INBOX
                            }
                            val initialProjectId = if (targetScreen == ActiveScreen.PROJECT_DETAIL) selectedProject?.id else null
                            val initialAreaId = if (targetScreen == ActiveScreen.AREA_DETAIL) selectedArea?.id else null
                            val initialCachedTags = if (targetScreen == ActiveScreen.TAG_DETAIL) selectedTagDetail?.title ?: "" else ""
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
                                areaId = initialAreaId,
                                cachedTags = initialCachedTags,
                                startDate = computedStartDate,
                                creationDate = System.currentTimeMillis()
                            )

                            viewModel.updateTask(newTask)
                            viewModel.setInlineExpandedTaskId(newTaskId)
                        }
                    }
                DraggableAddButton(
                    controller = fabDragController,
                    // Перетаскивать можно туда, где экран принимает сброс
                    canDrag = { fabDragController.dropTarget != null },
                    onClick = onFabClick,
                    onDropToInbox = {
                        taskToEdit = null
                        newTaskTitlePrefill = ""
                        showAddDialog = true
                    },
                    containerColor = ThingsTheme.colors.accent,
                    isEditorOpen = { inlineExpandedTaskId != null },
                    // Отступы 16 dp от краёв (Material Design 3) задаёт слот Scaffold
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("add_task_fab")
                )
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
                                navigateTo(ActiveScreen.PROJECT_DETAIL, proj.id)
                            },
                            onAreaClick = { area ->
                                selectedArea = area
                                navigateTo(ActiveScreen.AREA_DETAIL, area.id)
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
                            onSearchClick = { sourceBounds, wasPulled ->
                                searchMorphSource = sourceBounds
                                searchWasPulled = wasPulled
                                searchFromWideField = true
                                isSearchMorphReady = false
                                searchCapsuleAlpha = 0f
                                isSearchOverlayActive = true
                            },
                            isSearchOverlayActive = isSearchOverlayActive && isSearchMorphReady,
                            searchCapsuleAlpha = searchCapsuleAlpha,
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
                // Helper for the category lists
                val listScreenContent: @Composable (ActiveScreen, String?) -> Unit = { screen, entityId ->
                    // Мы запоминаем проект и область именно для данного инстанса экрана на момент его создания/отображения,
                    // чтобы при изменении глобальных selectedProject/selectedArea на других экранах (или при сбросе в null на "Назад")
                    // этот конкретный экран сохранял свое состояние и данные для плавной анимации ухода.
                    val screenProject = remember(screen, entityId, projects) {
                        if (screen == ActiveScreen.PROJECT_DETAIL) {
                            projects.firstOrNull { it.id == entityId } ?: selectedProject
                        } else null
                    }
                    val screenArea = remember(screen, entityId, areas) {
                        if (screen == ActiveScreen.AREA_DETAIL) {
                            areas.firstOrNull { it.id == entityId } ?: selectedArea
                        } else null
                    }
                    val screenTag = remember(screen, entityId, allSavedTagObjects) {
                        if (screen == ActiveScreen.TAG_DETAIL) {
                            allSavedTagObjects.firstOrNull { it.id == entityId || it.title == entityId } ?: selectedTagDetail
                        } else null
                    }

                    // Поток холодный: подписка живёт ровно столько, сколько отображается экран.
                    // Снимок нужен как начальное значение, пока первая эмиссия считается в фоне.
                    val screenStateFlow = remember(screen, screenProject, screenArea, screenTag) {
                        viewModel.getCategoryListStateFlow(screen, screenProject, screenArea, screenTag)
                    }
                    val initialScreenState = remember(screen, screenProject, screenArea, screenTag) {
                        viewModel.getCategoryListStateSnapshot(screen, screenProject, screenArea, screenTag)
                    }
                    val screenState by screenStateFlow.collectAsState(initial = initialScreenState)

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
                                    viewModel.setInlineExpandedTaskId(event.task.item.id)
                                }
                                is ThingsCategoryListEvent.ClickProject -> {
                                    selectedProject = event.project
                                    navigateTo(ActiveScreen.PROJECT_DETAIL, event.project.id)
                                }
                                is ThingsCategoryListEvent.ClickArea -> {
                                    selectedArea = event.area
                                    navigateTo(ActiveScreen.AREA_DETAIL, event.area.id)
                                }
                                is ThingsCategoryListEvent.ChangeInlineExpandedTaskId -> {
                                    viewModel.setInlineExpandedTaskId(event.taskId)
                                }
                                is ThingsCategoryListEvent.ClickSearch -> {
                                    // На экранах списков окно вырастает из синего круга оттяжки
                                    searchMorphSource = event.sourceBounds
                                    searchWasPulled = true
                                    searchFromWideField = false
                                    isSearchMorphReady = false
                                    searchCapsuleAlpha = 0f
                                    isSearchOverlayActive = true
                                }
                                ThingsCategoryListEvent.ClickBack -> {
                                    val wasSearch = activeScreen == ActiveScreen.SEARCH
                                    navController.popBackStack()
                                    viewModel.selectTag(null)
                                    if (wasSearch) viewModel.setSearchQuery("")
                                }
                                is ThingsCategoryListEvent.ChangeSearchQuery -> {
                                    viewModel.setSearchQuery(event.query)
                                }
                                is ThingsCategoryListEvent.ClickTag -> {
                                    selectedTagDetail = event.tag
                                    navigateTo(ActiveScreen.TAG_DETAIL, event.tag.id)
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
                                is ThingsCategoryListEvent.SaveHeading -> viewModel.saveHeading(event.heading)
                                is ThingsCategoryListEvent.DeleteHeading -> viewModel.deleteHeading(event.heading)
                                is ThingsCategoryListEvent.DiscardHeading -> viewModel.discardHeading(event.heading, event.tasksBack)
                                is ThingsCategoryListEvent.ArchiveHeading -> viewModel.archiveHeading(event.heading)
                                is ThingsCategoryListEvent.ReorderHeadings -> viewModel.reorderHeadings(event.headings)
                                is ThingsCategoryListEvent.CreateTaskAt -> {
                                    viewModel.createTaskAt(event.task, event.reorderedOthers)
                                    viewModel.setInlineExpandedTaskId(event.task.id)
                                }
                                is ThingsCategoryListEvent.InsertHeading -> viewModel.insertHeading(event.headings, event.movedTasks)
                                is ThingsCategoryListEvent.SwipeTaskLeft -> {
                                    viewModel.setInlineExpandedTaskId(null)
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
                                    viewModel.setInlineExpandedTaskId(null)
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
                                    viewModel.batchScheduleTasks(selectedTasks, event.startDate, event.isTonight, event.section)
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
                        listScreenContent(route.screen, route.entityId)
                    }
                }

                composable<SearchRoute> {
                    // Экран поиска — такая же панель списка, как у остальных экранов: те же строки задач,
                    // раскрытие и редактирование, выделение, свайпы
                    ScreenTransitionWrapper(isStartDestination = false, backgroundColor = backgroundColor) {
                        listScreenContent(ActiveScreen.SEARCH, null)
                    }
                }
            }

            // [ИЗМЕНЕНИЕ]: Полноэкранный оверлей поиска с бесшовным пространственным морфингом (Spatial UI)
            if (isSearchOverlayActive) {
                ThingsSearchOverlay(
                    morphSource = searchMorphSource,
                    morphFromWideField = searchFromWideField,
                    wasPulled = searchWasPulled,
                    onMorphReady = { isSearchMorphReady = true },
                    onDismissProgress = { searchCapsuleAlpha = it },
                    currentScreen = activeScreen,
                    currentProject = selectedProject,
                    currentArea = selectedArea,
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
                        val targetEntityId = when {
                            task.item.projectId != null -> proj?.id
                            task.item.areaId != null -> area?.id
                            else -> null
                        }
                        navigateTo(targetScreen, targetEntityId)
                        
                        // [ИЗМЕНЕНИЕ]: Кликнутая задача более не разворачивается для редактирования, а кратковременно подсвечивается
                        viewModel.setHighlightedTaskId(task.item.id)
                        scope.launch {
                            kotlinx.coroutines.delay(1500)
                            // Снимаем подсветку только если за это время не подсветили другую задачу
                            if (viewModel.highlightedTaskId.value == task.item.id) {
                                viewModel.setHighlightedTaskId(null)
                            }
                        }
                        
                        viewModel.setSearchQuery("")
                        isSearchOverlayActive = false
                    },
                    onProjectClick = { proj ->
                        // [ИЗМЕНЕНИЕ]: Добавление проекта в недавно искавшиеся объекты
                        addToRecent(SearchResultItem.ProjectResult(proj))

                        selectedProject = proj
                        navigateTo(ActiveScreen.PROJECT_DETAIL, proj.id)
                        viewModel.setSearchQuery("")
                        isSearchOverlayActive = false
                    },
                    onAreaClick = { area ->
                        // [ИЗМЕНЕНИЕ]: Добавление области в недавно искавшиеся объекты
                        addToRecent(SearchResultItem.AreaResult(area))

                        selectedArea = area
                        navigateTo(ActiveScreen.AREA_DETAIL, area.id)
                        viewModel.setSearchQuery("")
                        isSearchOverlayActive = false
                    },
                    allSavedTagObjects = allSavedTagObjects,
                    onTagClick = { tag ->
                        addToRecent(SearchResultItem.TagResult(tag))
                        selectedTagDetail = tag
                        navigateTo(ActiveScreen.TAG_DETAIL, tag.id)
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
                        searchCapsuleAlpha = 1f
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
                        .background(ThingsTheme.colors.scrim.copy(alpha = 0.4f))
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
                                .background(ThingsTheme.colors.overlaySurface, shape = ThingsTheme.shapes.menuShape)
                                .border(1.dp, ThingsTheme.colors.overlayContent.copy(alpha = 0.08f), ThingsTheme.shapes.menuShape)
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
                                        tint = ThingsTheme.colors.overlayContent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "New To-Do",
                                        style = ThingsTheme.type.menuItem.copy(color = ThingsTheme.colors.overlayContent)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Quickly add a to-do to your inbox.",
                                        style = ThingsTheme.type.bodySmall.copy(
                                            color = ThingsTheme.colors.overlayContentSecondary
                                        )
                                    )
                                }
                            }

                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(0.5.dp)
                                    .background(ThingsTheme.colors.overlayContent.copy(alpha = 0.08f))
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
                                        tint = ThingsTheme.colors.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "New Project",
                                        style = ThingsTheme.type.menuItem.copy(color = ThingsTheme.colors.overlayContent)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Define a goal, then work towards it one to-do at a time.",
                                        style = ThingsTheme.type.bodySmall.copy(
                                            color = ThingsTheme.colors.overlayContentSecondary
                                        )
                                    )
                                }
                            }

                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(0.5.dp)
                                    .background(ThingsTheme.colors.overlayContent.copy(alpha = 0.08f))
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
                                        tint = ThingsTheme.colors.area,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "New Area",
                                        style = ThingsTheme.type.menuItem.copy(color = ThingsTheme.colors.overlayContent)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Group projects and to-dos based on different responsibilities, such as Family or Work.",
                                        style = ThingsTheme.type.bodySmall.copy(
                                            color = ThingsTheme.colors.overlayContentSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Кнопки отмены и «во Входящие», пока тянут кнопку «+»
            FabDragActions(controller = fabDragController)
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
                    Text("Projects group tasks and track completion status with visual progress charts.", color = textSecondaryColor, style = ThingsTheme.type.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newProjectName,
                        onValueChange = { newProjectName = it },
                        label = { Text("Project Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThingsTheme.colors.accent,
                            unfocusedBorderColor = dividerColor,
                            focusedLabelColor = ThingsTheme.colors.accent
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Area of Responsibility:", color = textPrimaryColor, fontWeight = FontWeight.SemiBold, style = ThingsTheme.type.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, dividerColor, ThingsTheme.shapes.badgeShape)
                            .clickable { showAreaDropdownInNewProject = true }
                            .padding(12.dp)
                    ) {
                        val activeAreaName = if (selectedAreaIdForNewProject == null) "Без области" else {
                            areas.firstOrNull { it.id == selectedAreaIdForNewProject }?.title ?: "Без области"
                        }
                        Text(activeAreaName, color = textPrimaryColor)
                        
                        ThingsDropdownMenu(
                            expanded = showAreaDropdownInNewProject,
                            onDismissRequest = { showAreaDropdownInNewProject = false }
                        ) {
                            ThingsMenuItem("Без области", Icons.Default.Block, onClick = {
                                selectedAreaIdForNewProject = null
                                showAreaDropdownInNewProject = false
                            })
                            areas.forEach { area ->
                                ThingsMenuItem(area.title, AppIcons.Area, onClick = {
                                    selectedAreaIdForNewProject = area.id
                                    showAreaDropdownInNewProject = false
                                })
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
                    Text("Create", color = ThingsTheme.colors.accent, fontWeight = FontWeight.Bold)
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
                    Text("Areas (Области) organize related activities like Work, Personal Life, or Health, and do not have deadlines.", color = textSecondaryColor, style = ThingsTheme.type.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newAreaName,
                        onValueChange = { newAreaName = it },
                        label = { Text("Area Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("area_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThingsTheme.colors.accent,
                            unfocusedBorderColor = dividerColor,
                            focusedLabelColor = ThingsTheme.colors.accent
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
                    Text("Create", color = ThingsTheme.colors.accent, fontWeight = FontWeight.Bold)
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

    val scrimColor = ThingsTheme.colors.scrim
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            // Draw a dark semi-transparent dimming veil on top of the screen content when it is exiting/pop-entering
            .drawWithContent {
                drawContent()
                if (dimmingAlpha > 0f) {
                    drawRect(color = scrimColor.copy(alpha = dimmingAlpha))
                }
            }
    ) {
        content()
    }
}

