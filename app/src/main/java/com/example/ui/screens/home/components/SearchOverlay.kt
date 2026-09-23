package com.example.ui.screens.home.components

import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.ui.screens.home.ActiveScreen
import com.example.ui.theme.AppIcons
import com.example.ui.screens.home.subcomponents.TaskItemRow
import com.example.ui.components.ProjectProgressArc
import com.example.ui.components.HideTextSelectionHandles
import com.example.ui.components.hideSoftKeyboardNow
import com.example.ui.theme.*

/**
 * Оверлей полноэкранного поиска, точно стилизованный под Things 3 Quick Find.
 * Включает раздел "Recent" при пустом вводе, раздельные кнопки закрытия,
 * кастомные иконки прогресса проектов и визуальные эффекты подсветки.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThingsSearchOverlay(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    allTasks: List<ItemWithChecklist>,
    projects: List<Item>,
    areas: List<Area>,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    cardSurfaceColor: Color,
    dividerColor: Color,
    onTaskClick: (ItemWithChecklist) -> Unit,
    onProjectClick: (Item) -> Unit,
    onAreaClick: (Area) -> Unit,
    onSmartListClick: (ActiveScreen) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    // [ИЗМЕНЕНИЕ]: Список недавно искавшихся/выбранных в поиске объектов
    recentSearchItems: List<SearchResultItem> = emptyList(),
    onContinueSearchClick: () -> Unit = {},
    onTaskToggle: (ItemWithChecklist) -> Unit = {},
    // Оверлей уже закрывается (идёт exit-анимация), но поле поиска ещё в композиции
    isClosing: Boolean = false,
    initialTopOffset: Dp = 4.dp,
    wasPulled: Boolean = false
) {
    val isDark = isSystemInDarkTheme()
    val coroutineScope = rememberCoroutineScope()
    val expansionAnim = remember { Animatable(0f) }
    val exitAlphaAnim = remember { Animatable(1f) }
    var isMorphClosing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        expansionAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    val view = LocalView.current

    val handleClose: () -> Unit = remember(isMorphClosing) {
        {
            if (!isMorphClosing) {
                isMorphClosing = true
                view.hideSoftKeyboardNow()
                coroutineScope.launch {
                    exitAlphaAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = 180,
                            easing = FastOutSlowInEasing
                        )
                    )
                    onClose()
                }
            }
        }
    }

    BackHandler(enabled = !isMorphClosing) {
        handleClose()
    }

    LaunchedEffect(isClosing) {
        if (isClosing && !isMorphClosing) {
            handleClose()
        }
    }

    // Фокус и клавиатура
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Алгоритм умного поиска (Quick Find)
    val searchResults = remember(searchQuery, allTasks, projects, areas) {
        if (searchQuery.isBlank()) {
            emptyList<SearchResultItem>()
        } else {
            val results = mutableListOf<SearchResultItem>()
            val query = searchQuery.trim()

            // 1. Поиск по смарт-спискам
            val smartLists = listOf(
                Pair("Today", ActiveScreen.TODAY),
                Pair("Inbox", ActiveScreen.INBOX),
                Pair("Upcoming", ActiveScreen.UPCOMING),
                Pair("Anytime", ActiveScreen.ANYTIME),
                Pair("Someday", ActiveScreen.SOMEDAY),
                Pair("Logbook", ActiveScreen.LOGBOOK)
            )
            for ((name, screen) in smartLists) {
                if (name.contains(query, ignoreCase = true)) {
                    results.add(SearchResultItem.SmartListResult(name, screen))
                }
            }



            // 2. Поиск по областям ответственности
            val matchedAreas = areas.filter { it.title.contains(query, ignoreCase = true) }
            results.addAll(matchedAreas.map { SearchResultItem.AreaResult(it) })

            // 3. Поиск по проектам (только активные по названию)
            val matchedProjects = projects.filter {
                it.type == 1 && !it.isCompleted && it.title.contains(query, ignoreCase = true)
            }
            results.addAll(matchedProjects.map { SearchResultItem.ProjectResult(it) })

            // 4. Поиск по задачам (только активные по названию)
            val matchedTasks = allTasks.filter {
                it.item.type == 0 && !it.item.isCompleted && it.item.title.contains(query, ignoreCase = true)
            }
            results.addAll(matchedTasks.map { SearchResultItem.TaskResult(it) })

            results
        }
    }

    // [ИЗМЕНЕНИЕ]: Проверяем, есть ли искомый текст в заметках, чек-листах или Logbook (выполненных задачах/проектах)
    val showContinueSearch = remember(searchQuery, allTasks, projects) {
        if (searchQuery.isBlank()) {
            false
        } else {
            val query = searchQuery.trim()
            val matchesInTasks = allTasks.any { wrapper ->
                val matchesInNotes = wrapper.item.notes.contains(query, ignoreCase = true)
                val matchesInChecklist = wrapper.checklist.any { it.title.contains(query, ignoreCase = true) }
                val matchesInLogbook = wrapper.item.isCompleted && (
                    wrapper.item.title.contains(query, ignoreCase = true) ||
                    wrapper.item.notes.contains(query, ignoreCase = true) ||
                    wrapper.checklist.any { it.title.contains(query, ignoreCase = true) }
                )
                matchesInNotes || matchesInChecklist || matchesInLogbook
            }
            val matchesInProjectNotes = projects.any { proj ->
                proj.type == 1 && proj.notes.contains(query, ignoreCase = true)
            }
            val matchesInProjectLogbook = projects.any { proj ->
                proj.type == 1 && proj.isCompleted && proj.title.contains(query, ignoreCase = true)
            }
            matchesInTasks || matchesInProjectNotes || matchesInProjectLogbook
        }
    }

    // Имя первого доступного проекта для отображения в "Recent"
    val recentProjectName = remember(projects) {
        projects.firstOrNull { it.type == 1 }?.title ?: "Vacation in Rome"
    }
    val recentProject = remember(projects) {
        projects.firstOrNull { it.type == 1 }
    }

    val progress = expansionAnim.value

    val cardBackground = if (isDark) Color(0xFF1C1C1E) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF1C1C1E)
    val textSecondary = if (isDark) Color(0xFF8E8E93) else Color(0xFF8E8E93)
    val inputNormalBackground = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
    val startCardBg = if (wasPulled) ThingsBlue else inputNormalBackground
    val currentCardBg = androidx.compose.ui.graphics.lerp(startCardBg, cardBackground, progress)
    val currentInputBg = androidx.compose.ui.graphics.lerp(Color.Transparent, inputNormalBackground, progress)
    val closeButtonBackground = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)

    val currentHorizontalMargin = androidx.compose.ui.unit.lerp(20.dp, 14.dp, progress)
    val currentTopPadding = androidx.compose.ui.unit.lerp(initialTopOffset, 20.dp, progress)
    val currentCornerRadius = androidx.compose.ui.unit.lerp(22.dp, 24.dp, progress)
    val currentElevation = androidx.compose.ui.unit.lerp(0.dp, 12.dp, progress)

    val innerHorizontalPadding = androidx.compose.ui.unit.lerp(0.dp, 14.dp, progress)
    val innerTopPadding = androidx.compose.ui.unit.lerp(0.dp, 20.dp, progress)

    val exitAlpha = exitAlphaAnim.value
    val backdropAlpha = (progress * 0.45f * exitAlpha).coerceIn(0f, 0.45f)
    val closeButtonAlpha = ((progress - 0.35f) / 0.65f).coerceIn(0f, 1f)
    val closeButtonWidth = androidx.compose.ui.unit.lerp(0.dp, 44.dp, closeButtonAlpha)
    val closeButtonSpacer = androidx.compose.ui.unit.lerp(0.dp, 12.dp, closeButtonAlpha)

    val contentAlpha = ((progress - 0.4f) / 0.6f).coerceIn(0f, 1f)
    val contentTranslationY = (-16).dp * (1f - progress)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backdropAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = handleClose
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        // [ИЗМЕНЕНИЕ]: Контейнер диалога поиска с бесшовным морфингом при открытии и чистым fade-out при закрытии
        Card(
            colors = CardDefaults.cardColors(containerColor = currentCardBg),
            shape = RoundedCornerShape(currentCornerRadius),
            elevation = CardDefaults.cardElevation(defaultElevation = currentElevation),
            modifier = Modifier
                .graphicsLayer { alpha = exitAlpha }
                .fillMaxWidth()
                .padding(start = currentHorizontalMargin, end = currentHorizontalMargin, top = currentTopPadding, bottom = 20.dp)
                .widthIn(max = 480.dp)
                .clip(RoundedCornerShape(currentCornerRadius))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Игнорировать клики внутри карты
                )
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints.copy(minHeight = 0))
                    val minHeightPx = 44.dp.roundToPx()
                    val fullHeight = placeable.height
                    val currentHeight = if (fullHeight > minHeightPx) {
                        (minHeightPx + (fullHeight - minHeightPx) * progress).toInt()
                    } else {
                        minHeightPx
                    }
                    layout(placeable.width, currentHeight) {
                        placeable.place(0, 0)
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                // Верхний заголовок со строкой поиска и кнопкой закрытия
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = innerHorizontalPadding)
                        .padding(top = innerTopPadding)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Капсула поискового ввода
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(currentInputBg)
                                .padding(horizontal = 14.dp)
                        ) {
                            val startIconTint = if (wasPulled) Color.White else textSecondary
                            val currentIconTint = androidx.compose.ui.graphics.lerp(startIconTint, textSecondary, progress)
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = currentIconTint,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    val startTextTint = if (wasPulled) Color.White.copy(alpha = 0.9f) else textSecondary.copy(alpha = 0.6f)
                                    val currentTextTint = androidx.compose.ui.graphics.lerp(startTextTint, textSecondary.copy(alpha = 0.6f), progress)
                                    Text(
                                        text = "Quick Find",
                                        color = currentTextTint,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                                // Пока оверлей закрывается, курсор и его маркер не рисуем: фокус остаётся
                                // до полного скрытия клавиатуры
                                HideTextSelectionHandles(hidden = isMorphClosing || isClosing) {
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = onSearchQueryChange,
                                        textStyle = TextStyle(
                                            color = textPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Normal
                                        ),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        cursorBrush = SolidColor(if (isMorphClosing || isClosing) Color.Unspecified else Color.Black),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(focusRequester)
                                            .testTag("overlay_search_input")
                                    )
                                }
                            }
                            
                            // Кнопка очистки текста внутри инпута
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = "Clear",
                                        tint = textSecondary.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Standalone круглая кнопка "X" закрытия с плавным Fade-in
                        if (closeButtonAlpha > 0f) {
                            Spacer(modifier = Modifier.width(closeButtonSpacer))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(closeButtonWidth, 44.dp)
                                    .graphicsLayer {
                                        alpha = closeButtonAlpha
                                    }
                                    .clip(CircleShape)
                                    .background(closeButtonBackground)
                                    .clickable(onClick = handleClose)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Контент: Либо "Recent", либо "Результаты поиска" (с плавным проявлением и микросдвигом)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = contentAlpha
                            translationY = contentTranslationY.toPx()
                        }
                        .padding(horizontal = 14.dp)
                ) {
                    if (searchQuery.isBlank()) {
                        // РЕЖИМ 1: Стартовый экран (без запроса)
                        Text(
                            text = "Recent",
                            color = textSecondary.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Отображаем недавно найденные в поиске объекты, либо фоллбек по умолчанию
                            if (recentSearchItems.isNotEmpty()) {
                                recentSearchItems.take(5).forEach { result ->
                                    SearchResultRow(
                                        result = result,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        allTasks = allTasks,
                                        projects = projects,
                                        areas = areas,
                                        onTaskClick = onTaskClick,
                                        onProjectClick = onProjectClick,
                                        onAreaClick = onAreaClick,
                                        onSmartListClick = onSmartListClick,
                                        onTaskToggle = onTaskToggle
                                    )
                                }
                            } else {
                                // 1. Today
                                RecentRow(
                                    title = "Today",
                                    icon = {
                                        Icon(
                                            imageVector = AppIcons.Today,
                                            contentDescription = null,
                                            tint = Color.Unspecified,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    textPrimary = textPrimary,
                                    onClick = { onSmartListClick(ActiveScreen.TODAY) }
                                )

                                // 2. Important
                                RecentRow(
                                    title = "Important",
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Outlined.LocalOffer,
                                            contentDescription = null,
                                            tint = ThingsSomedayGrey,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    textPrimary = textPrimary,
                                    onClick = {
                                        onSmartListClick(ActiveScreen.ANYTIME)
                                    }
                                )

                                // 3. Динамический или статический проект (как Vacation in Rome)
                                RecentRow(
                                    title = recentProjectName,
                                    icon = {
                                        Canvas(modifier = Modifier.size(20.dp)) {
                                            drawCircle(
                                                color = ThingsAnytimeTeal.copy(alpha = 0.2f),
                                                radius = size.minDimension / 2f
                                            )
                                            drawArc(
                                                color = ThingsAnytimeTeal,
                                                startAngle = -90f,
                                                sweepAngle = 120f,
                                                useCenter = true
                                            )
                                        }
                                    },
                                    textPrimary = textPrimary,
                                    onClick = {
                                        if (recentProject != null) {
                                            onProjectClick(recentProject)
                                        } else {
                                            onSmartListClick(ActiveScreen.INBOX)
                                        }
                                    }
                                )

                                // 4. Upcoming
                                RecentRow(
                                    title = "Upcoming",
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = ThingsUpcomingRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    textPrimary = textPrimary,
                                    onClick = { onSmartListClick(ActiveScreen.UPCOMING) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Подпись внизу
                        Text(
                            text = "Quickly switch lists, find to-dos,\nsearch for tags...",
                            color = textSecondary.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Normal,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp)
                        )
                    } else {
                        // РЕЖИМ 2: Экран результатов поиска
                        if (searchResults.isEmpty()) {
                            if (!showContinueSearch) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Outlined.SearchOff,
                                            contentDescription = null,
                                            tint = textSecondary.copy(alpha = 0.4f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No results found for \"$searchQuery\"",
                                            color = textSecondary,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            } else {
                                // [ИЗМЕНЕНИЕ]: Показываем только элемент Continue Search, если видимых результатов нет, но есть совпадения в глубоком поиске
                                ContinueSearchTaskRow(
                                    onClick = onContinueSearchClick,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            // Список отфильтрованных пунктов
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 350.dp)
                                    .padding(bottom = 12.dp)
                            ) {
                                val visibleResults = searchResults.take(15)
                                itemsIndexed(visibleResults) { index, result ->
                                    val isFirstTask = result is SearchResultItem.TaskResult &&
                                            index > 0 &&
                                            visibleResults[index - 1] !is SearchResultItem.TaskResult // [ИЗМЕНЕНИЕ]: Проверяем только прошлый элемент, чтобы избежать разделителей между задачами
                                    
                                    if (isFirstTask) {
                                        HorizontalDivider(
                                            color = dividerColor,
                                            thickness = 0.5.dp,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                    }

                                    SearchResultRow(
                                        result = result,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        allTasks = allTasks,
                                        projects = projects,
                                        areas = areas,
                                        onTaskClick = onTaskClick,
                                        onProjectClick = onProjectClick,
                                        onAreaClick = onAreaClick,
                                        onSmartListClick = onSmartListClick,
                                        onTaskToggle = onTaskToggle
                                    )
                                }
                                
                                // [ИЗМЕНЕНИЕ]: Отображаем кнопку "Continue Search" в конце прокручиваемого списка результатов поиска в качестве элемента, аналогичного задаче, если showContinueSearch — true
                                if (showContinueSearch) {
                                    item {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        ContinueSearchTaskRow(
                                            onClick = onContinueSearchClick,
                                            modifier = Modifier.fillMaxWidth()
                                        )
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
 * [ИЗМЕНЕНИЕ]: Компонент строки "Continue Search" (только иконка и заголовок, оба черного цвета)
 */
@Composable
fun ContinueSearchTaskRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 4.dp)
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Continue Search",
            color = Color.Black,
            fontSize = MaterialTheme.typography.titleMedium.fontSize,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun RecentRow(
    title: String,
    icon: @Composable () -> Unit,
    textPrimary: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp)
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Запечатанный класс для единообразного отображения разных результатов (Quick Find)
 */
sealed class SearchResultItem {
    data class SmartListResult(val title: String, val screen: ActiveScreen) : SearchResultItem()
    data class SpecialResult(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val iconColor: Color) : SearchResultItem()
    data class AreaResult(val area: Area) : SearchResultItem()
    data class ProjectResult(val project: Item) : SearchResultItem()
    data class TaskResult(val taskWrapper: ItemWithChecklist) : SearchResultItem()
}

@Composable
fun SearchResultRow(
    result: SearchResultItem,
    textPrimary: Color,
    textSecondary: Color,
    allTasks: List<ItemWithChecklist>,
    projects: List<Item>,
    areas: List<Area> = emptyList(),
    onTaskClick: (ItemWithChecklist) -> Unit,
    onProjectClick: (Item) -> Unit,
    onAreaClick: (Area) -> Unit,
    onSmartListClick: (ActiveScreen) -> Unit,
    onTaskToggle: (ItemWithChecklist) -> Unit = {}
) {
    when (result) {
        is SearchResultItem.TaskResult -> {
            TaskItemRow(
                task = result.taskWrapper.item,
                textPrimaryColor = textPrimary,
                textSecondaryColor = textSecondary,
                dividerColor = Color.Transparent,
                onToggle = { onTaskToggle(result.taskWrapper) },
                onClick = { onTaskClick(result.taskWrapper) },
                projects = projects,
                areas = areas,
                leftColumnWidth = MaterialTheme.dimens.searchLeftColumnWidth,
                spacingToText = MaterialTheme.dimens.searchSpacingToText
            )
        }
        is SearchResultItem.ProjectResult -> {
            val totalCount = allTasks.count { it.item.projectId == result.project.id && it.item.type == 0 }
            val completedCount = allTasks.count { it.item.projectId == result.project.id && it.item.type == 0 && it.item.isCompleted }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onProjectClick(result.project) }
                    .padding(start = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(MaterialTheme.dimens.searchLeftColumnWidth),
                    contentAlignment = Alignment.Center
                ) {
                    ProjectProgressArc(
                        completed = completedCount,
                        total = totalCount,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(MaterialTheme.dimens.searchSpacingToText))
                Text(
                    text = result.project.title,
                    style = MaterialTheme.typography.displaySmall.copy(
                        color = textPrimary,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        else -> {
            val isHighlighted = remember { false }
            val rowBg = if (isHighlighted) ThingsInboxBlue.copy(alpha = 0.12f) else Color.Transparent
            val textStyleColor = if (isHighlighted) ThingsInboxBlue else textPrimary

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(rowBg)
                    .clickable {
                        when (result) {
                            is SearchResultItem.SmartListResult -> onSmartListClick(result.screen)
                            is SearchResultItem.SpecialResult -> onSmartListClick(ActiveScreen.INBOX)
                            is SearchResultItem.AreaResult -> onAreaClick(result.area)
                            else -> {}
                        }
                    }
                    .padding(start = 8.dp, end = 4.dp)
            ) {
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (result) {
                        is SearchResultItem.SmartListResult -> {
                            val info = when (result.screen) {
                                ActiveScreen.TODAY -> Pair(AppIcons.Today, Color.Unspecified)
                                ActiveScreen.INBOX -> Pair(AppIcons.Inbox, Color.Unspecified)
                                ActiveScreen.UPCOMING -> Pair(AppIcons.Upcoming, Color.Unspecified)
                                ActiveScreen.ANYTIME -> Pair(AppIcons.Anytime, Color.Unspecified)
                                ActiveScreen.SOMEDAY -> Pair(AppIcons.Someday, Color.Unspecified)
                                ActiveScreen.LOGBOOK -> Pair(AppIcons.Logbook, Color.Unspecified)
                                else -> Pair(Icons.Default.Layers, ThingsSomedayGrey)
                            }
                            Icon(imageVector = info.first, contentDescription = null, tint = info.second, modifier = Modifier.size(20.dp))
                        }
                        is SearchResultItem.SpecialResult -> {
                            Icon(imageVector = result.icon, contentDescription = null, tint = result.iconColor, modifier = Modifier.size(20.dp))
                        }
                        is SearchResultItem.AreaResult -> {
                            Icon(imageVector = Icons.Default.Layers, contentDescription = null, tint = ThingsSomedayGrey, modifier = Modifier.size(20.dp))
                        }
                        else -> {}
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val title = when (result) {
                        is SearchResultItem.SmartListResult -> result.title
                        is SearchResultItem.SpecialResult -> result.title
                        is SearchResultItem.AreaResult -> result.area.title
                        else -> ""
                    }
                    Text(
                        text = title,
                        color = textStyleColor,
                        fontSize = MaterialTheme.typography.titleMedium.fontSize,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val subText = when (result) {
                        is SearchResultItem.AreaResult -> "Area"
                        is SearchResultItem.SmartListResult -> "List"
                        else -> null
                    }

                    if (!subText.isNullOrBlank()) {
                        Text(
                            text = subText,
                            color = textSecondary.copy(alpha = 0.65f),
                            fontSize = MaterialTheme.typography.bodySmall.fontSize,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
