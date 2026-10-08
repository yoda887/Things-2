package com.example.ui.screens.home.components

import com.example.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.ui.components.ThingsConfirmDialog
import com.example.ui.theme.ThingsTheme
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.gestures.scrollBy
import com.example.ui.components.fabdrag.FabDropTarget
import com.example.ui.components.fabdrag.LocalFabDragController
import com.example.ui.screens.home.inlineeditor.utils.EditorOutsideTouch
import com.example.ui.screens.home.inlineeditor.utils.observeTouchesOutsideEditor

import kotlinx.coroutines.flow.first
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.toSize
import com.example.ui.components.PULL_TO_SEARCH_CIRCLE_CENTER_Y
import com.example.ui.components.PULL_TO_SEARCH_CIRCLE_RADIUS
import com.example.ui.components.pullToSearchIndicatorTranslationY
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.components.hideSoftKeyboardNow
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
import com.example.ui.screens.home.subcomponents.ProjectHeadingRow
import com.example.ui.screens.home.inlineeditor.ThingsTaskInlineEditor
import com.example.ui.screens.home.inlineeditor.dialogs.ThingsMoveDialog
import com.example.ui.screens.home.inlineeditor.dialogs.DeleteConfirmDialog
import com.example.ui.screens.home.inlineeditor.dialogs.ThingsWhenDialog
import com.example.ui.theme.*
import com.example.data.model.ChecklistItem
import java.util.Calendar
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat

// Извлеченные UI подкомпоненты
import com.example.ui.screens.home.subcomponents.MainCategoryHeader
import com.example.ui.screens.home.subcomponents.SubCategoryHeader
import com.example.ui.screens.home.subcomponents.UpcomingDateHeader
import com.example.ui.screens.home.subcomponents.UpcomingMonthHeader
import com.example.ui.screens.home.subcomponents.TagFilterRow
import com.example.ui.screens.home.subcomponents.ProjectItemRow
import com.example.ui.screens.home.subcomponents.EmptyStateView
import com.example.ui.screens.home.subcomponents.SearchEmptyState
import com.example.ui.screens.home.subcomponents.SearchEntityRow
import com.example.ui.screens.home.subcomponents.SearchQueryField
import com.example.ui.screens.home.subcomponents.SearchSectionHeader
import com.example.ui.screens.home.subcomponents.SEARCH_ROW_ICON_SIZE
import com.example.ui.screens.home.subcomponents.CategoryListTopAppBar
import com.example.ui.screens.home.subcomponents.TopAppBarSelectionState
import com.example.ui.screens.home.subcomponents.AnimatedTaskItem
import com.example.ui.screens.home.subcomponents.BatchActionToolbar
import com.example.ui.screens.home.inlineeditor.DeadlineDatePickerDialog
import com.example.ui.screens.home.inlineeditor.dialogs.ThingsTagDialog
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext

// Плашка места вставки «+», промежуток и подзаголовок проекта при перетаскивании
private val FAB_GAP_HEIGHT = 44.dp
private val NEW_HEADING_TEXT_ZONE = 22.dp
private val NEW_HEADING_TEXT_BOTTOM = 3.dp
private val HEADING_TOP_SPACING = 22.dp
private val CALENDAR_LAST_EVENT_GAP = 11.dp
// Оттяжка списка к Quick Find
private val PULL_THRESHOLD = 100.dp
private val PULL_MAX_OFFSET = 150.dp
private val PULL_ZONE_EXTRA = 96.dp
private val PULL_INDICATOR_WIDTH = 60.dp
// Запас прокрутки снизу и поле тулбара
private val LIST_BOTTOM_PADDING = 100.dp
private val TOOLBAR_EXTRA_MARGIN = 16.dp
private val EVENING_HEADER_GAP = 16.dp

private val TOP_APP_BAR_HEIGHT = 56.dp

/**
 * Передача заголовка тулбару при прокрутке — в долях собственной высоты заголовка экрана.
 * Заголовок экрана тает на [HEADER_FADE_START_FRACTION]..[HEADER_FADE_END_FRACTION], компактный
 * заголовок тулбара проявляется на [TOOLBAR_TITLE_START_FRACTION]..[TOOLBAR_TITLE_END_FRACTION].
 * Отрезки перекрываются чуть-чуть: там оба заголовка почти прозрачны — так нет ни момента, когда
 * не видно ни одного, ни заметного наложения одного на другой. Фон тулбара проявляется на всём пути.
 */
private const val HEADER_FADE_START_FRACTION = 0.40f
private const val HEADER_FADE_END_FRACTION = 0.68f
private const val TOOLBAR_TITLE_START_FRACTION = 0.62f
private const val TOOLBAR_TITLE_END_FRACTION = 0.92f
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
    // Касания вне раскрытого редактора: маркеры курсора прячутся ещё до отпускания пальца
    val editorOutsideTouch = remember { EditorOutsideTouch() }
    
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

    // Заголовки проекта: локальный порядок на время перетаскивания, из базы — когда ничего не несут
    var localHeadings by remember { mutableStateOf(state.headings) }
    LaunchedEffect(state.headings, dragDropState.draggedItemKey) {
        if (dragDropState.draggedItemKey == null) localHeadings = state.headings
    }
    val projectHeadingIds = remember(localHeadings) { localHeadings.map { it.id } }
    // Заголовок, название которого сейчас правят (в том числе только что созданный)
    var editingHeadingId by remember { mutableStateOf<String?>(null) }
    var headingToDelete by remember { mutableStateOf<Item?>(null) }

    /** Новый пустой заголовок в конце проекта: сразу открывается для ввода названия */
    fun addHeading() {
        val currentProject = project ?: return
        val heading = Item(
            type = Item.TYPE_HEADING,
            projectId = currentProject.id,
            sortOrder = (localHeadings.maxOfOrNull { it.sortOrder } ?: -1) + 1
        )
        editingHeadingId = heading.id
        onEvent(ThingsCategoryListEvent.SaveHeading(heading))
    }

    val view = androidx.compose.ui.platform.LocalView.current

    var showMoveDialog by remember { mutableStateOf(false) }
    // Поле поиска забирает фокус один раз за показ экрана, а не при каждом возвращении в видимую часть списка
    var searchFieldAutoFocused by rememberSaveable(screen) { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isWhenDialogOpen by remember { mutableStateOf(false) }
    // Окно тегов редактора: пока оно открыто, нижняя панель действий над задачей спрятана
    var isTagDialogOpen by remember { mutableStateOf(false) }
    var swipeWhenTask by remember { mutableStateOf<ItemWithChecklist?>(null) }
    // Смещение строки, по которой сделали свайп, от центра экрана: из неё вырастает диалог When
    var swipeWhenOrigin by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    val deletedTaskIds = remember { mutableStateListOf<String>() }

    // Состояния диалогов для пакетных операций
    var showBatchWhenDialog by remember { mutableStateOf(false) }
    var showBatchMoveDialog by remember { mutableStateOf(false) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var showBatchTagDialog by remember { mutableStateOf(false) }
    var showBatchDeadlineDialog by remember { mutableStateOf(false) }
    var isDragSelecting by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(state.isSelectionMode) {
        if (!state.isSelectionMode) {
            isDragSelecting = false
        }
    }

    // Обработка нажатия системной кнопки "Назад" для выхода из режима множественного выбора
    BackHandler(enabled = state.isSelectionMode) {
        onEvent(ThingsCategoryListEvent.ExitSelectionMode)
    }
    
    LaunchedEffect(
        showMoveDialog, showDeleteConfirm, isWhenDialogOpen, isTagDialogOpen, swipeWhenTask != null,
        showBatchWhenDialog, showBatchMoveDialog, showBatchDeleteConfirm, showBatchTagDialog, showBatchDeadlineDialog
    ) {
        val anyActive = showMoveDialog || showDeleteConfirm || isWhenDialogOpen || isTagDialogOpen || swipeWhenTask != null ||
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

    // Извлечение состояния предстоящих дней (UpcomingDays) и месяцев
    val upcomingSchedule = rememberUpcomingSchedule(localTasksList, calendarEvents)
    val upcomingDays = upcomingSchedule.days
    val upcomingMonths = upcomingSchedule.months
    val monthTaskBadges = upcomingSchedule.monthTaskBadges

    val standardToday = remember(localTasksList) { localTasksList.filter { !it.item.isTonight } }
    val eveningToday = remember(localTasksList) { localTasksList.filter { it.item.isTonight } }

    val displayTasks = localTasksList
    
    var isLaterItemsHidden by rememberSaveable { mutableStateOf(false) }
    var isTagsFilterVisible by remember(screen, project?.id, area?.id) { mutableStateOf(false) }

    // Извлечение выравнивания плоского списка для LazyColumn
    val flattened = rememberFlattenedList(
        screen = screen,
        standardToday = standardToday,
        eveningToday = eveningToday,
        draggedItemKey = dragDropState.draggedItemKey,
        upcomingDays = upcomingDays,
        upcomingMonths = upcomingMonths,
        projects = projects,
        area = area,
        displayTasks = displayTasks,
        isLaterItemsHidden = isLaterItemsHidden,
        tag = state.tag,
        allTasks = state.allTasks,
        areas = areasState,
        savedTags = allSavedTagObjects,
        searchQuery = state.searchQuery,
        headings = if (screen == ActiveScreen.PROJECT_DETAIL) localHeadings else emptyList()
    )

    // ── Добавление перетаскиванием кнопки «+» (см. FabDragController) ──
    val fabDrag = LocalFabDragController.current
    val acceptsFabDrop = screen in FAB_DROP_SCREENS && !state.isSelectionMode
    // Высота промежутка до его появления — по ней первый раз выбирается место
    val fabDefaultGapPx = with(androidx.compose.ui.platform.LocalDensity.current) { FAB_GAP_HEIGHT.toPx() }
    var listCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    var fabSlot by remember { mutableStateOf<FabSlot?>(null) }
    // Раздвигание после сброса: промежуток вырастает до высоты раскрытой задачи, и только потом создаётся задача
    val gapExtra = remember { androidx.compose.animation.core.Animatable(0f) }
    var pendingFabTaskId by remember { mutableStateOf<String?>(null) }
    val spreadScope = rememberCoroutineScope()
    val currentFlattened by rememberUpdatedState(flattened)
    // Промежуток перешёл на другое место — лёгкий отклик, как при обмене задач в обычном перетаскивании
    val fabHapticView = androidx.compose.ui.platform.LocalView.current
    var lastFabSlot by remember { mutableStateOf<FabSlot?>(null) }
    LaunchedEffect(fabSlot) {
        val previous = lastFabSlot
        lastFabSlot = fabSlot
        if (previous != null && fabSlot != null && previous != fabSlot && fabDrag?.isDragging == true) {
            fabHapticView.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
        }
    }
    // Как только новая строка появилась в списке, промежуток убирается в том же кадре — место не удваивается
    val shownFabSlot = fabSlot?.takeIf { slot ->
        pendingFabTaskId == null || localTasksList.none { it.item.id == pendingFabTaskId }
    }
    val currentLocalTasks by rememberUpdatedState(localTasksList)

    /** Новая задача с полями экрана — как при нажатии на «+» */
    fun newTaskForScreen(): Item {
        val start = when (screen) {
            ActiveScreen.TODAY -> 1
            ActiveScreen.UPCOMING, ActiveScreen.ANYTIME, ActiveScreen.AREA_DETAIL, ActiveScreen.TAG_DETAIL -> 2
            ActiveScreen.SOMEDAY -> 3
            else -> 0
        }
        return Item(
            type = Item.TYPE_TASK,
            start = start,
            startDate = if (screen == ActiveScreen.TODAY) System.currentTimeMillis() else null,
            projectId = if (screen == ActiveScreen.PROJECT_DETAIL) project?.id else null,
            areaId = if (screen == ActiveScreen.AREA_DETAIL) area?.id else null,
            cachedTags = if (screen == ActiveScreen.TAG_DETAIL) state.tag?.title ?: "" else ""
        )
    }

    /** Сброс кнопки «+» на промежуток: задача на его месте, у левого края проекта — заголовок */
    fun dropFab(): Boolean {
        val slot = fabSlot ?: return false
        // Центр промежутка после раздвигания — туда приземлится кнопка; считаем, пока промежуток ещё на месте.
        // Задача раскрывается на высоту [FabDragController.expandedRowHeightPx], поэтому центр — на её половине
        val gapInfoNow = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == FabGapItem.KEY }
        val finalGapHeight = if (slot.asHeading) (gapInfoNow?.size ?: 0).toFloat()
        else maxOf(fabDrag?.expandedRowHeightPx ?: 0f, (gapInfoNow?.size ?: 0).toFloat())
        fabDrag?.dropAnchor = listCoordinates?.let { coords ->
            gapInfoNow?.let { gap ->
                val info = lazyListState.layoutInfo
                val origin = coords.positionInRoot()
                androidx.compose.ui.geometry.Offset(
                    origin.x + coords.size.width / 2f,
                    // Кнопка только уходит в горизонтальный центр: вертикально — в верхнюю часть будущей карточки,
                    // то есть в центр промежутка до его раздвигания
                    origin.y + (gap.offset - info.viewportStartOffset) + gap.size / 2f
                )
            }
        }
        val gapHeightNow = (gapInfoNow?.size ?: 0).toFloat()
        val flat = currentFlattened
        val currentProject = project
        if (slot.asHeading && currentProject != null) {
            fabSlot = null
            val placement = FabInsertion.headingPlacement(flat, slot.index)
            val heading = Item(type = Item.TYPE_HEADING, projectId = currentProject.id, sortOrder = -1)
            val headings = FabInsertion.headingsWith(localHeadings, heading, placement.headingIndex)
            val moved = currentLocalTasks
                .filter { it.item.id in placement.movedTaskIds }
                .map { it.item.copy(headingId = heading.id, modificationDate = System.currentTimeMillis()) }
            editingHeadingId = heading.id
            onEvent(ThingsCategoryListEvent.InsertHeading(headings, moved))
        } else {
            val tasks = currentLocalTasks
            val placement = FabInsertion.taskPlacement(flat, slot.index, tasks)
            val newTask = newTaskForScreen().let { base ->
                base.copy(
                    headingId = placement.headingId,
                    isTonight = placement.isTonight,
                    // «Предстоящие»: дата раздела, под заголовком которого раскрылся промежуток
                    startDate = placement.startDate ?: base.startDate
                ).let { task ->
                    when {
                        // Экран области: секция «Когда-нибудь» и «Планы» задаются свойствами задачи
                        placement.inAreaSomeday -> task.copy(start = Item.START_SOMEDAY)
                        placement.inAreaUpcoming -> task.copy(startDate = tomorrowNoonMillis())
                        else -> task
                    }
                }
            }
            val insertAt = placement.taskIndex.coerceIn(0, tasks.size)
            val freeOrder = FabInsertion.freeSortOrder(tasks.map { it.item.sortOrder }, insertAt)
            val commit: () -> Unit = if (freeOrder != null) {
                // Место между соседями есть — остальные задачи не трогаем
                { onEvent(ThingsCategoryListEvent.CreateTaskAt(newTask.copy(sortOrder = freeOrder), emptyList())) }
            } else {
                val list = tasks.map { it.item }.toMutableList()
                list.add(insertAt, newTask)
                val renumbered = list.mapIndexed { index, item -> if (item.sortOrder == index) item else item.copy(sortOrder = index) }
                val created = renumbered.first { it.id == newTask.id }
                val previousOrder = tasks.associate { it.item.id to it.item.sortOrder }
                val others = renumbered.filter { it.id != newTask.id && previousOrder[it.id] != it.sortOrder }
                val emit: () -> Unit = { onEvent(ThingsCategoryListEvent.CreateTaskAt(created, others)) }
                emit
            }
            // Порядок: 1) задачи сверху и снизу раздвигаются на высоту раскрытой задачи, 2) в центре
            // освободившегося места раскрывается новая задача (см. AnimatedTaskItem)
            pendingFabTaskId = newTask.id
            spreadScope.launch {
                gapExtra.snapTo(0f)
                gapExtra.animateTo(
                    (finalGapHeight - gapHeightNow).coerceAtLeast(0f),
                    androidx.compose.animation.core.tween(FAB_SPREAD_MS, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                )
                fabDrag?.freshTaskId = newTask.id
                commit()
                // Промежуток уйдёт в том же кадре, в котором в списке появится новая строка
                kotlinx.coroutines.withTimeoutOrNull(800) {
                    snapshotFlow { localTasksList.any { it.item.id == newTask.id } }.first { it }
                }
                withFrameNanos { }
                fabSlot = null
                pendingFabTaskId = null
                gapExtra.snapTo(0f)
            }
        }
        return true
    }

    // Высота раскрытой строки — по ней промежуток раздвигается в следующий раз точно
    LaunchedEffect(inlineExpandedTaskId) {
        val id = inlineExpandedTaskId ?: return@LaunchedEffect
        kotlinx.coroutines.delay(ThingsMotion.LONG.toLong())
        lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == id }?.size
            ?.takeIf { it > 0 }?.let { fabDrag?.expandedRowHeightPx = it.toFloat() }
    }

    if (fabDrag != null && acceptsFabDrop) {
        DisposableEffect(fabDrag) {
            val target = FabDropTarget { dropFab() }
            fabDrag.dropTarget = target
            onDispose { if (fabDrag.dropTarget === target) fabDrag.dropTarget = null }
        }
    }

    // Пока тянут «+»: место промежутка по пальцу и автопрокрутка у краёв списка
    LaunchedEffect(fabDrag?.isDragging, acceptsFabDrop) {
        val controller = fabDrag ?: return@LaunchedEffect
        if (!controller.isDragging || !acceptsFabDrop) {
            // Пока промежуток раздвигается под новую задачу, он остаётся на месте
            if (pendingFabTaskId == null) fabSlot = null
            return@LaunchedEffect
        }
        var autoScrollArmed = false
        while (controller.isDragging) {
            withFrameNanos { }
            val coords = listCoordinates ?: continue
            val pointer = controller.pointer
            // Над кнопкой отмены или вне списка промежуток остаётся на месте: просто отпустить здесь — не создавать
            if (pointer == androidx.compose.ui.geometry.Offset.Unspecified || !coords.isAttached) continue
            if (controller.isOverAction && fabSlot != null) continue
            val origin = coords.positionInRoot()
            val x = pointer.x - origin.x
            // Место считается по центру кнопки, а не по пальцу: кнопка держится за точку захвата
            val y = pointer.y - controller.grabOffset.y - origin.y
            val height = coords.size.height.toFloat()
            val width = coords.size.width.toFloat()
            val layoutInfo = lazyListState.layoutInfo
            val flat = currentFlattened
            val indexByKey = HashMap<Any, Int>(flat.size)
            flat.forEachIndexed { index, element -> listItemKey(element)?.let { indexByKey[it] = index } }
            val gapInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.key == FabGapItem.KEY }
            val gapSize = gapInfo?.size ?: 0
            val rows = layoutInfo.visibleItemsInfo.mapNotNull { info ->
                val index = indexByKey[info.key] ?: return@mapNotNull null
                if (!FabInsertion.isAnchor(flat[index])) return@mapNotNull null
                var top = (info.offset - layoutInfo.viewportStartOffset).toFloat()
                if (gapInfo != null && info.offset > gapInfo.offset) top -= gapSize
                FabRow(index, top, info.size.toFloat())
            }
            val asHeading = screen == ActiveScreen.PROJECT_DETAIL && x < width * FAB_HEADING_EDGE_FRACTION
            fabSlot = if (rows.isEmpty()) {
                // Пустой список: промежуток в начале
                if (flat.none { FabInsertion.isAnchor(it) }) FabSlot(flat.size, asHeading) else fabSlot
            } else {
                FabInsertion.slotAt(y, rows, asHeading, gapHeight = if (gapSize > 0) gapSize.toFloat() else fabDefaultGapPx)
                    ?.let { FabInsertion.normalizeSlot(flat, it) }
            }
            // Автопрокрутка, когда палец у края списка
            val zone = height * FAB_SCROLL_ZONE_FRACTION
            // Кнопка стартует у нижнего края — прокрутка включается, только когда палец хотя бы раз
            // вышел из зон у краёв, иначе список уезжал бы сразу после долгого нажатия
            if (y in zone..(height - zone)) autoScrollArmed = true
            val delta = if (!autoScrollArmed) 0f else when {
                y < zone -> -FAB_SCROLL_MAX_STEP * ((zone - y) / zone).coerceIn(0f, 1f)
                y > height - zone -> FAB_SCROLL_MAX_STEP * ((y - (height - zone)) / zone).coerceIn(0f, 1f)
                else -> 0f
            }
            if (delta != 0f) lazyListState.scrollBy(delta)
        }
        if (pendingFabTaskId == null) fabSlot = null
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val itemHeightPx = with(density) { MaterialTheme.dimens.taskItemEstimatedHeight.roundToPx() }
    
    // Переход к задаче из Quick Find: экран сразу показывается прокрученным к ней, как в Things 3.
    // Список экрана приходит не в первом кадре — ждём, пока в нём появится задача, и прокручиваем
    // в том же кадре, без задержки (иначе экран успевал показаться с начала списка и прыгал)
    LaunchedEffect(highlightedTaskId) {
        if (highlightedTaskId != null) {
            val innerIndex = kotlinx.coroutines.withTimeoutOrNull(1000) {
                androidx.compose.runtime.snapshotFlow {
                    currentFlattened.indexOfFirst { item ->
                        when (item) {
                            is ItemWithChecklist -> item.item.id == highlightedTaskId
                            is Item -> item.id == highlightedTaskId
                            else -> false
                        }
                    }
                }.first { it != -1 }
            } ?: -1
            if (innerIndex != -1) {
                val headerOffset = run {
                    var count = 1 // main_header
                    if (screen == ActiveScreen.TODAY && todayCalendarEvents.isNotEmpty()) {
                        count += 1
                    }
                    if (allTags.isNotEmpty() && isTagsFilterVisible) {
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
    val isEditorOpen by rememberUpdatedState(anyExpanded)
    LaunchedEffect(inlineExpandedTaskId) {
        if (inlineExpandedTaskId != null) editorOutsideTouch.reset()
    }

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

    // Во время перетаскивания соседи должны успевать встать на место раньше, чем закончится
    // приземление карточки (200 мс): мягкая пружина доезжает около 370 мс, и после резкого
    // отпускания был виден хвост чужой анимации — соседняя строка доползала уже после посадки
    val placementSpec: androidx.compose.animation.core.FiniteAnimationSpec<androidx.compose.ui.unit.IntOffset>? = when {
        isTransitionActive -> null
        dragDropState.draggedItemKey != null -> spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
        else -> spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val pullOffset = remember { Animatable(0f) }
    val thresholdPx = with(density) { PULL_THRESHOLD.toPx() }
    // Контейнер индикатора оттяжки в координатах корня: из его круга вырастает Quick Find
    var pullIndicatorBoxBounds by remember { mutableStateOf<Rect?>(null) }
    val maxOffsetPx = with(density) { PULL_MAX_OFFSET.toPx() }

    // Тактильный отклик при пересечении порога активации поиска (100.dp)
    val isPastThreshold = pullOffset.value >= thresholdPx
    var hasTriggeredThresholdHaptic by remember { mutableStateOf(false) }
    LaunchedEffect(isPastThreshold) {
        if (isPastThreshold && !hasTriggeredThresholdHaptic) {
            hasTriggeredThresholdHaptic = true
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
        } else if (!isPastThreshold) {
            hasTriggeredThresholdHaptic = false
        }
    }

    // Извлечение nestedScrollConnection "PullToSearch" в CategoryListState
    val nestedScrollConnection = rememberPullToSearchConnection(
        coroutineScope = coroutineScope,
        pullOffset = pullOffset,
        lazyListState = lazyListState,
        thresholdPx = thresholdPx,
        maxOffsetPx = maxOffsetPx,
        onSearchClick = {
            val circleBounds = pullIndicatorBoxBounds?.let { box ->
                val radius = with(density) { PULL_TO_SEARCH_CIRCLE_RADIUS.toPx() }
                val centerY = box.top + pullToSearchIndicatorTranslationY(pullOffset.value, density) +
                    with(density) { PULL_TO_SEARCH_CIRCLE_CENTER_Y.toPx() }
                Rect(center = Offset(box.center.x, centerY), radius = radius)
            }
            onEvent(ThingsCategoryListEvent.ClickSearch(circleBounds))
        }
    )

    // Насколько заголовок экрана уехал под тулбар, в долях его собственной высоты
    val headerScrollFraction by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex > 0) {
                1f
            } else {
                val headerHeight = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.toFloat() ?: 0f
                if (headerHeight <= 0f) 0f
                else lazyListState.firstVisibleItemScrollOffset / headerHeight
            }
        }
    }
    fun ramp(from: Float, to: Float) = ((headerScrollFraction - from) / (to - from)).coerceIn(0f, 1f)
    val headerScrollAlpha by remember {
        derivedStateOf { 1f - ramp(HEADER_FADE_START_FRACTION, HEADER_FADE_END_FRACTION) }
    }
    val toolbarTitleProgress by remember {
        derivedStateOf { ramp(TOOLBAR_TITLE_START_FRACTION, TOOLBAR_TITLE_END_FRACTION) }
    }
    val toolbarBackgroundProgress by remember {
        derivedStateOf { ramp(HEADER_FADE_START_FRACTION, TOOLBAR_TITLE_END_FRACTION) }
    }

    val isDark = ThingsTheme.colors.isDark
    val topPaddingTotal = TOP_APP_BAR_HEIGHT

    val bkgColor by animateColorAsState(
        targetValue = if (anyExpanded) {
            ThingsTheme.colors.listDim
        } else {
            ThingsTheme.colors.background
        },
        label = "backgroundColor"
    )

    val toolbarHeightPx = with(density) { TOP_APP_BAR_HEIGHT.toPx() }
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density)
    val toolbarExtraMarginPx = with(density) { TOOLBAR_EXTRA_MARGIN.toPx() }
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
                .then(if (screen == ActiveScreen.SEARCH) Modifier else Modifier.nestedScroll(nestedScrollConnection))
                .nestedScroll(scrollTrackingConnection)
                .graphicsLayer { translationY = pullOffset.value }
                .observeTouchesOutsideEditor(editorOutsideTouch) { isEditorOpen }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        editorOutsideTouch.onCollapseRequested()
                        // Клавиатуру прячем первой — в этом же кадре (см. hideSoftKeyboardNow),
                        // не дожидаясь, пока сворачивание редактора пройдёт через цепочку ViewModel.
                        view.hideSoftKeyboardNow()
                        onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                    })
                }
                // [ИЗМЕНЕНИЕ]: Установлен аккуратный отступ 10.dp от края экрана до карточек задач
                .padding(horizontal = ThingsSpacing.S_PLUS)
                .onGloballyPositioned { listCoordinates = it }
                .testTag("tasks_lazy_list"),
            contentPadding = PaddingValues(top = topPaddingTotal, bottom = LIST_BOTTOM_PADDING)
        ) {
            item(key = TaskListKeys.MAIN_HEADER) {
                // Извлеченный подкомпонент заголовка
                Box(
                    modifier = Modifier
                        .padding(horizontal = ThingsSpacing.S)
                        .graphicsLayer { alpha = headerScrollAlpha }
                ) {
                    MainCategoryHeader(
                        screen = screen,
                        project = project,
                        tasks = state.allTasks,
                        area = area,
                        tag = state.tag,
                        scaleFactor = scaleFactor,
                        textPrimaryColor = textPrimaryColor,
                        globalDimAlpha = globalDimAlpha,
                        onDeleteProject = { onEvent(ThingsCategoryListEvent.DeleteProject(it)) },
                        onAddHeading = { addHeading() },
                        onDeleteArea = { onEvent(ThingsCategoryListEvent.DeleteArea(it)) }
                    )
                }
            }

            if (screen == ActiveScreen.SEARCH) {
                item(key = "search_field") {
                    SearchQueryField(
                        autoFocus = !searchFieldAutoFocused,
                        onAutoFocused = { searchFieldAutoFocused = true },
                        query = state.searchQuery,
                        onQueryChange = { onEvent(ThingsCategoryListEvent.ChangeSearchQuery(it)) },
                        textPrimaryColor = textPrimaryColor,
                        textSecondaryColor = textSecondaryColor,
                        modifier = Modifier
                            .padding(horizontal = ThingsSpacing.S)
                            .graphicsLayer { alpha = globalDimAlpha }
                    )
                    Spacer(modifier = Modifier.height(ThingsSpacing.M))
                }
            }

            if (screen == ActiveScreen.TODAY && todayCalendarEvents.isNotEmpty()) {
                item(key = TaskListKeys.CALENDAR_WIDGET) {
                    CalendarEventsWidget(
                        events = todayCalendarEvents,
                        textSecondaryColor = textSecondaryColor,
                        modifier = Modifier
                            .padding(horizontal = ThingsSpacing.S)
                            .graphicsLayer { alpha = globalDimAlpha }
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.calendarBetweenSectionSpacing))
                }
            }

            if (allTags.isNotEmpty() && isTagsFilterVisible) {
                item(key = TaskListKeys.TAG_FILTER) {
                    // Извлеченный подкомпонент строки тегов
                    TagFilterRow(
                        allTags = allTags,
                        selectedTag = selectedTag,
                        textSecondaryColor = textSecondaryColor,
                        dividerColor = dividerColor,
                        onTagSelect = { onEvent(ThingsCategoryListEvent.SelectTag(it)) },
                        modifier = Modifier
                            .padding(horizontal = ThingsSpacing.S)
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
                ActiveScreen.TAG_DETAIL -> {
                    val tagTitle = state.tag?.title ?: ""
                    val tagProjCount = projects.count { !it.trashed && !it.isCompleted && it.status != Item.STATUS_CANCELLED && it.tags.contains(tagTitle) }
                    val tagTasksCount = displayTasks.count { !it.item.trashed && !it.item.isCompleted && it.item.status != Item.STATUS_CANCELLED }
                    val logbookCount = state.allTasks.count { !it.item.trashed && (it.item.isCompleted || it.item.status == Item.STATUS_CANCELLED) && it.item.tags.contains(tagTitle) }
                    tagProjCount > 0 || tagTasksCount > 0 || logbookCount > 0
                }
                ActiveScreen.PROJECT_DETAIL -> displayTasks.isNotEmpty() || localHeadings.isNotEmpty()
                else -> displayTasks.isNotEmpty()
            }

            if (screen == ActiveScreen.SEARCH && flattened.isEmpty()) {
                item(key = TaskListKeys.EMPTY_STATE) {
                    if (state.searchQuery.isBlank()) {
                        SearchEmptyState(
                            icon = Icons.Default.Search,
                            text = stringResource(R.string.ui_quickly_find_to_dos_notes_checklists),
                            textSecondaryColor = textSecondaryColor
                        )
                    } else {
                        SearchEmptyState(
                            icon = Icons.Outlined.SearchOff,
                            text = stringResource(R.string.ui_no_results_for, state.searchQuery),
                            textSecondaryColor = textSecondaryColor
                        )
                    }
                }
            } else if (!hasTasks && screen != ActiveScreen.SEARCH && shownFabSlot == null) {
                item(key = TaskListKeys.EMPTY_STATE) {
                    Box(modifier = Modifier.padding(horizontal = ThingsSpacing.S)) {
                        EmptyStateView(textSecondaryColor = textSecondaryColor)
                    }
                }
            } else {
                items(FabInsertion.withGap(flattened, shownFabSlot), key = { item ->
                    when (item) {
                        is FabGapItem -> item.key
                        is ItemWithChecklist -> item.item.id
                        is Item -> item.id
                        is UpcomingHeaderItem -> "${TaskListKeys.DAY_HEADER_PREFIX}${item.dateMillis}"
                        is UpcomingMonthHeaderItem -> "${TaskListKeys.MONTH_HEADER_PREFIX}${item.monthMillis}"
                        is UpcomingEventItem -> "${TaskListKeys.CALENDAR_EVENT_PREFIX}${item.event.id}_${item.dateMillis}"
                        is SearchSectionHeaderItem -> item.key
                        is SearchAreaItem -> "search_area_${item.area.id}"
                        is SearchTagItem -> "search_tag_${item.tag.id}"
                        is ProjectHeadingItem -> item.key
                        else -> item.toString()
                    }
                }) { item ->
                    when (item) {
                        is ItemWithChecklist -> {
                            AnimatedTaskItem(
                                taskWrapper = item,
                                dateBadge = if (screen == ActiveScreen.UPCOMING) monthTaskBadges[item.item.id] else null,
                                dragDropState = dragDropState,
                                inlineExpandedTaskId = inlineExpandedTaskId,
                                editorOutsideTouch = editorOutsideTouch,
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
                                        swipeWhenOrigin = event.growFromOffset
                                    } else {
                                        onEvent(event)
                                    }
                                },
                                coroutineScope = coroutineScope,
                                screen = screen,
                                upcomingDays = upcomingDays,
                                projectHeadingIds = projectHeadingIds,
                                localTasksList = localTasksList,
                                displayTasks = state.displayTasks,
                                allTasks = state.allTasks,
                                projectProgressMap = state.projectProgressMap,
                                onLocalTasksListChange = { localTasksList = it },
                                lazyListState = lazyListState,
                                onWhenDialogVisibilityChange = { isWhenDialogOpen = it },
                                onTagDialogVisibilityChange = { isTagDialogOpen = it },
                                isSelectionMode = state.isSelectionMode,
                                isSelected = state.selectedTaskIds.contains(item.item.id),
                                isDragSelecting = isDragSelecting,
                                onToggleSelect = { onEvent(ThingsCategoryListEvent.ToggleTaskSelection(item.item.id)) },
                                selectedTaskIds = state.selectedTaskIds,
                                onExitSelectionMode = { onEvent(ThingsCategoryListEvent.ExitSelectionMode) },
                                modifier = if (dragDropState.draggedItemKey == item.item.id) {
                                    Modifier
                                } else {
                                    Modifier.animateItem(
                                        // Новая задача после сброса «+» раскрывается из центра сама (AnimatedTaskItem)
                                        fadeInSpec = if (fabDrag?.freshTaskId == item.item.id) null
                                        else androidx.compose.animation.core.tween(ThingsMotion.STANDARD),
                                        fadeOutSpec = androidx.compose.animation.core.tween(ThingsMotion.STANDARD),
                                        placementSpec = placementSpec
                                    )
                                }
                            )
                        }
                        is Item -> {
                            val task = item
                            if (task.type == Item.TYPE_PROJECT) {
                                // Извлеченный подкомпонент строки проекта
                                ProjectItemRow(
                                    project = task,
                                    tasks = state.displayTasks,
                                    projectProgressMap = state.projectProgressMap,
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
                                    .padding(horizontal = ThingsSpacing.S)
                                    .animateItem(
                                        placementSpec = placementSpec
                                    )
                                    .graphicsLayer { alpha = dimAlpha }
                            )
                        }
                        is UpcomingMonthHeaderItem -> {
                            val header = item
                            val shouldDim = inlineExpandedTaskId != null
                            val dimAlpha by animateFloatAsState(
                                targetValue = if (shouldDim) 0.3f else 1f,
                                label = "dimAlpha_mhdr_${header.monthMillis}"
                            )
                            UpcomingMonthHeader(
                                monthLabel = header.monthLabel,
                                textPrimaryColor = textPrimaryColor,
                                dividerColor = dividerColor,
                                modifier = Modifier
                                    .padding(horizontal = ThingsSpacing.S)
                                    .animateItem(
                                        placementSpec = placementSpec
                                    )
                                    .graphicsLayer { alpha = dimAlpha }
                            )
                        }
                        is SearchSectionHeaderItem -> {
                            val header = item
                            val dimAlpha by animateFloatAsState(
                                targetValue = if (inlineExpandedTaskId != null) 0.3f else 1f,
                                label = "dimAlpha_${header.key}"
                            )
                            SearchSectionHeader(
                                title = header.title,
                                icon = { SearchSectionIcon(header, state.projectProgressMap) },
                                dividerColor = dividerColor,
                                textPrimaryColor = textPrimaryColor,
                                onClick = when {
                                    header.project != null -> { { onEvent(ThingsCategoryListEvent.ClickProject(header.project)) } }
                                    header.area != null -> { { onEvent(ThingsCategoryListEvent.ClickArea(header.area)) } }
                                    else -> null
                                },
                                showChevron = header.project != null || header.area != null,
                                modifier = Modifier
                                    .animateItem(placementSpec = placementSpec)
                                    .graphicsLayer { alpha = dimAlpha }
                            )
                        }
                        is FabGapItem -> {
                            FabGapRow(
                                asHeading = item.asHeading,
                                extraPx = { gapExtra.value },
                                // Строка новой задачи встаёт на это место сразу — промежуток не растворяется
                                modifier = Modifier.animateItem(placementSpec = placementSpec, fadeOutSpec = null)
                            )
                        }
                        is ProjectHeadingItem -> {
                            val heading = item.heading
                            val isDragged = dragDropState.draggedItemKey == item.key
                            val lift by animateFloatAsState(
                                targetValue = if (isDragged && dragDropState.isInteracting) 1f else 0f,
                                label = "headingLift_${heading.id}"
                            )
                            val isLifted = isDragged || lift > 0f
                            val dimAlpha by animateFloatAsState(
                                targetValue = if (inlineExpandedTaskId != null) 0.3f else 1f,
                                label = "dimAlpha_${item.key}"
                            )
                            val chipShape = ThingsTheme.shapes.chipShape
                            Box(
                                modifier = Modifier
                                    .zIndex(if (isLifted) 1f else 0f)
                                    .then(if (!isLifted) Modifier.animateItem(placementSpec = placementSpec) else Modifier)
                                    .padding(top = HEADING_TOP_SPACING, bottom = ThingsSpacing.XS)
                            ) {
                                if (isDragged && dragDropState.isInteracting) {
                                    // Место, куда встанет заголовок с задачами, — та же серая плашка, что
                                    // остаётся на месте задачи при её перетаскивании (см. AnimatedTaskItem)
                                    val placeholderColor = ThingsTheme.colors.dropPlaceholder
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .graphicsLayer { alpha = 0.5f }
                                            .background(placeholderColor, chipShape)
                                    )
                                }
                                if (isLifted) {
                                    // Заголовок едет вместе со своими задачами — под ним стопка карточек,
                                    // как у группы задач в режиме выбора: слой на каждую задачу, не больше двух
                                    val headingTaskCount = localTasksList.count { it.item.headingId == heading.id }
                                    val stackBorder = ThingsTheme.colors.hairline
                                    for (layer in minOf(headingTaskCount, 2) downTo 1) {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .graphicsLayer {
                                                    val offset = if (isDragged) dragDropState.visualDragOffsetY(item.key) else 0f
                                                    val scale = 1f + 0.03f * lift
                                                    translationX = (4 * layer).dp.toPx() * lift
                                                    translationY = offset + (4 * layer).dp.toPx() * lift
                                                    scaleX = scale
                                                    scaleY = scale
                                                    rotationZ = 1.6f * layer * lift
                                                    alpha = lift.coerceIn(0f, 1f)
                                                    shadowElevation = (8 - 2 * layer).dp.toPx() * lift
                                                    shape = chipShape
                                                    clip = true
                                                }
                                                .background(bkgColor, chipShape)
                                                .border(ThingsStroke.HAIRLINE, stackBorder, chipShape)
                                        )
                                    }
                                }
                                ProjectHeadingRow(
                                    heading = heading,
                                    isEditing = editingHeadingId == heading.id,
                                    dividerColor = dividerColor,
                                    onStartEditing = { editingHeadingId = heading.id },
                                    onTitleCommit = { title ->
                                        if (editingHeadingId == heading.id) editingHeadingId = null
                                        when {
                                            // Пустой новый заголовок не нужен, но задачи, ушедшие под него
                                            // при создании, возвращаются в группу выше — не удаляются с ним
                                            title.isEmpty() && heading.title.isEmpty() -> {
                                                val index = localHeadings.indexOfFirst { it.id == heading.id }
                                                val groupAbove = localHeadings.getOrNull(index - 1)?.id
                                                val now = System.currentTimeMillis()
                                                val tasksBack = state.allTasks
                                                    .filter { it.item.headingId == heading.id }
                                                    .map { it.item.copy(headingId = groupAbove, modificationDate = now) }
                                                onEvent(ThingsCategoryListEvent.DiscardHeading(heading, tasksBack))
                                            }
                                            title.isNotEmpty() && title != heading.title ->
                                                onEvent(ThingsCategoryListEvent.SaveHeading(heading.copy(title = title)))
                                        }
                                    },
                                    onArchive = { onEvent(ThingsCategoryListEvent.ArchiveHeading(heading)) },
                                    onDelete = {
                                        val hasTasks = state.allTasks.any { it.item.headingId == heading.id }
                                        if (hasTasks) headingToDelete = heading
                                        else onEvent(ThingsCategoryListEvent.DeleteHeading(heading))
                                    },
                                    modifier = Modifier
                                        .graphicsLayer {
                                            alpha = dimAlpha
                                            translationY = if (isDragged) dragDropState.visualDragOffsetY(item.key) else 0f
                                            val scale = 1f + 0.03f * lift
                                            scaleX = scale
                                            scaleY = scale
                                            shadowElevation = ThingsElevation.CARD.toPx() * lift
                                            shape = chipShape
                                            clip = false
                                        }
                                        .background(if (isLifted) bkgColor else Color.Transparent, chipShape)
                                        .then(
                                            if (editingHeadingId == heading.id) Modifier
                                            else Modifier.headingDragAndDrop(
                                                state = dragDropState,
                                                heading = heading,
                                                headings = localHeadings,
                                                onHeadingsChange = { localHeadings = it },
                                                onDragEnd = { onEvent(ThingsCategoryListEvent.ReorderHeadings(localHeadings)) }
                                            )
                                        )
                                )
                            }
                        }
                        is SearchAreaItem -> {
                            SearchEntityRow(
                                title = item.area.title,
                                icon = {
                                    Icon(
                                        imageVector = AppIcons.Area,
                                        contentDescription = null,
                                        tint = ThingsTheme.colors.area,
                                        modifier = Modifier.requiredSize(SEARCH_ROW_ICON_SIZE)
                                    )
                                },
                                textPrimaryColor = textPrimaryColor,
                                onClick = { onEvent(ThingsCategoryListEvent.ClickArea(item.area)) },
                                modifier = Modifier.animateItem(placementSpec = placementSpec)
                            )
                        }
                        is SearchTagItem -> {
                            SearchEntityRow(
                                title = item.tag.title,
                                icon = {
                                    Icon(
                                        imageVector = AppIcons.Tag,
                                        contentDescription = null,
                                        tint = ThingsTheme.colors.someday,
                                        modifier = Modifier.requiredSize(SEARCH_ROW_ICON_SIZE)
                                    )
                                },
                                textPrimaryColor = textPrimaryColor,
                                onClick = { onEvent(ThingsCategoryListEvent.ClickTag(item.tag)) },
                                modifier = Modifier.animateItem(placementSpec = placementSpec)
                            )
                        }
                        is UpcomingEventItem -> {
                            val event = item.event
                            val shouldDim = inlineExpandedTaskId != null
                            val dimAlpha by animateFloatAsState(
                                targetValue = if (shouldDim) 0.3f else 1f,
                                label = "dimAlpha_ev_${event.id}"
                            )
                            val bottomSpacing = if (item.isLastBeforeTasks) CALENDAR_LAST_EVENT_GAP else ThingsSpacing.NONE
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = ThingsSpacing.S)
                                    .padding(bottom = bottomSpacing)
                                    .animateItem(
                                        placementSpec = placementSpec
                                    )
                                    .graphicsLayer { alpha = dimAlpha }
                            ) {
                                UpcomingCalendarEventRow(
                                    event = event,
                                    textSecondaryColor = textSecondaryColor,
                                    textPrimaryColor = textPrimaryColor,
                                    datePrefix = item.dayOfMonthLabel
                                )
                            }
                        }
                        else -> { // заголовки секций: вечер, проекты сферы, задачи сферы, спейсеры
                            val headerText = item as String
                            if (headerText == TaskListKeys.AREA_PROJECTS_SPACER) {
                                Spacer(
                                    modifier = Modifier
                                        .height(EVENING_HEADER_GAP)
                                        .animateItem(placementSpec = placementSpec)
                                )
                            } else {
                                val shouldDim = inlineExpandedTaskId != null
                                val dimAlpha by animateFloatAsState(
                                    targetValue = if (shouldDim) 0.3f else 1f,
                                    label = "dimAlpha_$headerText"
                                )
                                val isAreaHeader = headerText.startsWith("area_")
                                // Извлеченные подзаголовки разделов в CategoryListPanel
                                SubCategoryHeader(
                                    headerText = headerText,
                                    textPrimaryColor = textPrimaryColor,
                                    textSecondaryColor = textSecondaryColor,
                                    dividerColor = dividerColor,
                                    dimAlpha = dimAlpha,
                                    isLaterItemsHidden = isLaterItemsHidden,
                                    onLaterToggleClick = { isLaterItemsHidden = !isLaterItemsHidden },
                                    modifier = if (isAreaHeader) {
                                        Modifier.animateItem(placementSpec = placementSpec)
                                    } else {
                                        Modifier
                                            .padding(horizontal = ThingsSpacing.S)
                                            .animateItem(placementSpec = placementSpec)
                                    }
                                )
                            }
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
        headingToDelete?.let { heading ->
            val count = state.allTasks.count { it.item.headingId == heading.id }
            ThingsConfirmDialog(
                title = stringResource(R.string.delete_heading_title),
                message = pluralStringResource(R.plurals.delete_heading_message, count, heading.title, count),
                confirmText = stringResource(R.string.delete),
                dismissText = stringResource(R.string.cancel),
                onConfirm = {
                    headingToDelete = null
                    onEvent(ThingsCategoryListEvent.DeleteHeading(heading))
                },
                onDismiss = { headingToDelete = null }
            )
        }

        CategoryListTopAppBar(
            screen = screen,
            onBackClick = { onEvent(ThingsCategoryListEvent.ClickBack) },
            modifier = Modifier.graphicsLayer { translationY = toolbarOffsetY },
            textPrimaryColor = textPrimaryColor,
            textSecondaryColor = textSecondaryColor,
            project = project,
            area = area,
            tag = state.tag,
            tasks = state.allTasks,
            backgroundProgress = toolbarBackgroundProgress,
            titleProgress = toolbarTitleProgress,
            selectionState = TopAppBarSelectionState(
                isSelectionMode = state.isSelectionMode,
                selectedCount = state.selectedTaskIds.size,
                isAllSelected = state.selectedTaskIds.isNotEmpty() && state.selectedTaskIds.size == state.displayTasks.size,
                onCancelSelection = { onEvent(ThingsCategoryListEvent.ExitSelectionMode) },
                onSelectAllClick = { onEvent(ThingsCategoryListEvent.SelectAllTasks) },
                onDeselectAllClick = { onEvent(ThingsCategoryListEvent.DeselectAllTasks) },
                onEnterSelectionMode = { onEvent(ThingsCategoryListEvent.EnterSelectionMode(null)) }
            ),
            hasTags = allTags.isNotEmpty(),
            isTagsFilterVisible = isTagsFilterVisible,
            onTitleClick = { bounds -> onEvent(ThingsCategoryListEvent.ClickSearch(bounds)) },
            onToggleTagsFilter = {
                val newVisible = !isTagsFilterVisible
                isTagsFilterVisible = newVisible
                if (!newVisible && selectedTag != null) {
                    onEvent(ThingsCategoryListEvent.SelectTag(null))
                }
            },
            onAddHeading = if (screen == ActiveScreen.PROJECT_DETAIL) { { addHeading() } } else null
        )

    // PullToSearchIndicator
    if (pullOffset.value > 0f) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { pullOffset.value.toDp() } + PULL_ZONE_EXTRA)
                .offset(y = (-96).dp)
                .onGloballyPositioned { coords ->
                    pullIndicatorBoxBounds = Rect(coords.positionInRoot(), coords.size.toSize())
                },
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
                    delay(ThingsMotion.LONG.toLong())
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
            growFromOffset = swipeWhenOrigin,
            onDismissRequest = {
                val tagsList = targetTask.item.tags

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
                swipeWhenOrigin = null
            }
        )
    }

    FloatingBottomCapsuleToolbar(
        visible = inlineExpandedTaskId != null && activeTask != null && !isWhenDialogOpen && !isTagDialogOpen && swipeWhenTask == null,
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
        // Начальное состояние — общее для выбранных задач: если все они уже в одной секции и с одной
        // датой, диалог показывает её отмеченной (и даёт кнопку «Clear»); если задачи разные —
        // не отмечено ничего. Пока пользователь ничего не выбрал, закрытие крестиком ничего не меняет.
        val batchSelected = remember(state.selectedTaskIds, state.allTasks) {
            state.allTasks.filter { state.selectedTaskIds.contains(it.item.id) }
        }
        var batchStartDate by remember {
            mutableStateOf(batchSelected.map { it.item.startDate }.distinct().singleOrNull())
        }
        var batchSection by remember {
            mutableStateOf(batchSelected.map { it.item.section }.distinct().singleOrNull() ?: TaskSection.ANYTIME)
        }
        var batchIsTonight by remember {
            mutableStateOf(batchSelected.isNotEmpty() && batchSelected.all { it.item.isTonight })
        }
        var batchPicked by remember { mutableStateOf(false) }

        ThingsWhenDialog(
            startDate = batchStartDate,
            onStartDateChange = { batchStartDate = it; batchPicked = true },
            section = batchSection,
            onSectionChange = { batchSection = it; batchPicked = true },
            isTonight = batchIsTonight,
            onIsTonightChange = { batchIsTonight = it; batchPicked = true },
            onShowCalendarHelperChange = { },
            onDismissRequest = {
                if (batchPicked) {
                    onEvent(
                        ThingsCategoryListEvent.BatchScheduleTasks(
                            batchStartDate,
                            batchIsTonight,
                            batchSection
                        )
                    )
                }
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

    // Полоса быстрого свайп-выбора задач (drag-to-select) в режиме множественного выбора
    if (state.isSelectionMode) {
        val currentSelectedIds by rememberUpdatedState(state.selectedTaskIds)
        val currentOnEvent by rememberUpdatedState(onEvent)
        val currentAllTasks by rememberUpdatedState(state.allTasks)
        val currentView = androidx.compose.ui.platform.LocalView.current

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .width(PULL_INDICATOR_WIDTH)
                .padding(top = topPaddingTotal, bottom = LIST_BOTTOM_PADDING)
                .pointerInput(state.isSelectionMode) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)

                        // Поиск задач, пересекающих вертикальный интервал движения пальца [y1, y2]
                        fun findTaskIdsInRange(y1: Float, y2: Float, isMovingDown: Boolean): List<String> {
                            val minY = minOf(y1, y2) - pullOffset.value
                            val maxY = maxOf(y1, y2) - pullOffset.value
                            val items = lazyListState.layoutInfo.visibleItemsInfo
                            val filtered = items.filter { itemInfo ->
                                val top = itemInfo.offset.toFloat()
                                val bottom = top + itemInfo.size.toFloat()
                                top <= maxY && bottom >= minY
                            }
                            val ordered = if (isMovingDown) filtered else filtered.asReversed()
                            return ordered.mapNotNull { hitItem ->
                                val key = hitItem.key as? String ?: return@mapNotNull null
                                if (currentAllTasks.any { it.item.id == key }) key else null
                            }
                        }

                        var lastY = down.position.y
                        val initialHitTasks = findTaskIdsInRange(lastY, lastY, isMovingDown = true)
                        val hitTaskId = initialHitTasks.firstOrNull()

                        if (hitTaskId != null) {
                            down.consume()
                            isDragSelecting = true
                            try {
                                val initialSelected = currentSelectedIds.contains(hitTaskId)
                                val targetSelected = !initialSelected
                                val touchedTaskIds = mutableSetOf<String>()

                                touchedTaskIds.add(hitTaskId)
                                currentView.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                currentOnEvent(ThingsCategoryListEvent.SetTaskSelected(hitTaskId, targetSelected))

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (!change.pressed) break
                                    change.consume()

                                    val currentY = change.position.y
                                    val crossedTaskIds = findTaskIdsInRange(lastY, currentY, isMovingDown = currentY >= lastY)
                                    for (taskId in crossedTaskIds) {
                                        if (touchedTaskIds.add(taskId)) {
                                            currentView.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                            currentOnEvent(ThingsCategoryListEvent.SetTaskSelected(taskId, targetSelected))
                                        }
                                    }
                                    lastY = currentY
                                }
                            } finally {
                                isDragSelecting = false
                            }
                        }
                    }
                }
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

/** Иконка заголовка секции результатов поиска — те же иконки, что у проектов, областей и списков. */
@Composable
private fun SearchSectionIcon(
    header: SearchSectionHeaderItem,
    projectProgressMap: Map<String, ProjectProgress>
) {
    val iconModifier = Modifier.requiredSize(SEARCH_ROW_ICON_SIZE)
    when (header.kind) {
        SearchSectionKind.AREA -> Icon(
            imageVector = AppIcons.Area,
            contentDescription = null,
            tint = ThingsTheme.colors.area,
            modifier = iconModifier
        )
        SearchSectionKind.PROJECT -> {
            val progress = header.project?.let { projectProgressMap[it.id] }
            ProjectProgressArc(
                completed = progress?.completed ?: 0,
                total = progress?.total ?: 0,
                color = ThingsTheme.colors.project,
                modifier = iconModifier
            )
        }
        SearchSectionKind.LOGBOOK -> Icon(
            imageVector = AppIcons.Logbook,
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = iconModifier
        )
    }
}

/** Экраны, на которые можно сбросить кнопку «+» */
private val FAB_DROP_SCREENS = setOf(
    ActiveScreen.INBOX, ActiveScreen.TODAY, ActiveScreen.UPCOMING, ActiveScreen.ANYTIME, ActiveScreen.SOMEDAY,
    ActiveScreen.PROJECT_DETAIL, ActiveScreen.AREA_DETAIL, ActiveScreen.TAG_DETAIL
)

/** Доля ширины списка у левого края, где на экране проекта промежуток превращается в «новый заголовок» */
private const val FAB_HEADING_EDGE_FRACTION = 0.2f

/** Длительность раздвигания промежутка под новую задачу, мс */
private const val FAB_SPREAD_MS = 100

/** Автопрокрутка, пока тянут «+»: зона у краёв и наибольший шаг за кадр */
private const val FAB_SCROLL_ZONE_FRACTION = 0.12f
private const val FAB_SCROLL_MAX_STEP = 28f

/** Ключ элемента плоского списка — тот же, что у строк LazyColumn */
private fun listItemKey(element: Any): Any? = when (element) {
    is ItemWithChecklist -> element.item.id
    is Item -> element.id
    is ProjectHeadingItem -> element.key
    is UpcomingHeaderItem -> "${TaskListKeys.DAY_HEADER_PREFIX}${element.dateMillis}"
    is UpcomingMonthHeaderItem -> "${TaskListKeys.MONTH_HEADER_PREFIX}${element.monthMillis}"
    is String -> element
    else -> null
}

/** Промежуток на месте пальца: серая плашка задачи или пунктир «NEW HEADING» */
@Composable
private fun FabGapRow(asHeading: Boolean, extraPx: () -> Float, modifier: Modifier = Modifier) {
    if (asHeading) {
        val lineColor = ThingsTheme.colors.newHeadingLine
        // Вместо серой плашки будущей задачи: та же высота 44 dp и те же отступы, пунктир — по её
        // вертикальному центру, надпись — прямо над ним
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(FAB_GAP_HEIGHT)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = lineColor,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                    strokeWidth = ThingsStroke.BOLD.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(18f, 12f))
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(NEW_HEADING_TEXT_ZONE),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = stringResource(R.string.ui_new_heading),
                    style = ThingsTheme.type.overline.copy(color = ThingsTheme.colors.newHeadingText),
                    modifier = Modifier.padding(bottom = NEW_HEADING_TEXT_BOTTOM)
                )
            }
        }
    } else {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val baseHeightPx = with(density) { FAB_GAP_HEIGHT.roundToPx() }
        Box(
            modifier = modifier
                .fillMaxWidth()
                // Высота читается только при раскладке: раздвигание не пересобирает список
                .layout { measurable, constraints ->
                    val height = baseHeightPx + extraPx().toInt()
                    val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                    layout(placeable.width, height) { placeable.place(0, 0) }
                },
            contentAlignment = Alignment.Center
        ) {
            // Та же серая плашка, что остаётся на месте задачи при обычном перетаскивании
            // (см. AnimatedTaskItem): тот же цвет, полупрозрачность и скругление
            val plateColor = ThingsTheme.colors.dropPlaceholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FAB_GAP_HEIGHT)
                    // Плашка тает по мере раздвигания: на её месте раскроется задача
                    .graphicsLayer { alpha = 0.5f * (1f - extraPx() / (baseHeightPx * 3f)).coerceIn(0f, 1f) }
                    .background(plateColor, ThingsTheme.shapes.rowShape)
            )
        }
    }
}

/** Завтра, 12:00 — дата для задачи, брошенной в секцию «Планы» на экране области */
private fun tomorrowNoonMillis(): Long = java.util.Calendar.getInstance().run {
    add(java.util.Calendar.DAY_OF_YEAR, 1)
    set(java.util.Calendar.HOUR_OF_DAY, 12)
    set(java.util.Calendar.MINUTE, 0)
    set(java.util.Calendar.SECOND, 0)
    set(java.util.Calendar.MILLISECOND, 0)
    timeInMillis
}
