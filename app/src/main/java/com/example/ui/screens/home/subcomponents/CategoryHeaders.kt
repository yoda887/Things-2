package com.example.ui.screens.home.subcomponents

import com.example.ui.components.hideSoftKeyboardNow
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import com.example.ui.viewmodel.ActiveScreen
import com.example.ui.theme.AppIcons
import com.example.ui.components.ThingsMenuDivider
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import com.example.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.ui.components.ThingsConfirmDialog
import com.example.ui.components.ThingsConfirmPopover
import com.example.ui.components.ThingsDropdownMenu
import com.example.ui.components.ThingsMenuItem
import com.example.ui.theme.ThingsTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.Tag
import androidx.compose.material.icons.outlined.LocalOffer
import com.example.ui.components.ProjectProgressArc
import com.example.ui.screens.home.components.TaskListKeys
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Отступы над подзаголовками: секция области, проекты/задачи области, день и месяц в «Планах»
private val AREA_SECTION_TOP = 22.dp
private val AREA_GROUP_TOP = 48.dp
private val UPCOMING_DAY_TOP = 36.dp
private val UPCOMING_MONTH_TOP = 40.dp

private const val OPTIONS_ICON_ALPHA = 0.45f

/**
 * Компонент основного заголовка категории (Inbox, Today, Upcoming и т.д.).
 */
@Composable
fun MainCategoryHeader(
    screen: ActiveScreen,
    project: Item?,
    tasks: List<ItemWithChecklist>,
    area: Area?,
    tag: Tag? = null,
    scaleFactor: Float,
    textPrimaryColor: Color,
    globalDimAlpha: Float,
    onDeleteProject: (Item) -> Unit = {},
    onDeleteArea: (Area) -> Unit = {},
    onAddHeading: () -> Unit = {},
    onProjectAction: (ProjectAction) -> Unit = {},
    // Переименование: нажатие на название проекта или области (или «Перейменувати» в меню «…»)
    onRenameProject: (Item, String) -> Unit = { _, _ -> },
    onRenameArea: (Area, String) -> Unit = { _, _ -> },
    // Меню «…» области: «Додати теги» и «Поділитися»
    onAreaAction: (AreaAction) -> Unit = {},
    // Правка названия проекта или области — состояние у экрана: на время правки верхняя панель показывает «Готово»
    isEditingTitle: Boolean = false,
    onEditingTitleChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val headerEmojiFontSize = ThingsTheme.type.heroEmoji.fontSize
    val headerTitleFontSize = ThingsTheme.type.largeTitle.fontSize

    var showOptionsMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var showAreaDeleteConfirmDialog by remember { mutableStateOf(false) }
    val areaDeleteScope = rememberCoroutineScope()

    if (showDeleteConfirmDialog && project != null) {
        val taskCountInProj = tasks.count { it.item.projectId == project.id }
        ThingsConfirmDialog(
            title = stringResource(R.string.delete_project_title),
            message = if (taskCountInProj > 0) {
                pluralStringResource(R.plurals.delete_project_message_tasks, taskCountInProj, project.name, taskCountInProj)
            } else {
                stringResource(R.string.delete_project_message, project.name)
            },
            confirmText = stringResource(R.string.delete),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                showDeleteConfirmDialog = false
                onDeleteProject(project)
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            // У проекта под названием сразу идут его свойства и заметки — отступ меньше, как в Things
            .padding(
                top = MaterialTheme.dimens.mainHeaderPaddingTop,
                bottom = if (screen == ActiveScreen.PROJECT_DETAIL) ThingsSpacing.S else MaterialTheme.dimens.mainHeaderPaddingBottom
            )
            .graphicsLayer { alpha = globalDimAlpha },
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (screen) {
            ActiveScreen.TODAY -> {
                // Используем кастомную иконку AppIcons.Today вместо обычного эмодзи звезды
                Icon(
                    imageVector = AppIcons.Today,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size((32 * scaleFactor).dp)
                )
                Spacer(modifier = Modifier.width((10 * scaleFactor).dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.R.string.category_today),
                    style = ThingsTheme.type.largeTitle.copy(color = textPrimaryColor)
                )
            }
            ActiveScreen.INBOX -> {
                Icon(
                    // Используем кастомную иконку AppIcons.Inbox
                    imageVector = AppIcons.Inbox,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size((32 * scaleFactor).dp)
                )
                Spacer(modifier = Modifier.width((10 * scaleFactor).dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.R.string.category_inbox),
                    style = ThingsTheme.type.largeTitle.copy(color = textPrimaryColor)
                )
            }
            ActiveScreen.UPCOMING -> {
                Icon(
                    // Используем кастомную иконку AppIcons.Upcoming
                    imageVector = AppIcons.Upcoming,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size((32 * scaleFactor).dp)
                )
                Spacer(modifier = Modifier.width((10 * scaleFactor).dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.R.string.category_upcoming),
                    style = ThingsTheme.type.largeTitle.copy(color = textPrimaryColor)
                )
            }
            ActiveScreen.ANYTIME -> {
                Icon(
                    // Используем кастомную иконку AppIcons.Anytime
                    imageVector = AppIcons.Anytime,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size((32 * scaleFactor).dp)
                )
                Spacer(modifier = Modifier.width((10 * scaleFactor).dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.R.string.category_anytime),
                    style = ThingsTheme.type.largeTitle.copy(color = textPrimaryColor)
                )
            }
            ActiveScreen.SOMEDAY -> {
                Icon(
                    // Используем кастомную иконку AppIcons.Someday
                    imageVector = AppIcons.Someday,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size((32 * scaleFactor).dp)
                )
                Spacer(modifier = Modifier.width((10 * scaleFactor).dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.R.string.category_someday),
                    style = ThingsTheme.type.largeTitle.copy(color = textPrimaryColor)
                )
            }
            ActiveScreen.LOGBOOK -> {
                Icon(
                    // Используем кастомную иконку AppIcons.Logbook
                    imageVector = AppIcons.Logbook,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size((32 * scaleFactor).dp)
                )
                Spacer(modifier = Modifier.width((10 * scaleFactor).dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.R.string.category_logbook),
                    style = ThingsTheme.type.largeTitle.copy(color = textPrimaryColor)
                )
            }
            // Особые списки проектов: значок проекта — как у строк, которые в них стоят
            ActiveScreen.ALL_PROJECTS, ActiveScreen.LOGGED_PROJECTS, ActiveScreen.LATER_PROJECTS -> {
                com.example.ui.components.ProjectStackIcon(
                    logged = screen == ActiveScreen.LOGGED_PROJECTS,
                    // «Пізніші проєкти» — значок «Усі проєкти» серым, как строка на главном экране
                    color = if (screen == ActiveScreen.LATER_PROJECTS) ThingsTheme.colors.textSecondary else ThingsTheme.colors.project,
                    modifier = Modifier.height((26 * scaleFactor).dp)
                )
                Spacer(modifier = Modifier.width((10 * scaleFactor).dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(
                        when (screen) {
                            ActiveScreen.ALL_PROJECTS -> com.example.R.string.special_all_projects
                            ActiveScreen.LATER_PROJECTS -> com.example.R.string.special_later_projects
                            else -> com.example.R.string.special_logged_projects
                        }
                    ),
                    style = ThingsTheme.type.largeTitle.copy(color = textPrimaryColor)
                )
            }
            ActiveScreen.REPEATING -> {
                com.example.ui.components.RepeatTemplateIcon(
                    modifier = Modifier.size((26 * scaleFactor).dp)
                )
                Spacer(modifier = Modifier.width((10 * scaleFactor).dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.R.string.special_repeating),
                    style = ThingsTheme.type.largeTitle.copy(color = textPrimaryColor)
                )
            }
            ActiveScreen.PROJECT_DETAIL -> {
                // Завершённый проект — кольцо полное, даже если задач в нём нет
                val projectDone = project != null && (project.isCompleted || project.status == Item.STATUS_CANCELLED)
                val totalCount = tasks.count { it.item.projectId == project?.id }.let { if (projectDone) maxOf(it, 1) else it }
                val completedCount = if (projectDone) totalCount else tasks.count { it.item.projectId == project?.id && it.item.isClosed }
                // Иконка проекта (ProgressArc) окрашена в синий цвет и выровнена по верхнему краю заголовка с компенсационным отступом
                // У шаблона повторяющегося проекта — тоже кольцо: что он повторяется, видно по строке правила под заголовком.
                // Выполненный — синий круг с белой галочкой. Нажатие на кольцо открытого проекта завершает его, как «Завершить
                // проект» в меню (с открытыми задачами — с подтверждением)
                val canComplete = project != null && !projectDone && project.type != Item.TYPE_PROJECT_TEMPLATE
                val headerView = androidx.compose.ui.platform.LocalView.current
                // Как в Things: значок — перед первой строкой названия, следующие строки уходят под него, к краю
                val projectIconSize = (26 * scaleFactor).dp
                val projectTitleIndent = titleIndent(projectIconSize)
                Box(modifier = Modifier.weight(1f)) {
                com.example.ui.components.ProjectStatusIcon(
                    project = project,
                    completed = completedCount,
                    total = totalCount,
                    modifier = Modifier
                        .padding(top = (4 * scaleFactor).dp)
                        .size(projectIconSize)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .then(
                            if (canComplete) Modifier.clickable {
                                headerView.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                onProjectAction(ProjectAction.COMPLETE)
                            } else Modifier
                        )
                )

                val projectNameText = project?.name ?: stringResource(R.string.fallback_project)
                val projectTitleAnnotated = remember(projectNameText) {
                    buildAnnotatedString {
                        append(projectNameText)
                        append(" ")
                        appendInlineContent("project_options_icon", "[options]")
                    }
                }

                val inlineContentMap = mapOf(
                    "project_options_icon" to InlineTextContent(
                        Placeholder(
                            width = (headerTitleFontSize.value * 1.1f).sp,
                            height = headerTitleFontSize,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showOptionsMenu = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = stringResource(R.string.cd_project_options),
                                tint = textPrimaryColor.copy(alpha = OPTIONS_ICON_ALPHA),
                                modifier = Modifier.size((headerTitleFontSize.value * 0.88f).dp)
                            )

                            ThingsDropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = { showOptionsMenu = false }
                            ) {
                                // Пункты и их порядок — как в меню опций проекта Things
                                fun action(a: ProjectAction) {
                                    showOptionsMenu = false
                                    onProjectAction(a)
                                }
                                // У шаблона повтора нет своих дат и завершения — их задаёт правило
                                val isTemplate = project?.type == Item.TYPE_PROJECT_TEMPLATE
                                if (project?.isCompleted != true && !isTemplate) {
                                    ThingsMenuItem(stringResource(R.string.project_complete), Icons.Outlined.CheckCircle, onClick = { action(ProjectAction.COMPLETE) })
                                }
                                ThingsMenuItem(stringResource(R.string.ui_rename), Icons.Default.Edit, onClick = {
                                    showOptionsMenu = false
                                    onEditingTitleChange(true)
                                })
                                // У шаблона «Когда» переносит его ближайший повтор
                                ThingsMenuItem(stringResource(R.string.batch_action_when), AppIcons.Upcoming, onClick = { action(ProjectAction.WHEN) })
                                // Копию повторяющегося проекта повторяющейся не делают — повторяется её шаблон
                                if (project != null && (isTemplate || com.example.domain.repeat.RepeatProjects.canRepeat(project))) {
                                    ThingsMenuItem(stringResource(R.string.ui_repeat), AppIcons.Repeat, onClick = { action(ProjectAction.REPEAT) })
                                }
                                ThingsMenuItem(stringResource(R.string.batch_action_set_tags), AppIcons.Tag, onClick = { action(ProjectAction.TAGS) })
                                if (!isTemplate) {
                                    ThingsMenuItem(stringResource(R.string.batch_action_set_deadline), AppIcons.Deadline, onClick = { action(ProjectAction.DEADLINE) })
                                }
                                ThingsMenuItem(stringResource(R.string.ui_add_heading), Icons.Default.Add, onClick = {
                                    showOptionsMenu = false
                                    onAddHeading()
                                })
                                ThingsMenuDivider()
                                ThingsMenuItem(stringResource(R.string.batch_action_move), Icons.AutoMirrored.Filled.ArrowForward, onClick = { action(ProjectAction.MOVE) })
                                if (!isTemplate) {
                                    ThingsMenuItem(stringResource(R.string.batch_action_duplicate), Icons.Default.ContentCopy, onClick = { action(ProjectAction.DUPLICATE) })
                                }
                                ThingsMenuItem(stringResource(R.string.ui_delete_project), Icons.Default.Delete, destructive = true, onClick = {
                                    showOptionsMenu = false
                                    showDeleteConfirmDialog = true
                                })
                                ThingsMenuDivider()
                                ThingsMenuItem(stringResource(R.string.batch_action_share), Icons.Default.Share, onClick = { action(ProjectAction.SHARE) })
                            }
                        }
                    }
                )

                // В правке «…» после названия нет — оно возвращается, когда правка закончена
                if (isEditingTitle && project != null) {
                    EditableLargeTitle(
                        initial = project.title,
                        textColor = textPrimaryColor,
                        textIndent = projectTitleIndent,
                        onCommit = { title -> onEditingTitleChange(false); if (title != project.title) onRenameProject(project, title) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = projectTitleAnnotated,
                        inlineContent = inlineContentMap,
                        style = ThingsTheme.type.largeTitle.copy(color = textPrimaryColor, textIndent = projectTitleIndent),
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = project != null
                            ) { onEditingTitleChange(true) }
                    )
                }
                }
            }
            ActiveScreen.AREA_DETAIL -> {
                // Как у проекта: следующие строки названия уходят под значок
                val areaIconSize = (30 * scaleFactor).dp
                val areaTitleIndent = titleIndent(areaIconSize)
                Box(modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = AppIcons.Area,
                    contentDescription = null,
                    tint = ThingsTheme.colors.area,
                    modifier = Modifier
                        .size(areaIconSize)
                        .padding(top = (2 * scaleFactor).dp)
                )

                val areaTitleText = area?.title ?: stringResource(R.string.fallback_area)
                val areaTitleAnnotated = remember(areaTitleText) {
                    buildAnnotatedString {
                        append(areaTitleText)
                        append(" ")
                        appendInlineContent("area_options_icon", "[options]")
                    }
                }

                var showAreaOptionsMenu by remember { mutableStateOf(false) }

                val areaInlineContentMap = mapOf(
                    "area_options_icon" to InlineTextContent(
                        Placeholder(
                            width = (headerTitleFontSize.value * 1.1f).sp,
                            height = headerTitleFontSize,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showAreaOptionsMenu = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = stringResource(R.string.cd_area_options),
                                tint = textPrimaryColor.copy(alpha = OPTIONS_ICON_ALPHA),
                                modifier = Modifier.size((headerTitleFontSize.value * 0.88f).dp)
                            )

                            ThingsDropdownMenu(
                                expanded = showAreaOptionsMenu,
                                onDismissRequest = { showAreaOptionsMenu = false }
                            ) {
                                // Как в Things: «Додати теги», «Видалити область», «Поділитися»; плюс «Перейменувати»
                                ThingsMenuItem(stringResource(R.string.ui_add_tags), AppIcons.Tag, onClick = {
                                    showAreaOptionsMenu = false
                                    onAreaAction(AreaAction.TAGS)
                                })
                                ThingsMenuItem(stringResource(R.string.ui_rename), Icons.Default.Edit, onClick = {
                                    showAreaOptionsMenu = false
                                    onEditingTitleChange(true)
                                })
                                ThingsMenuDivider()
                                ThingsMenuItem(stringResource(R.string.ui_delete_area), Icons.Default.Delete, destructive = true, onClick = {
                                    showAreaOptionsMenu = false
                                    showAreaDeleteConfirmDialog = true
                                })
                                ThingsMenuDivider()
                                ThingsMenuItem(stringResource(R.string.batch_action_share), Icons.Default.Share, onClick = {
                                    showAreaOptionsMenu = false
                                    onAreaAction(AreaAction.SHARE)
                                })
                            }

                            // Как в Things: подтверждение — карточкой у «…»; удаляется и всё содержимое области
                            if (area != null) {
                                ThingsConfirmPopover(
                                    expanded = showAreaDeleteConfirmDialog,
                                    message = stringResource(R.string.delete_area_message),
                                    confirmText = stringResource(R.string.ui_delete_area),
                                    onConfirm = {
                                        showAreaDeleteConfirmDialog = false
                                        // Как в Things: карточка ушла, область ещё миг видна целиком — затем
                                        // удаление и переход назад на главный экран
                                        areaDeleteScope.launch {
                                            delay(ThingsMotion.SLOW.toLong())
                                            onDeleteArea(area)
                                        }
                                    },
                                    onDismiss = { showAreaDeleteConfirmDialog = false }
                                )
                            }
                        }
                    }
                )

                if (isEditingTitle && area != null) {
                    EditableLargeTitle(
                        initial = area.title,
                        textColor = textPrimaryColor,
                        textIndent = areaTitleIndent,
                        onCommit = { title -> onEditingTitleChange(false); if (title != area.title) onRenameArea(area, title) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = areaTitleAnnotated,
                        inlineContent = areaInlineContentMap,
                        style = ThingsTheme.type.largeTitle.copy(color = textPrimaryColor, textIndent = areaTitleIndent),
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = area != null
                            ) { onEditingTitleChange(true) }
                    )
                }
                }
            }
            ActiveScreen.TAG_DETAIL -> {
                Icon(
                    imageVector = AppIcons.Tag,
                    contentDescription = null,
                    tint = ThingsTheme.colors.area,
                    modifier = Modifier
                        .size((30 * scaleFactor).dp)
                        .align(Alignment.Top)
                        .padding(top = (2 * scaleFactor).dp)
                )
                Spacer(modifier = Modifier.width(ThingsSpacing.M))
                Text(
                    text = tag?.title ?: stringResource(R.string.fallback_tag),
                    style = ThingsTheme.type.largeTitle.copy(color = textPrimaryColor),
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            ActiveScreen.SEARCH -> {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = textPrimaryColor,
                    modifier = Modifier.size((32 * scaleFactor).dp)
                )
                Spacer(modifier = Modifier.width((10 * scaleFactor).dp))
                Text(
                    text = stringResource(R.string.cd_search),
                    style = ThingsTheme.type.largeTitle.copy(color = textPrimaryColor)
                )
            }
            else -> {}
        }
    }
}

/**
 * Вспомогательный заголовок для подразделов ("This Evening", "TASKS", "PROJECTS", "Планы", "Скрыть более поздние объекты").
 */
@Composable
fun SubCategoryHeader(
    headerText: String,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    dividerColor: Color,
    dimAlpha: Float,
    isLaterItemsHidden: Boolean = false,
    // Сколько объектов под плашкой «пізніші» области (дела и проекты «Планов» и «Когда-нибудь»)
    laterCount: Int = 0,
    onLaterToggleClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (headerText == TaskListKeys.PROJECTS_HEADING) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = dimAlpha }
                .padding(top = AREA_SECTION_TOP, bottom = ThingsSpacing.S_PLUS)
        ) {
            Text(
                text = stringResource(R.string.ui_projects),
                style = ThingsTheme.type.overline.copy(color = textSecondaryColor.copy(alpha = ThingsAlpha.HALF)),
                modifier = Modifier.padding(bottom = ThingsSpacing.XS_PLUS)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ThingsStroke.THIN)
                    .background(dividerColor)
            )
        }
    } else if (headerText == TaskListKeys.TASKS_HEADING) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = dimAlpha }
                .padding(top = AREA_SECTION_TOP, bottom = ThingsSpacing.S_PLUS)
        ) {
            Text(
                text = stringResource(R.string.ui_tasks),
                style = ThingsTheme.type.overline.copy(color = textSecondaryColor.copy(alpha = ThingsAlpha.HALF)),
                modifier = Modifier.padding(bottom = ThingsSpacing.XS_PLUS)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ThingsStroke.THIN)
                    .background(dividerColor)
            )
        }
    } else if (headerText == TaskListKeys.AREA_UPCOMING_HEADING) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = dimAlpha }
                .padding(top = AREA_GROUP_TOP, bottom = ThingsSpacing.XS_PLUS)
        ) {
            Row(
                modifier = Modifier.padding(start = MaterialTheme.dimens.taskRowStartPadding, bottom = ThingsSpacing.XS_PLUS),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(MaterialTheme.dimens.taskLeftColumnWidthDefault),
                    contentAlignment = Alignment.Center
                ) {
                    // Иконка крупнее колонки чекбокса и выступает из неё поровну: центр — на оси
                    // чекбоксов задач, заголовок — с того же края, что и названия задач
                    Icon(
                        imageVector = AppIcons.Upcoming,
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.requiredSize(ThingsIconSize.M)
                    )
                }
                Spacer(modifier = Modifier.width(MaterialTheme.dimens.taskSpacingToTextDefault))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.R.string.area_upcoming_heading),
                    style = ThingsTheme.type.sectionHeader.copy(
                        color = textPrimaryColor
                    )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = MaterialTheme.dimens.taskRowStartPadding,
                        end = MaterialTheme.dimens.taskRowEndPadding
                    )
                    .height(ThingsStroke.DIVIDER)
                    .background(dividerColor)
            )
        }
    } else if (headerText == TaskListKeys.AREA_SOMEDAY_HEADING) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = dimAlpha }
                .padding(top = AREA_GROUP_TOP, bottom = ThingsSpacing.XS_PLUS)
        ) {
            Row(
                modifier = Modifier.padding(start = MaterialTheme.dimens.taskRowStartPadding, bottom = ThingsSpacing.XS_PLUS),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(MaterialTheme.dimens.taskLeftColumnWidthDefault),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Someday,
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.requiredSize(ThingsIconSize.M)
                    )
                }
                Spacer(modifier = Modifier.width(MaterialTheme.dimens.taskSpacingToTextDefault))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.R.string.area_someday_heading),
                    style = ThingsTheme.type.sectionHeader.copy(
                        color = textPrimaryColor
                    )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = MaterialTheme.dimens.taskRowStartPadding,
                        end = MaterialTheme.dimens.taskRowEndPadding
                    )
                    .height(ThingsStroke.DIVIDER)
                    .background(dividerColor)
            )
        }
    } else if (headerText == TaskListKeys.AREA_LATER_TOGGLE) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = dimAlpha }
                .padding(start = MaterialTheme.dimens.taskRowStartPadding, top = ThingsSpacing.L_PLUS, bottom = ThingsSpacing.M)
        ) {
            Text(
                // Как в Things: «Показати 12 пізніших об'єктів»
                text = androidx.compose.ui.res.pluralStringResource(
                    if (isLaterItemsHidden) com.example.R.plurals.area_show_later_count else com.example.R.plurals.area_hide_later_count,
                    laterCount, laterCount
                ),
                style = ThingsTheme.type.bodyMedium.copy(
                    color = textSecondaryColor.copy(alpha = ThingsAlpha.HINT)
                ),
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onLaterToggleClick()
                    }
            )
        }
    } else {
        Column(
            modifier = modifier
                .graphicsLayer { alpha = dimAlpha }
        ) {
            // Свободное пространство сверху увеличено на 16.dp по запросу пользователя.
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.eveningSectionSpacing + ThingsSpacing.L))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = ThingsSpacing.XS_PLUS),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Иконка "Вечер" с автоматическим подбором цвета и размера
                Icon(
                    imageVector = AppIcons.Evening,
                    contentDescription = stringResource(R.string.category_this_evening),
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .padding(end = ThingsSpacing.S)
                        .size(ThingsIconSize.M)
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.R.string.category_this_evening),
                    style = ThingsTheme.type.sectionHeader.copy(
                        color = textPrimaryColor
                    )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ThingsStroke.THIN)
                    .background(dividerColor)
            )
            // Свободное пространство в 8.dp снизу от разделительной линии по запросу пользователя.
            Spacer(modifier = Modifier.height(ThingsSpacing.S))
        }
    }
}

/**
 * Заголовок с датой на экране предстоящих событий (Upcoming). Renders tomorrow, day labels, divider line.
 */
@Composable
fun UpcomingDateHeader(
    dayOfMonth: String,
    dayOfWeekLabel: String,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    dividerColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = UPCOMING_DAY_TOP, bottom = ThingsSpacing.S_PLUS),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = dayOfMonth,
            style = ThingsTheme.type.largeTitle.copy(color = textPrimaryColor)
        )
        Spacer(modifier = Modifier.width(ThingsSpacing.S))
        Column(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.Bottom)
                .padding(bottom = ThingsSpacing.XS)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ThingsStroke.DIVIDER)
                    .background(dividerColor)
            )
            Spacer(modifier = Modifier.height(ThingsSpacing.XS))
            Text(
                text = dayOfWeekLabel,
                style = ThingsTheme.type.headlineStrong.copy(color = textSecondaryColor.copy(alpha = ThingsAlpha.HALF)
                )
            )
        }
    }
}

/**
 * Заголовок месяца на экране «Предстоящие» (Upcoming) для событий и задач позже 14 дней.
 */
@Composable
fun UpcomingMonthHeader(
    monthLabel: String,
    textPrimaryColor: Color,
    dividerColor: Color,
    modifier: Modifier = Modifier,
    // Остаток месяца («16 – 31 жовтня»): числа и тире — серым, название месяца — основным цветом
    isRemainder: Boolean = false,
    // Отступ сверху вдвое больше — раздел «Повторювані завдання» в конце «Планов» отделён заметнее
    doubleTop: Boolean = false
) {
    val secondaryColor = ThingsTheme.colors.textSecondary
    val label = if (!isRemainder) androidx.compose.ui.text.AnnotatedString(monthLabel) else {
        androidx.compose.ui.text.buildAnnotatedString {
            monthLabel.forEach { char ->
                if (char.isDigit() || char == '–') {
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = secondaryColor))
                    append(char)
                    pop()
                } else append(char)
            }
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (doubleTop) UPCOMING_MONTH_TOP * 2 else UPCOMING_MONTH_TOP, bottom = ThingsSpacing.S)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ThingsStroke.DIVIDER)
                .background(dividerColor)
        )
        Spacer(modifier = Modifier.height(ThingsSpacing.XS_PLUS))
        Text(
            text = label,
            style = ThingsTheme.type.headlineStrong.copy(color = textPrimaryColor
            )
        )
    }
}

/**
 * Заголовок раздела «Журнала» и «Завершённых проектов» («Сьогодні», «Вчора», месяц), как в Logbook
 * Things: название над линией, размером с название проекта.
 */
@Composable
fun LogbookSectionHeader(
    label: String,
    textPrimaryColor: Color,
    dividerColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = UPCOMING_MONTH_TOP, bottom = ThingsSpacing.S)
    ) {
        Text(
            text = label,
            style = ThingsTheme.type.listTitle.copy(color = textPrimaryColor, fontWeight = ThingsTheme.type.headlineStrong.fontWeight)
        )
        Spacer(modifier = Modifier.height(ThingsSpacing.XS_PLUS))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ThingsStroke.DIVIDER)
                .background(dividerColor)
        )
    }
}

/**
 * Отступ первой строки названия под значок перед ним ([iconSize] и промежуток): следующие строки длинного
 * названия начинаются от края, под значком, как в Things.
 */
@Composable
private fun titleIndent(iconSize: androidx.compose.ui.unit.Dp): androidx.compose.ui.text.style.TextIndent {
    val firstLine = with(androidx.compose.ui.platform.LocalDensity.current) { (iconSize + ThingsSpacing.M).toSp() }
    return androidx.compose.ui.text.style.TextIndent(firstLine = firstLine)
}

/**
 * Название проекта или области в режиме правки — тем же крупным шрифтом и на том же месте, что заголовок
 * экрана, как в Things: длинное переносится на следующие строки (под значок, [textIndent]). Курсор сразу в
 * конце; «Готово» на клавиатуре или в верхней панели (уход фокуса) сохраняют ([onCommit] с обрезанным
 * названием), пустое название не сохраняется — остаётся прежнее. Перевода строки в названии нет: Enter — тоже «Готово».
 */
@Composable
private fun EditableLargeTitle(
    initial: String,
    textColor: Color,
    onCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
    textIndent: androidx.compose.ui.text.style.TextIndent? = null
) {
    var value by remember {
        mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(initial, androidx.compose.ui.text.TextRange(initial.length)))
    }
    var committed by remember { mutableStateOf(false) }
    var hadFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val view = androidx.compose.ui.platform.LocalView.current
    fun commit() {
        if (committed) return
        committed = true
        onCommit(value.text.trim().ifEmpty { initial })
    }
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = { changed ->
            // Enter с аппаратной клавиатуры или вставленный перевод строки — конец правки, а не новая строка
            if (changed.text.contains('\n')) {
                value = changed.copy(text = changed.text.replace("\n", ""))
                view.hideSoftKeyboardNow()
                commit()
            } else {
                value = changed
            }
        },
        textStyle = ThingsTheme.type.largeTitle.copy(color = textColor, textIndent = textIndent),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(ThingsTheme.colors.accent),
        // Многострочное: длинное название переносится, а клавиша клавиатуры остаётся «Готово»
        singleLine = false,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = androidx.compose.ui.text.input.ImeAction.Done,
            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
            view.hideSoftKeyboardNow()
            commit()
        }),
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                if (state.isFocused) hadFocus = true else if (hadFocus) commit()
            }
    )
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
