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
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.geometry.Offset
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
    onUpdateArea: (Area) -> Unit = {}
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

    Box(modifier = Modifier.fillMaxSize()) {
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

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .graphicsLayer { translationY = pullOffset.value }
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
        }

        // Smart Lists Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SmartListRow(
                    title = "Inbox",
                    icon = Icons.Outlined.Inbox,
                    iconColor = ThingsInboxBlue,
                    count = inboxCount,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    onClick = { onSmartListClick(ActiveScreen.INBOX) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                SmartListRow(
                    title = "Today",
                    icon = Icons.Default.Star,
                    iconColor = ThingsTodayStar,
                    count = todayCount,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    onClick = { onSmartListClick(ActiveScreen.TODAY) }
                )
                SmartListRow(
                    title = "Upcoming",
                    icon = Icons.Outlined.CalendarToday,
                    iconColor = ThingsUpcomingRed,
                    count = 0,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    onClick = { onSmartListClick(ActiveScreen.UPCOMING) }
                )
                SmartListRow(
                    title = "Anytime",
                    icon = Icons.Outlined.Archive,
                    iconColor = ThingsAnytimeTeal,
                    count = 0,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    onClick = { onSmartListClick(ActiveScreen.ANYTIME) }
                )
                SmartListRow(
                    title = "Someday",
                    icon = Icons.Outlined.Folder,
                    iconColor = ThingsSomedayGrey,
                    count = 0,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    onClick = { onSmartListClick(ActiveScreen.SOMEDAY) }
                )
                SmartListRow(
                    title = "Logbook",
                    icon = Icons.Outlined.AssignmentTurnedIn,
                    iconColor = ThingsLogbookGreen,
                    count = 0,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    onClick = { onSmartListClick(ActiveScreen.LOGBOOK) }
                )
            }
        }

        // [ИЗМЕНЕНИЕ]: Горизонтальный разделитель между списком умных категорий и началом списка проектов/областей
        item {
            HorizontalDivider(
                color = dividerColor,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }

        // Two-level Areas & Projects tree
        val noAreaProjects = projects.filter { it.areaId == null }

        // Render projects without area first, as standalone items
        noAreaProjects.forEach { project ->
            item {
                val projectTasks = tasksByProject[project.id] ?: emptyList()
                val completedCount = projectTasks.count { it.item.isCompleted }
                val totalCount = projectTasks.size
                val isEditing = project.id == editingProjectId

                // [ИЗМЕНЕНИЕ]: При inline-редактировании цвет фона становится полупрозрачным голубым
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isEditing) ThingsBlue.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable(enabled = !isEditing) { onProjectClick(project) }
                        .padding(vertical = 6.dp, horizontal = 6.dp),
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
                        // [ИЗМЕНЕНИЕ]: Поле встроенного inline-редактирования названия проекта с защитой от преждевременного удаления
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

        // [ИЗМЕНЕНИЕ]: Уменьшен отступ у разделителя для уменьшения пространства между списком Non-area проектов и областями
        if (noAreaProjects.isNotEmpty() && areas.isNotEmpty()) {
            item {
                HorizontalDivider(
                    color = dividerColor,
                    modifier = Modifier.padding(vertical = 0.dp)
                )
            }
        }

        // Render Areas
        areas.forEachIndexed { index, area ->
            val areaProjects = projects.filter { it.areaId == area.id }
            val hasProjects = areaProjects.isNotEmpty()
            val isAreaEditing = area.id == editingAreaId

            item {
                if (index > 0) {
                    // [ИЗМЕНЕНИЕ]: Горизонтальный разделитель выше любой карточки области (кроме первой, у которой разделитель обеспечивается другими условиями)
                    HorizontalDivider(
                        color = dividerColor,
                        modifier = Modifier.padding(vertical = 0.dp)
                    )
                }
                val isExpanded = expandedStates[area.id] ?: true
                val rotationAngle by animateFloatAsState(
                    targetValue = if (isExpanded) 90f else 0f,
                    label = "rotationAngle"
                )
                // [ИЗМЕНЕНИЕ]: Уменьшен верхний отступ (top padding) до карточки области на 4.dp для компенсации увеличенной высоты иконки
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    // Title block for Area
                    // [ИЗМЕНЕНИЕ]: Установка минимальной высоты карточки пустой области в 48.dp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (!hasProjects) Modifier.heightIn(min = 48.dp) else Modifier
                            )
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isAreaEditing) ThingsBlue.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable(enabled = !isAreaEditing) { onAreaClick(area) }
                            .padding(vertical = 2.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // [ИЗМЕНЕНИЕ]: Ширина иконки области установлена равной 20.dp (как у проекта), а высота увеличена пропорционально до 28.dp
                        // Используем анимированную иконку области, которая реагирует на состояние раскрытия (isClosed = !isExpanded)
                        AreaIconAnimated(
                            isClosed = !isExpanded,
                            onToggle = {}, // Клик на иконку напрямую ничего не делает по требованию
                            modifier = Modifier.size(width = 20.dp, height = 28.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        
                        if (isAreaEditing) {
                            // [ИЗМЕНЕНИЕ]: Поле встроенного inline-редактирования названия области (сферы) с защитой от преждевременного удаления
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
                        
                        // [ИЗМЕНЕНИЕ]: Если в области есть проекты, показываем кнопку раскрытия. Иначе отображаем Spacer(44.dp) для сохранения выравнивания.
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

                    // [ИЗМЕНЕНИЕ]: Плавная анимация раскрытия и сворачивания списка проектов с использованием AnimatedVisibility.
                    // Применяется плавное расширение по высоте (expandVertically) и появление (fadeIn) при открытии,
                    // и сжатие по высоте (shrinkVertically) со скрытием (fadeOut) при закрытии.
                    AnimatedVisibility(
                        visible = isExpanded && hasProjects,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 0.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            areaProjects.forEach { project ->
                                val projectTasks = tasksByProject[project.id] ?: emptyList()
                                val completedCount = projectTasks.count { it.item.isCompleted }
                                val totalCount = projectTasks.size
                                val isProjectEditing = project.id == editingProjectId

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isProjectEditing) ThingsBlue.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable(enabled = !isProjectEditing) { onProjectClick(project) }
                                        .padding(vertical = 6.dp, horizontal = 6.dp),
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
                                    
                                    if (isProjectEditing) {
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
