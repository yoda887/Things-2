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
import com.example.ui.screens.home.ActiveScreen
import com.example.ui.screens.home.subcomponents.TaskItemRow
import com.example.ui.screens.home.inlineeditor.ThingsTaskInlineEditor
import com.example.ui.screens.home.inlineeditor.dialogs.ThingsMoveDialog
import com.example.ui.screens.home.inlineeditor.dialogs.DeleteConfirmDialog
import com.example.ui.screens.home.inlineeditor.dialogs.ThingsWhenDialog
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
import com.example.ui.screens.home.subcomponents.BatchActionToolbar
import com.example.ui.screens.home.inlineeditor.DeadlineDatePickerDialog
import com.example.ui.screens.home.inlineeditor.dialogs.ThingsTagDialog
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext

private val TOP_APP_BAR_HEIGHT = 56.dp
private const val DELETE_ANIMATION_DELAY_MS = 300L

/**
 * Аккумулятор для отслеживания ручного скролла пользователя (Scroll_B)
 * без перекомпозиции на каждый кадр.
 */
private class FloatAccumulator(var value: Float = 0f)

/**
 * Основная панель отображения списка задач по выбранной категории.
 * Комбинирует поддиалоги, скролл, свайпы и встроенное редактирование.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThingsCategoryListPanel(
    state: ThingsCategoryListState,
    onEvent: (ThingsCategoryListEvent) -> Unit,
    onDialogsActiveChange: (Boolean) -> Unit = {}
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
    
    var localTasksList by remember { mutableStateOf(state.displayTasks) }
    val currentDisplayTasks by rememberUpdatedState(state.displayTasks)
    // Флаг: идёт ли анимация сворачивания редактора (300 мс)
    var isEditorCollapsing by remember { mutableStateOf(false) }
    // Предыдущий ID развёрнутой задачи для обнаружения момента сворачивания
    var previousExpandedId by remember { mutableStateOf(inlineExpandedTaskId) }

    // Обнаружение момента сворачивания редактора и задержка удаления задачи из списка
    LaunchedEffect(inlineExpandedTaskId) {
        val wasExpanded = previousExpandedId != null
        previousExpandedId = inlineExpandedTaskId
        if (wasExpanded && inlineExpandedTaskId == null) {
            // Редактор только что начал сворачиваться — блокируем удаление задач из списка на 300 мс
            isEditorCollapsing = true
            delay(com.example.ui.theme.AnimationConstants.TASK_EXPANSION_DURATION_MS)
            isEditorCollapsing = false
            // После завершения анимации — полная синхронизация списка с ViewModel
            localTasksList = currentDisplayTasks
        } else {
            isEditorCollapsing = false
        }
    }

    // Синхронизация localTasksList с displayTasks из ViewModel
    LaunchedEffect(state.displayTasks) {
        if (isEditorCollapsing) {
            // Во время сворачивания: обновляем ДАННЫЕ задач (новый заголовок и т.д.),
            // но НЕ УДАЛЯЕМ задачи из списка (чтобы анимация сворачивания не прерывалась)
            localTasksList = localTasksList.map { localTask ->
                currentDisplayTasks.find { it.item.id == localTask.item.id } ?: localTask
            }
        } else {
            localTasksList = state.displayTasks
        }
    }

    val focusManager = LocalFocusManager.current

    var showMoveDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isWhenDialogOpen by remember { mutableStateOf(false) }
    var swipeWhenTask by remember { mutableStateOf<ItemWithChecklist?>(null) }
    val deletedTaskIds = remember { mutableStateListOf<String>() }

    // Состояния диалогов для пакетных операций
    var showBatchWhenDialog by remember { mutableStateOf(false) }
    var showBatchMoveDialog by remember { mutableStateOf(false) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var showBatchTagDialog by remember { mutableStateOf(false) }
    var showBatchDeadlineDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Обработка нажатия системной кнопки "Назад" для выхода из режима множественного выбора
    BackHandler(enabled = state.isSelectionMode) {
        onEvent(ThingsCategoryListEvent.ExitSelectionMode)
    }
    
    LaunchedEffect(
        showMoveDialog, showDeleteConfirm, isWhenDialogOpen, swipeWhenTask != null,
        showBatchWhenDialog, showBatchMoveDialog, showBatchDeleteConfirm, showBatchTagDialog, showBatchDeadlineDialog
    ) {
        val anyActive = showMoveDialog || showDeleteConfirm || isWhenDialogOpen || swipeWhenTask != null ||
                showBatchWhenDialog || showBatchMoveDialog || showBatchDeleteConfirm || showBatchTagDialog || showBatchDeadlineDialog
        onDialogsActiveChange(anyActive)
    }
    
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
        draggedItemKey = dragDropState.draggedItemKey,
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

    // --- Scroll_B: аккумулятор ручного скролла пользователя ---
    val manualScrollDelta = remember { FloatAccumulator() }
    val scrollTrackingConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // consumed.y отрицателен при скролле вперёд (палец вверх),
                // инвертируем, чтобы forward scroll = positive
                manualScrollDelta.value += -consumed.y
                return Offset.Zero
            }
        }
    }

    // --- Bottom spacer: управляется вручную для синхронизации с обратным скроллом ---
    val collapsedSpacerHeightPx = with(density) { MaterialTheme.dimens.listBottomSpacerHeight.toPx() }
    val expandedSpacerHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val verticalGapLimitPx = with(density) { MaterialTheme.dimens.taskExpandedVerticalGap.toPx() }

    var bottomSpacerHeightPx by remember { mutableStateOf(collapsedSpacerHeightPx) }
    val bottomSpacerHeight = with(density) { bottomSpacerHeightPx.toDp() }

    // Guard: отслеживаем, было ли предыдущее раскрытие, чтобы не скроллить на первой композиции
    var previousExpandedTaskId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(inlineExpandedTaskId) {
        if (inlineExpandedTaskId != null) {
            // Раскрытие задачи: сбросить аккумулятор, увеличить spacer
            manualScrollDelta.value = 0f
            bottomSpacerHeightPx = expandedSpacerHeightPx
            previousExpandedTaskId = inlineExpandedTaskId
        } else if (previousExpandedTaskId != null) {
            // Сворачивание задачи
            previousExpandedTaskId = null
            // Scroll_A уже реверсируется AnimatedTaskItem (dispatchRawDelta при collapse),
            // поэтому здесь реверсируем только Scroll_B (ручной скролл пользователя)
            val scrollB = manualScrollDelta.value

            val layoutInfo = lazyListState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = layoutInfo.totalItemsCount

            // Обратный скролл нужен, если пользователь скроллил вперёд (scrollB > 0)
            // И bottom spacer виден (позиция может не выдержать уменьшения spacer'а).
            // Если bottom spacer не виден — контента достаточно, позиция устойчива.
            val shouldReverse = scrollB > 0.5f &&
                    lastVisibleItem != null &&
                    lastVisibleItem.index >= totalItems - 1

            try {
                if (shouldReverse) {
                    // Покадровый обратный скролл scrollB через dispatchRawDelta.
                    // В отличие от animateScrollBy, dispatchRawDelta не накапливает
                    // «задолженность» при достижении края списка — unconsumed delta
                    // просто теряется, что исключает «двойную анимацию».
                    val durationNs = com.example.ui.theme.AnimationConstants.TASK_EXPANSION_DURATION_MS * 1_000_000L
                    val startNanos = withFrameNanos { it }
                    var lastEased = 0f
                    while (true) {
                        val now = withFrameNanos { it }
                        val rawProgress = ((now - startNanos).toFloat() / durationNs).coerceAtMost(1f)
                        val eased = androidx.compose.animation.core.FastOutSlowInEasing.transform(rawProgress)
                        val frameDelta = -scrollB * (eased - lastEased)
                        lazyListState.dispatchRawDelta(frameDelta)
                        lastEased = eased
                        if (rawProgress >= 1f) break
                    }
                } else {
                    // Обратный скролл не нужен — ждём завершения анимации сворачивания,
                    // прежде чем уменьшать spacer (иначе короткий список дёрнется)
                    delay(com.example.ui.theme.AnimationConstants.TASK_EXPANSION_DURATION_MS)
                }
            } finally {
                bottomSpacerHeightPx = collapsedSpacerHeightPx
            }
        }
    }

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

    val toolbarHeightPx = with(density) { TOP_APP_BAR_HEIGHT.toPx() }
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density)
    val toolbarExtraMarginPx = with(density) { 16.dp.toPx() }
    val toolbarOffsetY by animateFloatAsState(
        targetValue = if (anyExpanded) -(toolbarHeightPx + statusBarHeightPx + toolbarExtraMarginPx) else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = com.example.ui.theme.AnimationConstants.TASK_EXPANSION_DURATION_MS.toInt(),
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "toolbarOffset"
    )

    Box(modifier = Modifier.fillMaxSize().background(bkgColor)) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .nestedScroll(scrollTrackingConnection)
                .graphicsLayer { translationY = pullOffset.value }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                        focusManager.clearFocus()
                    })
                }
                // [ИЗМЕНЕНИЕ]: Установлен аккуратный отступ 10.dp от края экрана до карточек задач
                .padding(horizontal = 10.dp)
                .testTag("tasks_lazy_list"),
            contentPadding = PaddingValues(top = topPaddingTotal, bottom = 100.dp)
        ) {
            item(key = "main_header") {
                // Извлеченный подкомпонент заголовка
                Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                    MainCategoryHeader(
                        screen = screen,
                        project = project,
                        tasks = state.allTasks,
                        area = area,
                        scaleFactor = scaleFactor,
                        textPrimaryColor = textPrimaryColor,
                        globalDimAlpha = globalDimAlpha,
                        onDeleteProject = { onEvent(ThingsCategoryListEvent.DeleteProject(it)) },
                        onDeleteArea = { onEvent(ThingsCategoryListEvent.DeleteArea(it)) }
                    )
                }
            }

            if (screen == ActiveScreen.TODAY && todayCalendarEvents.isNotEmpty()) {
                item {
                    CalendarEventsWidget(
                        events = todayCalendarEvents,
                        textSecondaryColor = textSecondaryColor,
                        isDark = false,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .graphicsLayer { alpha = globalDimAlpha }
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
                        onTagSelect = { onEvent(ThingsCategoryListEvent.SelectTag(it)) },
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .graphicsLayer { alpha = globalDimAlpha }
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.tagsBetweenSectionSpacing))
                }
            }

            val hasTasks = when (screen) {
                ActiveScreen.TODAY -> standardToday.isNotEmpty() || eveningToday.isNotEmpty() || dragDropState.draggedItemKey != null
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
                    Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                        EmptyStateView(textSecondaryColor = textSecondaryColor)
                    }
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
                                onEvent = { event ->
                                    if (event is ThingsCategoryListEvent.SwipeTaskRight) {
                                        swipeWhenTask = event.task
                                    } else {
                                        onEvent(event)
                                    }
                                },
                                coroutineScope = coroutineScope,
                                screen = screen,
                                upcomingDays = upcomingDays,
                                localTasksList = localTasksList,
                                displayTasks = state.displayTasks,
                                allTasks = state.allTasks,
                                projectProgressMap = state.projectProgressMap,
                                onLocalTasksListChange = { localTasksList = it },
                                lazyListState = lazyListState,
                                onWhenDialogVisibilityChange = { isWhenDialogOpen = it },
                                isSelectionMode = state.isSelectionMode,
                                isSelected = state.selectedTaskIds.contains(item.item.id),
                                onToggleSelect = { onEvent(ThingsCategoryListEvent.ToggleTaskSelection(item.item.id)) },
                                modifier = if (dragDropState.draggedItemKey == item.item.id) {
                                    Modifier
                                } else {
                                    Modifier.animateItem(
                                        fadeInSpec = androidx.compose.animation.core.tween(300),
                                        fadeOutSpec = androidx.compose.animation.core.tween(300),
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
                                    .padding(horizontal = 8.dp)
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
                                    .padding(horizontal = 8.dp)
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
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .animateItem(
                                        placementSpec = placementSpec
                                    )
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(bottomSpacerHeight))
                }
            }
        }

        // TopAppBar
        // [ИЗМЕНЕНИЕ]: Передаем в AppBar дополнительные параметры (состояние скролла, экран, проект, область ответственности и список задач) для вывода иконки и полужирного заголовка
        CategoryListTopAppBar(
            modifier = Modifier.graphicsLayer { translationY = toolbarOffsetY },
            isDark = isDark,
            textPrimaryColor = textPrimaryColor,
            textSecondaryColor = textSecondaryColor,
            onBackClick = { onEvent(ThingsCategoryListEvent.ClickBack) },
            screen = screen,
            project = project,
            area = area,
            tasks = state.allTasks,
            isScrolled = isScrolledPastHeader,
            isSelectionMode = state.isSelectionMode,
            selectedCount = state.selectedTaskIds.size,
            isAllSelected = state.selectedTaskIds.isNotEmpty() && state.selectedTaskIds.size == state.displayTasks.size,
            onCancelSelection = { onEvent(ThingsCategoryListEvent.ExitSelectionMode) },
            onSelectAllClick = { onEvent(ThingsCategoryListEvent.SelectAllTasks) },
            onDeselectAllClick = { onEvent(ThingsCategoryListEvent.DeselectAllTasks) },
            onEnterSelectionMode = { onEvent(ThingsCategoryListEvent.EnterSelectionMode(null)) }
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
                onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                showDeleteConfirm = false
                coroutineScope.launch {
                    // 1. Сначала сворачиваем открытый редактор в обычную белую строку (300 мс)
                    delay(com.example.ui.theme.AnimationConstants.TASK_EXPANSION_DURATION_MS)
                    // 2. Включаем серое закрашивание серым кругом от чекбокса и серость текста
                    deletedTaskIds.add(activeTask.item.id)
                    // 3. Ждём 500 мс (стандартный таймер выполнения чекбокса)
                    delay(500L)
                    // 4. Удаляем из ViewModel -> animateItem растворяет карточку и сдвигает список
                    onEvent(ThingsCategoryListEvent.DeleteTask(activeTask))
                }
            }
        )
    }

    if (swipeWhenTask != null) {
        val targetTask = swipeWhenTask!!
        var pendingStartDate by remember(targetTask) { mutableStateOf(targetTask.item.startDate) }
        var pendingSection by remember(targetTask) { mutableStateOf(targetTask.item.section) }
        var pendingIsTonight by remember(targetTask) { mutableStateOf(targetTask.item.isTonight) }

        ThingsWhenDialog(
            startDate = pendingStartDate,
            onStartDateChange = { pendingStartDate = it },
            section = pendingSection,
            onSectionChange = { pendingSection = it },
            isTonight = pendingIsTonight,
            onIsTonightChange = { pendingIsTonight = it },
            onShowCalendarHelperChange = { },
            onDismissRequest = {
                val tagsList = targetTask.item.cachedTags
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                onEvent(
                    ThingsCategoryListEvent.SaveTask(
                        taskWrapper = targetTask,
                        title = targetTask.item.title,
                        notes = targetTask.item.notes,
                        section = pendingSection,
                        isTonight = pendingIsTonight,
                        startDate = pendingStartDate,
                        dueDate = targetTask.item.dueDate,
                        tags = tagsList,
                        projectId = targetTask.item.projectId,
                        priority = targetTask.item.priority,
                        checklist = targetTask.checklist
                    )
                )
                swipeWhenTask = null
            }
        )
    }

    FloatingBottomCapsuleToolbar(
        visible = inlineExpandedTaskId != null && activeTask != null && !isWhenDialogOpen && swipeWhenTask == null,
        onMoveClick = { showMoveDialog = true },
        onDeleteClick = { showDeleteConfirm = true },
        onDuplicateClick = {
            if (activeTask != null) {
                onEvent(ThingsCategoryListEvent.DuplicateTask(activeTask))
            }
        },
        modifier = Modifier.align(Alignment.BottomCenter)
    )

    // Диалоги для пакетных операций
    if (showBatchWhenDialog) {
        var batchStartDate by remember { mutableStateOf<Long?>(null) }
        var batchSection by remember { mutableStateOf(TaskSection.TODAY) }
        var batchIsTonight by remember { mutableStateOf(false) }

        ThingsWhenDialog(
            startDate = batchStartDate,
            onStartDateChange = { batchStartDate = it },
            section = batchSection,
            onSectionChange = { batchSection = it },
            isTonight = batchIsTonight,
            onIsTonightChange = { batchIsTonight = it },
            onShowCalendarHelperChange = { },
            onDismissRequest = {
                onEvent(ThingsCategoryListEvent.BatchScheduleTasks(batchStartDate, batchIsTonight))
                showBatchWhenDialog = false
            }
        )
    }

    if (showBatchMoveDialog) {
        ThingsMoveDialog(
            currentProjectId = null,
            currentAreaId = null,
            currentIsInbox = false,
            projects = projects,
            areas = areasState,
            allTasks = state.allTasks,
            onMove = { projectId, areaId, moveToInbox ->
                onEvent(ThingsCategoryListEvent.BatchMoveTasks(projectId, areaId, moveToInbox))
                showBatchMoveDialog = false
            },
            onDismissRequest = { showBatchMoveDialog = false }
        )
    }

    if (showBatchDeleteConfirm) {
        DeleteConfirmDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            onConfirmDelete = {
                showBatchDeleteConfirm = false
                onEvent(ThingsCategoryListEvent.BatchDeleteTasks)
            }
        )
    }

    if (showBatchTagDialog) {
        ThingsTagDialog(
            activeTags = emptyList(),
            allSavedTags = allSavedTags,
            allSavedTagObjects = allSavedTagObjects,
            onNewTagCreated = { name, parentId -> onEvent(ThingsCategoryListEvent.CreateTag(name, parentId)) },
            onDeleteTag = { tag -> onEvent(ThingsCategoryListEvent.DeleteTag(tag)) },
            onUpdateTag = { tag -> onEvent(ThingsCategoryListEvent.UpdateTag(tag)) },
            onUpdateTagsOrder = { tags -> onEvent(ThingsCategoryListEvent.UpdateTagsOrder(tags)) },
            onTagsSelected = { newTags ->
                onEvent(ThingsCategoryListEvent.BatchSetTags(newTags))
                showBatchTagDialog = false
            },
            onDismissRequest = { showBatchTagDialog = false }
        )
    }

    if (showBatchDeadlineDialog) {
        DeadlineDatePickerDialog(
            initialSelectedDateMillis = null,
            onDateSelected = { deadline ->
                onEvent(ThingsCategoryListEvent.BatchSetDeadline(deadline))
                showBatchDeadlineDialog = false
            },
            onDismiss = { showBatchDeadlineDialog = false }
        )
    }

    // Плавающий тулбар пакетных операций
    BatchActionToolbar(
        isVisible = state.isSelectionMode,
        selectedCount = state.selectedTaskIds.size,
        onWhenClick = { showBatchWhenDialog = true },
        onMoveClick = { showBatchMoveDialog = true },
        onDeleteClick = { showBatchDeleteConfirm = true },
        onCompleteClick = { onEvent(ThingsCategoryListEvent.BatchCompleteTasks(true)) },
        onSetTagsClick = { showBatchTagDialog = true },
        onSetDeadlineClick = { showBatchDeadlineDialog = true },
        onDuplicateClick = { onEvent(ThingsCategoryListEvent.BatchDuplicateTasks) },
        onShareClick = {
            val selectedTasks = state.allTasks.filter { state.selectedTaskIds.contains(it.item.id) }
            val shareText = selectedTasks.joinToString("\n") { "• " + it.item.title }
            if (shareText.isNotEmpty()) {
                val sendIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                context.startActivity(android.content.Intent.createChooser(sendIntent, null))
            }
        },
        modifier = Modifier.align(Alignment.BottomCenter)
    )
  }
}
