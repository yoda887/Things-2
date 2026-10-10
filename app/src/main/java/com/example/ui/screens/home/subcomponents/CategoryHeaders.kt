package com.example.ui.screens.home.subcomponents

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
import com.example.ui.components.ThingsDropdownMenu
import com.example.ui.components.ThingsMenuItem
import com.example.ui.theme.ThingsTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
    modifier: Modifier = Modifier
) {
    val headerEmojiFontSize = ThingsTheme.type.heroEmoji.fontSize
    val headerTitleFontSize = ThingsTheme.type.largeTitle.fontSize

    var showOptionsMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var showAreaDeleteConfirmDialog by remember { mutableStateOf(false) }

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

    if (showAreaDeleteConfirmDialog && area != null) {
        ThingsConfirmDialog(
            title = stringResource(R.string.delete_area_title),
            message = stringResource(R.string.delete_area_message, area.title),
            confirmText = stringResource(R.string.delete),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                showAreaDeleteConfirmDialog = false
                onDeleteArea(area)
            },
            onDismiss = { showAreaDeleteConfirmDialog = false }
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
            ActiveScreen.PROJECT_DETAIL -> {
                // Завершённый проект — кольцо полное, даже если задач в нём нет
                val projectDone = project != null && (project.isCompleted || project.status == Item.STATUS_CANCELLED)
                val totalCount = tasks.count { it.item.projectId == project?.id }.let { if (projectDone) maxOf(it, 1) else it }
                val completedCount = if (projectDone) totalCount else tasks.count { it.item.projectId == project?.id && it.item.isClosed }
                // Иконка проекта (ProgressArc) окрашена в синий цвет и выровнена по верхнему краю заголовка с компенсационным отступом
                ProjectProgressArc(
                    completed = completedCount,
                    total = totalCount,
                    color = ThingsTheme.colors.project,
                    dashed = project?.start == com.example.data.model.Item.START_SOMEDAY,
                    modifier = Modifier
                        .size((26 * scaleFactor).dp)
                        .align(Alignment.Top)
                        .padding(top = (4 * scaleFactor).dp)
                )
                Spacer(modifier = Modifier.width(ThingsSpacing.M))

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
                                if (project?.isCompleted != true) {
                                    ThingsMenuItem(stringResource(R.string.project_complete), Icons.Outlined.CheckCircle, onClick = { action(ProjectAction.COMPLETE) })
                                }
                                ThingsMenuItem(stringResource(R.string.batch_action_when), AppIcons.Upcoming, onClick = { action(ProjectAction.WHEN) })
                                ThingsMenuItem(stringResource(R.string.batch_action_set_tags), AppIcons.Tag, onClick = { action(ProjectAction.TAGS) })
                                ThingsMenuItem(stringResource(R.string.batch_action_set_deadline), AppIcons.Deadline, onClick = { action(ProjectAction.DEADLINE) })
                                ThingsMenuItem(stringResource(R.string.ui_add_heading), Icons.Default.Add, onClick = {
                                    showOptionsMenu = false
                                    onAddHeading()
                                })
                                ThingsMenuDivider()
                                ThingsMenuItem(stringResource(R.string.batch_action_move), Icons.AutoMirrored.Filled.ArrowForward, onClick = { action(ProjectAction.MOVE) })
                                ThingsMenuItem(stringResource(R.string.batch_action_duplicate), Icons.Default.ContentCopy, onClick = { action(ProjectAction.DUPLICATE) })
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

                Text(
                    text = projectTitleAnnotated,
                    inlineContent = inlineContentMap,
                    style = ThingsTheme.type.largeTitle.copy(color = textPrimaryColor),
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            ActiveScreen.AREA_DETAIL -> {
                Icon(
                    imageVector = AppIcons.Area,
                    contentDescription = null,
                    tint = ThingsTheme.colors.area,
                    modifier = Modifier
                        .size((30 * scaleFactor).dp)
                        .align(Alignment.Top)
                        .padding(top = (2 * scaleFactor).dp)
                )
                Spacer(modifier = Modifier.width(ThingsSpacing.M))

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
                                ThingsMenuItem(stringResource(R.string.ui_delete_area), Icons.Default.Delete, destructive = true, onClick = {
                                    showAreaOptionsMenu = false
                                    showAreaDeleteConfirmDialog = true
                                })
                            }
                        }
                    }
                )

                Text(
                    text = areaTitleAnnotated,
                    inlineContent = areaInlineContentMap,
                    style = ThingsTheme.type.largeTitle.copy(color = textPrimaryColor),
                    modifier = Modifier.weight(1f, fill = false)
                )
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
                text = androidx.compose.ui.res.stringResource(
                    if (isLaterItemsHidden) com.example.R.string.area_show_later_items else com.example.R.string.area_hide_later_items
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = UPCOMING_MONTH_TOP, bottom = ThingsSpacing.S)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ThingsStroke.DIVIDER)
                .background(dividerColor)
        )
        Spacer(modifier = Modifier.height(ThingsSpacing.XS_PLUS))
        Text(
            text = monthLabel,
            style = ThingsTheme.type.headlineStrong.copy(color = textPrimaryColor
            )
        )
    }
}
