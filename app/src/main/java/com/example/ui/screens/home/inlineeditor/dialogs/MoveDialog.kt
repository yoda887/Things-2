package com.example.ui.screens.home.inlineeditor.dialogs

import com.example.ui.theme.ThingsAlpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Add
import com.example.ui.components.hideSoftKeyboardNow
import com.example.ui.theme.ThingsMotion
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import com.example.domain.lists.ListRules
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.pluralStringResource
import com.example.domain.lists.HomeList
import com.example.ui.theme.ThingsStroke
import com.example.ui.theme.ThingsSpacing
import com.example.ui.theme.ThingsIconSize
import com.example.R
import androidx.compose.ui.res.stringResource
import com.example.ui.theme.ThingsTheme
import com.example.ui.theme.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.ui.components.ProjectProgressArc
import androidx.compose.material.icons.outlined.Layers
import com.example.ui.theme.dimens



/**
 * [ИЗМЕНЕНИЕ]: Диалог выбора назначения перемещения ("Move") для задачи.
 * Стиль полностью унифицирован с ThingsTagDialog, включая цвета, размеры, круглый крестик.
 * Список содержит "Inbox", "No project", далее разделитель, проекты без области и проекты, разбитые по областям.
 * С [areasOnly] — для перемещения самого проекта: только «Без области» и области, без «Входящих» и проектов.
 */
@Composable
fun ThingsMoveDialog(
    currentProjectId: String?,
    currentAreaId: String?,
    currentIsInbox: Boolean,
    projects: List<Item>,
    areas: List<Area>,
    allTasks: List<ItemWithChecklist>,
    onMove: (projectId: String?, areaId: String?, moveToInbox: Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    areasOnly: Boolean = false,
    // «+ Новый проект „…“» в конце результатов поиска: проект с названием из поиска, задачи — в него.
    // null — строки нет (перенос самого проекта в область)
    onMoveToNewProject: ((title: String) -> Unit)? = null,
    // Подзаголовки проектов: поиск находит и их — строкой проекта с подписью подзаголовка; выбор переносит
    // задачу в проект сразу под подзаголовок. null — подзаголовки не ищутся
    headings: List<Item> = emptyList(),
    onMoveToHeading: ((projectId: String, areaId: String?, headingId: String) -> Unit)? = null
) {
    // Группы (id области, NO_AREA_GROUP — без области), где поздние проекты раскрыты. Если задача уже в
    // позднем проекте, его группа раскрыта сразу — иначе отметка текущего проекта была бы спрятана
    var expandedLater by remember {
        mutableStateOf(
            projects.firstOrNull { it.id == currentProjectId && !HomeList.showsProject(it) }
                ?.let { setOf(it.areaId?.takeIf { id -> id.isNotBlank() } ?: NO_AREA_GROUP) } ?: emptySet()
        )
    }
    var query by remember { mutableStateOf("") }
    var searchFocused by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onDismissRequest,
        // Окно во весь экран, без системных отступов: затемнение, появление и закрытие рисуем сами, как у
        // окна тегов. Нажатие мимо карточки ловит сам диалог
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false
        )
    ) {
        val dialogWindow = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        // Затемнение — той же силы, что у системы и у остальных окон (как в окне тегов)
        val dimAmount = remember(dialogWindow) {
            val amount = dialogWindow?.attributes?.dimAmount ?: 0f
            dialogWindow?.setWindowAnimations(R.style.DialogWindowAnimationNone)
            dialogWindow?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            dialogWindow?.addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                dialogWindow?.let { window ->
                    window.attributes = window.attributes.apply {
                        fitInsetsTypes = 0
                        layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    }
                }
            }
            amount
        }
        val appear = remember { androidx.compose.animation.core.Animatable(0f) }
        var closing by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val dialogView = androidx.compose.ui.platform.LocalView.current
        LaunchedEffect(Unit) {
            appear.animateTo(1f, androidx.compose.animation.core.tween(ThingsMotion.QUICK, easing = androidx.compose.animation.core.FastOutSlowInEasing))
        }

        /** Закрывает окно: карточка сжимается и гаснет вместе с затемнением, затем окно убирается. */
        fun closeWindow() {
            if (closing) return
            closing = true
            dialogView.hideSoftKeyboardNow()
            scope.launch {
                appear.animateTo(0f, androidx.compose.animation.core.tween(ThingsMotion.FAST, easing = androidx.compose.animation.core.LinearEasing))
                onDismissRequest()
            }
        }
        /** Выбор места: окно закрывается той же анимацией, что при отмене, затем выполняется [action]. */
        fun closeThen(action: () -> Unit) {
            if (closing) return
            closing = true
            dialogView.hideSoftKeyboardNow()
            scope.launch {
                appear.animateTo(0f, androidx.compose.animation.core.tween(ThingsMotion.FAST, easing = androidx.compose.animation.core.LinearEasing))
                action()
            }
        }
        val moveAnimated: (String?, String?, Boolean) -> Unit = { projectId, areaId, toInbox ->
            closeThen { onMove(projectId, areaId, toInbox) }
        }
        androidx.activity.compose.BackHandler { closeWindow() }

        val scrimColor = ThingsTheme.colors.scrim
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(scrimColor, alpha = dimAmount * appear.value) }
                .pointerInput(Unit) { detectTapGestures(onTap = { closeWindow() }) },
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                shape = ThingsTheme.shapes.dialogShape,
                colors = CardDefaults.cardColors(
                    containerColor = ThingsTheme.colors.overlaySurface
                ),
                modifier = Modifier
                    // Ближе к верху экрана, как в эталоне
                    .padding(top = maxHeight * MOVE_DIALOG_TOP_FRACTION)
                    .graphicsLayer {
                        val progress = appear.value
                        val fromScale = if (closing) ThingsMotion.DIALOG_EXIT_SCALE else ThingsMotion.DIALOG_ENTER_SCALE
                        val scale = fromScale + (1f - fromScale) * progress
                        scaleX = scale
                        scaleY = scale
                        alpha = progress
                    }
                    .width(maxWidth * MaterialTheme.dimens.dialogWidthFraction)
                    .height(maxHeight * MaterialTheme.dimens.dialogHeightFraction)
                    .pointerInput(Unit) { detectTapGestures() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = MaterialTheme.dimens.dialogInnerContentPadding,
                            bottom = MaterialTheme.dimens.dialogInnerContentPadding
                        )
                ) {
                    // Header Row with Cancel Button alignment matching Tag Dialog
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = MaterialTheme.dimens.dialogInnerContentPadding,
                                end = MaterialTheme.dimens.dialogInnerContentPadding,
                                bottom = ThingsSpacing.L // до поля поиска — как между строками списка
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Invisible placeholder for centering
                        Box(modifier = Modifier.size(MaterialTheme.dimens.dialogHeaderButtonSize))

                        Text(
                            text = stringResource(R.string.batch_action_move),
                            color = ThingsTheme.colors.overlayContent,
                            style = ThingsTheme.type.dialogTitle,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )

                        // Cancel 'X' Button in Circle
                        Box(
                            modifier = Modifier
                                .size(MaterialTheme.dimens.dialogHeaderButtonSize)
                                .clip(CircleShape)
                                .background(ThingsTheme.colors.overlayControl)
                                .clickable { closeWindow() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.tag_dialog_cancel),
                                tint = ThingsTheme.colors.overlayContent,
                                modifier = Modifier.size(ThingsIconSize.S)
                            )
                        }
                    }

                    // Результаты поиска: проекты, подзаголовки, области; первый подсвечен — «Go» выбирает его,
                    // а если ничего не найдено — создаёт новый проект с этим названием
                    val inboxTitle = stringResource(R.string.category_inbox)
                    val noPlaceTitle = stringResource(if (areasOnly) R.string.move_no_area else R.string.move_no_project)
                    val found = remember(query, projects, areas, headings, areasOnly, inboxTitle, noPlaceTitle) {
                        moveSearch(
                            query.trim(), if (areasOnly) emptyList() else projects, areas,
                            if (onMoveToHeading == null) emptyList() else headings,
                            inboxTitle = if (areasOnly) null else inboxTitle,
                            noPlaceTitle = noPlaceTitle
                        )
                    }
                    fun choose(result: MoveFound) {
                        when (result) {
                            is MoveFound.Project -> moveAnimated(result.project.id, result.project.areaId, false)
                            is MoveFound.Heading -> closeThen { onMoveToHeading?.invoke(result.project.id, result.project.areaId, result.heading.id) }
                            is MoveFound.Area -> moveAnimated(null, result.area.id, false)
                            MoveFound.Inbox -> moveAnimated(null, null, true)
                            MoveFound.NoPlace -> moveAnimated(null, null, false)
                        }
                    }
                    MoveSearchField(
                        query = query,
                        onQueryChange = { query = it },
                        onFocusChange = { searchFocused = it },
                        onGo = {
                            val first = found.firstOrNull()
                            when {
                                first != null -> choose(first)
                                query.isNotBlank() && onMoveToNewProject != null -> closeThen { onMoveToNewProject(query.trim()) }
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = MaterialTheme.dimens.dialogInnerContentPadding)
                            // Зазор до «Входящих» — как между строками списка (их поле S_PLUS + промежуток XXS)
                            .padding(bottom = ThingsSpacing.S_PLUS)
                    )

                    // Content List — пока поле поиска в фокусе и пустое, список приглушён, как в эталоне
                    val listAlpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (searchFocused && query.isEmpty()) ThingsAlpha.MUTED else 1f,
                        animationSpec = androidx.compose.animation.core.tween(ThingsMotion.QUICK),
                        label = "moveListAlpha"
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .graphicsLayer { alpha = listAlpha }
                            // Строки и разделители — чуть дальше от краёв окна, чем крестик и поле поиска:
                            // этот отступ + их собственный L_PLUS
                            .padding(horizontal = MaterialTheme.dimens.dialogInnerContentPadding - ThingsSpacing.M),
                        contentPadding = PaddingValues(top = ThingsSpacing.XXS, bottom = ThingsSpacing.XS),
                        verticalArrangement = Arrangement.spacedBy(ThingsSpacing.XXS)
                    ) {
                        if (query.isNotBlank()) {
                            searchResults(
                                found = found,
                                currentProjectId = currentProjectId,
                                currentAreaId = currentAreaId,
                                allTasks = allTasks,
                                currentIsInbox = currentIsInbox,
                                inboxTitle = inboxTitle,
                                noPlaceTitle = noPlaceTitle,
                                onChoose = ::choose
                            )
                            if (onMoveToNewProject != null) {
                                newProjectItem(query.trim(), highlighted = found.isEmpty(), onCreate = { title -> closeThen { onMoveToNewProject(title) } })
                            }
                        } else {
                        // 1. Inbox Option
                        if (!areasOnly) item {
                            val isSelected = currentProjectId == null && currentAreaId == null && currentIsInbox
                            MoveDialogRow(
                                title = stringResource(R.string.category_inbox),
                                icon = Icons.Default.Inbox,
                                iconColor = ThingsTheme.colors.overlayContentSecondary,
                                isSelected = isSelected,
                                onClick = {
                                    moveAnimated(null, null, true)
                                }
                            )
                        }

                        // 2. Clear/No project Option
                        item {
                            val isSelected = currentProjectId == null && currentAreaId == null && !currentIsInbox
                            MoveDialogRow(
                                title = stringResource(if (areasOnly) R.string.move_no_area else R.string.move_no_project),
                                icon = Icons.Default.Close,
                                iconColor = ThingsTheme.colors.overlayContentSecondary,
                                isSelected = isSelected,
                                onClick = {
                                    moveAnimated(null, null, false)
                                }
                            )
                        }

                        // Horizontal Divider
                        item {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = ThingsSpacing.XS, horizontal = ThingsSpacing.L_PLUS),
                                color = ThingsTheme.colors.overlayDivider,
                                thickness = ThingsStroke.THIN
                            )
                        }

                        // Filters for Projects
                        val projectsWithNoArea = projects.filter { it.areaId.isNullOrBlank() }
                        val (noAreaShown, noAreaLater) = splitLater(projectsWithNoArea, expandedLater, NO_AREA_GROUP)

                        // 3. Projects without Area
                        if (!areasOnly && projectsWithNoArea.isNotEmpty()) {
                            items(noAreaShown, key = { "proj_no_area_${it.id}" }) { project ->
                                val isSelected = currentProjectId == project.id
                                val projectTasks = allTasks.filter { it.item.projectId == project.id }
                                val completedCount = projectTasks.count { it.item.isClosed }
                                val totalCount = projectTasks.size
                                MoveDialogRow(
                                    title = project.title,
                                    icon = null,
                                    iconColor = ThingsTheme.colors.overlayContent,
                                    isSelected = isSelected,
                                    isProject = true,
                                    completed = completedCount,
                                    total = totalCount,
                                    dashed = project.start == Item.START_SOMEDAY,
                                    indentation = ThingsSpacing.NONE,
                                    onClick = {
                                        moveAnimated(project.id, null, false)
                                    }
                                )
                            }
                            laterToggleItem(NO_AREA_GROUP, noAreaLater, expandedLater) { expandedLater = it }
                        }

                        // 4. Areas and Projects grouped under Areas
                        areas.forEach { area ->
                            // Horizontal divider before each area
                            item(key = "divider_${area.id}") {
                                HorizontalDivider(
                                    modifier = Modifier.padding(top = ThingsSpacing.XS, bottom = ThingsSpacing.XS, start = ThingsSpacing.L_PLUS, end = ThingsSpacing.L_PLUS),
                                    color = ThingsTheme.colors.overlayDivider,
                                    thickness = ThingsStroke.THIN
                                )
                            }
                            // Header of Area Group (renders Area as clickable as well)
                            item(key = "area_${area.id}") {
                                val isSelected = currentAreaId == area.id && currentProjectId == null
                                MoveDialogRow(
                                    title = area.title,
                                    icon = AppIcons.Area,
                                    iconColor = ThingsTheme.colors.area,
                                    isSelected = isSelected,
                                    isAreaHeader = true,
                                    onClick = {
                                        moveAnimated(null, area.id, false)
                                    }
                                )
                            }

                            // Projects belonging to this Area
                            val areaProjects = if (areasOnly) emptyList() else projects.filter { it.areaId == area.id }
                            val (areaShown, areaLater) = splitLater(areaProjects, expandedLater, area.id)
                            items(areaShown, key = { "proj_${area.id}_${it.id}" }) { project ->
                                val isSelected = currentProjectId == project.id
                                val projectTasks = allTasks.filter { it.item.projectId == project.id }
                                val completedCount = projectTasks.count { it.item.isClosed }
                                val totalCount = projectTasks.size
                                MoveDialogRow(
                                    title = project.title,
                                    icon = null,
                                    iconColor = ThingsTheme.colors.overlayContent,
                                    isSelected = isSelected,
                                    isProject = true,
                                    completed = completedCount,
                                    total = totalCount,
                                    dashed = project.start == Item.START_SOMEDAY,
                                    indentation = ThingsSpacing.NONE,
                                    onClick = {
                                        moveAnimated(project.id, area.id, false)
                                    }
                                )
                            }
                            laterToggleItem(area.id, areaLater, expandedLater) { expandedLater = it }
                        }
                        }
                    }
                }
            }
        }
    }
}

/**
 * [ИЗМЕНЕНИЕ]: Отдельная строка списка выбора перемещения для обеспечения чистоты и переиспользования.
 */
@Composable
private fun MoveDialogRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    iconColor: Color,
    isSelected: Boolean,
    indentation: androidx.compose.ui.unit.Dp = ThingsSpacing.NONE,
    isAreaHeader: Boolean = false,
    isProject: Boolean = false,
    completed: Int = 0,
    total: Int = 0,
    dashed: Boolean = false,
    // Первый результат поиска — залит акцентом, как в эталоне: его выбирает «Go» на клавиатуре
    isHighlighted: Boolean = false,
    // Вторая строка — найденный подзаголовок проекта
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indentation)
            .clip(ThingsTheme.shapes.rowShape)
            .background(if (isHighlighted) ThingsTheme.colors.accent else Color.Transparent)
            // Текущее место отмечено только галочкой, как в эталоне; нажатие подсвечивает строку
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = androidx.compose.foundation.LocalIndication.current
            ) { onClick() }
            // Чуть дальше от краёв окна, чем разделители и поле поиска
            // Строка с подзаголовком — той же высоты, что обычная (как в эталоне): поля почти нет,
            // обе строки текста — плотно
            .padding(horizontal = ThingsSpacing.L_PLUS, vertical = if (subtitle != null) ThingsSpacing.XS else ThingsSpacing.S_PLUS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isProject) {
            Box(
                modifier = Modifier.size(ThingsIconSize.M),
                contentAlignment = Alignment.Center
            ) {
                ProjectProgressArc(
                    completed = completed,
                    total = total,
                    // Голубой акцент темы; на подсвеченной строке — белым
                    color = if (isHighlighted) ThingsTheme.colors.onAccent else ThingsTheme.colors.accent,
                    dashed = dashed,
                    modifier = Modifier.size(ThingsIconSize.M)
                )
            }
            Spacer(modifier = Modifier.width(ThingsSpacing.M))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isHighlighted) ThingsTheme.colors.onAccent else iconColor,
                modifier = Modifier.size(ThingsIconSize.M)
            )
            Spacer(modifier = Modifier.width(ThingsSpacing.M))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isHighlighted) ThingsTheme.colors.onAccent else ThingsTheme.colors.overlayContent,
                // Проекты и «Входящие» — обычным начертанием (стиль dialogRow), заголовки областей — жирным
                style = if (subtitle != null) ThingsTheme.type.dialogRow.let { it.copy(lineHeight = it.fontSize) } else ThingsTheme.type.dialogRow,
                fontWeight = if (isAreaHeader) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = ThingsTheme.type.caption.let {
                        it.copy(
                            lineHeight = it.fontSize,
                            color = if (isHighlighted) ThingsTheme.colors.onAccent else ThingsTheme.colors.overlayContentSecondary
                        )
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.cd_selected),
                tint = ThingsTheme.colors.accent,
                modifier = Modifier.size(ThingsIconSize.XS)
            )
        }
    }
}

/** Ключ группы проектов без области — для раскрытия поздних проектов. */
private const val NO_AREA_GROUP = "no_area"

/**
 * Открытые проекты группы, которые видны ([first]), и более поздние ([second]) — отложенные и запланированные,
 * как в «Всех проектах»: сначала запланированные по дате, затем отложенные. Раскрытая группа ([group] в
 * [expanded]) показывает и поздние — после активных.
 */
private fun splitLater(list: List<Item>, expanded: Set<String>, group: String): Pair<List<Item>, List<Item>> {
    // Закрытые проекты сюда не попадают: переносить в них нечего
    val (active, later) = list.filter { ListRules.isOpenProject(it) }.partition { HomeList.showsProject(it) }
    val (scheduled, someday) = later.partition { it.start != Item.START_SOMEDAY && it.startDate != null }
    val laterOrdered = scheduled.sortedBy { it.startDate } + someday
    return if (group in expanded) Pair(active + laterOrdered, laterOrdered) else Pair(active, laterOrdered)
}

/** Строка «Показать / Скрыть N более поздних проектов» под проектами группы, если поздние есть. */
private fun androidx.compose.foundation.lazy.LazyListScope.laterToggleItem(
    group: String,
    later: List<Item>,
    expanded: Set<String>,
    onExpandedChange: (Set<String>) -> Unit
) {
    if (later.isEmpty()) return
    item(key = "later_$group") {
        val isExpanded = group in expanded
        Text(
            text = pluralStringResource(
                if (isExpanded) R.plurals.all_projects_hide_later else R.plurals.all_projects_show_later,
                later.size, later.size
            ),
            style = ThingsTheme.type.bodyMedium.copy(color = ThingsTheme.colors.overlayContentSecondary),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { onExpandedChange(if (isExpanded) expanded - group else expanded + group) }
                .padding(horizontal = ThingsSpacing.L_PLUS, vertical = ThingsSpacing.S)
        )
    }
}

/** Окно «Переместить» начинается на этой доле высоты экрана — ближе к верху, как в эталоне Things */
private const val MOVE_DIALOG_TOP_FRACTION = 0.17f

/** Поле поиска по проектам и областям — залитая плашка с лупой, как в эталоне. */
@Composable
private fun MoveSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onGo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    // Не в фокусе и пусто — лупа и подсказка по центру; в фокусе или с текстом — с левого края, как в эталоне
    val centered = !focused && query.isEmpty()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MaterialTheme.dimens.dialogFieldHeight)
            .clip(CircleShape)
            .background(ThingsTheme.colors.overlayControl)
            .padding(horizontal = ThingsSpacing.M),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (centered) Arrangement.Center else Arrangement.Start
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = ThingsTheme.colors.overlayContentSecondary,
            modifier = Modifier.size(ThingsIconSize.S)
        )
        Spacer(modifier = Modifier.width(ThingsSpacing.XS_PLUS))
        Box(modifier = if (centered) Modifier else Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.cd_search),
                    style = ThingsTheme.type.dialogRow.copy(color = ThingsTheme.colors.overlayContentSecondary)
                )
            }
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                // «Go» на клавиатуре — подсвеченный результат
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Go),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onGo = { onGo() }),
                textStyle = ThingsTheme.type.dialogRow.copy(color = ThingsTheme.colors.overlayContent),
                cursorBrush = SolidColor(ThingsTheme.colors.accent),
                modifier = (if (centered) Modifier.matchParentSize() else Modifier.fillMaxWidth())
                    .onFocusChanged { state ->
                        focused = state.isFocused
                        onFocusChange(state.isFocused)
                    }
            )
        }
        // Есть текст — справа кнопка очистки
        if (query.isNotEmpty()) {
            Spacer(modifier = Modifier.width(ThingsSpacing.XS_PLUS))
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = stringResource(R.string.cd_clear),
                tint = ThingsTheme.colors.overlayContentSecondary,
                modifier = Modifier
                    .size(ThingsIconSize.S)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { onQueryChange("") }
            )
        }
    }
}

/** Найденное в окне «Переместить»: проект, подзаголовок проекта или область. */
private sealed interface MoveFound {
    data class Project(val project: Item) : MoveFound
    data class Heading(val project: Item, val heading: Item) : MoveFound
    data class Area(val area: com.example.data.model.Area) : MoveFound
    data object Inbox : MoveFound
    /** «Без проекта» (при переносе проекта — «Без области») */
    data object NoPlace : MoveFound
}

/**
 * Поиск по названиям: открытые проекты, подзаголовки открытых проектов, области, затем «Входящие»
 * ([inboxTitle], null — их в окне нет) и «Без проекта» — в этом порядке. Пустой запрос — ничего.
 */
private fun moveSearch(
    query: String,
    projects: List<Item>,
    areas: List<Area>,
    headings: List<Item>,
    inboxTitle: String?,
    noPlaceTitle: String
): List<MoveFound> {
    if (query.isEmpty()) return emptyList()
    val openProjects = projects.filter { ListRules.isOpenProject(it) }
    val projectsById = openProjects.associateBy { it.id }
    return openProjects.filter { it.title.contains(query, ignoreCase = true) }.map { MoveFound.Project(it) } +
        headings.filter { it.title.contains(query, ignoreCase = true) }
            .mapNotNull { h -> projectsById[h.projectId]?.let { MoveFound.Heading(it, h) } } +
        areas.filter { !it.trashed && it.title.contains(query, ignoreCase = true) }.map { MoveFound.Area(it) } +
        listOfNotNull(
            MoveFound.Inbox.takeIf { inboxTitle?.contains(query, ignoreCase = true) == true },
            MoveFound.NoPlace.takeIf { noPlaceTitle.contains(query, ignoreCase = true) }
        )
}

/** Строки результатов поиска; первая подсвечена. */
private fun androidx.compose.foundation.lazy.LazyListScope.searchResults(
    found: List<MoveFound>,
    currentProjectId: String?,
    currentAreaId: String?,
    allTasks: List<ItemWithChecklist>,
    currentIsInbox: Boolean,
    inboxTitle: String,
    noPlaceTitle: String,
    onChoose: (MoveFound) -> Unit
) {
    found.forEachIndexed { index, result ->
        val key = when (result) {
            is MoveFound.Project -> "found_proj_${result.project.id}"
            is MoveFound.Heading -> "found_heading_${result.heading.id}"
            is MoveFound.Area -> "found_area_${result.area.id}"
            MoveFound.Inbox -> "found_inbox"
            MoveFound.NoPlace -> "found_no_place"
        }
        item(key = key) {
            when (result) {
                MoveFound.Inbox -> MoveDialogRow(
                    title = inboxTitle,
                    icon = Icons.Default.Inbox,
                    iconColor = ThingsTheme.colors.overlayContentSecondary,
                    isSelected = currentProjectId == null && currentAreaId == null && currentIsInbox,
                    isHighlighted = index == 0,
                    onClick = { onChoose(result) }
                )
                MoveFound.NoPlace -> MoveDialogRow(
                    title = noPlaceTitle,
                    icon = Icons.Default.Close,
                    iconColor = ThingsTheme.colors.overlayContentSecondary,
                    isSelected = currentProjectId == null && currentAreaId == null && !currentIsInbox,
                    isHighlighted = index == 0,
                    onClick = { onChoose(result) }
                )
                is MoveFound.Area -> MoveDialogRow(
                    title = result.area.title,
                    icon = AppIcons.Area,
                    iconColor = ThingsTheme.colors.area,
                    isSelected = currentAreaId == result.area.id && currentProjectId == null,
                    isAreaHeader = true,
                    isHighlighted = index == 0,
                    onClick = { onChoose(result) }
                )
                else -> {
                    val project = (result as? MoveFound.Project)?.project ?: (result as MoveFound.Heading).project
                    val projectTasks = allTasks.filter { it.item.projectId == project.id }
                    MoveDialogRow(
                        title = project.title,
                        icon = null,
                        iconColor = ThingsTheme.colors.overlayContent,
                        isSelected = result is MoveFound.Project && currentProjectId == project.id,
                        isProject = true,
                        completed = projectTasks.count { it.item.isClosed },
                        total = projectTasks.size,
                        dashed = project.start == Item.START_SOMEDAY,
                        isHighlighted = index == 0,
                        subtitle = (result as? MoveFound.Heading)?.heading?.title,
                        onClick = { onChoose(result) }
                    )
                }
            }
        }
    }
}

/** Через разделитель — строка «+ Новый проект „[query]“». */
private fun androidx.compose.foundation.lazy.LazyListScope.newProjectItem(query: String, highlighted: Boolean, onCreate: (String) -> Unit) {
    if (query.isEmpty()) return
    // Разделитель — только под найденным; если ничего не найдено, строка стоит первой
    if (!highlighted) item(key = "new_project_divider") {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = ThingsSpacing.XS, horizontal = ThingsSpacing.L_PLUS),
            color = ThingsTheme.colors.overlayDivider,
            thickness = ThingsStroke.THIN
        )
    }
    item(key = "new_project") {
        MoveDialogRow(
            title = stringResource(R.string.move_new_project, query),
            icon = Icons.Default.Add,
            iconColor = ThingsTheme.colors.overlayContent,
            isSelected = false,
            // Ничего не найдено — подсвечена она: «Go» создаёт проект
            isHighlighted = highlighted,
            onClick = { onCreate(query) }
        )
    }
}
