package com.example.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.Tag
import com.example.ui.components.ProjectProgressArc
import com.example.ui.components.ThingsCheckbox
import com.example.ui.components.hideSoftKeyboardNow
import com.example.ui.theme.*

/**
 * Модель секции результатов поиска на экране Search.
 */
private data class SearchSectionData(
    val id: String,
    val title: String,
    val icon: @Composable () -> Unit,
    val onClick: (() -> Unit)? = null,
    val showChevron: Boolean = false,
    val tasks: List<ItemWithChecklist> = emptyList(),
    val isLogbook: Boolean = false
)

/**
 * Полноэкранный экран поиска (ActiveScreen.SEARCH) в точном соответствии с эталоном Things 3.
 * Отображает найденные задачи, распределенные по секциям (Logbook, Проекты, Области, Списки),
 * чистыми строками задач без искусственных рамок-карточек.
 */
@Composable
fun ThingsSearchScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    allTasks: List<ItemWithChecklist>,
    projects: List<Item>,
    areas: List<Area>,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    dividerColor: Color,
    onTaskClick: (ItemWithChecklist) -> Unit,
    onTaskToggle: (ItemWithChecklist) -> Unit,
    onBack: () -> Unit,
    onFabClick: () -> Unit,
    allSavedTagObjects: List<Tag> = emptyList(),
    onProjectClick: (Item) -> Unit = {},
    onAreaClick: (Area) -> Unit = {},
    onTagClick: (Tag) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val bkgColor = if (isDark) ThingsBackgroundDark else ThingsBackgroundLight
    val capsuleBkg = if (isDark) Color(0xFF2C2C2E) else Color(0xFFEFEFF0)

    val focusRequester = remember { FocusRequester() }
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = searchQuery,
                selection = TextRange(searchQuery.length)
            )
        )
    }

    LaunchedEffect(searchQuery) {
        if (textFieldValue.text != searchQuery) {
            textFieldValue = textFieldValue.copy(
                text = searchQuery,
                selection = TextRange(searchQuery.length)
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Прямой поиск проектов и областей по названию/заметкам (только неудаленные)
    val matchingProjects = remember(searchQuery, projects) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val q = searchQuery.trim()
            projects.filter {
                it.type == 1 && !it.trashed && (
                    it.title.contains(q, ignoreCase = true) ||
                    it.notes.contains(q, ignoreCase = true)
                )
            }
        }
    }

    val matchingAreas = remember(searchQuery, areas) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val q = searchQuery.trim()
            areas.filter { !it.trashed && it.title.contains(q, ignoreCase = true) }
        }
    }

    val matchingTags = remember(searchQuery, allSavedTagObjects) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val q = searchQuery.trim()
            allSavedTagObjects.filter { it.title.isNotBlank() && it.title.contains(q, ignoreCase = true) }
        }
    }

    // Поиск задач (активных и завершенных) по заголовку, заметкам и чеклистам (только неудаленные)
    val (looseActiveTasks, sections) = remember(searchQuery, allTasks, projects, areas) {
        if (searchQuery.isBlank()) {
            emptyList<ItemWithChecklist>() to emptyList<SearchSectionData>()
        } else {
            val q = searchQuery.trim()
            val matchedTasks = allTasks.filter { wrapper ->
                wrapper.item.type == 0 && !wrapper.item.trashed && (
                    wrapper.item.title.contains(q, ignoreCase = true) ||
                    wrapper.item.notes.contains(q, ignoreCase = true) ||
                    wrapper.checklist.any { it.title.contains(q, ignoreCase = true) }
                )
            }

            // 1. Активные задачи без проекта и области (Inbox, Today, etc.)
            // В эталоне Things 3 отображаются прямо в списке без искусственных заголовков
            val loose = matchedTasks.filter { !it.item.isCompleted && it.item.status != 2 && it.item.projectId == null && it.item.areaId == null }

            val resultSections = mutableListOf<SearchSectionData>()

            // 2. Активные задачи по проектам
            val projectTasksMap = matchedTasks.filter { !it.item.isCompleted && it.item.status != 2 && it.item.projectId != null }
                .groupBy { it.item.projectId!! }

            for ((projId, pTasks) in projectTasksMap) {
                val proj = projects.firstOrNull { it.id == projId && !it.trashed } ?: continue
                val projTitle = proj.title
                val allProjTasks = allTasks.filter { it.item.projectId == projId && it.item.type == 0 && !it.item.trashed }
                val completedCount = allProjTasks.count { it.item.isCompleted }

                resultSections.add(
                    SearchSectionData(
                        id = "proj_$projId",
                        title = projTitle,
                        icon = {
                            ProjectProgressArc(
                                completed = completedCount,
                                total = allProjTasks.size,
                                modifier = Modifier.size(18.dp),
                                color = ThingsBlue
                            )
                        },
                        onClick = { onProjectClick(proj) },
                        showChevron = true,
                        tasks = pTasks,
                        isLogbook = false
                    )
                )
            }

            // 3. Активные задачи по областям (без проекта)
            val areaTasksMap = matchedTasks.filter { !it.item.isCompleted && it.item.status != 2 && it.item.projectId == null && it.item.areaId != null }
                .groupBy { it.item.areaId!! }

            for ((arId, aTasks) in areaTasksMap) {
                val area = areas.firstOrNull { it.id == arId && !it.trashed } ?: continue
                val areaTitle = area.title

                resultSections.add(
                    SearchSectionData(
                        id = "area_$arId",
                        title = areaTitle,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = ThingsSomedayGrey,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = { onAreaClick(area) },
                        showChevron = true,
                        tasks = aTasks,
                        isLogbook = false
                    )
                )
            }

            // 4. Завершенные и отмененные задачи (Logbook) - только неудаленные
            val logbookTasks = matchedTasks.filter { (it.item.isCompleted || it.item.status == 2) && !it.item.trashed }
                .sortedByDescending { it.item.stopDate ?: it.item.modificationDate }

            if (logbookTasks.isNotEmpty()) {
                resultSections.add(
                    SearchSectionData(
                        id = "logbook",
                        title = "Logbook",
                        icon = {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ThingsLogbookGreen)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        },
                        tasks = logbookTasks,
                        isLogbook = true
                    )
                )
            }

            loose to resultSections
        }
    }

    val hasAnyResults = matchingTags.isNotEmpty() || matchingProjects.isNotEmpty() || matchingAreas.isNotEmpty() || looseActiveTasks.isNotEmpty() || sections.isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bkgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Верхняя панель: Стрелка назад слева, кнопка «...» справа
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val view = LocalView.current
                IconButton(onClick = {
                    view.hideSoftKeyboardNow()
                    onBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = textPrimaryColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = { /* Опции контекста поиска */ },
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, textSecondaryColor.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Options",
                        tint = textSecondaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Заголовок: Лупа + "Search" (как в эталоне)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = textPrimaryColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Search",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimaryColor
                )
            }

            // Поисковая капсула
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(capsuleBkg)
                    .padding(horizontal = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = textSecondaryColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search",
                            color = textSecondaryColor.copy(alpha = 0.5f),
                            fontSize = 16.sp
                        )
                    }
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                            if (newValue.text != searchQuery) {
                                onSearchQueryChange(newValue.text)
                            }
                        },
                        textStyle = TextStyle(
                            color = textPrimaryColor,
                            fontSize = 16.sp
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .testTag("fullscreen_search_input")
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(textSecondaryColor.copy(alpha = 0.5f))
                            .clickable {
                                onSearchQueryChange("")
                                textFieldValue = TextFieldValue("", selection = TextRange.Zero)
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = if (isDark) ThingsBackgroundDark else Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            // Список результатов
            if (searchQuery.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Quickly find to-dos, notes,\nchecklists, and completed items.",
                        color = textSecondaryColor.copy(alpha = 0.55f),
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (!hasAnyResults) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.SearchOff,
                            contentDescription = null,
                            tint = textSecondaryColor.copy(alpha = 0.35f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No results found for \"$searchQuery\"",
                            color = textSecondaryColor.copy(alpha = 0.8f),
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
                ) {
                    // 1. Активные задачи верхнего уровня без проектов (Inbox и т.д.)
                    // В эталоне Things 3 выводятся в начале списка чистыми строками без искусственных заголовков
                    if (looseActiveTasks.isNotEmpty()) {
                        itemsIndexed(looseActiveTasks, key = { _, t -> "loose_${t.item.id}" }) { index, wrapper ->
                            SearchTaskRow(
                                taskWrapper = wrapper,
                                isLogbook = false,
                                textPrimaryColor = textPrimaryColor,
                                textSecondaryColor = textSecondaryColor,
                                onTaskClick = onTaskClick,
                                onTaskToggle = onTaskToggle
                            )
                            if (index < looseActiveTasks.size - 1) {
                                HorizontalDivider(
                                    color = dividerColor.copy(alpha = 0.5f),
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 36.dp)
                                )
                            }
                        }
                        item(key = "space_loose") {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Секция найденных проектов (если есть совпадения)
                    if (matchingProjects.isNotEmpty()) {
                        item(key = "hdr_projects") {
                            SearchSectionHeader(
                                title = "Projects",
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.PieChart,
                                        contentDescription = null,
                                        tint = ThingsAnytimeTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                textPrimaryColor = textPrimaryColor,
                                textSecondaryColor = textSecondaryColor,
                                dividerColor = dividerColor
                            )
                        }
                        itemsIndexed(matchingProjects, key = { _, p -> "p_${p.id}" }) { index, proj ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onProjectClick(proj) }
                                    .padding(vertical = 11.dp, horizontal = 4.dp)
                            ) {
                                val allPTasks = allTasks.filter { it.item.projectId == proj.id && it.item.type == 0 }
                                val pCompleted = allPTasks.count { it.item.isCompleted }
                                ProjectProgressArc(
                                    completed = pCompleted,
                                    total = allPTasks.size,
                                    modifier = Modifier.size(18.dp),
                                    color = ThingsBlue
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = proj.title,
                                    color = textPrimaryColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = textSecondaryColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (index < matchingProjects.size - 1) {
                                HorizontalDivider(
                                    color = dividerColor.copy(alpha = 0.5f),
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 32.dp)
                                )
                            }
                        }
                        item(key = "space_projects") {
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }

                    // Секция найденных областей (если есть совпадения)
                    if (matchingAreas.isNotEmpty()) {
                        item(key = "hdr_areas") {
                            SearchSectionHeader(
                                title = "Areas",
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = ThingsSomedayGrey,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                textPrimaryColor = textPrimaryColor,
                                textSecondaryColor = textSecondaryColor,
                                dividerColor = dividerColor
                            )
                        }
                        itemsIndexed(matchingAreas, key = { _, a -> "a_${a.id}" }) { index, area ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAreaClick(area) }
                                    .padding(vertical = 11.dp, horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = ThingsSomedayGrey,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = area.title,
                                    color = textPrimaryColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = textSecondaryColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (index < matchingAreas.size - 1) {
                                HorizontalDivider(
                                    color = dividerColor.copy(alpha = 0.5f),
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 32.dp)
                                )
                            }
                        }
                        item(key = "space_areas") {
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }

                    // Секция найденных тегов (если есть совпадения)
                    if (matchingTags.isNotEmpty()) {
                        item(key = "hdr_tags") {
                            SearchSectionHeader(
                                title = "Tags",
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.LocalOffer,
                                        contentDescription = null,
                                        tint = ThingsSomedayGrey,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                textPrimaryColor = textPrimaryColor,
                                textSecondaryColor = textSecondaryColor,
                                dividerColor = dividerColor
                            )
                        }
                        itemsIndexed(matchingTags, key = { _, tag -> "tag_${tag.id}" }) { index, tag ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTagClick(tag) }
                                    .padding(vertical = 11.dp, horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LocalOffer,
                                    contentDescription = null,
                                    tint = ThingsSomedayGrey,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = tag.title,
                                    color = textPrimaryColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = textSecondaryColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (index < matchingTags.size - 1) {
                                HorizontalDivider(
                                    color = dividerColor.copy(alpha = 0.5f),
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 32.dp)
                                )
                            }
                        }
                        item(key = "space_tags") {
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }

                    // Секции найденных задач
                    sections.forEach { sec ->
                        item(key = "hdr_${sec.id}") {
                            SearchSectionHeader(
                                title = sec.title,
                                icon = sec.icon,
                                onClick = sec.onClick,
                                showChevron = sec.showChevron,
                                textPrimaryColor = textPrimaryColor,
                                textSecondaryColor = textSecondaryColor,
                                dividerColor = dividerColor
                            )
                        }

                        itemsIndexed(sec.tasks, key = { _, t -> "${sec.id}_${t.item.id}" }) { index, wrapper ->
                            SearchTaskRow(
                                taskWrapper = wrapper,
                                isLogbook = sec.isLogbook,
                                textPrimaryColor = textPrimaryColor,
                                textSecondaryColor = textSecondaryColor,
                                onTaskClick = onTaskClick,
                                onTaskToggle = onTaskToggle
                            )
                            if (index < sec.tasks.size - 1) {
                                HorizontalDivider(
                                    color = dividerColor.copy(alpha = 0.5f),
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 36.dp)
                                )
                            }
                        }

                        item(key = "space_${sec.id}") {
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }
                }
            }
        }

        // Кнопка FAB «+» в правом нижнем углу
        FloatingActionButton(
            onClick = onFabClick,
            containerColor = ThingsBlue,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(56.dp)
                .testTag("search_add_task_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create Task From Query",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Заголовок секции результатов поиска с иконкой и тонким разделителем.
 */
@Composable
private fun SearchSectionHeader(
    title: String,
    icon: @Composable () -> Unit,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = false,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    dividerColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimaryColor,
                modifier = Modifier.weight(1f)
            )
            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = textSecondaryColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        HorizontalDivider(
            color = dividerColor,
            thickness = 0.5.dp
        )
    }
}

/**
 * Плоская строка задачи в результатах поиска, точно повторяющая стиль Things 3.
 */
@Composable
private fun SearchTaskRow(
    taskWrapper: ItemWithChecklist,
    isLogbook: Boolean,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    onTaskClick: (ItemWithChecklist) -> Unit,
    onTaskToggle: (ItemWithChecklist) -> Unit
) {
    val task = taskWrapper.item
    val stopMillis = task.stopDate ?: task.modificationDate
    val dateText = remember(stopMillis) {
        java.text.SimpleDateFormat("MM/dd/yy", java.util.Locale.US).format(java.util.Date(stopMillis))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTaskClick(taskWrapper) }
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        val isCancelled = task.status == 2
        val isStruckThrough = task.isCompleted || isCancelled

        if (isCancelled) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ThingsBlue)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancelled",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        } else {
            ThingsCheckbox(
                checked = task.isCompleted,
                onCheckedChange = { onTaskToggle(taskWrapper) },
                size = 18.dp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        if (isLogbook) {
            Text(
                text = dateText,
                color = ThingsBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = if (task.title.isBlank()) "Untitled To-Do" else task.title,
            color = if (isStruckThrough) textSecondaryColor else textPrimaryColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            textDecoration = if (isStruckThrough) TextDecoration.LineThrough else TextDecoration.None,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Индикаторы справа (Заметки, Чеклист, Теги)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (task.notes.isNotBlank()) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = "Notes",
                    tint = textSecondaryColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(15.dp)
                )
            }
            if (taskWrapper.checklist.isNotEmpty()) {
                Icon(
                    imageVector = AppIcons.BulletList,
                    contentDescription = "Checklist",
                    tint = textSecondaryColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(15.dp)
                )
            }
            if (task.cachedTags.isNotBlank()) {
                Icon(
                    imageVector = Icons.Outlined.LocalOffer,
                    contentDescription = "Tags",
                    tint = textSecondaryColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
