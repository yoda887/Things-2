package com.example.ui.screens.home.components

import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.ui.layout.findRootCoordinates
import com.example.ui.screens.home.subcomponents.EXPANDED_BOTTOM_GAP
import com.example.ui.screens.home.subcomponents.EXPANDED_TOP_LIMIT
import com.example.domain.lists.HomeList
import com.example.domain.lists.AllProjectsList
import com.example.domain.lists.PlaceGroups
import com.example.domain.lists.HeadingOrder
import com.example.domain.edits.ProjectEdit
import com.example.domain.edits.DropSpot
import com.example.ui.viewmodel.ProjectProgress
import com.example.ui.viewmodel.ThingsCategoryListState
import com.example.ui.viewmodel.ActiveScreen
import com.example.domain.lists.TagList
import com.example.domain.lists.AreaList
import com.example.domain.lists.LogbookList
import com.example.domain.lists.SomedayList
import com.example.domain.lists.TodayList
import com.example.ui.screens.home.subcomponents.projectShareText
import com.example.ui.screens.home.subcomponents.ProjectDialogs
import com.example.ui.screens.home.subcomponents.ProjectDetailsBlock
import com.example.ui.screens.home.subcomponents.ProjectAction
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
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.gestures.animateScrollBy
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
import com.example.ui.theme.ThingsElevation
import com.example.ui.components.dragdrop.universalDragAndDrop
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
import com.example.ui.screens.home.subcomponents.LogbookSectionHeader
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
    val textPrimaryColor = ThingsTheme.colors.textPrimary
    val textSecondaryColor = ThingsTheme.colors.textSecondary
    val dividerColor = ThingsTheme.colors.divider
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
    // Несохранённые дата и напоминание открытого редактора — окну «Повторять» поверх него
    val editorLiveFields = remember { com.example.ui.screens.home.inlineeditor.utils.EditorLiveFields() }
    
    var localTasksList by remember { mutableStateOf(state.displayTasks) }
    val currentDisplayTasks by rememberUpdatedState(state.displayTasks)
    val currentAllTasks by rememberUpdatedState(state.allTasks)
    // Флаг: идёт ли анимация сворачивания редактора (300 мс)
    var isEditorCollapsing by remember { mutableStateOf(false) }
    // Предыдущий ID развёрнутой задачи для обнаружения момента сворачивания
    var previousExpandedId by remember { mutableStateOf(inlineExpandedTaskId) }

    // Задача в открытом редакторе держит место в списке до сворачивания: правки (день, «Сегодня вечером»,
    // проект, правило повтора…) сохраняются сразу, но список раскладывается по её снимку, снятому при
    // раскрытии (у новой задачи — когда она появилась в списке). Сам редактор показывает живые данные
    var frozenEdited by remember { mutableStateOf<ItemWithChecklist?>(null) }
    var frozenRule by remember { mutableStateOf<com.example.data.model.RecurrenceRule?>(null) }
    val currentRepeatRules by rememberUpdatedState(state.repeatRules)
    // Новое в «Сегодня»: плашка вверху списка, жёлтые точки — в строках задач и проектов
    val todayNew = com.example.ui.components.LocalTodayNew.current
    // «Входящие»: ушедшие задачи под плашкой — их id (строки бледные, пока свёрнуты)
    val inboxGhostIds = remember(state.inboxGhosts) { state.inboxGhosts.mapTo(HashSet()) { it.item.id } }

    fun freeze(id: String?) {
        val snapshot = id?.let { editedId -> currentDisplayTasks.firstOrNull { it.item.id == editedId } }
        frozenEdited = snapshot
        frozenRule = snapshot?.let { currentRepeatRules[it.item.id] }
    }

    /** Список для раскладки: живой, но задача из редактора — снимком и на прежнем месте */
    fun layoutOf(fresh: List<ItemWithChecklist>): List<ItemWithChecklist> {
        val frozen = frozenEdited ?: return fresh
        val freshIndex = fresh.indexOfFirst { it.item.id == frozen.item.id }
        // Задачу удалили — уходит сразу
        if (freshIndex == -1) return fresh
        val others = fresh.filterIndexed { index, _ -> index != freshIndex }
        val oldIndex = localTasksList.indexOfFirst { it.item.id == frozen.item.id }
        val at = (if (oldIndex >= 0) oldIndex else freshIndex).coerceAtMost(others.size)
        return others.toMutableList().apply { add(at, frozen) }
    }

    // Обнаружение момента сворачивания редактора и задержка удаления задачи из списка
    LaunchedEffect(inlineExpandedTaskId) {
        val wasExpanded = previousExpandedId != null
        previousExpandedId = inlineExpandedTaskId
        if (wasExpanded && inlineExpandedTaskId == null) {
            // Редактор только что начал сворачиваться — блокируем удаление задач из списка на 300 мс
            isEditorCollapsing = true
            delay(com.example.ui.theme.AnimationConstants.TASK_EXPANSION_DURATION_MS)
            isEditorCollapsing = false
            // После завершения анимации — полная синхронизация списка с ViewModel: только теперь
            // отредактированная задача переезжает туда, куда её перенесли
            frozenEdited = null
            frozenRule = null
            localTasksList = currentDisplayTasks
        } else {
            isEditorCollapsing = false
            freeze(inlineExpandedTaskId)
            localTasksList = layoutOf(currentDisplayTasks)
        }
    }

    // Синхронизация localTasksList с displayTasks из ViewModel
    LaunchedEffect(state.displayTasks) {
        // Новая задача раскрывается раньше, чем появляется в списке, — снимок снимается при появлении
        if (frozenEdited == null && inlineExpandedTaskId != null) freeze(inlineExpandedTaskId)
        if (isEditorCollapsing) {
            // Во время сворачивания: обновляем ДАННЫЕ задач (новый заголовок и т.д.),
            // но НЕ УДАЛЯЕМ задачи из списка (чтобы анимация сворачивания не прерывалась);
            // отредактированная задача остаётся снимком до конца сворачивания
            localTasksList = localTasksList.map { localTask ->
                if (localTask.item.id == frozenEdited?.item?.id) localTask
                else currentDisplayTasks.find { it.item.id == localTask.item.id } ?: localTask
            }
        } else {
            localTasksList = layoutOf(state.displayTasks)
        }
    }
    // Остальные данные раскладки — тоже со снимком задачи из редактора: её правило повтора («Повторяющиеся»)
    // и она среди всех задач (Logbook тега, иначе выполненная из редактора задача встала бы в нём второй раз)
    val layoutRepeatRules = remember(state.repeatRules, frozenEdited, frozenRule) {
        val frozen = frozenEdited ?: return@remember state.repeatRules
        state.repeatRules.toMutableMap().apply {
            val rule = frozenRule
            if (rule != null) put(frozen.item.id, rule) else remove(frozen.item.id)
        }
    }
    val layoutAllTasks = remember(state.allTasks, frozenEdited) {
        val frozen = frozenEdited ?: return@remember state.allTasks
        state.allTasks.map { if (it.item.id == frozen.item.id) frozen else it }
    }

    // Плашка «Входящих» с ушедшими задачами живёт, пока открыт экран: при входе и при уходе с него она снимается,
    // как по «OK»
    if (screen == ActiveScreen.INBOX) {
        val currentOnEvent by rememberUpdatedState(onEvent)
        DisposableEffect(Unit) {
            currentOnEvent(ThingsCategoryListEvent.DismissInboxMoved)
            onDispose { currentOnEvent(ThingsCategoryListEvent.DismissInboxMoved) }
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
        val headingId = java.util.UUID.randomUUID().toString()
        editingHeadingId = headingId
        onEvent(ThingsCategoryListEvent.AddHeading(headingId, currentProject.id, localHeadings))
    }

    val view = androidx.compose.ui.platform.LocalView.current

    var showMoveDialog by remember { mutableStateOf(false) }
    // Окно «Повторять» для раскрытой задачи
    var showRepeatDialog by remember { mutableStateOf(false) }
    // Окно «Повторять» для проекта экрана: сделать повторяющимся или изменить правило шаблона
    var repeatDialogProject by remember { mutableStateOf<Item?>(null) }
    // Перенос повторяющейся задачи по расписанию: id шаблона и новый день появления — ждёт выбора
    // «Исключение» или «Обновить правило»
    var repeatMove by remember { mutableStateOf<Pair<String, Long>?>(null) }

    /**
     * Перенести ближайший повтор шаблона [templateId] на день [day], как в Things: «после выполнения» — сразу,
     * регулярный — после выбора «Исключение» / «Обновить правило». False — переносить нечего (нет правила).
     */
    fun requestRepeatMove(templateId: String, day: Long): Boolean {
        val rule = state.repeatRules[templateId]?.let { com.example.domain.repeat.RepeatRule.decode(it.rrule) } ?: return false
        if (rule.afterCompletion) {
            onEvent(ThingsCategoryListEvent.MoveRepeat(templateId, day, updateRule = false))
        } else {
            repeatMove = templateId to day
        }
        return true
    }
    // Поле поиска забирает фокус один раз за показ экрана, а не при каждом возвращении в видимую часть списка
    var searchFieldAutoFocused by rememberSaveable(screen) { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isWhenDialogOpen by remember { mutableStateOf(false) }
    // Окно тегов редактора: пока оно открыто, нижняя панель действий над задачей спрятана
    var isTagDialogOpen by remember { mutableStateOf(false) }
    // Открытое окно из меню опций проекта («Когда», теги, дедлайн, перемещение, завершение)
    var projectDialog by remember { mutableStateOf<ProjectAction?>(null) }
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

    val todayCalendarEvents = remember(calendarEvents) { TodayList.events(calendarEvents) }

    // Извлечение состояния предстоящих дней (UpcomingDays) и месяцев
    // Проекты, которые тянут в «Планах» (UpcomingDragController), — с новым днём и порядком, пока база их не вернёт
    val upcomingProjectOverrides = remember { androidx.compose.runtime.mutableStateMapOf<String, Item>() }
    val overridesNow = upcomingProjectOverrides.toMap()
    val upcomingProjects = remember(projects, overridesNow) {
        if (overridesNow.isEmpty()) projects else projects.map { overridesNow[it.id] ?: it }
    }
    val upcomingSchedule = rememberUpcomingSchedule(localTasksList, calendarEvents, upcomingProjects)
    val upcomingDays = upcomingSchedule.days
    val upcomingMonths = upcomingSchedule.months
    val monthTaskBadges = upcomingSchedule.monthTaskBadges

    val standardToday = remember(localTasksList) { TodayList.day(localTasksList) }
    val eveningToday = remember(localTasksList) { TodayList.evening(localTasksList) }

    val displayTasks = localTasksList
    
    var isLaterItemsHidden by rememberSaveable { mutableStateOf(false) }
    // Число объектов под плашкой «пізніші» на экране области — то, что она прячет
    val areaLaterCount = remember(screen, area?.id, state.displayTasks, projects, state.allTasks, state.selectedTagFilter) {
        if (screen != ActiveScreen.AREA_DETAIL) 0
        else AreaList.content(area?.id, state.displayTasks, projects, templates = state.allTasks.map { it.item }, tagFilter = state.selectedTagFilter).let {
            it.upcoming.size + it.someday.size + it.upcomingProjects.size + it.somedayProjects.size
        }
    }
    var isTagsFilterVisible by remember(screen, project?.id, area?.id) { mutableStateOf(false) }

    // Извлечение выравнивания плоского списка для LazyColumn
    // Задача в открытом редакторе стоит в своей группе до закрытия: место запоминается, когда она раскрылась
    // (у новой задачи — когда появилась в списке), перенос в другой проект или область сдвинет её только потом
    val editedTaskShown = inlineExpandedTaskId != null && state.displayTasks.any { it.item.id == inlineExpandedTaskId }
    val editedTaskPlace = remember(inlineExpandedTaskId, editedTaskShown) {
        state.displayTasks.firstOrNull { it.item.id == inlineExpandedTaskId }?.item?.let { PlaceGroups.Place(it.projectId, it.areaId) }
    }
    val pinnedPlaces = if (inlineExpandedTaskId != null && editedTaskPlace != null) mapOf(inlineExpandedTaskId to editedTaskPlace) else emptyMap()
    // «Все проекты»: раскрытые группы поздних проектов и показ завершённых — пока открыт экран
    var expandedLaterProjects by remember(screen) { mutableStateOf(emptySet<String>()) }
    var showLoggedProjects by remember(screen) { mutableStateOf(false) }
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
        allTasks = layoutAllTasks,
        areas = areasState,
        savedTags = allSavedTagObjects,
        searchQuery = state.searchQuery,
        // Проект — его подзаголовки; поиск — все (находит их по названию)
        headings = when (screen) {
            ActiveScreen.PROJECT_DETAIL -> localHeadings
            ActiveScreen.SEARCH -> state.allHeadings
            else -> emptyList()
        },
        pinnedPlaces = pinnedPlaces,
        expandedLaterProjects = expandedLaterProjects,
        showLoggedProjects = showLoggedProjects,
        repeatRules = layoutRepeatRules,
        inboxGhosts = state.inboxGhosts,
        todayNewCount = if (screen == ActiveScreen.TODAY) todayNew.ids.size else 0,
        todayGroupedDay = screen == ActiveScreen.TODAY && com.example.ui.components.LocalTodayGrouped.current
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
    // Нажатие на «+» уже выполняется (список докручивается к началу) — повторное не начинает второе
    var fabTapRunning by remember { mutableStateOf(false) }
    // Промежуток от нажатия на «+»: растёт от нуля, продолжая движение прокрутки (см. tapFab), строки под ним
    // идут за его высотой без своей анимации
    var fabTapGap by remember { mutableStateOf(false) }
    // Клавиатура для новой задачи от «+» выезжает сразу, вместе с промежутком, как в Things: фокус сначала в
    // невидимом поле, потом переходит в название раскрывшейся задачи — клавиатура при этом не прячется.
    // Поле — обычный EditText: в отличие от поля Compose, он сразу сообщает клавиатуре заглавную в начале
    var imePrimerView by remember { mutableStateOf<android.widget.EditText?>(null) }
    fun showImePrimer() {
        val view = imePrimerView ?: return
        view.requestFocus()
        val imm = view.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }
    val spreadScope = rememberCoroutineScope()
    val currentFlattened by rememberUpdatedState(flattened)
    val currentUpcomingProjects by rememberUpdatedState(upcomingProjects)
    // Перетаскивание задач и проектов в днях «Планов» — общий порядок дня, перенос в другой день
    val upcomingDrag = remember(screen) {
        if (screen != ActiveScreen.UPCOMING) null else UpcomingDragController(
            rows = { currentFlattened },
            keyOf = { listItemKey(it) },
            tasks = { localTasksList },
            setTasks = { localTasksList = it },
            projects = { currentUpcomingProjects },
            projectOverrides = upcomingProjectOverrides
        )
    }
    LaunchedEffect(projects) { upcomingDrag?.onProjectsLoaded(projects) }
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

    /** Сброс кнопки «+» на промежуток: задача на его месте, у левого края проекта — заголовок */
    fun dropFab(
        focusTitle: Boolean = false,
        tapGrowMs: Int? = null,
        presetTaskId: String? = null,
        tapGrowEasing: androidx.compose.animation.core.Easing = FAB_TAP_GROW_EASING
    ): Boolean {
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
            val headingId = java.util.UUID.randomUUID().toString()
            val moved = currentLocalTasks.filter { it.item.id in placement.movedTaskIds }.map { it.item }
            editingHeadingId = headingId
            onEvent(ThingsCategoryListEvent.InsertHeadingAt(headingId, currentProject.id, localHeadings, placement.headingIndex, moved))
        } else {
            val tasks = currentLocalTasks
            val placement = FabInsertion.taskPlacement(flat, slot.index, tasks)
            val newTaskId = presetTaskId ?: java.util.UUID.randomUUID().toString()
            // Где оказалась задача — поля по этому месту назначит домен (NewTasks.atDrop)
            val spot = DropSpot(
                headingId = placement.headingId,
                isTonight = placement.isTonight,
                startDate = placement.startDate,
                areaSection = when {
                    placement.inAreaSomeday -> AreaList.Section.SOMEDAY
                    placement.inAreaUpcoming -> AreaList.Section.UPCOMING
                    else -> null
                },
                groupProject = placement.placeHeader?.project,
                groupAreaId = placement.placeHeader?.area?.id
            )
            val place = newTaskPlace(screen, project?.id, area?.id, state.tag?.title)
            val commit: () -> Unit = {
                onEvent(ThingsCategoryListEvent.CreateTaskAt(newTaskId, place, spot, placement.taskIndex, tasks.map { it.item }))
            }
            // Порядок: 1) задачи сверху и снизу раздвигаются на высоту раскрытой задачи, 2) в центре
            // освободившегося места раскрывается новая задача (см. AnimatedTaskItem)
            pendingFabTaskId = newTaskId
            spreadScope.launch {
                gapExtra.snapTo(0f)
                gapExtra.animateTo(
                    (finalGapHeight - gapHeightNow).coerceAtLeast(0f),
                    // Нажатие на «+» — промежуток подхватывает скорость прокрутки и плавно останавливается
                    if (tapGrowMs != null) androidx.compose.animation.core.tween(tapGrowMs, easing = tapGrowEasing)
                    else androidx.compose.animation.core.tween(FAB_SPREAD_MS, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                )
                // Нажатие на «+»: задача появляется под конец полёта кнопки — вырастает из неё, пока она гаснет сверху
                if (tapGrowMs != null) {
                    kotlinx.coroutines.withTimeoutOrNull(FAB_TAP_LANDING_WAIT_MS) {
                        snapshotFlow {
                            val fab = fabDrag
                            fab == null || (!fab.tapPending && (!fab.isSettling || fab.settleProgress >= FAB_TAP_CARD_AT_FLIGHT))
                        }.first { it }
                    }
                }
                fabDrag?.freshTaskId = newTaskId
                // Новая задача от нажатия на «+» — сразу с курсором в названии и клавиатурой
                if (focusTitle) fabDrag?.focusTaskId = newTaskId
                commit()
                // Промежуток уйдёт в том же кадре, в котором в списке появится новая строка
                kotlinx.coroutines.withTimeoutOrNull(800) {
                    snapshotFlow { localTasksList.any { it.item.id == newTaskId } }.first { it }
                }
                withFrameNanos { }
                fabSlot = null
                fabTapGap = false
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

    /**
     * Нажатие на «+», как в Things: длинный список сначала плавно докручивается к началу — к месту будущей задачи;
     * как только место в кадре, на нём раскрывается промежуток, кнопка влетает в его центр из угла, и оттуда
     * раскрывается новая задача с курсором в названии. Этапы идут друг за другом без пауз
     */
    fun tapFab(): Boolean {
        if (screen !in FAB_TAP_SCREENS || state.isSelectionMode || inlineExpandedTaskId != null || fabTapRunning) return false
        fabTapRunning = true
        spreadScope.launch {
            try {
                // Место будущей задачи — первой строкой списка; промежуток там пока нулевой высоты, его не видно
                val flat = currentFlattened
                // Над группами проектов и областей («Сегодня» по проектам, тег): новая задача — в самом верху, без
                // проекта и области, а не первой задачей первой группы
                val first = flat.indexOfFirst { FabInsertion.isAnchor(it) || it is SearchSectionHeaderItem }
                fabTapGap = true
                fabSlot = FabInsertion.normalizeSlot(flat, FabSlot(if (first >= 0) first else flat.size, asHeading = false))
                // Промежуток вырастет до высоты раскрытой задачи H за FAB_TAP_GROW_MS = T, начиная со скорости 2H/T
                // (FAB_TAP_GROW_EASING), — с этой скоростью прокрутка и заканчивается: она резко обрывается, а
                // строки под промежутком продолжают движение и плавно останавливаются
                val growHeight = fabDrag?.expandedRowHeightPx ?: fabDefaultGapPx
                val handoffPxPerMs = 2f * growHeight / FAB_TAP_GROW_MS
                // Список и так у начала (короткий) — промежуток растёт с места, плавно разгоняясь и останавливаясь
                val scrolls = lazyListState.firstVisibleItemIndex != 0 || lazyListState.firstVisibleItemScrollOffset != 0
                fabDrag?.tapPending = true
                scrollListToTop(lazyListState, endPxPerMs = handoffPxPerMs)
                // Список у начала — промежуток тут же растёт, и вместе с ним, как в Things, открывается редактор новой
                // задачи: остальное гаснет, выезжает клавиатура
                val newTaskId = java.util.UUID.randomUUID().toString()
                onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(newTaskId))
                showImePrimer()
                var anchor: androidx.compose.ui.geometry.Offset? = null
                var frames = 0
                while (true) {
                    val info = lazyListState.layoutInfo
                    val gapInfo = info.visibleItemsInfo.firstOrNull { it.key == FabGapItem.KEY }
                    val coords = listCoordinates
                    if (gapInfo != null && coords != null && coords.isAttached) {
                        val origin = coords.positionInRoot()
                        val gapTop = gapInfo.offset - info.viewportStartOffset + lazyListState.firstVisibleItemScrollOffset
                        anchor = androidx.compose.ui.geometry.Offset(origin.x + coords.size.width / 2f, origin.y + gapTop + fabDefaultGapPx / 2f)
                        break
                    }
                    if (frames++ > FAB_TAP_GAP_WAIT_FRAMES) break
                    withFrameNanos { }
                }
                val growEasing = if (scrolls) FAB_TAP_GROW_EASING else androidx.compose.animation.core.FastOutSlowInEasing
                if (!dropFab(focusTitle = true, tapGrowMs = FAB_TAP_GROW_MS, presetTaskId = newTaskId, tapGrowEasing = growEasing)) {
                    fabSlot = null
                    fabTapGap = false
                    fabDrag?.tapPending = false
                    onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                    imePrimerView?.clearFocus()
                    return@launch
                }
                // Без строки впереди (пустой конец списка) — летит туда, где промежуток встал при сбросе
                val target = anchor ?: fabDrag?.dropAnchor
                fabDrag?.dropAnchor = null
                // Кнопка взлетает чуть позже промежутка — садится, когда он раскрылся, и из неё вырастает задача
                launch {
                    delay(FAB_TAP_FLIGHT_DELAY_MS)
                    target?.let { fabDrag?.settleFromHome(it) }
                    fabDrag?.tapPending = false
                }
            } finally {
                fabTapRunning = false
                if (fabDrag?.isSettling != true && fabSlot == null) fabDrag?.tapPending = false
            }
        }
        return true
    }

    if (fabDrag != null && acceptsFabDrop) {
        DisposableEffect(fabDrag) {
            val target = object : FabDropTarget {
                override fun onFabDrop(): Boolean = dropFab()
                override fun onFabTap(): Boolean = tapFab()
            }
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
                            // Подзаголовок проекта, найденный поиском
                            is ProjectHeadingItem -> item.heading.id == highlightedTaskId
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

    // Пока задача раскрыта, прокрутка не уводит её карточку из видимой зоны — той же, в которую карточку
    // ставит раскрытие: от тулбара до плашки действий (или клавиатуры). Карточка ниже зоны видна целиком
    // и не уезжает; карточка выше зоны прокручивается только в своих границах, от верха до низа.
    // Движение, которое возвращает карточку в зону, разрешено всегда
    val currentExpandedId by rememberUpdatedState(inlineExpandedTaskId)
    val imeInsetsForClamp = WindowInsets.ime
    val navBarInsetsForClamp = WindowInsets.navigationBars
    val cardTopLimitPx = WindowInsets.statusBars.getTop(density) + with(density) { EXPANDED_TOP_LIMIT.toPx() }
    val cardBottomReservePx = with(density) {
        (MaterialTheme.dimens.floatingToolbarBottomPadding + MaterialTheme.dimens.floatingToolbarHeight + EXPANDED_BOTTOM_GAP).toPx()
    }
    val cardGapPx = with(density) { MaterialTheme.dimens.taskExpandedVerticalGap.toPx() }
    val currentCardLimits by rememberUpdatedState(Triple(cardTopLimitPx, cardBottomReservePx, cardGapPx))
    val expandedCardScrollClamp = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val id = currentExpandedId ?: return Offset.Zero
                val coords = listCoordinates?.takeIf { it.isAttached } ?: return Offset.Zero
                val layoutInfo = lazyListState.layoutInfo
                val info = layoutInfo.visibleItemsInfo.firstOrNull { it.key == id } ?: return Offset.Zero
                val (topLimit, bottomReserve, gap) = currentCardLimits
                val rootHeight = coords.findRootCoordinates().size.height.toFloat()
                // Карточка — строка без отступов раскрытия сверху и снизу, в координатах экрана
                val cardTop = coords.positionInRoot().y + (info.offset - layoutInfo.viewportStartOffset) + gap
                val cardHeight = info.size - 2 * gap
                val zoneBottom = minOf(
                    rootHeight - imeInsetsForClamp.getBottom(density),
                    rootHeight - navBarInsetsForClamp.getBottom(density) - bottomReserve
                )
                val lo = minOf(topLimit, zoneBottom - cardHeight)
                val hi = maxOf(topLimit, zoneBottom - cardHeight)
                val dy = available.y
                val allowed = if (dy > 0f) minOf(dy, maxOf(0f, hi - cardTop)) else maxOf(dy, minOf(0f, lo - cardTop))
                return Offset(0f, dy - allowed)
            }
        }
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
        // Промежуток от нажатия на «+» растёт, продолжая прокрутку: строки под ним идут ровно за его высотой
        fabTapGap -> null
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
                // Первым в цепочке: ограничение раскрытой карточкой забирает лишнее раньше оттяжки поиска
                .nestedScroll(expandedCardScrollClamp)
                // Оттяжка к Quick Find — только без раскрытой задачи: иначе прокрутка вниз у начала списка
                // открывала поиск и закрывала редактор
                .then(if (screen == ActiveScreen.SEARCH || anyExpanded) Modifier else Modifier.nestedScroll(nestedScrollConnection))
                .nestedScroll(scrollTrackingConnection)
                .graphicsLayer {
                    translationY = pullOffset.value
                }
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
                        onDeleteArea = { onEvent(ThingsCategoryListEvent.DeleteArea(it)) },
                        onRenameProject = { proj, title -> onEvent(ThingsCategoryListEvent.EditProject(proj, ProjectEdit.Rename(title))) },
                        onRenameArea = { a, title -> onEvent(ThingsCategoryListEvent.RenameArea(a, title)) },
                        onProjectAction = { projectAction ->
                            val currentProject = project ?: return@MainCategoryHeader
                            when (projectAction) {
                                ProjectAction.DUPLICATE -> onEvent(ThingsCategoryListEvent.DuplicateProject(currentProject))
                                ProjectAction.REPEAT -> repeatDialogProject = currentProject
                                ProjectAction.SHARE -> {
                                    val sendIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, projectShareText(currentProject, state.headings, state.allTasks))
                                        type = "text/plain"
                                    }
                                    context.startActivity(android.content.Intent.createChooser(sendIntent, null))
                                }
                                // Без открытых задач подтверждать нечего — проект завершается сразу
                                ProjectAction.COMPLETE -> if (state.allTasks.none { it.item.projectId == currentProject.id && it.item.status == Item.STATUS_OPEN }) {
                                    onEvent(ThingsCategoryListEvent.CompleteProject(currentProject))
                                } else {
                                    projectDialog = projectAction
                                }
                                else -> projectDialog = projectAction
                            }
                        }
                    )
                }
            }

            // Свойства проекта под названием: теги, «Когда», дедлайн, заметки
            if (screen == ActiveScreen.PROJECT_DETAIL && project != null) {
                item(key = TaskListKeys.PROJECT_DETAILS) {
                    ProjectDetailsBlock(
                        project = project,
                        textPrimaryColor = textPrimaryColor,
                        onAction = { projectDialog = it },
                        onNotesChange = { notes ->
                            onEvent(ThingsCategoryListEvent.EditProject(project, ProjectEdit.Notes(notes)))
                        },
                        onRepeatAction = { action ->
                            if (action == RepeatAction.EDIT) repeatDialogProject = project
                            else onEvent(ThingsCategoryListEvent.RepeatTemplateAction(project.id, action))
                        },
                        onOpenRepeatTemplate = { project.repeatTemplateId?.let { onEvent(ThingsCategoryListEvent.OpenRepeatTemplate(it)) } },
                        modifier = Modifier
                            .padding(horizontal = ThingsSpacing.S)
                            .graphicsLayer { alpha = headerScrollAlpha * globalDimAlpha }
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

            // Все экраны перечислены явно (без else): новый экран компилятор заставит указать и здесь
            val hasTasks = when (screen) {
                ActiveScreen.TODAY -> !TodayScreen.isEmpty(standardToday, eveningToday, isDragging = dragDropState.draggedItemKey != null, projects = com.example.domain.lists.TodayList.projects(projects))
                ActiveScreen.UPCOMING -> !UpcomingScreen.isEmpty(upcomingSchedule)
                ActiveScreen.AREA_DETAIL -> !AreaList.content(area?.id, displayTasks, projects, templates = state.allTasks.map { it.item }).isEmpty
                ActiveScreen.LOGBOOK -> LogbookList.entries(displayTasks, projects).isNotEmpty()
                ActiveScreen.TAG_DETAIL -> !TagList.content(state.tag?.title, displayTasks, state.allTasks, projects, areasState).isEmpty
                ActiveScreen.PROJECT_DETAIL -> !ProjectScreen.isEmpty(displayTasks, localHeadings)
                ActiveScreen.ANYTIME -> displayTasks.isNotEmpty()
                // «Когда-нибудь» показывает и отложенные проекты — без задач экран не пуст
                ActiveScreen.SOMEDAY -> SomedayList.hasContent(displayTasks, projects)
                // Поиск показывает свою пустую заглушку отдельно; у главного экрана списка нет
                // «Входящие» не пусты, пока над ними плашка с ушедшими задачами
                ActiveScreen.INBOX -> displayTasks.isNotEmpty() || state.inboxGhosts.isNotEmpty()
                ActiveScreen.SEARCH, ActiveScreen.HOME -> displayTasks.isNotEmpty()
                ActiveScreen.ALL_PROJECTS -> AllProjectsList.hasContent(AllProjectsList.content(projects, areasState, templates = state.allTasks.map { it.item }))
                ActiveScreen.LOGGED_PROJECTS -> projects.any { LogbookList.includesProject(it) }
                ActiveScreen.LATER_PROJECTS -> AllProjectsList.content(projects, areasState, templates = state.allTasks.map { it.item })
                    .groups.any { it.area == null && it.later.isNotEmpty() }
                ActiveScreen.REPEATING -> displayTasks.isNotEmpty()
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
                        is LogbookHeaderItem -> item.key
                        is ListToggleItem -> item.key
                        is UpcomingEventItem -> "${TaskListKeys.CALENDAR_EVENT_PREFIX}${item.event.id}_${item.dateMillis}"
                        is SearchSectionHeaderItem -> item.key
                        is SearchAreaItem -> "search_area_${item.area.id}"
                        is SearchTagItem -> "search_tag_${item.tag.id}"
                        is SearchHeadingItem -> "search_heading_${item.heading.id}"
                        is InboxMovedBannerItem -> TaskListKeys.INBOX_MOVED_BANNER
                        is TodayNewBannerItem -> TaskListKeys.TODAY_NEW_BANNER
                        is UpcomingRepeatingHeaderItem -> TaskListKeys.UPCOMING_REPEATING_HEADER
                        is ProjectHeadingItem -> item.key
                        else -> item.toString()
                    }
                }) { item ->
                    when (item) {
                        is ItemWithChecklist -> {
                            // Задача из редактора стоит в списке снимком — сама строка и редактор берут живые данные
                            val liveItem = if (item.item.id == frozenEdited?.item?.id) {
                                state.displayTasks.firstOrNull { it.item.id == item.item.id } ?: item
                            } else item
                            // Ушедшая из «Входящих» (под плашкой) — та же строка, что у задачи: съезжает под плашку по ключу,
                            // с ней можно всё; бледная, пока не раскрыта
                            val isInboxGhost = screen == ActiveScreen.INBOX && item.item.id in inboxGhostIds &&
                                inlineExpandedTaskId != item.item.id
                            AnimatedTaskItem(
                                taskWrapper = liveItem,
                                // В «Повторюваних завданнях» в конце «Планов» у шаблона без следующего повтора — «Пауза» (на паузе), иначе «Чекає»
                                dateBadge = if (screen == ActiveScreen.UPCOMING) monthTaskBadges[UpcomingScreen.monthBadgeKey(item.item)]
                                    ?: item.item.takeIf { it.isRepeatTemplate && com.example.domain.lists.RepeatingList.waits(state.repeatRules[it.id]) }
                                        ?.let { stringResource(if (state.repeatRules[it.id]?.isActive == false) R.string.repeat_paused else R.string.repeat_waiting) }
                                else null,
                                dragDropState = dragDropState,
                                inlineExpandedTaskId = inlineExpandedTaskId,
                                editorOutsideTouch = editorOutsideTouch,
                                editorLiveFields = editorLiveFields,
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
                                    } else if (event is ThingsCategoryListEvent.RequestMoveDialog) {
                                        showMoveDialog = true
                                    } else if (event is ThingsCategoryListEvent.RequestRepeatMove) {
                                        requestRepeatMove(event.templateId, event.day)
                                    } else if (event is ThingsCategoryListEvent.ReorderTasks && event.items.any { it.isRepeatTemplate }) {
                                        // Повторяющуюся задачу перенесли на другой день — переносится её ближайший повтор;
                                        // сам шаблон дат не хранит и так не сохраняется
                                        val (templates, others) = event.items.partition { it.isRepeatTemplate }
                                        if (others.isNotEmpty()) onEvent(ThingsCategoryListEvent.ReorderTasks(others))
                                        // Перенесён только тот шаблон, чей день не совпадает ни с одним днём, где он стоял: шаблон
                                        // стоит в «Планах» на каждом дне своих повторов (один id), а в сохраняемые попадают и
                                        // соседи, у которых сменились лишь место или порядок дня — для них предупреждения нет
                                        val zone = java.time.ZoneId.systemDefault()
                                        fun dayOf(millis: Long) = java.time.Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
                                        templates.firstOrNull { moved ->
                                            val day = moved.startDate ?: return@firstOrNull false
                                            state.displayTasks.none { it.item.id == moved.id && it.item.startDate?.let { d -> dayOf(d) } == dayOf(day) }
                                        }?.let { moved ->
                                            val day = moved.startDate ?: return@let
                                            if (!requestRepeatMove(moved.id, day)) localTasksList = state.displayTasks
                                        }
                                    } else if (event is ThingsCategoryListEvent.RepeatTemplateAction && event.action == RepeatAction.EDIT) {
                                        // «Изменить правило» — окно «Повторять» с правилом шаблона
                                        showRepeatDialog = true
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
                                upcomingDrag = upcomingDrag,
                                lazyListState = lazyListState,
                                onWhenDialogVisibilityChange = { isWhenDialogOpen = it },
                                onTagDialogVisibilityChange = { isTagDialogOpen = it },
                                isSelectionMode = state.isSelectionMode,
                                isSelected = state.selectedTaskIds.contains(item.item.id),
                                isDragSelecting = isDragSelecting,
                                onToggleSelect = { onEvent(ThingsCategoryListEvent.ToggleTaskSelection(item.item.id)) },
                                selectedTaskIds = state.selectedTaskIds,
                                onExitSelectionMode = { onEvent(ThingsCategoryListEvent.ExitSelectionMode) },
                                modifier = (if (isInboxGhost) Modifier.graphicsLayer { alpha = ThingsAlpha.HALF } else Modifier).then(if (dragDropState.draggedItemKey == item.item.id) {
                                    Modifier
                                } else {
                                    Modifier.animateItem(
                                        // Новая задача после сброса «+» раскрывается из центра сама (AnimatedTaskItem)
                                        fadeInSpec = if (fabDrag?.freshTaskId == item.item.id) null
                                        else androidx.compose.animation.core.tween(ThingsMotion.STANDARD),
                                        fadeOutSpec = androidx.compose.animation.core.tween(ThingsMotion.STANDARD),
                                        placementSpec = placementSpec
                                    )
                                })
                            )
                        }
                        is Item -> {
                            val task = item
                            if (task.type == Item.TYPE_PROJECT || task.type == Item.TYPE_PROJECT_TEMPLATE) {
                                // Извлеченный подкомпонент строки проекта (и шаблона повторяющегося проекта)
                                ProjectItemRow(
                                    project = task,
                                    isNew = screen == ActiveScreen.TODAY && task.id in todayNew.ids,
                                    tasks = state.displayTasks,
                                    projectProgressMap = state.projectProgressMap,
                                    textPrimaryColor = textPrimaryColor,
                                    inlineExpandedTaskId = inlineExpandedTaskId,
                                    onProjectClick = { onEvent(ThingsCategoryListEvent.ClickProject(it)) },
                                    // Дата закрытия — у закрытых проектов в «Журнале» и особых списках проектов
                                    closedAt = if (
                                        (screen == ActiveScreen.LOGBOOK || screen == ActiveScreen.LOGGED_PROJECTS || screen == ActiveScreen.ALL_PROJECTS) &&
                                        LogbookList.includesProject(task)
                                    ) LogbookList.closedAt(task) else null,
                                    dateBadge = when (screen) {
                                        ActiveScreen.UPCOMING -> monthTaskBadges[UpcomingScreen.monthBadgeKey(task)]
                                            ?: task.takeIf { it.type == Item.TYPE_PROJECT_TEMPLATE && com.example.domain.lists.RepeatingList.waits(state.repeatRules[it.id]) }
                                                ?.let { stringResource(if (state.repeatRules[it.id]?.isActive == false) R.string.repeat_paused else R.string.repeat_waiting) }
                                        // Шаблон проекта — день следующего повтора или «Пауза»
                                        // Шаблон проекта — день следующего повтора, «Пауза» или «Ждёт»; копия, созданная заранее, — её день
                                        ActiveScreen.REPEATING -> if (task.type == Item.TYPE_PROJECT_TEMPLATE) {
                                            com.example.ui.components.repeatBadge(state.repeatRules[task.id])
                                        } else task.startDate?.let { com.example.ui.screens.home.subcomponents.startDateBadgeText(it) }
                                        // Шаблон повторяющегося проекта — день следующего повтора, «Пауза» или «Чекає»
                                        ActiveScreen.ALL_PROJECTS, ActiveScreen.LATER_PROJECTS -> if (task.type == Item.TYPE_PROJECT_TEMPLATE) {
                                            com.example.ui.components.repeatBadge(state.repeatRules[task.id])
                                        } else if (LogbookList.includesProject(task)) null else AllProjectsScreen.laterDateBadge(task)
                                            ?.let { com.example.ui.screens.home.subcomponents.startDateBadgeText(task.startDate!!) }
                                        // В «Планах» области — дата старта запланированного проекта; у шаблона повторяющегося
                                        // проекта — день следующего повтора, «Пауза» или «Чекає»
                                        ActiveScreen.AREA_DETAIL -> if (task.type == Item.TYPE_PROJECT_TEMPLATE) {
                                            com.example.ui.components.repeatBadge(state.repeatRules[task.id])
                                        } else if (HomeList.showsProject(task)) null else AllProjectsScreen.laterDateBadge(task)
                                            ?.let { com.example.ui.screens.home.subcomponents.startDateBadgeText(task.startDate!!) }
                                        // На экране тега — дата старта запланированного проекта (сегодня — звезда или луна)
                                        // (шаблон повторяющегося проекта — день следующего повтора, «Пауза» или «Чекає»)
                                        // В поиске — у шаблона повторяющегося проекта день следующего повтора
                                        ActiveScreen.SEARCH -> if (task.type == Item.TYPE_PROJECT_TEMPLATE) {
                                            com.example.ui.components.repeatBadge(state.repeatRules[task.id])
                                        } else null
                                        ActiveScreen.TAG_DETAIL -> if (task.type == Item.TYPE_PROJECT_TEMPLATE) {
                                            com.example.ui.components.repeatBadge(state.repeatRules[task.id])
                                        } else task.startDate
                                            ?.takeIf { it > com.example.data.model.DayBounds.now().endOfToday }
                                            ?.let { com.example.ui.screens.home.subcomponents.startDateBadgeText(it) }
                                        else -> null
                                    },
                                    // Копия проекта на сегодня в «Повторяющихся» и проект на сегодня на экране тега — звезда или луна вместо плашки
                                    todayIcon = if (
                                        (screen == ActiveScreen.REPEATING || screen == ActiveScreen.TAG_DETAIL) && task.type == Item.TYPE_PROJECT &&
                                        (task.start == Item.START_TODAY || (task.startDate ?: Long.MAX_VALUE) <= com.example.data.model.DayBounds.now().endOfToday)
                                    ) {
                                        if (task.isTonight) com.example.ui.theme.AppIcons.Evening else com.example.ui.theme.AppIcons.Today
                                    } else null,
                                    todayIconTint = if (task.isTonight) ThingsTheme.colors.eveningIndicator else Color.Unspecified,
                                    // Под закрытым проектом — его область, как в Logbook Things; в «Предстоящих» — область
                                    // запланированного проекта, как подпись под задачей проекта или области
                                    subtitle = if (
                                        ((screen == ActiveScreen.LOGBOOK || screen == ActiveScreen.LOGGED_PROJECTS || screen == ActiveScreen.ALL_PROJECTS) &&
                                            LogbookList.includesProject(task)) || screen == ActiveScreen.UPCOMING
                                    ) areasState.firstOrNull { it.id == task.areaId && !it.trashed }?.title else null,
                                    modifier = if (dragDropState.draggedItemKey == task.id) {
                                        // Проект тянут в «Планах» — карточкой над строками, как задачу
                                        val dragShape = ThingsTheme.shapes.rowShape
                                        Modifier
                                            .zIndex(UPCOMING_DRAG_Z)
                                            .graphicsLayer {
                                                translationY = dragDropState.visualDragOffsetY(task.id)
                                                translationX = dragDropState.dragAccumulatedX
                                                scaleX = UPCOMING_DRAG_SCALE
                                                scaleY = UPCOMING_DRAG_SCALE
                                                shadowElevation = ThingsElevation.CARD.toPx()
                                                shape = dragShape
                                                clip = true
                                            }
                                            .background(ThingsTheme.colors.background)
                                    } else {
                                        Modifier.animateItem(placementSpec = placementSpec)
                                    }.then(
                                        if (upcomingDrag != null && task.type == Item.TYPE_PROJECT) {
                                            Modifier.upcomingProjectDrag(dragDropState, task.id, upcomingDrag) { saved ->
                                                if (saved.isNotEmpty()) onEvent(ThingsCategoryListEvent.ReorderTasks(saved))
                                            }
                                        } else Modifier
                                    )
                                )
                            }
                        }
                        is InboxMovedBannerItem -> {
                            InboxMovedBanner(
                                count = item.count,
                                onOk = { onEvent(ThingsCategoryListEvent.DismissInboxMoved) },
                                modifier = Modifier.animateItem(placementSpec = placementSpec)
                            )
                        }
                        is TodayNewBannerItem -> {
                            // Новое в «Сегодня» — жёлтая плашка, как в Things
                            ListBanner(
                                text = androidx.compose.ui.res.pluralStringResource(R.plurals.today_new_items, item.count, item.count),
                                number = item.count.toString(),
                                background = ThingsTheme.colors.todayNewBanner,
                                buttonBackground = ThingsTheme.colors.todayNewButton,
                                onOk = todayNew.onDismiss,
                                modifier = Modifier.animateItem(placementSpec = placementSpec)
                            )
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
                        is UpcomingRepeatingHeaderItem -> {
                            val dimAlpha by animateFloatAsState(
                                targetValue = if (inlineExpandedTaskId != null) 0.3f else 1f,
                                label = "dimAlpha_upcoming_repeating"
                            )
                            // Как заголовок месяца, с вдвое большим отступом сверху; переносить на него нельзя
                            // (TaskListKeys.nonDroppable)
                            UpcomingMonthHeader(
                                monthLabel = item.label,
                                isRemainder = false,
                                doubleTop = true,
                                textPrimaryColor = textPrimaryColor,
                                dividerColor = dividerColor,
                                modifier = Modifier
                                    .padding(horizontal = ThingsSpacing.S)
                                    .animateItem(placementSpec = placementSpec)
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
                                isRemainder = header.isRemainder,
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
                        is ListToggleItem -> {
                            ListToggleRow(
                                label = item.label,
                                onClick = {
                                    if (item.key == AllProjectsScreen.LOGGED_KEY && screen == ActiveScreen.ALL_PROJECTS) {
                                        onEvent(ThingsCategoryListEvent.OpenLoggedProjects)
                                    } else if (item.key == AllProjectsScreen.LOGGED_KEY) {
                                        showLoggedProjects = !showLoggedProjects
                                    } else {
                                        expandedLaterProjects = if (item.key in expandedLaterProjects) {
                                            expandedLaterProjects - item.key
                                        } else {
                                            expandedLaterProjects + item.key
                                        }
                                    }
                                },
                                modifier = Modifier.animateItem(placementSpec = placementSpec)
                            )
                        }
                        is LogbookHeaderItem -> {
                            LogbookSectionHeader(
                                label = item.label,
                                textPrimaryColor = textPrimaryColor,
                                dividerColor = dividerColor,
                                modifier = Modifier
                                    .padding(horizontal = ThingsSpacing.S)
                                    .animateItem(placementSpec = placementSpec)
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
                                strong = header.kind == SearchSectionKind.AREA,
                                modifier = Modifier
                                    .animateItem(placementSpec = placementSpec)
                                    .graphicsLayer { alpha = dimAlpha }
                            )
                        }
                        is FabGapItem -> {
                            FabGapRow(
                                asHeading = item.asHeading,
                                extraPx = { gapExtra.value },
                                growsFromZero = fabTapGap,
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
                                    isFound = heading.id == highlightedTaskId,
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
                                                val tasksUnder = HeadingOrder.tasksOf(state.allTasks, heading.id, localHeadings.map { it.id }).map { it.item }
                                                onEvent(ThingsCategoryListEvent.DiscardHeading(heading, tasksUnder, groupAbove))
                                            }
                                            title.isNotEmpty() && title != heading.title ->
                                                onEvent(ThingsCategoryListEvent.RenameHeading(heading, title))
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
                        is SearchHeadingItem -> {
                            SearchEntityRow(
                                title = item.heading.title,
                                icon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ShortText,
                                        contentDescription = null,
                                        tint = ThingsTheme.colors.project,
                                        modifier = Modifier.requiredSize(SEARCH_ROW_ICON_SIZE)
                                    )
                                },
                                textPrimaryColor = textPrimaryColor,
                                subtitle = item.projectTitle,
                                onClick = { onEvent(ThingsCategoryListEvent.OpenHeading(item.heading)) },
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
                                    laterCount = areaLaterCount,
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
            onMoveToNewProject = { title ->
                onEvent(ThingsCategoryListEvent.MoveTaskToNewProject(activeTask, title))
                showMoveDialog = false
            },
            headings = state.allHeadings,
            onMoveToHeading = { projectId, areaId, headingId ->
                onEvent(ThingsCategoryListEvent.MoveTask(activeTask, projectId, areaId, false, headingId))
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
        var pendingReminderTime by remember(targetTask) { mutableStateOf(targetTask.item.reminderTime) }

        ThingsWhenDialog(
            startDate = pendingStartDate,
            onStartDateChange = { pendingStartDate = it },
            section = pendingSection,
            onSectionChange = { pendingSection = it },
            isTonight = pendingIsTonight,
            onIsTonightChange = { pendingIsTonight = it },
            onShowCalendarHelperChange = { },
            reminderTime = pendingReminderTime,
            onReminderTimeChange = { pendingReminderTime = it },
            growFromOffset = swipeWhenOrigin,
            onDismissRequest = {
                // Повторяющаяся задача: выбранный день — новый день её ближайшего повтора
                if (targetTask.item.isRepeatTemplate) {
                    val day = if (pendingSection == com.example.data.model.TaskSection.TODAY) {
                        com.example.data.model.DayBounds.now().todayStart
                    } else pendingStartDate
                    if (day != null && day != targetTask.item.startDate) requestRepeatMove(targetTask.item.id, day)
                    swipeWhenTask = null
                    swipeWhenOrigin = null
                    return@ThingsWhenDialog
                }
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
                        checklist = targetTask.checklist,
                        reminderTime = pendingReminderTime
                    )
                )
                swipeWhenTask = null
                swipeWhenOrigin = null
            }
        )
    }

    // Невидимое поле: в нём фокус, пока раскрывается новая задача от «+» (см. imePrimerView). Клавиатура — как у
    // названия задачи (с заглавной, «Готово»), но поле однострочное: при переходе фокуса клавиатура видит другое поле
    // и заново проверяет заглавную (у названия Compose сообщает её только так — иначе первая буква строчная)
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { context ->
            android.widget.EditText(context).apply {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                    android.text.InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
                imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE or
                    android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN
                background = null
                alpha = 0f
                isFocusableInTouchMode = true
                imePrimerView = this
            }
        },
        modifier = Modifier.size(IME_PRIMER_SIZE)
    )

    FloatingBottomCapsuleToolbar(
        // Пока открыто окно «Переместить», тулбар скрыт — как в Things
        visible = inlineExpandedTaskId != null && activeTask != null && !isWhenDialogOpen && !isTagDialogOpen && swipeWhenTask == null &&
            !showMoveDialog && !showRepeatDialog,
        onMoveClick = { showMoveDialog = true },
        onDeleteClick = { showDeleteConfirm = true },
        onDuplicateClick = {
            val taskId = activeTask?.item?.id ?: return@FloatingBottomCapsuleToolbar
            // Как в Things: редактор сворачивается и сохраняет правки, копия — сохранённой задачи (с только что
            // набранным названием), встаёт сразу под ней
            onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
            coroutineScope.launch {
                kotlinx.coroutines.delay(com.example.ui.theme.AnimationConstants.TASK_EXPANSION_DURATION_MS)
                currentAllTasks.firstOrNull { it.item.id == taskId }?.let { onEvent(ThingsCategoryListEvent.DuplicateTask(it)) }
            }
        },
        isCancelled = activeTask?.item?.status == Item.STATUS_CANCELLED,
        onCancelClick = {
            val taskId = activeTask?.item?.id
            if (taskId != null) {
                // Сначала редактор сворачивается и сохраняет правки, потом задача отменяется — иначе
                // сохранение при сворачивании вернуло бы ей прежний статус
                onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                coroutineScope.launch {
                    kotlinx.coroutines.delay(com.example.ui.theme.AnimationConstants.TASK_EXPANSION_DURATION_MS)
                    onEvent(ThingsCategoryListEvent.ToggleTaskCancel(taskId))
                }
            }
        },
        onRepeatClick = activeTask?.item?.takeIf { com.example.domain.repeat.RepeatSetup.canRepeat(it) }?.let { { showRepeatDialog = true } },
        // Шаблон повторяющейся задачи — в меню только «Поделиться», как в Things
        onlyShare = activeTask?.item?.isRepeatTemplate == true,
        onShareClick = {
            val taskId = activeTask?.item?.id ?: return@FloatingBottomCapsuleToolbar
            // Сначала редактор сворачивается и сохраняет правки — иначе ушло бы название до правки
            // (у только что созданной задачи — пустое); делится сохранённой задачей
            onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
            coroutineScope.launch {
                kotlinx.coroutines.delay(com.example.ui.theme.AnimationConstants.TASK_EXPANSION_DURATION_MS)
                val shared = currentAllTasks.firstOrNull { it.item.id == taskId } ?: return@launch
                // Название, заметки и пункты чек-листа — простым текстом
                val text = buildString {
                    append(shared.item.title)
                    if (shared.item.notes.isNotBlank()) append("\n\n").append(shared.item.notes.trim())
                    if (shared.checklist.isNotEmpty()) {
                        append("\n")
                        shared.checklist.forEach { append("\n• ").append(it.title) }
                    }
                }
                val sendIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, text)
                    type = "text/plain"
                }
                context.startActivity(android.content.Intent.createChooser(sendIntent, null))
            }
        },
        modifier = Modifier.align(Alignment.BottomCenter)
    )

    if (showRepeatDialog && activeTask != null) {
        val repeatTask = activeTask
        val isTemplate = repeatTask.item.type == Item.TYPE_TEMPLATE
        val currentRule = state.repeatRules[repeatTask.item.id]?.let { com.example.domain.repeat.RepeatRule.decode(it.rrule) }
        com.example.ui.screens.home.inlineeditor.dialogs.ThingsRepeatDialog(
            // С днём и напоминанием, выбранными в редакторе, но ещё не сохранёнными
            task = editorLiveFields.applyTo(repeatTask.item),
            initialRule = currentRule,
            onDismissRequest = { showRepeatDialog = false },
            onSave = { rule, reminderTime ->
                if (isTemplate) {
                    // Шаблон: только новое правило, редактор остаётся открытым
                    onEvent(ThingsCategoryListEvent.UpdateRepeatRule(repeatTask.item.id, rule, reminderTime))
                    return@ThingsRepeatDialog
                }
                // Сначала редактор сворачивается и сохраняет правки, потом задача становится шаблоном повтора:
                // сохранение при сворачивании иначе вернуло бы её в обычные задачи
                onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                coroutineScope.launch {
                    kotlinx.coroutines.delay(com.example.ui.theme.AnimationConstants.TASK_EXPANSION_DURATION_MS)
                    onEvent(ThingsCategoryListEvent.MakeRepeating(repeatTask.item.id, rule, reminderTime))
                }
            }
        )
    }

    repeatMove?.let { (templateId, day) ->
        val finish = { repeatMove = null }
        com.example.ui.components.ThingsChoiceDialog(
            title = stringResource(R.string.repeat_move_title),
            message = stringResource(R.string.repeat_move_message),
            options = listOf(
                stringResource(R.string.repeat_move_exception) to {
                    onEvent(ThingsCategoryListEvent.MoveRepeat(templateId, day, updateRule = false)); finish()
                },
                stringResource(R.string.repeat_move_update_rule) to {
                    onEvent(ThingsCategoryListEvent.MoveRepeat(templateId, day, updateRule = true)); finish()
                }
            ),
            dismissText = stringResource(R.string.cancel),
            onDismiss = {
                // Отмена — задача возвращается на свой день
                localTasksList = state.displayTasks
                finish()
            }
        )
    }

    repeatDialogProject?.let { repeatProject ->
        val currentRule = state.repeatRules[repeatProject.id]?.let { com.example.domain.repeat.RepeatRule.decode(it.rrule) }
        com.example.ui.screens.home.inlineeditor.dialogs.ThingsRepeatDialog(
            task = repeatProject,
            initialRule = currentRule,
            onDismissRequest = { repeatDialogProject = null },
            onSave = { rule, reminderTime ->
                if (repeatProject.isRepeatTemplate) {
                    onEvent(ThingsCategoryListEvent.UpdateRepeatRule(repeatProject.id, rule, reminderTime))
                } else {
                    onEvent(ThingsCategoryListEvent.MakeRepeating(repeatProject.id, rule, reminderTime))
                }
            }
        )
    }

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
            onMoveToNewProject = { title ->
                onEvent(ThingsCategoryListEvent.BatchMoveTasksToNewProject(title))
                showBatchMoveDialog = false
            },
            headings = state.allHeadings,
            onMoveToHeading = { projectId, areaId, headingId ->
                onEvent(ThingsCategoryListEvent.BatchMoveTasks(projectId, areaId, false, headingId))
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

    if (project != null && projectDialog != null) {
        ProjectDialogs(
            project = project,
            openDialog = projectDialog,
            onClose = { projectDialog = null },
            allTasks = state.allTasks,
            projects = projects,
            areas = areasState,
            allSavedTags = allSavedTags,
            allSavedTagObjects = allSavedTagObjects,
            onNewTagCreated = { name, parentId -> onEvent(ThingsCategoryListEvent.CreateTag(name, parentId)) },
            onDeleteTag = { tag -> onEvent(ThingsCategoryListEvent.DeleteTag(tag)) },
            onUpdateTag = { tag -> onEvent(ThingsCategoryListEvent.UpdateTag(tag)) },
            onUpdateTagsOrder = { tags -> onEvent(ThingsCategoryListEvent.UpdateTagsOrder(tags)) },
            onEditProject = { edit -> onEvent(ThingsCategoryListEvent.EditProject(project, edit)) },
            onCompleteProject = { onEvent(ThingsCategoryListEvent.CompleteProject(it)) },
            onRepeatMove = { day -> requestRepeatMove(project.id, day) }
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
        isVisible = state.isSelectionMode && !showBatchMoveDialog,
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
        modifier = Modifier.align(Alignment.BottomCenter),
        onRepeatAction = state.allTasks
            .filter { state.selectedTaskIds.contains(it.item.id) && it.item.isRepeatTemplate }
            .map { it.item.id }
            .takeIf { it.isNotEmpty() }
            ?.let { templateIds ->
                { action ->
                    onEvent(ThingsCategoryListEvent.BatchRepeatAction(templateIds, action))
                    onEvent(ThingsCategoryListEvent.ExitSelectionMode)
                }
            }
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
            com.example.ui.components.ProjectStatusIcon(
                project = header.project,
                completed = progress?.completed ?: 0,
                total = progress?.total ?: 0,
                modifier = iconModifier
            )
        }
        SearchSectionKind.LOGBOOK -> Icon(
            imageVector = AppIcons.Logbook,
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = iconModifier
        )
        SearchSectionKind.UPCOMING -> Icon(
            imageVector = AppIcons.Upcoming,
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = iconModifier
        )
        SearchSectionKind.SOMEDAY -> Icon(
            imageVector = AppIcons.Someday,
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = iconModifier
        )
    }
}

/** Экраны, на которые можно сбросить кнопку «+» */
/**
 * Экраны, где нажатие на «+» добавляет задачу вверху списка с приземлением кнопки. В «Предстоящих» — первой
 * в «Завтра»: промежуток встаёт сразу под заголовком первого дня (FabInsertion.normalizeSlot), дата — его
 */
private val FAB_TAP_SCREENS = setOf(
    ActiveScreen.INBOX, ActiveScreen.TODAY, ActiveScreen.UPCOMING, ActiveScreen.ANYTIME, ActiveScreen.SOMEDAY,
    ActiveScreen.PROJECT_DETAIL, ActiveScreen.AREA_DETAIL, ActiveScreen.TAG_DETAIL
)

/** Нажатие на «+»: средняя скорость прокрутки списка к началу, px за мс — время прокрутки по расстоянию */
private const val FAB_TAP_SCROLL_PX_PER_MS = 4f

/** Нажатие на «+»: прокрутка к началу — не длиннее, мс */
private const val FAB_TAP_SCROLL_MAX_MS = 900

/**
 * Нажатие на «+»: средняя скорость прокрутки — не меньше стольких скоростей подхвата промежутком: и короткий путь
 * начинается быстро, а к самой остановке замедляется до скорости промежутка
 */
private const val FAB_TAP_SCROLL_MIN_AVG_RATIO = 1.5f

/** Нажатие на «+»: плавно прокручиваются не больше стольких экранов; что дальше — пропускается сразу, в начале */
private const val FAB_TAP_SCROLL_MAX_SCREENS = 2.5f

/** Нажатие на «+»: промежуток растёт до высоты раскрытой задачи за столько мс */
private const val FAB_TAP_GROW_MS = 260

/** Нажатие на «+»: кнопка взлетает позже начала промежутка на столько мс, как в Things (полёт — 270 мс) */
private const val FAB_TAP_FLIGHT_DELAY_MS = 80L

/** Нажатие на «+»: задача ждёт посадки кнопки не дольше, мс */
private const val FAB_TAP_LANDING_WAIT_MS = 600L

/** Нажатие на «+»: задача начинает появляться на этой доле полёта кнопки — кнопка гаснет уже поверх неё */
private const val FAB_TAP_CARD_AT_FLIGHT = 0.8f

/** Невидимое поле для клавиатуры новой задачи от «+» */
private val IME_PRIMER_SIZE = 1.dp

/** Рост промежутка от нажатия: с полной скорости (2H/T в начале) равномерно замедляется до остановки */
private val FAB_TAP_GROW_EASING = androidx.compose.animation.core.Easing { u -> 1f - (1f - u) * (1f - u) }

/**
 * Прокрутка списка к началу одним плавным движением: быстро, а перед самой остановкой — замедление до скорости
 * [endPxPerMs] (px за мс), с ней движение подхватывает промежуток новой задачи. Стандартная animateScrollToItem до невидимой строки едет
 * кусками по экрану и перескакивает, поэтому — своя: сколько осталось до начала, считается заново каждый кадр
 * (вдали — по средней высоте видимых строк, когда первая строка в кадре — точно), и список приходит ровно к началу
 * без докрутки. Очень длинный путь сначала сразу сокращается до [FAB_TAP_SCROLL_MAX_SCREENS] экранов — иначе на
 * такой скорости экраны мелькали бы.
 */
private suspend fun scrollListToTop(state: androidx.compose.foundation.lazy.LazyListState, endPxPerMs: Float) {
    fun remaining(): Float {
        if (state.firstVisibleItemIndex == 0) return state.firstVisibleItemScrollOffset.toFloat()
        val visible = state.layoutInfo.visibleItemsInfo
        if (visible.isEmpty()) return 0f
        val averageRow = visible.sumOf { it.size }.toFloat() / visible.size
        return state.firstVisibleItemIndex * averageRow + state.firstVisibleItemScrollOffset
    }
    if (remaining() <= 0f) return
    val viewport = (state.layoutInfo.viewportEndOffset - state.layoutInfo.viewportStartOffset).toFloat()
    state.scroll {
        val maxSmooth = viewport * FAB_TAP_SCROLL_MAX_SCREENS
        if (viewport > 0f && remaining() > maxSmooth) scrollBy(-(remaining() - maxSmooth))
        val total = remaining()
        if (total <= 0f) return@scroll
        // Время — по расстоянию, но средняя скорость не ниже FAB_TAP_SCROLL_MIN_AVG_RATIO скоростей подхвата
        val duration = minOf(
            (total / FAB_TAP_SCROLL_PX_PER_MS).coerceAtMost(FAB_TAP_SCROLL_MAX_MS.toFloat()),
            total / (endPxPerMs * FAB_TAP_SCROLL_MIN_AVG_RATIO)
        ).coerceAtLeast(1f)
        // Скорость v(u) = endV + (startV - endV)·(1 - u²): почти не падает в начале и сходится с endV к самой
        // остановке. Путь total = duration·(endV + (startV - endV)·2/3)
        val endV = endPxPerMs
        val startV = endV + (total / duration - endV) * 1.5f
        androidx.compose.animation.core.animate(
            0f, 1f,
            animationSpec = androidx.compose.animation.core.tween(duration.toInt().coerceAtLeast(1), easing = androidx.compose.animation.core.LinearEasing)
        ) { u, _ ->
            val travelled = duration * (endV * u + (startV - endV) * (u - u * u * u / 3f))
            scrollBy(-(remaining() - (total - travelled)))
        }
        // Последний кадр: остаток (доли пикселя) — до самого начала
        if (remaining() > 0f) scrollBy(-remaining())
    }
}

/** Нажатие на «+»: сколько кадров ждать промежуток в кадре, прежде чем лететь без него */
private const val FAB_TAP_GAP_WAIT_FRAMES = 30

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
    is LogbookHeaderItem -> element.key
    is ListToggleItem -> element.key
    is String -> element
    else -> null
}

/** Строка-переключатель списка серым текстом — как «Показать более поздние объекты» на экране области. */
@Composable
private fun ListToggleRow(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = MaterialTheme.dimens.taskRowStartPadding, top = ThingsSpacing.S, bottom = ThingsSpacing.M)
    ) {
        Text(
            text = label,
            style = ThingsTheme.type.bodyMedium.copy(color = ThingsTheme.colors.textSecondary.copy(alpha = ThingsAlpha.HINT)),
            modifier = Modifier.clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
        )
    }
}

/** Промежуток на месте пальца: серая плашка задачи или пунктир «NEW HEADING» */
@Composable
private fun FabGapRow(asHeading: Boolean, extraPx: () -> Float, modifier: Modifier = Modifier, growsFromZero: Boolean = false) {
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
                    // От нажатия на «+» — растёт от нуля сразу до высоты задачи, без плашки 44 dp
                    val height = (if (growsFromZero) 0 else baseHeightPx) + extraPx().toInt()
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

/**
 * Плашка «Входящих» над ушедшими оттуда задачами, как в Things: «**2** завдання переміщено з «Вхідних»» и «OK» —
 * после него эти задачи там больше не показываются.
 */
@Composable
private fun InboxMovedBanner(count: Int, onOk: () -> Unit, modifier: Modifier = Modifier) {
    ListBanner(
        text = androidx.compose.ui.res.pluralStringResource(R.plurals.inbox_moved_out, count, count),
        number = count.toString(),
        background = ThingsTheme.colors.searchField,
        buttonBackground = ThingsTheme.colors.textSecondary.copy(alpha = ThingsAlpha.SUBTLE),
        onOk = onOk,
        modifier = modifier
    )
}

/** Плашка над списком: текст с числом жирным и кнопка «OK» («Вхідні» — переехавшие, «Сьогодні» — новое) */
@Composable
private fun ListBanner(
    text: String,
    number: String,
    background: Color,
    buttonBackground: Color,
    onOk: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = androidx.compose.ui.platform.LocalView.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = ThingsSpacing.XS)
            .clip(ThingsTheme.shapes.rowShape)
            .background(background)
            .padding(start = ThingsSpacing.M, end = ThingsSpacing.S, top = ThingsSpacing.S, bottom = ThingsSpacing.S),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = androidx.compose.ui.text.buildAnnotatedString {
                val at = text.indexOf(number)
                if (at < 0) append(text) else {
                    append(text.substring(0, at))
                    // Число — жирным, начертанием из темы
                    withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = ThingsTheme.type.headlineStrong.fontWeight)) { append(number) }
                    append(text.substring(at + number.length))
                }
            },
            style = ThingsTheme.type.bodyMedium.copy(color = ThingsTheme.colors.textPrimary),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(ThingsSpacing.S))
        Box(
            modifier = Modifier
                .clip(ThingsTheme.shapes.smallShape)
                .background(buttonBackground)
                .clickable {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                    onOk()
                }
                .padding(horizontal = ThingsSpacing.M, vertical = ThingsSpacing.XS),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.ui_ok),
                style = ThingsTheme.type.bodyMedium.copy(color = ThingsTheme.colors.textPrimary)
            )
        }
    }
}

/** Перетаскиваемый проект в «Планах» — над строками и чуть крупнее, как задача */
private const val UPCOMING_DRAG_Z = 100f
private const val UPCOMING_DRAG_SCALE = 1.04f

/**
 * Проект в днях «Планов» можно перетаскивать, как задачу: между любыми задачами и проектами дня или в другой день
 * ([UpcomingDragController]); по отпусканию — [onSave] с тем, что сохранить
 */
private fun Modifier.upcomingProjectDrag(
    state: com.example.ui.components.dragdrop.GenericDragDropState,
    projectId: String,
    controller: UpcomingDragController,
    onSave: (List<Item>) -> Unit
): Modifier = universalDragAndDrop(
        state = state,
        key = projectId,
        canDropOver = { targetKey ->
            val key = targetKey as? String
            key != null && !key.startsWith(TaskListKeys.CALENDAR_EVENT_PREFIX) && key !in TaskListKeys.nonDroppable
        },
        onMoveIfNecessary = { draggedKey, targetKey ->
            val visible = state.lazyListState.layoutInfo.visibleItemsInfo
            val from = visible.firstOrNull { it.key == draggedKey } ?: return@universalDragAndDrop false
            val to = visible.firstOrNull { it.key == targetKey } ?: return@universalDragAndDrop false
            controller.move(draggedKey as String, targetKey, movingDown = to.index > from.index)
        },
        onDragEnd = { onSave(controller.end()) }
    )
