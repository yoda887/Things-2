package com.example.ui.screens.home.components

import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.layout
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.TaskSection
import com.example.data.model.Area
import com.example.data.model.Tag
import com.example.ui.components.ProjectProgressArc
import com.example.ui.components.dragdrop.rememberGenericDragDropState
import com.example.ui.components.dragdrop.draggedTaskId
import com.example.ui.screens.home.ActiveScreen
import com.example.ui.screens.home.subcomponents.TaskItemRow
import com.example.ui.screens.home.inlineeditor.ThingsTaskInlineEditor
import com.example.ui.screens.home.inlineeditor.dialogs.ThingsMoveDialog
import com.example.ui.screens.home.inlineeditor.dialogs.DeleteConfirmDialog
import com.example.ui.theme.*
import com.example.ui.theme.ThingsBackgroundDark
import com.example.ui.theme.ThingsBackgroundLight
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.data.model.ChecklistItem
import java.util.Calendar
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat

// Извлеченные UI подкомпоненты
import com.example.ui.screens.home.subcomponents.MainCategoryHeader
import com.example.ui.screens.home.subcomponents.SubCategoryHeader
import com.example.ui.screens.home.subcomponents.UpcomingDateHeader
import com.example.ui.screens.home.subcomponents.TagFilterRow
import com.example.ui.screens.home.subcomponents.ProjectItemRow
import com.example.ui.screens.home.subcomponents.EmptyStateView
import com.example.ui.screens.home.subcomponents.CategoryListTopAppBar
import com.example.ui.screens.home.subcomponents.AnimatedTaskItem

private val TOP_APP_BAR_HEIGHT = 56.dp
private const val DELETE_ANIMATION_DELAY_MS = 300L

/**
 * Основная панель отображения списка задач по выбранной категории.
 * Комбинирует поддиалоги, скролл, свайпы и встроенное редактирование.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThingsCategoryListPanel(
    state: ThingsCategoryListState,
    onEvent: (ThingsCategoryListEvent) -> Unit
) {
    val screen = state.screen
    val project = state.project
    val area = state.area
    val inlineExpandedTaskId = state.inlineExpandedTaskId
    val selectedTag = state.selectedTagFilter
    val allTags = state.allTags
    val textPrimaryColor = state.textPrimaryColor
    val textSecondaryColor = state.textSecondaryColor
    val dividerColor = state.dividerColor
    val highlightedTaskId = state.highlightedTaskId
    val calendarEvents = state.calendarEvents
    val allSavedTags = state.allSavedTags
    val allSavedTagObjects = state.allSavedTagObjects
    val projects = state.projects
    val areasState = state.areas

    val configuration = LocalConfiguration.current
    val isLargeScreen = configuration.screenWidthDp >= 600
    val scaleFactor = if (isLargeScreen) 1.25f else 1.0f

    val lazyListState = rememberLazyListState()
    // Используем новое универсальное состояние жестов перетаскивания вместо старого TaskDragDropState
    val dragDropState = rememberGenericDragDropState(lazyListState)
    
    var localTasksList by remember(state.displayTasks) { mutableStateOf(state.displayTasks) }

    val focusManager = LocalFocusManager.current

    var showMoveDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isWhenDialogOpen by remember { mutableStateOf(false) }
    val deletedTaskIds = remember { mutableStateListOf<String>() }
    
    val activeTask = remember(inlineExpandedTaskId, state.displayTasks) {
        state.displayTasks.find { it.item.id == inlineExpandedTaskId }
    }

    val todayCalendarEvents = remember(calendarEvents) {
        calendarEvents.filter { event ->
            val start = event.eventStartMillis
            start != null && com.example.ui.screens.home.inlineeditor.utils.isTodayDate(start)
        }
    }

    // Извлечение состояния предстоящих дней (UpcomingDays)
    val upcomingDays = rememberUpcomingDays(localTasksList, calendarEvents)

    val standardToday = remember(localTasksList) { localTasksList.filter { !it.item.isTonight } }
    val eveningToday = remember(localTasksList) { localTasksList.filter { it.item.isTonight } }

    val displayTasks = localTasksList
    
    // Извлечение выравнивания плоского списка для LazyColumn
    val flattened = rememberFlattenedList(
        screen = screen,
        standardToday = standardToday,
        eveningToday = eveningToday,
        draggedTaskId = dragDropState.draggedTaskId,
        upcomingDays = upcomingDays,
        projects = projects,
        area = area,
        displayTasks = displayTasks
    )

    val density = androidx.compose.ui.platform.LocalDensity.current
    val itemHeightPx = with(density) { MaterialTheme.dimens.taskItemEstimatedHeight.roundToPx() }
    
    LaunchedEffect(highlightedTaskId) {
        if (highlightedTaskId != null) {
            kotlinx.coroutines.delay(50)
            val innerIndex = flattened.indexOfFirst { item ->
                when (item) {
                    is ItemWithChecklist -> item.item.id == highlightedTaskId
                    is Item -> item.id == highlightedTaskId
                    else -> false
                }
            }
            if (innerIndex != -1) {
                val headerOffset = run {
                    var count = 1 // main_header
                    if (screen == ActiveScreen.TODAY && todayCalendarEvents.isNotEmpty()) {
                        count += 1
                    }
                    if (allTags.isNotEmpty()) {
                        count += 1
                    }
                    count
                }
                val targetIndex = headerOffset + innerIndex
                val viewportHeight = lazyListState.layoutInfo.viewportSize.height.let { 
                    if (it > 0) it else with(density) { configuration.screenHeightDp.dp.roundToPx() }
                }
                val offset = - (viewportHeight / 2 - itemHeightPx / 2)
                lazyListState.scrollToItem(targetIndex, offset)
            }
        }
    }

    val anyExpanded = inlineExpandedTaskId != null
    val globalDimAlpha by animateFloatAsState(targetValue = if (anyExpanded) 0.3f else 1f, label = "globalDim")

    var isTransitionActive by remember { mutableStateOf(false) }

    LaunchedEffect(inlineExpandedTaskId) {
        isTransitionActive = true
        kotlinx.coroutines.delay(com.example.ui.theme.AnimationConstants.TASK_EXPANSION_DURATION_MS)
        isTransitionActive = false
    }

    val placementSpec: androidx.compose.animation.core.FiniteAnimationSpec<androidx.compose.ui.unit.IntOffset>? = if (isTransitionActive) {
        null
    } else {
        spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val pullOffset = remember { Animatable(0f) }
    val thresholdPx = with(density) { 100.dp.toPx() }
    val maxOffsetPx = with(density) { 150.dp.toPx() }

    // Извлечение nestedScrollConnection "PullToSearch" в CategoryListState
    val nestedScrollConnection = rememberPullToSearchConnection(
        coroutineScope = coroutineScope,
        pullOffset = pullOffset,
        lazyListState = lazyListState,
        thresholdPx = thresholdPx,
        maxOffsetPx = maxOffsetPx,
        onSearchClick = { onEvent(ThingsCategoryListEvent.ClickSearch) }
    )

    // [ИЗМЕНЕНИЕ]: Вычисляем порог прокрутки списка (56.dp) для активации анимации заголовков и elevation на AppBar
    val scrollThresholdPx = with(density) { TOP_APP_BAR_HEIGHT.toPx() }
    val isScrolledPastHeader by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 ||
            lazyListState.firstVisibleItemScrollOffset > scrollThresholdPx
        }
    }

    val isDark = textPrimaryColor == ThingsTextPrimaryDark
    val topPaddingTotal = TOP_APP_BAR_HEIGHT

    val bkgColor by animateColorAsState(
        targetValue = if (anyExpanded) {
            if (isDark) Color(0xFF151618) else Color(0xFFF4F4F6)
        } else {
            if (isDark) ThingsBackgroundDark else ThingsBackgroundLight
        },
        label = "backgroundColor"
    )

    Box(modifier = Modifier.fillMaxSize().background(bkgColor)) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .graphicsLayer { translationY = pullOffset.value }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                        focusManager.clearFocus()
                    })
                }
                // [ИЗМЕНЕНИЕ]: Уменьшено расстояние от левой и правой стороны экрана до списков с 20.dp до 14.dp
                .padding(horizontal = 14.dp)
                .testTag("tasks_lazy_list"),
            contentPadding = PaddingValues(top = topPaddingTotal, bottom = 100.dp)
        ) {
            item(key = "main_header") {
                // Извлеченный подкомпонент заголовка
                // [ИЗМЕНЕНИЕ]: Передаем state.allTasks вместо state.displayTasks, чтобы степень выполнения проекта рассчитывалась корректно с учетом завершенных задач
                MainCategoryHeader(
                    screen = screen,
                    project = project,
                    tasks = state.allTasks,
                    area = area,
                    scaleFactor = scaleFactor,
                    textPrimaryColor = textPrimaryColor,
                    globalDimAlpha = globalDimAlpha
                )
            }

            if (screen == ActiveScreen.TODAY && todayCalendarEvents.isNotEmpty()) {
                item {
                    CalendarEventsWidget(
                        events = todayCalendarEvents,
                        textSecondaryColor = textSecondaryColor,
                        isDark = false
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.calendarBetweenSectionSpacing))
                }
            }

            if (allTags.isNotEmpty()) {
                item {
                    // Извлеченный подкомпонент строки тегов
                    TagFilterRow(
                        allTags = allTags,
                        selectedTag = selectedTag,
                        textSecondaryColor = textSecondaryColor,
                        dividerColor = dividerColor,
                        onTagSelect = { onEvent(ThingsCategoryListEvent.SelectTag(it)) }
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.tagsBetweenSectionSpacing))
                }
            }

            val hasTasks = when (screen) {
                ActiveScreen.TODAY -> standardToday.isNotEmpty() || eveningToday.isNotEmpty() || dragDropState.draggedTaskId != null
                ActiveScreen.UPCOMING -> upcomingDays.isNotEmpty()
                ActiveScreen.AREA_DETAIL -> {
                    val areaProjCount = projects.count { it.areaId == area?.id }
                    val areaTasksCount = displayTasks.count { it.item.areaId == area?.id && (it.item.projectId == null || it.item.projectId == "") }
                    areaProjCount > 0 || areaTasksCount > 0
                }
                else -> displayTasks.isNotEmpty()
            }

            if (!hasTasks) {
                item {
                    EmptyStateView(textSecondaryColor = textSecondaryColor)
                }
            } else {
                items(flattened, key = { item ->
                    when (item) {
                        is ItemWithChecklist -> item.item.id
                        is Item -> item.id
                        is UpcomingHeaderItem -> "hdr_${item.dateMillis}"
                        is UpcomingEventItem -> "ev_${item.event.id}_${item.dateMillis}"
                        else -> item.toString()
                    }
                }) { item ->
                    when (item) {
                        is ItemWithChecklist -> {
                            AnimatedTaskItem(
                                taskWrapper = item,
                                dragDropState = dragDropState,
                                inlineExpandedTaskId = inlineExpandedTaskId,
                                textPrimaryColor = textPrimaryColor,
                                textSecondaryColor = textSecondaryColor,
                                dividerColor = dividerColor,
                                highlightedTaskId = highlightedTaskId,
                                projects = projects,
                                areas = areasState,
                                allSavedTags = allSavedTags,
                                allSavedTagObjects = allSavedTagObjects,
                                deletedTaskIds = deletedTaskIds,
                                onDeletedTaskIdAdd = { deletedTaskIds.add(it) },
                                onEvent = onEvent,
                                coroutineScope = coroutineScope,
                                screen = screen,
                                upcomingDays = upcomingDays,
                                localTasksList = localTasksList,
                                displayTasks = state.displayTasks,
                                onLocalTasksListChange = { localTasksList = it },
                                lazyListState = lazyListState,
                                onWhenDialogVisibilityChange = { isWhenDialogOpen = it },
                                modifier = if (dragDropState.draggedTaskId == item.item.id) {
                                    Modifier
                                } else {
                                    Modifier.animateItem(
                                        placementSpec = placementSpec
                                    )
                                }
                            )
                        }
                        is Item -> {
                            val task = item
                            if (task.type == 1) {
                                // Извлеченный подкомпонент строки проекта
                                ProjectItemRow(
                                    project = task,
                                    tasks = state.displayTasks,
                                    textPrimaryColor = textPrimaryColor,
                                    inlineExpandedTaskId = inlineExpandedTaskId,
                                    onProjectClick = { onEvent(ThingsCategoryListEvent.ClickProject(it)) },
                                    modifier = Modifier.animateItem(
                                        placementSpec = placementSpec
                                    )
                                )
                            }
                        }
                        is UpcomingHeaderItem -> {
                            val header = item
                            val shouldDim = inlineExpandedTaskId != null
                            val dimAlpha by animateFloatAsState(
                                targetValue = if (shouldDim) 0.3f else 1f,
                                label = "dimAlpha_hdr_${header.dateMillis}"
                            )
                            // Извлеченный подкомпонент заголовка предстоящей даты
                            UpcomingDateHeader(
                                dayOfMonth = header.dayOfMonth,
                                dayOfWeekLabel = header.dayOfWeekLabel,
                                textPrimaryColor = textPrimaryColor,
                                textSecondaryColor = textSecondaryColor,
                                dividerColor = dividerColor,
                                modifier = Modifier
                                    .animateItem(
                                        placementSpec = placementSpec
                                    )
                                    .graphicsLayer { alpha = dimAlpha }
                            )
                        }
                        is UpcomingEventItem -> {
                            val event = item.event
                            val shouldDim = inlineExpandedTaskId != null
                            val dimAlpha by animateFloatAsState(
                                targetValue = if (shouldDim) 0.3f else 1f,
                                label = "dimAlpha_ev_${event.id}"
                            )
                            Column(
                                modifier = Modifier
                                    .animateItem(
                                        placementSpec = placementSpec
                                    )
                                    .graphicsLayer { alpha = dimAlpha }
                            ) {
                                UpcomingCalendarEventRow(
                                    event = event,
                                    textSecondaryColor = textSecondaryColor,
                                    textPrimaryColor = textPrimaryColor
                                )
                            }
                        }
                        else -> { // like "evening_header", "projects_heading", "tasks_heading"
                            val headerText = item as String
                            val shouldDim = inlineExpandedTaskId != null
                            val dimAlpha by animateFloatAsState(
                                targetValue = if (shouldDim) 0.3f else 1f,
                                label = "dimAlpha_$headerText"
                            )
                            // Извлеченные подзаголовки разделов в CategoryListPanel
                            SubCategoryHeader(
                                headerText = headerText,
                                textPrimaryColor = textPrimaryColor,
                                textSecondaryColor = textSecondaryColor,
                                dividerColor = dividerColor,
                                dimAlpha = dimAlpha,
                                modifier = Modifier.animateItem(
                                    placementSpec = placementSpec
                                )
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }

        // TopAppBar
        // [ИЗМЕНЕНИЕ]: Передаем в AppBar дополнительные параметры (состояние скролла, экран, проект, область ответственности и список задач) для вывода иконки и полужирного заголовка
        CategoryListTopAppBar(
            isDark = isDark,
            textPrimaryColor = textPrimaryColor,
            textSecondaryColor = textSecondaryColor,
            onBackClick = { onEvent(ThingsCategoryListEvent.ClickBack) },
            screen = screen,
            project = project,
            area = area,
            tasks = state.allTasks,
            isScrolled = isScrolledPastHeader
        )

    // PullToSearchIndicator
    if (pullOffset.value > 0f) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { pullOffset.value.toDp() } + 96.dp)
                .offset(y = (-96).dp),
            contentAlignment = Alignment.TopCenter
        ) {
            com.example.ui.components.PullToSearchIndicator(
                pullOffset = pullOffset.value,
                thresholdPx = thresholdPx
            )
        }
    }

    // Dialogs & Capsule Toolbar
    if (showMoveDialog && activeTask != null) {
        ThingsMoveDialog(
            currentProjectId = activeTask.item.projectId,
            currentAreaId = activeTask.item.areaId,
            currentIsInbox = activeTask.item.isInbox,
            projects = projects,
            areas = areasState,
            allTasks = state.allTasks,
            onMove = { projectId, areaId, moveToInbox ->
                onEvent(ThingsCategoryListEvent.MoveTask(activeTask, projectId, areaId, moveToInbox))
                showMoveDialog = false
            },
            onDismissRequest = { showMoveDialog = false }
        )
    }

    if (showDeleteConfirm && activeTask != null) {
        DeleteConfirmDialog(
            onDismissRequest = { showDeleteConfirm = false },
            onConfirmDelete = {
                deletedTaskIds.add(activeTask.item.id)
                onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                showDeleteConfirm = false
                coroutineScope.launch {
                    delay(DELETE_ANIMATION_DELAY_MS)
                    onEvent(ThingsCategoryListEvent.DeleteTask(activeTask))
                }
            }
        )
    }

    FloatingBottomCapsuleToolbar(
        visible = inlineExpandedTaskId != null && activeTask != null && !isWhenDialogOpen,
        onMoveClick = { showMoveDialog = true },
        onDeleteClick = { showDeleteConfirm = true },
        onDuplicateClick = {
            if (activeTask != null) {
                onEvent(ThingsCategoryListEvent.DuplicateTask(activeTask))
            }
        },
        modifier = Modifier.align(Alignment.BottomCenter)
    )
  }
}
