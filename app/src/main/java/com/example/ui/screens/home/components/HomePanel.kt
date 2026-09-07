package com.example.ui.screens.home.components

import kotlinx.coroutines.launch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import com.example.ui.components.dragdrop.GenericDragDropState
import com.example.ui.components.dragdrop.rememberGenericDragDropState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.TaskSection
import com.example.ui.components.ProjectProgressArc
import com.example.ui.components.AreaIconAnimated
import com.example.ui.screens.home.ActiveScreen
import com.example.ui.screens.home.subcomponents.SmartListRow
import com.example.ui.theme.*
import com.example.ui.theme.ThingsBackgroundDark
import com.example.ui.theme.ThingsBackgroundLight
import androidx.compose.foundation.isSystemInDarkTheme

/**
 * Элементы плоского дерева проектов и областей на главном экране.
 * Объединение в один плоский список гарантирует непрерывность корутины
 * Drag-and-Drop (pointerInput) при перемещении проекта между областями.
 */
private sealed interface HomeTreeItem {
    val key: String
    val contentType: String

    data class ProjectItem(
        val project: Item,
        val areaId: String?
    ) : HomeTreeItem {
        override val key: String get() = "proj_${project.id}"
        override val contentType: String get() = "project"
    }

    data class AreaHeaderItem(
        val area: Area,
        val index: Int,
        val hasProjects: Boolean,
        val isExpanded: Boolean,
        val showTopDivider: Boolean
    ) : HomeTreeItem {
        override val key: String get() = "area_${area.id}"
        override val contentType: String get() = "area_header"
    }
}

@Composable
fun ThingsHomePanel(
    allTasks: List<ItemWithChecklist>,
    projects: List<Item>,
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
    onProjectClick: (Item) -> Unit,
    onAddProjectClick: () -> Unit,
    onSyncClick: (String) -> Unit,
    // Two-level Areas management additions
    areas: List<Area> = emptyList(),
    onAddAreaClick: () -> Unit = {},
    onDeleteArea: (Area) -> Unit = {},
    onDeleteProject: (Item) -> Unit = {},
    onAreaClick: (Area) -> Unit = {},
    onSearchClick: () -> Unit = {},
    // [ИЗМЕНЕНИЕ]: Добавлено состояние активности поискового оверлея
    isSearchOverlayActive: Boolean = false,
    editingProjectId: String? = null,
    onEditingProjectIdChange: (String?) -> Unit = {},
    editingAreaId: String? = null,
    onEditingAreaIdChange: (String?) -> Unit = {},
    onUpdateProject: (Item) -> Unit = {},
    onUpdateArea: (Area) -> Unit = {},
    onProjectsReordered: (List<Item>) -> Unit = {},
    onAreasReordered: (List<Area>) -> Unit = {}
) {
    var rawTokenInput by remember { mutableStateOf(googleToken) }
    var isSyncConfigExpanded by remember { mutableStateOf(false) }

    val inboxCount = allTasks.count { it.item.isInbox && !it.item.isCompleted }
    val todayCount = allTasks.count { it.item.isToday }

    val tasksByProject = remember(allTasks) {
        allTasks.groupBy { it.item.projectId }
    }

    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }
    
    LaunchedEffect(areas) {
        areas.forEach { area ->
            if (expandedStates[area.id] == null) {
                expandedStates[area.id] = true // expanded by default
            }
        }
        if (expandedStates["no_area"] == null) {
            expandedStates["no_area"] = true
        }
    }

    // Confirmation dialog for project deletion (CASCADE warning)
    var projectToDelete by remember { mutableStateOf<Item?>(null) }

    if (projectToDelete != null) {
        val proj = projectToDelete!!
        val taskCountInProj = allTasks.count { it.item.projectId == proj.id }
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Удалить проект?", fontWeight = FontWeight.Bold, color = textPrimaryColor) },
            text = {
                Text(
                    "Вы действительно хотите удалить проект \"${proj.title}\"? Проект удалится вместе с $taskCountInProj задачами.",
                    color = textSecondaryColor,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteProject(proj)
                        projectToDelete = null
                    }
                ) {
                    Text("Удалить", color = ThingsUpcomingRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Отмена", color = textSecondaryColor)
                }
            },
            containerColor = cardSurfaceColor
        )
    }

    // [ИЗМЕНЕНИЕ]: Начальный индекс изменен с 1 на 0 для немедленного отображения поиска
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = 0)
    val dragDropState = rememberGenericDragDropState(lazyListState)

    var localProjects by remember { mutableStateOf(projects) }
    var localAreas by remember { mutableStateOf(areas) }
    LaunchedEffect(projects, dragDropState.isInteracting, dragDropState.draggedItemKey) {
        if (!dragDropState.isInteracting && dragDropState.draggedItemKey == null) {
            localProjects = projects
        }
    }
    LaunchedEffect(areas, dragDropState.isInteracting, dragDropState.draggedItemKey) {
        if (!dragDropState.isInteracting && dragDropState.draggedItemKey == null) {
            localAreas = areas
        }
    }

    // [ИЗМЕНЕНИЕ]: Блок автоматического "прилипания" (snapping) и авто-скролла при поиске удалены для поддержки свободного скролла поиска без авто-доводки.

    // [ИЗМЕНЕНИЕ]: Реализация жеста pull-down с порогом 100.dp, лимитом свайпа 150.dp
    // и вызовом поиска только после завершения возвращающей анимации
    val density = androidx.compose.ui.platform.LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val pullOffset = remember { Animatable(0f) }
    val thresholdPx = with(density) { 100.dp.toPx() }
    val maxOffsetPx = with(density) { 150.dp.toPx() }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val currentOffset = pullOffset.value
                return if (delta < 0 && currentOffset > 0f) {
                    val newOffset = (currentOffset + delta).coerceAtLeast(0f)
                    val consumed = newOffset - currentOffset
                    coroutineScope.launch { pullOffset.snapTo(newOffset) }
                    Offset(0f, consumed)
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                val isAtTop = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
                // [ИЗМЕНЕНИЕ]: Реагировать только на непосредственный жест пользователя, игнорируя инерционный скролл
                return if (source == NestedScrollSource.UserInput && delta > 0 && isAtTop) {
                    val newOffset = (pullOffset.value + delta * 0.5f).coerceAtMost(maxOffsetPx)
                    coroutineScope.launch { pullOffset.snapTo(newOffset) }
                    Offset(0f, delta)
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val currentOffset = pullOffset.value
                if (currentOffset > 0f) {
                    // [ИЗМЕНЕНИЕ]: Вызываем поиск немедленно при отпускании, если порог превышен, 
                    // наряду с запуском возвращающей анимации
                    val triggered = currentOffset >= thresholdPx
                    if (triggered) {
                        onSearchClick()
                    }
                    pullOffset.animateTo(0f, spring())
                    return available
                }
                return super.onPreFling(available)
            }
        }
    }

    val isDarkTheme = isSystemInDarkTheme()
    val bkgColor = if (isDarkTheme) ThingsBackgroundDark else ThingsBackgroundLight

    Box(modifier = Modifier.fillMaxSize().background(bkgColor)) {
        // [ИЗМЕНЕНИЕ]: Отображение динамического индикатора поиска в свободном пространстве свайпа
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

        // Two-level Areas & Projects tree (flattened to maintain gesture detector lifecycle across areas)
        val flattenedTree = remember(localProjects, localAreas, expandedStates.toMap(), dragDropState.draggedItemKey) {
            val result = mutableListOf<HomeTreeItem>()
            val noAreaProjects = localProjects.filter { it.areaId == null }
            noAreaProjects.forEach { project ->
                result.add(HomeTreeItem.ProjectItem(project, null))
            }
            localAreas.forEachIndexed { index, area ->
                val areaProjects = localProjects.filter { it.areaId == area.id }
                val hasProjects = areaProjects.isNotEmpty()
                val isExpanded = expandedStates[area.id] ?: true
                val showTopDivider = index > 0 || noAreaProjects.isNotEmpty()
                result.add(
                    HomeTreeItem.AreaHeaderItem(
                        area = area,
                        index = index,
                        hasProjects = hasProjects,
                        isExpanded = isExpanded,
                        showTopDivider = showTopDivider
                    )
                )
                if (isExpanded && hasProjects) {
                    areaProjects.forEach { project ->
                        result.add(HomeTreeItem.ProjectItem(project, area.id))
                    }
                } else if (!isExpanded && hasProjects) {
                    // Если область свёрнута, но один из её проектов сейчас удерживается/перетаскивается пользователем — сохраняем его в дереве списка
                    val draggedKey = dragDropState.draggedItemKey
                    areaProjects.filter { "proj_${it.id}" == draggedKey }.forEach { project ->
                        result.add(HomeTreeItem.ProjectItem(project, area.id))
                    }
                }
            }
            result
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .graphicsLayer { translationY = pullOffset.value }
                // [ИЗМЕНЕНИЕ]: Уменьшено расстояние от левой и правой стороны экрана до списков с 20.dp до 14.dp
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
        // Search Filter row
        item {
            // [ИЗМЕНЕНИЕ]: Изменен размер замещающего Spacer до 52.dp в замену новой высоте капсулы 44.dp для бесшовного перехода
            if (isSearchOverlayActive) {
                Spacer(modifier = Modifier.fillMaxWidth().height(52.dp))
            } else {
                val isDark = isSystemInDarkTheme()
                val inputBackground = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)

                // [ИЗМЕНЕНИЕ]: Поле поиска на стартовом окне визуально стилизовано под капсулу ввода в поисковом оверлее (без обводки, с фоном оверлея и высотой 44.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(inputBackground)
                        .clickable { onSearchClick() }
                        .padding(horizontal = 14.dp)
                        .testTag("home_search_input"),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = textSecondaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Quick Find",
                            color = textSecondaryColor.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Smart Lists Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SmartListRow(
                    title = "Inbox",
                    // Используем кастомную иконку AppIcons.Inbox
                    icon = AppIcons.Inbox,
                    // Задаём Unspecified цвет, чтобы отображался оригинальный красивый градиент/цвет иконки
                    iconColor = Color.Unspecified,
                    count = inboxCount,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    isGrayCountAndNoBg = true, // [ИЗМЕНЕНИЕ]: Количество задач серого цвета и без фона
                    onClick = { onSmartListClick(ActiveScreen.INBOX) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                SmartListRow(
                    title = "Today",
                    // Используем кастомную иконку AppIcons.Today
                    icon = AppIcons.Today,
                    iconColor = Color.Unspecified,
                    count = todayCount,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    isGrayCountAndNoBg = true, // [ИЗМЕНЕНИЕ]: Количество задач серого цвета и без фона
                    onClick = { onSmartListClick(ActiveScreen.TODAY) }
                )
                SmartListRow(
                    title = "Upcoming",
                    // Используем кастомную иконку AppIcons.Upcoming
                    icon = AppIcons.Upcoming,
                    iconColor = Color.Unspecified,
                    count = 0,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    onClick = { onSmartListClick(ActiveScreen.UPCOMING) }
                )
                SmartListRow(
                    title = "Anytime",
                    // Используем кастомную иконку AppIcons.Anytime
                    icon = AppIcons.Anytime,
                    iconColor = Color.Unspecified,
                    count = 0,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    onClick = { onSmartListClick(ActiveScreen.ANYTIME) }
                )
                SmartListRow(
                    title = "Someday",
                    // Используем кастомную иконку AppIcons.Someday
                    icon = AppIcons.Someday,
                    iconColor = Color.Unspecified,
                    count = 0,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    onClick = { onSmartListClick(ActiveScreen.SOMEDAY) }
                )
                SmartListRow(
                    title = "Logbook",
                    // Используем кастомную иконку AppIcons.Logbook
                    icon = AppIcons.Logbook,
                    iconColor = Color.Unspecified,
                    count = 0,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    onClick = { onSmartListClick(ActiveScreen.LOGBOOK) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // [ИЗМЕНЕНИЕ]: Горизонтальный разделитель между списком умных категорий и началом списка проектов/областей
        item(key = "root_divider") {
            HorizontalDivider(
                color = dividerColor,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }


        items(
            items = flattenedTree,
            key = { it.key },
            contentType = { it.contentType }
        ) { treeItem ->
            when (treeItem) {
                is HomeTreeItem.ProjectItem -> {
                    ProjectItemRow(
                        project = treeItem.project,
                        allTasks = allTasks,
                        isEditing = treeItem.project.id == editingProjectId,
                        dragDropState = dragDropState,
                        originalProjects = projects,
                        localProjectsList = localProjects,
                        areas = localAreas,
                        expandedStates = expandedStates,
                        cardSurfaceColor = cardSurfaceColor,
                        textPrimaryColor = textPrimaryColor,
                        textSecondaryColor = textSecondaryColor,
                        onProjectClick = onProjectClick,
                        onDeleteProject = onDeleteProject,
                        onUpdateProject = onUpdateProject,
                        onEditingProjectIdChange = onEditingProjectIdChange,
                        onLocalProjectsListChange = { localProjects = it },
                        onProjectsReordered = onProjectsReordered,
                        onExpandArea = { areaId -> expandedStates[areaId] = true }
                    )
                }
                is HomeTreeItem.AreaHeaderItem -> {
                    val area = treeItem.area
                    val isExpanded = treeItem.isExpanded
                    val hasProjects = treeItem.hasProjects
                    val isAreaEditing = area.id == editingAreaId
                    val rotationAngle by animateFloatAsState(
                        targetValue = if (isExpanded) 90f else 0f,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                        label = "rotationAngle_${area.id}"
                    )

                    val isAreaDragging = dragDropState.draggedItemKey == "area_${area.id}"
                    val areaDragScale by animateFloatAsState(
                        targetValue = if (isAreaDragging) 1.04f else 1f,
                        animationSpec = spring(),
                        label = "areaDragScale_${area.id}"
                    )
                    val areaDragElev by animateDpAsState(
                        targetValue = if (isAreaDragging) 8.dp else 0.dp,
                        animationSpec = tween(
                            durationMillis = 250,
                            easing = FastOutSlowInEasing
                        ),
                        label = "areaDragElev_${area.id}"
                    )
                    val areaTranslationY = if (isAreaDragging) dragDropState.dragAccumulatedOffset.value else 0f
                    val areaTranslationX = if (isAreaDragging) dragDropState.dragAccumulatedOffsetHorizontal.value else 0f
                    val areaZIndex = if (isAreaDragging || areaDragElev > 0.dp) 100f else 0f

                    Column(
                        modifier = (if (!isAreaDragging && areaDragElev == 0.dp) {
                            Modifier.animateItem(
                                placementSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                            )
                        } else {
                            Modifier
                        })
                            .fillMaxWidth()
                            .zIndex(areaZIndex)
                    ) {
                        if (treeItem.showTopDivider) {
                            Spacer(modifier = Modifier.height(13.dp))
                            HorizontalDivider(
                                color = dividerColor,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            Spacer(modifier = Modifier.height(13.dp))
                        }

                        // Title block for Area
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Подложка на физическом месте области при перетаскивании (placeholder slot)
                            if (isAreaDragging) {
                                val isDark = isSystemInDarkTheme()
                                val placeholderBgColor = if (isDark) Color(0xFF2C2D32) else Color(0xFFE5E6EB)
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .alpha(0.5f)
                                        .background(placeholderBgColor, RoundedCornerShape(10.dp))
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .graphicsLayer {
                                        translationX = areaTranslationX
                                        translationY = areaTranslationY
                                        scaleX = areaDragScale
                                        scaleY = areaDragScale
                                        shadowElevation = areaDragElev.toPx()
                                        shape = RoundedCornerShape(10.dp)
                                        this.clip = false
                                    }
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isAreaDragging || areaDragElev > 0.dp) cardSurfaceColor
                                        else if (isAreaEditing) ThingsBlue.copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                                    .areaDragAndDrop(
                                        state = dragDropState,
                                        area = area,
                                        hasProjects = hasProjects,
                                        originalAreas = areas,
                                        localAreasList = localAreas,
                                        expandedStates = expandedStates,
                                        onLocalAreasListChange = { localAreas = it },
                                        onAreasReordered = onAreasReordered
                                    )
                                    .clickable(enabled = !isAreaEditing && !isAreaDragging) { onAreaClick(area) }
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                            AreaIconAnimated(
                                isClosed = hasProjects && !isExpanded,
                                onToggle = {},
                                modifier = Modifier.size(width = 20.dp, height = 28.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))

                            if (isAreaEditing) {
                                var textState by remember { mutableStateOf(area.title) }
                                var hasFocused by remember { mutableStateOf(false) }
                                val focusRequester = remember { FocusRequester() }

                                BasicTextField(
                                    value = textState,
                                    onValueChange = { textState = it },
                                    textStyle = MaterialTheme.typography.displaySmall.copy(
                                        color = textPrimaryColor
                                    ),
                                    cursorBrush = SolidColor(ThingsBlue),
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(focusRequester)
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                hasFocused = true
                                            } else if (hasFocused) {
                                                hasFocused = false
                                                val trimmed = textState.trim()
                                                if (trimmed.isEmpty() && area.title.isEmpty()) {
                                                    onDeleteArea(area)
                                                } else if (trimmed.isNotEmpty() && trimmed != area.title) {
                                                    onUpdateArea(area.copy(title = trimmed))
                                                }
                                                onEditingAreaIdChange(null)
                                            }
                                        },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Done,
                                        capitalization = KeyboardCapitalization.Sentences
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (hasFocused) {
                                                hasFocused = false
                                                val trimmed = textState.trim()
                                                if (trimmed.isEmpty()) {
                                                    onDeleteArea(area)
                                                } else {
                                                    onUpdateArea(area.copy(title = trimmed))
                                                }
                                                onEditingAreaIdChange(null)
                                            }
                                        }
                                    ),
                                    decorationBox = { innerTextField ->
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            if (textState.isEmpty()) {
                                                Text(
                                                    text = "New Area",
                                                    style = MaterialTheme.typography.displaySmall.copy(
                                                        color = textSecondaryColor.copy(alpha = 0.6f)
                                                    )
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )

                                LaunchedEffect(Unit) {
                                    focusRequester.requestFocus()
                                }
                            } else {
                                Text(
                                    text = area.title,
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        color = textPrimaryColor
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (hasProjects) {
                                IconButton(
                                    onClick = { expandedStates[area.id] = !isExpanded },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = "Toggle Area",
                                        tint = textSecondaryColor,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .graphicsLayer(rotationZ = rotationAngle)
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(44.dp))
                            }
                        }
                    }
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
}

/**
 * Отдельный компонент строки проекта с поддержкой жестов Drag-and-Drop,
 * анимации подъема карточки, отображения прогресса и встроенного редактирования.
 */
@Composable
private fun LazyItemScope.ProjectItemRow(
    project: Item,
    allTasks: List<ItemWithChecklist>,
    isEditing: Boolean,
    dragDropState: GenericDragDropState,
    originalProjects: List<Item>,
    localProjectsList: List<Item>,
    areas: List<Area>,
    expandedStates: Map<String, Boolean>,
    cardSurfaceColor: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    onProjectClick: (Item) -> Unit,
    onDeleteProject: (Item) -> Unit,
    onUpdateProject: (Item) -> Unit,
    onEditingProjectIdChange: (String?) -> Unit,
    onLocalProjectsListChange: (List<Item>) -> Unit,
    onProjectsReordered: (List<Item>) -> Unit,
    onExpandArea: (String) -> Unit
) {
    val projectTasks = remember(allTasks, project.id) {
        allTasks.filter { it.item.projectId == project.id }
    }
    val completedCount = projectTasks.count { it.item.isCompleted }
    val totalCount = projectTasks.size

    val isDragging = dragDropState.draggedItemKey == "proj_${project.id}"
    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.04f else 1f,
        animationSpec = spring(),
        label = "projDragScale_${project.id}"
    )
    val dragElev by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        animationSpec = tween(
            durationMillis = 250,
            easing = FastOutSlowInEasing
        ),
        label = "projDragElev_${project.id}"
    )
    val translationY = if (isDragging) dragDropState.dragAccumulatedOffset.value else 0f
    val translationX = if (isDragging) dragDropState.dragAccumulatedOffsetHorizontal.value else 0f
    val zIndexVal = if (isDragging || dragElev > 0.dp) 100f else 0f

    Box(
        modifier = Modifier
            .zIndex(zIndexVal)
            .then(
                if (!isDragging && dragElev == 0.dp) {
                    Modifier.animateItem(
                        placementSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                    )
                } else {
                    Modifier
                }
            )
            .height(46.dp)
            .fillMaxWidth()
    ) {
        // Подложка на физическом месте проекта при перетаскивании (placeholder slot)
        if (isDragging) {
            val isDark = isSystemInDarkTheme()
            val placeholderBgColor = if (isDark) Color(0xFF2C2D32) else Color(0xFFE5E6EB)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.5f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(placeholderBgColor)
            )
        }

        Row(
            modifier = Modifier
                .graphicsLayer {
                    this.translationX = translationX
                    this.translationY = translationY
                    this.scaleX = dragScale
                    this.scaleY = dragScale
                    this.shadowElevation = dragElev.toPx()
                    this.shape = RoundedCornerShape(10.dp)
                    this.clip = false
                }
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isDragging || dragElev > 0.dp) cardSurfaceColor
                    else if (isEditing) ThingsBlue.copy(alpha = 0.15f)
                    else Color.Transparent
                )
                .projectDragAndDrop(
                    state = dragDropState,
                    project = project,
                    originalProjects = originalProjects,
                    localProjectsList = localProjectsList,
                    areas = areas,
                    expandedStates = expandedStates,
                    onLocalProjectsListChange = onLocalProjectsListChange,
                    onProjectsReordered = onProjectsReordered,
                    onExpandArea = onExpandArea
                )
                .clickable(enabled = !isEditing && !isDragging) { onProjectClick(project) }
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                ProjectProgressArc(
                    completed = completedCount,
                    total = totalCount,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(7.dp))
            if (isEditing) {
                var textState by remember { mutableStateOf(project.title) }
                var hasFocused by remember { mutableStateOf(false) }
                val focusRequester = remember { FocusRequester() }

                BasicTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    textStyle = MaterialTheme.typography.displaySmall.copy(
                        color = textPrimaryColor,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(ThingsBlue),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                hasFocused = true
                            } else if (hasFocused) {
                                hasFocused = false
                                val trimmed = textState.trim()
                                if (trimmed.isEmpty() && project.title.isEmpty()) {
                                    onDeleteProject(project)
                                } else if (trimmed.isNotEmpty() && trimmed != project.title) {
                                    onUpdateProject(project.copy(title = trimmed))
                                }
                                onEditingProjectIdChange(null)
                            }
                        },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (hasFocused) {
                                hasFocused = false
                                val trimmed = textState.trim()
                                if (trimmed.isEmpty()) {
                                    onDeleteProject(project)
                                } else {
                                    onUpdateProject(project.copy(title = trimmed))
                                }
                                onEditingProjectIdChange(null)
                            }
                        }
                    ),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (textState.isEmpty()) {
                                Text(
                                    text = "New Project",
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        color = textSecondaryColor.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Normal
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            } else {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.displaySmall.copy(
                        color = textPrimaryColor,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
