package com.example.ui.screens.home

import com.example.ui.screens.home.components.RepeatAction
import androidx.navigation.NavDestination.Companion.hasRoute
import com.example.ui.screens.home.components.HomeProjectBatchLayer
import com.example.ui.screens.home.components.ProjectSwipeWhenDialog
import com.example.domain.lists.HomeList
import java.util.UUID
import com.example.ui.screens.home.components.newTaskPlace
import com.example.domain.edits.ProjectEdit
import com.example.domain.edits.MoveTarget
import com.example.ui.viewmodel.ActiveScreen
import com.example.R
import androidx.compose.ui.res.stringResource
import com.example.ui.components.ThingsDropdownMenu
import com.example.ui.components.hideSoftKeyboardNow
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
import androidx.compose.ui.graphics.graphicsLayer
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

private val FAB_SIZE = 56.dp

private const val FAB_SHOW_FADE_MS = 220
private const val FAB_HIDE_FADE_MS = 180
// Переход из Quick Find к найденной задаче — экран проступает под гаснущим окном
private const val SEARCH_RESULT_CROSSFADE_MS = 120


/**
 * ThingsHomeScreen: Now serves purely as the top-level orchestration point. 
 * It retains the global navigation state (active screens), Floating Action Button action 
 * triggers, permission request launchers, dialog manager flags, and coordinate cross-fades 
 * between panels.
 */


@Serializable object HomeRoute
@Serializable object SearchRoute
@Serializable data class ListRoute(val screen: ActiveScreen, val entityId: String? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThingsHomeScreen(viewModel: ThingsViewModel = hiltViewModel()) {
    val tasks by viewModel.filteredTasks.collectAsState()
    val allTasksRaw by viewModel.tasks.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val projectTemplates by viewModel.projectTemplates.collectAsState()
    val areas by viewModel.areas.collectAsState()
    val collapsedAreaIds by viewModel.collapsedAreaIds.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTagFilter by viewModel.selectedTagFilter.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val allSavedTags by viewModel.allSavedTags.collectAsState()
    val allSavedTagObjects by viewModel.allSavedTagObjects.collectAsState()
    
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    // Название задачи, сохранённой без названия, — на языке системы
    val untitledTaskTitle = stringResource(R.string.untitled_task)
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

    LaunchedEffect(navBackStackEntry, projects, projectTemplates, areas, allSavedTagObjects) {
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
                            selectedProject = (projects + projectTemplates).firstOrNull { it.id == listRoute.entityId } ?: selectedProject
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

    // Следующий переход между экранами — простое проявление (см. navigateTo)
    var crossfadeNavigation by remember { mutableStateOf(false) }

    /**
     * @param crossfade переход из Quick Find к найденной задаче: как в Things 3, новый экран просто
     *   проступает под гаснущим окном поиска, без роста из точки
     */
    fun navigateTo(screen: ActiveScreen, entityId: String? = null, crossfade: Boolean = false) {
        // Главный экран — всегда дно стека: к нему возвращаемся, а не открываем второй поверх
        if (screen == ActiveScreen.HOME) {
            navController.popBackStack(HomeRoute, inclusive = false)
            return
        }
        // Тот же экран уже открыт — ничего не делаем (и двойное нажатие не кладёт его в стек дважды).
        // Сверяемся с верхом стека напрямую: он меняется сразу после navigate, а activeScreen — позже
        val top = navController.currentBackStackEntry
        val alreadyOpen = when {
            top == null -> false
            screen == ActiveScreen.SEARCH -> top.destination.hasRoute<SearchRoute>()
            top.destination.hasRoute<ListRoute>() -> top.toRoute<ListRoute>() == ListRoute(screen, entityId)
            else -> false
        }
        if (alreadyOpen) return
        crossfadeNavigation = crossfade
        val route: Any = if (screen == ActiveScreen.SEARCH) SearchRoute else ListRoute(screen, entityId)
        // Без launchSingleTop: он сравнивает только маршрут без аргументов, и все списки (области, проекты,
        // теги) — один ListRoute. Переход из области в проект заменял область в стеке, и «Назад» из проекта
        // вёл сразу на главный экран, минуя область
        navController.navigate(route)
    }
    var taskToEdit by remember { mutableStateOf<ItemWithChecklist?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProjectId by remember { mutableStateOf<String?>(null) }
    var editingAreaId by remember { mutableStateOf<String?>(null) }
    var showFabMenu by remember { mutableStateOf(false) }
    var isSearchOverlayActive by remember { mutableStateOf(false) }
    // Прозрачность окна поиска при уходе к найденной задаче
    val searchOverlayFade = remember { androidx.compose.animation.core.Animatable(1f) }
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
    // Множественный выбор проектов на главном экране (свайп влево по проекту) и «Когда» по свайпу вправо
    var selectedProjectIds by remember { mutableStateOf(emptySet<String>()) }
    val isProjectSelectionMode = selectedProjectIds.isNotEmpty()
    var swipeWhenProject by remember { mutableStateOf<Item?>(null) }
    var swipeWhenProjectOrigin by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
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
    
    val scope = rememberCoroutineScope()
    val rootView = androidx.compose.ui.platform.LocalView.current

    /**
     * Уход из Quick Find к выбранному: как в Things 3, окно поиска гаснет за 100 мс, а под ним уже
     * проступает новый экран. Раньше при выборе проекта, области, тега или списка окно вместе с
     * затемнением исчезало за один кадр — вспыхивал главный экран, и только потом рос новый.
     * Клавиатура уходит сразу, а не после окна.
     * @param keepQuery запрос нужен дальше («Продолжить поиск» открывает с ним экран поиска)
     */
    /**
     * Показать задачу на её экране и подсветить — копию повторяющейся задачи («Предыдущая копия») или шаблон
     * в «Повторяющихся» («Это копия повторяющейся задачи»). Экран — по месту задачи, как в Quick Find.
     */
    fun revealTask(task: ItemWithChecklist) {
        val item = task.item
        val (target, entity) = when {
            // Проект — его экран: копия повторяющегося проекта или шаблон
            item.type == Item.TYPE_PROJECT || item.type == Item.TYPE_PROJECT_TEMPLATE -> ActiveScreen.PROJECT_DETAIL to item.id
            item.type == Item.TYPE_TEMPLATE -> ActiveScreen.REPEATING to null
            item.projectId != null -> ActiveScreen.PROJECT_DETAIL to item.projectId
            item.areaId != null -> ActiveScreen.AREA_DETAIL to item.areaId
            item.isClosed -> ActiveScreen.LOGBOOK to null
            item.isToday -> ActiveScreen.TODAY to null
            item.isUpcoming -> ActiveScreen.UPCOMING to null
            item.isInbox -> ActiveScreen.INBOX to null
            item.isSomeday -> ActiveScreen.SOMEDAY to null
            else -> ActiveScreen.ANYTIME to null
        }
        navigateTo(target, entity)
        viewModel.setHighlightedTaskId(item.id)
        scope.launch {
            kotlinx.coroutines.delay(com.example.ui.screens.home.subcomponents.FOUND_HIGHLIGHT_DELAY_MS + 1500)
            if (viewModel.highlightedTaskId.value == item.id) viewModel.setHighlightedTaskId(null)
        }
    }

    fun leaveSearchOverlay(keepQuery: Boolean = false) {
        rootView.hideSoftKeyboardNow()
        scope.launch {
            searchOverlayFade.animateTo(0f, tween(durationMillis = ThingsMotion.FAST))
            if (!keepQuery) viewModel.setSearchQuery("")
            isSearchOverlayActive = false
            searchOverlayFade.snapTo(1f)
        }
    }
    
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
            // Скрывать FAB при открытии inline-редактора, FAB-меню, диалогов, окна быстрой задачи (QuickAddDialog) или режима мультивыбора.
            // В «Журнале» кнопки нет вовсе: там только закрытое, новые задачи не создают (как в Things).
            // На особых списках проектов она, как на главном экране, открывает меню: задача, проект, область
            AnimatedVisibility(
                visible = activeScreen != ActiveScreen.LOGBOOK && (fabDragController.isDragging || fabDragController.isSettling ||
                    (inlineExpandedTaskId == null && !showFabMenu && !isListDialogActive && !showAddDialog && !isSelectionMode &&
                        !(activeScreen == ActiveScreen.HOME && isProjectSelectionMode))),
                enter = slideInVertically(
                    initialOffsetY = { it * 2 },
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = 240f
                    )
                ) + fadeIn(animationSpec = tween(durationMillis = FAB_SHOW_FADE_MS)),
                exit = slideOutVertically(
                    targetOffsetY = { it * 2 },
                    animationSpec = tween(durationMillis = ThingsMotion.BASE, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = FAB_HIDE_FADE_MS))
            ) {
                val view = androidx.compose.ui.platform.LocalView.current
                val onFabClick: () -> Unit = {
                                                if (activeScreen == ActiveScreen.HOME || activeScreen == ActiveScreen.ALL_PROJECTS ||
                            activeScreen == ActiveScreen.LOGGED_PROJECTS
                        ) {
                            showFabMenu = true
                        } else if (activeScreen == ActiveScreen.SEARCH) {
                            // На экране поиска новая задача создаётся с текстом запроса
                            taskToEdit = null
                            newTaskTitlePrefill = searchQuery
                            showAddDialog = true
                        } else {
                            // Поля новой задачи по месту назначает домен (NewTasks); задача сразу раскрывается,
                            // курсор — в названии
                            val newTaskId = UUID.randomUUID().toString()
                            fabDragController.focusTaskId = newTaskId
                            viewModel.createTask(
                                newTaskPlace(activeScreen, selectedProject?.id, selectedArea?.id, selectedTagDetail?.title),
                                newTaskId
                            )
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
                        .size(FAB_SIZE)
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
                    if (crossfadeNavigation) fadeIn(animationSpec = tween(durationMillis = SEARCH_RESULT_CROSSFADE_MS)) else scaleIn(
                        initialScale = 0f,
                        transformOrigin = TransformOrigin.Center,
                        animationSpec = tween(durationMillis = ThingsMotion.STANDARD, easing = FastOutSlowInEasing)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = ThingsMotion.BASE)
                    )
                },
                exitTransition = {
                    if (crossfadeNavigation) fadeOut(animationSpec = tween(durationMillis = SEARCH_RESULT_CROSSFADE_MS)) else scaleOut(
                        targetScale = 0.9f,
                        transformOrigin = TransformOrigin.Center,
                        animationSpec = tween(durationMillis = ThingsMotion.STANDARD, easing = FastOutSlowInEasing)
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = ThingsMotion.BASE)
                    )
                },
                popEnterTransition = {
                    scaleIn(
                        initialScale = 0.9f,
                        transformOrigin = TransformOrigin.Center,
                        animationSpec = tween(durationMillis = ThingsMotion.STANDARD, easing = FastOutSlowInEasing)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = ThingsMotion.BASE)
                    )
                },
                popExitTransition = {
                    scaleOut(
                        targetScale = 0f,
                        transformOrigin = TransformOrigin.Center,
                        animationSpec = tween(durationMillis = ThingsMotion.STANDARD, easing = CubicBezierEasing(0.4f, 0f, 1f, 1f))
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = ThingsMotion.BASE)
                    )
                }
            ) {
                composable<HomeRoute> {
                    // Применяем обертку ScreenTransitionWrapper со сплошным фоном и эффектом затемнения для уходящего экрана
                    ScreenTransitionWrapper(isStartDestination = true, backgroundColor = backgroundColor) {
                        // Выход из выбора проектов — «Назад»
                        androidx.activity.compose.BackHandler(enabled = isProjectSelectionMode) { selectedProjectIds = emptySet() }
                        Box(modifier = Modifier.fillMaxSize()) {
                        ThingsHomePanel(
                            isProjectSelectionMode = isProjectSelectionMode,
                            selectedProjectIds = selectedProjectIds,
                            onProjectSwipeRight = { project, origin ->
                                swipeWhenProject = project
                                swipeWhenProjectOrigin = origin
                            },
                            onProjectSwipeLeft = { project ->
                                selectedProjectIds = if (project.id in selectedProjectIds) selectedProjectIds - project.id
                                else selectedProjectIds + project.id
                            },
                            onToggleProjectSelection = { project ->
                                selectedProjectIds = if (project.id in selectedProjectIds) selectedProjectIds - project.id
                                else selectedProjectIds + project.id
                            },
                            collapsedAreaIds = collapsedAreaIds,
                            onAreaExpandedChange = viewModel::setAreaExpanded,
                            allTasks = allTasksRaw,
                            // Только проекты, с которыми можно работать сейчас (HomeList): закрытые — в Logbook,
                            // отложенные и запланированные — в «Когда-нибудь» и «Предстоящих»
                            projects = projects.filter { HomeList.showsProject(it) },
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
                            onCreateProjectAt = { id, areaId, index, projects -> viewModel.createProjectAt(id, areaId, index, projects) },
                            onRenameProject = { project, title -> viewModel.editProject(project, ProjectEdit.Rename(title)) },
                            onUpdateArea = { viewModel.updateArea(it) },
                            onProjectsReordered = { viewModel.updateTasks(it) },
                            onAreasReordered = { viewModel.updateAreas(it) }
                        )
                        HomeProjectBatchLayer(
                            isActive = isProjectSelectionMode,
                            selected = projects.filter { it.id in selectedProjectIds },
                            allTasks = allTasksRaw,
                            projects = projects,
                            areas = areas,
                            onEditProjects = { list, edit -> list.forEach { viewModel.editProject(it, edit) } },
                            onDeleteProjects = { list -> list.forEach { viewModel.deleteProject(it) } },
                            onCompleteProjects = { list -> list.forEach { viewModel.completeProject(it) } },
                            onDuplicateProjects = { list -> list.forEach { viewModel.duplicateProject(it) } },
                            onDone = { selectedProjectIds = emptySet() }
                        )
                        swipeWhenProject?.let { project ->
                            ProjectSwipeWhenDialog(
                                project = project,
                                growFromOffset = swipeWhenProjectOrigin,
                                onEditProject = { p, edit -> viewModel.editProject(p, edit) },
                                onDismiss = { swipeWhenProject = null; swipeWhenProjectOrigin = null }
                            )
                        }
                        }
                    }
                }
                // Helper for the category lists
                val listScreenContent: @Composable (ActiveScreen, String?) -> Unit = { screen, entityId ->
                    // Мы запоминаем проект и область именно для данного инстанса экрана на момент его создания/отображения,
                    // чтобы при изменении глобальных selectedProject/selectedArea на других экранах (или при сбросе в null на "Назад")
                    // этот конкретный экран сохранял свое состояние и данные для плавной анимации ухода.
                    // Удалённый проект или область исчезают из списков раньше, чем экран успевает уйти:
                    // экран держит последний найденный объект, иначе на время ухода он оставался без него —
                    // мелькали заголовок «Проект» и пустой список
                    val lastFound = remember(screen, entityId) { arrayOfNulls<Any>(2) }
                    val screenProject = remember(screen, entityId, projects, projectTemplates) {
                        if (screen == ActiveScreen.PROJECT_DETAIL) {
                            // Шаблон повторяющегося проекта — тоже экран проекта, хотя в списке проектов его нет
                            ((projects + projectTemplates).firstOrNull { it.id == entityId } ?: lastFound[0] as Item? ?: selectedProject)
                                ?.also { lastFound[0] = it }
                        } else null
                    }
                    val screenArea = remember(screen, entityId, areas) {
                        if (screen == ActiveScreen.AREA_DETAIL) {
                            (areas.firstOrNull { it.id == entityId } ?: lastFound[1] as Area? ?: selectedArea)
                                ?.also { lastFound[1] = it }
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
                    val liveScreenState by screenStateFlow.collectAsState(initial = initialScreenState)
                    // Проект или область удалены, а экран ещё уходит — показываем последнее состояние,
                    // а не опустевший список
                    val keptState = remember(screen, entityId) { arrayOfNulls<com.example.ui.viewmodel.ThingsCategoryListState>(1) }
                    val entityGone = (screen == ActiveScreen.PROJECT_DETAIL && projects.none { it.id == entityId } && projectTemplates.none { it.id == entityId }) ||
                        (screen == ActiveScreen.AREA_DETAIL && areas.none { it.id == entityId })
                    val screenState = if (entityGone) keptState[0] ?: liveScreenState else liveScreenState.also { keptState[0] = it }

                    // Правила повтора — строкам шаблонов и редактору шаблона и его копий
                    androidx.compose.runtime.CompositionLocalProvider(
                        com.example.ui.components.LocalRepeatRules provides screenState.repeatRules
                    ) {
                    ThingsCategoryListPanel(
                        state = screenState.copy(
                            isSelectionMode = isSelectionMode,
                            selectedTaskIds = selectedTaskIds
                        ),
                        onDialogsActiveChange = { isListDialogActive = it },
                        onEvent = { event ->
                            when (event) {
                                is ThingsCategoryListEvent.SelectTag -> {
                                    viewModel.selectTag(event.tag)
                                }
                                is ThingsCategoryListEvent.ToggleEditedTask ->
                                    viewModel.toggleEditedTask(event.task, event.fields, event.checklist, untitledTaskTitle)
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
                                    viewModel.createTagInGroup(event.title, event.parentId)
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
                                        priority = event.priority,
                                        reminderTime = event.reminderTime,
                                        untitledTitle = untitledTaskTitle
                                    )
                                }
                                is ThingsCategoryListEvent.DeleteTask -> {
                                    viewModel.deleteTask(event.taskWrapper)
                                }
                                is ThingsCategoryListEvent.ToggleTaskCancel -> viewModel.toggleTaskCancel(event.taskId)
                                is ThingsCategoryListEvent.MakeRepeating -> viewModel.makeRepeating(event.taskId, event.rule, event.reminderTime)
                                is ThingsCategoryListEvent.UpdateRepeatRule -> viewModel.updateRepeatRule(event.templateId, event.rule, event.reminderTime)
                                // Перенос из редактора панель уже превратила в MoveRepeat (после выбора)
                                is ThingsCategoryListEvent.RequestRepeatMove -> Unit
                                is ThingsCategoryListEvent.MoveRepeat -> viewModel.moveRepeat(event.templateId, event.appearOn, event.updateRule)
                                is ThingsCategoryListEvent.BatchRepeatAction -> event.templateIds.forEach { id ->
                                    when (event.action) {
                                        RepeatAction.PAUSE -> viewModel.pauseRepeat(id)
                                        RepeatAction.RESUME -> viewModel.resumeRepeat(id)
                                        RepeatAction.STOP -> viewModel.stopRepeat(id)
                                        else -> Unit
                                    }
                                }
                                is ThingsCategoryListEvent.OpenRepeatTemplate -> {
                                    viewModel.setInlineExpandedTaskId(null)
                                    allTasksRaw.firstOrNull { it.item.id == event.templateId }?.let { revealTask(it) }
                                }
                                is ThingsCategoryListEvent.RepeatTemplateAction -> when (event.action) {
                                    RepeatAction.NEXT_COPY -> viewModel.repeatNextCopy(event.templateId)
                                    RepeatAction.COMPLETE_EARLY -> viewModel.completeRepeatEarly(event.templateId)
                                    RepeatAction.PAUSE -> viewModel.pauseRepeat(event.templateId)
                                    RepeatAction.RESUME -> viewModel.resumeRepeat(event.templateId)
                                    RepeatAction.STOP -> viewModel.stopRepeat(event.templateId)
                                    // Предыдущая копия — самая поздняя из созданных
                                    RepeatAction.PREVIOUS_COPY -> {
                                        val previous = allTasksRaw
                                            .filter { it.item.repeatTemplateId == event.templateId && !it.item.isTemplatePart && !it.item.trashed }
                                            .maxByOrNull { it.item.startDate ?: it.item.creationDate }
                                        if (previous != null) {
                                            viewModel.setInlineExpandedTaskId(null)
                                            revealTask(previous)
                                        }
                                    }
                                    RepeatAction.EDIT -> Unit
                                }
                                is ThingsCategoryListEvent.DuplicateTask -> {
                                    viewModel.duplicateTask(event.taskWrapper)
                                }
                                is ThingsCategoryListEvent.MoveTaskToNewProject ->
                                    viewModel.moveTasksToNewProject(listOf(event.taskWrapper), event.title)
                                is ThingsCategoryListEvent.MoveTask -> viewModel.moveTask(
                                    event.taskWrapper,
                                    if (event.moveToInbox) MoveTarget.Inbox else MoveTarget.Place(event.projectId, event.areaId, event.headingId)
                                )
                                is ThingsCategoryListEvent.ReorderTasks -> {
                                    viewModel.updateTasks(event.items)
                                }
                                is ThingsCategoryListEvent.DeleteProject -> {
                                    viewModel.deleteProject(event.project)
                                    navController.popBackStack()
                                    selectedProject = null
                                }
                                is ThingsCategoryListEvent.RenameArea -> viewModel.updateArea(event.area.copy(title = event.title))
                                is ThingsCategoryListEvent.EditProject -> viewModel.editProject(event.project, event.edit)
                                is ThingsCategoryListEvent.CompleteProject -> viewModel.completeProject(event.project)
                                is ThingsCategoryListEvent.DuplicateProject -> viewModel.duplicateProject(event.project)
                                is ThingsCategoryListEvent.DeleteArea -> {
                                    viewModel.deleteArea(event.area)
                                    navController.popBackStack()
                                    selectedArea = null
                                }
                                is ThingsCategoryListEvent.SaveHeading -> viewModel.saveHeading(event.heading)
                                is ThingsCategoryListEvent.DeleteHeading -> viewModel.deleteHeading(event.heading)
                                is ThingsCategoryListEvent.DiscardHeading -> viewModel.discardHeading(event.heading, event.tasks, event.groupAboveId)
                                is ThingsCategoryListEvent.ArchiveHeading -> viewModel.archiveHeading(event.heading)
                                is ThingsCategoryListEvent.ReorderHeadings -> viewModel.reorderHeadings(event.headings)
                                is ThingsCategoryListEvent.CreateTaskAt ->
                                    viewModel.createTaskAt(event.taskId, event.place, event.spot, event.insertIndex, event.listTasks)
                                is ThingsCategoryListEvent.InsertHeadingAt ->
                                    viewModel.insertHeadingAt(event.headingId, event.projectId, event.headings, event.index, event.movedTasks)
                                is ThingsCategoryListEvent.AddHeading -> viewModel.addHeading(event.headingId, event.projectId, event.headings)
                                is ThingsCategoryListEvent.RenameHeading -> viewModel.renameHeading(event.heading, event.title)
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
                                is ThingsCategoryListEvent.BatchMoveTasksToNewProject -> {
                                    val selectedTasks = screenState.allTasks.filter { selectedTaskIds.contains(it.item.id) }
                                    viewModel.moveTasksToNewProject(selectedTasks, event.title)
                                    isSelectionMode = false
                                    selectedTaskIds = emptySet()
                                }
                                is ThingsCategoryListEvent.BatchMoveTasks -> {
                                    val selectedTasks = screenState.allTasks.filter { selectedTaskIds.contains(it.item.id) }
                                    viewModel.batchMoveTasks(selectedTasks, event.projectId, event.areaId, event.moveToInbox, event.headingId)
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

            // «Назад» сначала закрывает то, что открыто поверх экрана, и только потом уводит с экрана.
            // Обработчики объявлены после NavHost, поэтому срабатывают раньше его перехода назад
            // Раскрытая задача — сворачивается (правки сохраняет сам редактор при сворачивании)
            androidx.activity.compose.BackHandler(enabled = inlineExpandedTaskId != null) {
                viewModel.setInlineExpandedTaskId(null)
            }
            // Меню кнопки «+» — закрывается, а не закрывает приложение
            androidx.activity.compose.BackHandler(enabled = showFabMenu) { showFabMenu = false }

            // [ИЗМЕНЕНИЕ]: Полноэкранный оверлей поиска с бесшовным пространственным морфингом (Spatial UI)
            // Названия смарт-списков для Recent — на языке системы
            val smartListTitles = mapOf(
                ActiveScreen.TODAY to stringResource(R.string.category_today),
                ActiveScreen.INBOX to stringResource(R.string.category_inbox),
                ActiveScreen.UPCOMING to stringResource(R.string.category_upcoming),
                ActiveScreen.ANYTIME to stringResource(R.string.category_anytime),
                ActiveScreen.SOMEDAY to stringResource(R.string.category_someday),
                ActiveScreen.LOGBOOK to stringResource(R.string.category_logbook),
                ActiveScreen.ALL_PROJECTS to stringResource(R.string.special_all_projects),
                ActiveScreen.LOGGED_PROJECTS to stringResource(R.string.special_logged_projects),
                ActiveScreen.REPEATING to stringResource(R.string.special_repeating)
            )
            if (isSearchOverlayActive) {
                Box(modifier = Modifier.graphicsLayer { alpha = searchOverlayFade.value }) {
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
                            // Повторяющаяся задача (шаблон) живёт в «Повторяющихся» — даже если у неё есть проект или область
                            task.item.isRepeatTemplate -> ActiveScreen.REPEATING
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
                            task.item.isRepeatTemplate -> null
                            task.item.projectId != null -> proj?.id
                            task.item.areaId != null -> area?.id
                            else -> null
                        }
                        navigateTo(targetScreen, targetEntityId, crossfade = true)
                        
                        // [ИЗМЕНЕНИЕ]: Кликнутая задача более не разворачивается для редактирования, а кратковременно подсвечивается
                        viewModel.setHighlightedTaskId(task.item.id)
                        scope.launch {
                            // Подсветка начинается после паузы — время считается от её начала
                            kotlinx.coroutines.delay(com.example.ui.screens.home.subcomponents.FOUND_HIGHLIGHT_DELAY_MS + 1500)
                            // Снимаем подсветку только если за это время не подсветили другую задачу
                            if (viewModel.highlightedTaskId.value == task.item.id) {
                                viewModel.setHighlightedTaskId(null)
                            }
                        }
                        
                        leaveSearchOverlay()
                    },
                    onProjectClick = { proj ->
                        // [ИЗМЕНЕНИЕ]: Добавление проекта в недавно искавшиеся объекты
                        addToRecent(SearchResultItem.ProjectResult(proj))

                        selectedProject = proj
                        navigateTo(ActiveScreen.PROJECT_DETAIL, proj.id, crossfade = true)
                        leaveSearchOverlay()
                    },
                    onAreaClick = { area ->
                        // [ИЗМЕНЕНИЕ]: Добавление области в недавно искавшиеся объекты
                        addToRecent(SearchResultItem.AreaResult(area))

                        selectedArea = area
                        navigateTo(ActiveScreen.AREA_DETAIL, area.id, crossfade = true)
                        leaveSearchOverlay()
                    },
                    allSavedTagObjects = allSavedTagObjects,
                    onTagClick = { tag ->
                        addToRecent(SearchResultItem.TagResult(tag))
                        selectedTagDetail = tag
                        navigateTo(ActiveScreen.TAG_DETAIL, tag.id, crossfade = true)
                        leaveSearchOverlay()
                    },
                    onSmartListClick = { smartScreen ->
                        // [ИЗМЕНЕНИЕ]: Добавление смарт-списка в недавно искавшиеся объекты
                        val title = smartListTitles[smartScreen] ?: smartScreen.name
                        addToRecent(SearchResultItem.SmartListResult(title, smartScreen))

                        navigateTo(smartScreen, crossfade = true)
                        leaveSearchOverlay()
                    },
                    onClose = {
                        isSearchOverlayActive = false
                        searchCapsuleAlpha = 1f
                        viewModel.setSearchQuery("")
                    },
                    // [ИЗМЕНЕНИЕ]: Передача списка недавно найденных/искавшихся объектов
                    recentSearchItems = recentSearchItems,
                    onContinueSearchClick = {
                        // Фильтр по тегу общий для списков: в поиске он молча отсекал бы результаты
                        viewModel.selectTag(null)
                        navigateTo(ActiveScreen.SEARCH, crossfade = true)
                        leaveSearchOverlay(keepQuery = true)
                    }
                )
                }
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
                        .background(ThingsTheme.colors.scrim.copy(alpha = ThingsAlpha.MUTED))
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
                            .padding(bottom = ThingsSpacing.XXL, start = ThingsSpacing.L, end = ThingsSpacing.L)
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ThingsTheme.colors.overlaySurface, shape = ThingsTheme.shapes.menuShape)
                                .border(ThingsStroke.THIN, ThingsTheme.colors.overlayContent.copy(alpha = ThingsAlpha.FAINT), ThingsTheme.shapes.menuShape)
                                .padding(vertical = ThingsSpacing.XS)
                        ) {
                            // 1. New To-Do
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showFabMenu = false
                                        showAddDialog = true
                                    }
                                    .padding(ThingsSpacing.L),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(ThingsIconSize.XL)
                                        .padding(top = ThingsSpacing.XXS),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = ThingsTheme.colors.overlayContent,
                                        modifier = Modifier.size(ThingsIconSize.M)
                                    )
                                }
                                Spacer(modifier = Modifier.width(ThingsSpacing.L))
                                Column {
                                    Text(
                                        text = stringResource(R.string.ui_new_to_do),
                                        style = ThingsTheme.type.menuItem.copy(color = ThingsTheme.colors.overlayContent)
                                    )
                                    Spacer(modifier = Modifier.height(ThingsSpacing.XXS))
                                    Text(
                                        text = stringResource(R.string.ui_quickly_add_a_to_do_to),
                                        style = ThingsTheme.type.bodySmall.copy(
                                            color = ThingsTheme.colors.overlayContentSecondary
                                        )
                                    )
                                }
                            }

                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = ThingsSpacing.L)
                                    .height(ThingsStroke.HAIRLINE)
                                    .background(ThingsTheme.colors.overlayContent.copy(alpha = ThingsAlpha.FAINT))
                            )

                            // 2. New Project
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showFabMenu = false
                                        // Проект и область создаются и называются на главном экране — с особого
                                        // списка сначала возвращаемся туда
                                        if (activeScreen != ActiveScreen.HOME) navController.popBackStack(HomeRoute, inclusive = false)
                                        val newProjectId = java.util.UUID.randomUUID().toString()
                                        viewModel.createProject(newProjectId)
                                        editingProjectId = newProjectId
                                    }
                                    .padding(ThingsSpacing.L),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(ThingsIconSize.XL)
                                        .padding(top = ThingsSpacing.XXS),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = ThingsTheme.colors.accent,
                                        modifier = Modifier.size(ThingsIconSize.M)
                                    )
                                }
                                Spacer(modifier = Modifier.width(ThingsSpacing.L))
                                Column {
                                    Text(
                                        text = stringResource(R.string.ui_new_project),
                                        style = ThingsTheme.type.menuItem.copy(color = ThingsTheme.colors.overlayContent)
                                    )
                                    Spacer(modifier = Modifier.height(ThingsSpacing.XXS))
                                    Text(
                                        text = stringResource(R.string.ui_define_a_goal_then_work_towards),
                                        style = ThingsTheme.type.bodySmall.copy(
                                            color = ThingsTheme.colors.overlayContentSecondary
                                        )
                                    )
                                }
                            }

                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = ThingsSpacing.L)
                                    .height(ThingsStroke.HAIRLINE)
                                    .background(ThingsTheme.colors.overlayContent.copy(alpha = ThingsAlpha.FAINT))
                            )

                            // 3. New Area
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showFabMenu = false
                                        if (activeScreen != ActiveScreen.HOME) navController.popBackStack(HomeRoute, inclusive = false)
                                        val newAreaId = java.util.UUID.randomUUID().toString()
                                        val newArea = Area(
                                            id = newAreaId,
                                            title = ""
                                        )
                                        viewModel.updateArea(newArea)
                                        editingAreaId = newAreaId
                                    }
                                    .padding(ThingsSpacing.L),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(ThingsIconSize.XL)
                                        .padding(top = ThingsSpacing.XXS),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = ThingsTheme.colors.area,
                                        modifier = Modifier.size(ThingsIconSize.M)
                                    )
                                }
                                Spacer(modifier = Modifier.width(ThingsSpacing.L))
                                Column {
                                    Text(
                                        text = stringResource(R.string.ui_new_area),
                                        style = ThingsTheme.type.menuItem.copy(color = ThingsTheme.colors.overlayContent)
                                    )
                                    Spacer(modifier = Modifier.height(ThingsSpacing.XXS))
                                    Text(
                                        text = stringResource(R.string.ui_group_projects_and_to_dos_based),
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
            onSave = { title, notes, section, isTonight, startDate, dueDate, tags, projectId, checklistItems, priority, reminderTime ->
                viewModel.addTask(
                    title = title,
                    notes = notes,
                    section = section,
                    isTonight = isTonight,
                    startDate = startDate,
                    // Срок из окна быстрого добавления раньше терялся: он не передавался при создании задачи
                    dueDate = dueDate,
                    reminderTime = reminderTime,
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
            tween(durationMillis = ThingsMotion.STANDARD, easing = FastOutSlowInEasing)
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

