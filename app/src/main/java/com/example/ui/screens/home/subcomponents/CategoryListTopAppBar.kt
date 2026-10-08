package com.example.ui.screens.home.subcomponents

import com.example.ui.theme.ThingsStroke
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import com.example.ui.components.ThingsDropdownMenu
import com.example.ui.components.ThingsMenuItem
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.R
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.Tag
import androidx.compose.material.icons.outlined.LocalOffer
import com.example.ui.screens.home.ActiveScreen
import com.example.ui.theme.AppIcons
import com.example.ui.theme.ThingsTheme
import com.example.ui.theme.topAppBarTitle

private val TOP_APP_BAR_ACTION_ICON_SIZE = 24.dp

// [ИЗМЕНЕНИЕ]: Константы анимации, размеров и отступов для исключения хардкода во всем компоненте
private val SCROLLED_ELEVATION = 4.dp
private val UNSCROLLED_ELEVATION = 0.dp
private val TOP_APP_BAR_ICON_SIZE = 22.dp
private val PROJECT_PROGRESS_ARC_SIZE = 20.dp
private val AREA_ICON_SIZE = 20.dp
private val TOP_APP_BAR_SPACING = 8.dp

/**
 * Состояние и действия режима множественного выбора для верхнего тулбара.
 *
 * @param isSelectionMode Флаг активного режима множественного выбора.
 * @param selectedCount Количество выбранных задач.
 * @param isAllSelected Флаг того, что выбраны все задачи в текущем списке.
 * @param onCancelSelection Обработчик отмены режима выбора.
 * @param onSelectAllClick Обработчик выбора всех задач.
 * @param onDeselectAllClick Обработчик снятия выделения со всех задач.
 * @param onEnterSelectionMode Обработчик перехода в режим множественного выбора.
 */
data class TopAppBarSelectionState(
    val isSelectionMode: Boolean = false,
    val selectedCount: Int = 0,
    val isAllSelected: Boolean = false,
    val onCancelSelection: () -> Unit = {},
    val onSelectAllClick: () -> Unit = {},
    val onDeselectAllClick: () -> Unit = {},
    val onEnterSelectionMode: () -> Unit = {}
)

/**
 * Верхняя панель навигации (TopAppBar) для экрана категории.
 * Предоставляет кнопку возврата и иконку суб-опций.
 *
 * @param screen Текущий активный экран для вывода заголовка/иконки.
 * @param onBackClick Обработчик нажатия на кнопку "Назад".
 * @param modifier Модификатор для внешнего контейнера.
 * @param textPrimaryColor Основной цвет текста названия экрана.
 * @param textSecondaryColor Цвет текста и границ элементов управления.
 * @param project Объект проекта (для экрана PROJECT_DETAIL).
 * @param area Объект области ответственности (для экрана AREA_DETAIL).
 * @param tasks Список задач проекта (для вычисления прогресса проекта).
 * @param backgroundProgress Проявленность фона и разделителя тулбара, 0..1.
 * @param titleProgress Проявленность компактного заголовка тулбара, 0..1. Он проступает, когда
 *   заголовок экрана уже почти растворился, чтобы они не накладывались друг на друга.
 * @param selectionState Состояние и действия режима множественного выбора.
 * @param hasTags Флаг наличия тегов в текущем списке.
 * @param isTagsFilterVisible Флаг отображения строки фильтрации по тегам.
 * @param onToggleTagsFilter Обработчик переключения видимости фильтра по тегам.
 * @param onTitleClick Нажатие на компактный заголовок (список прокручен) — открыть Quick Find;
 *   передаются границы заголовка на экране, из них вырастает окно поиска.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListTopAppBar(
    screen: ActiveScreen,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    textPrimaryColor: Color = Color.Unspecified,
    textSecondaryColor: Color = Color.Unspecified,
    project: Item? = null,
    area: Area? = null,
    tag: Tag? = null,
    tasks: List<ItemWithChecklist> = emptyList(),
    backgroundProgress: Float = 0f,
    titleProgress: Float = 0f,
    selectionState: TopAppBarSelectionState = TopAppBarSelectionState(),
    hasTags: Boolean = false,
    isTagsFilterVisible: Boolean = false,
    onToggleTagsFilter: () -> Unit = {},
    // Экран проекта: пункт «Add Heading»
    onAddHeading: (() -> Unit)? = null,
    onTitleClick: ((androidx.compose.ui.geometry.Rect?) -> Unit)? = null
) {
    // Фон и разделитель проявляются вместе с прокруткой, синхронно с растворением заголовка экрана,
    // а не включаются скачком по порогу
    val dividerAlpha = backgroundProgress * 0.2f
    val baseColor = ThingsTheme.colors.background
    val containerColor = baseColor.copy(alpha = backgroundProgress)
    var isOptionsMenuExpanded by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        title = {
            // Компактный заголовок проступает и выезжает снизу вместе с прокруткой, подхватывая заголовок
            // экрана в тот момент, когда тот уже почти растворился
            var titleBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = titleProgress
                        translationY = (1f - titleProgress) * size.height
                    }
                    .onGloballyPositioned { titleBounds = it.boundsInRoot() }
                    // Нажимается, только когда заголовок уже проступил: пока он прозрачный, его не видно
                    .clickable(
                        enabled = onTitleClick != null && titleProgress > 0.5f,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTitleClick?.invoke(titleBounds) }
            ) {
                CategoryTopAppBarTitleContent(
                    screen = screen,
                    project = project,
                    area = area,
                    tag = tag,
                    tasks = tasks,
                    textPrimaryColor = textPrimaryColor
                )
            }
        },
        navigationIcon = {
            if (selectionState.isSelectionMode) {
                IconButton(onClick = selectionState.onCancelSelection) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = textPrimaryColor,
                        modifier = Modifier.size(TOP_APP_BAR_ACTION_ICON_SIZE)
                    )
                }
            } else {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = textPrimaryColor,
                        modifier = Modifier.size(TOP_APP_BAR_ACTION_ICON_SIZE)
                    )
                }
            }
        },
        actions = {
            Box {
                IconButton(
                    onClick = { isOptionsMenuExpanded = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.cd_options),
                        tint = textPrimaryColor,
                        modifier = Modifier.size(TOP_APP_BAR_ACTION_ICON_SIZE)
                    )
                }

                ThingsDropdownMenu(
                    expanded = isOptionsMenuExpanded,
                    onDismissRequest = { isOptionsMenuExpanded = false }
                ) {
                    if (selectionState.isSelectionMode) {
                        if (selectionState.isAllSelected) {
                            ThingsMenuItem(stringResource(R.string.deselect_all), Icons.Default.RemoveDone, onClick = {
                                isOptionsMenuExpanded = false
                                selectionState.onDeselectAllClick()
                            })
                        } else {
                            ThingsMenuItem(stringResource(R.string.select_all), Icons.Default.DoneAll, onClick = {
                                isOptionsMenuExpanded = false
                                selectionState.onSelectAllClick()
                            })
                        }
                    } else {
                        if (onAddHeading != null) {
                            ThingsMenuItem(stringResource(R.string.ui_add_heading), Icons.Default.Add, onClick = {
                                isOptionsMenuExpanded = false
                                onAddHeading()
                            })
                        }
                        ThingsMenuItem(stringResource(R.string.select_items), Icons.Default.Checklist, onClick = {
                            isOptionsMenuExpanded = false
                            selectionState.onEnterSelectionMode()
                        })
                        if (hasTags) {
                            ThingsMenuItem(
                                stringResource(if (isTagsFilterVisible) R.string.hide_tags else R.string.show_tags),
                                AppIcons.Tag,
                                onClick = {
                                    isOptionsMenuExpanded = false
                                    onToggleTagsFilter()
                                }
                            )
                        }
                    }
                }
            }
        },
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor
        ),
        modifier = modifier
            .clipToBounds()
            .drawWithContent {
                drawContent()
                if (dividerAlpha > 0f) {
                    val strokeWidth = ThingsStroke.THIN.toPx()
                    val y = size.height - strokeWidth / 2
                    drawLine(
                        color = textSecondaryColor.copy(alpha = dividerAlpha),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )
                }
            }
    )
}

/**
 * Отображение содержимого компактного заголовка (иконка + название/прогресс) для текущего экрана.
 *
 * @param screen Текущий активный экран.
 * @param project Объект проекта (если открыт экран проекта).
 * @param area Объект области ответственности (если открыт экран области).
 * @param tasks Список задач проекта (для вычисления прогресса проекта).
 * @param textPrimaryColor Цвет текста заголовка.
 */
@Composable
private fun CategoryTopAppBarTitleContent(
    screen: ActiveScreen,
    project: Item?,
    area: Area?,
    tag: Tag?,
    tasks: List<ItemWithChecklist>,
    textPrimaryColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (screen) {
            ActiveScreen.TODAY -> {
                Icon(
                    imageVector = AppIcons.Today,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(TOP_APP_BAR_ICON_SIZE)
                )
                Spacer(modifier = Modifier.width(TOP_APP_BAR_SPACING))
                Text(
                    text = stringResource(R.string.category_today),
                    style = ThingsTheme.type.topAppBarTitle,
                    color = textPrimaryColor
                )
            }
            ActiveScreen.INBOX -> {
                Icon(
                    imageVector = AppIcons.Inbox,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(TOP_APP_BAR_ICON_SIZE)
                )
                Spacer(modifier = Modifier.width(TOP_APP_BAR_SPACING))
                Text(
                    text = stringResource(R.string.category_inbox),
                    style = ThingsTheme.type.topAppBarTitle,
                    color = textPrimaryColor
                )
            }
            ActiveScreen.UPCOMING -> {
                Icon(
                    imageVector = AppIcons.Upcoming,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(TOP_APP_BAR_ICON_SIZE)
                )
                Spacer(modifier = Modifier.width(TOP_APP_BAR_SPACING))
                Text(
                    text = stringResource(R.string.category_upcoming),
                    style = ThingsTheme.type.topAppBarTitle,
                    color = textPrimaryColor
                )
            }
            ActiveScreen.ANYTIME -> {
                Icon(
                    imageVector = AppIcons.Anytime,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(TOP_APP_BAR_ICON_SIZE)
                )
                Spacer(modifier = Modifier.width(TOP_APP_BAR_SPACING))
                Text(
                    text = stringResource(R.string.category_anytime),
                    style = ThingsTheme.type.topAppBarTitle,
                    color = textPrimaryColor
                )
            }
            ActiveScreen.SOMEDAY -> {
                Icon(
                    imageVector = AppIcons.Someday,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(TOP_APP_BAR_ICON_SIZE)
                )
                Spacer(modifier = Modifier.width(TOP_APP_BAR_SPACING))
                Text(
                    text = stringResource(R.string.category_someday),
                    style = ThingsTheme.type.topAppBarTitle,
                    color = textPrimaryColor
                )
            }
            ActiveScreen.LOGBOOK -> {
                Icon(
                    imageVector = AppIcons.Logbook,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(TOP_APP_BAR_ICON_SIZE)
                )
                Spacer(modifier = Modifier.width(TOP_APP_BAR_SPACING))
                Text(
                    text = stringResource(R.string.category_logbook),
                    style = ThingsTheme.type.topAppBarTitle,
                    color = textPrimaryColor
                )
            }
            ActiveScreen.PROJECT_DETAIL -> {
                val completedCount = tasks.count { it.item.projectId == project?.id && it.item.isCompleted }
                val totalCount = tasks.count { it.item.projectId == project?.id }
                com.example.ui.components.ProjectProgressArc(
                    completed = completedCount,
                    total = totalCount,
                    color = ThingsTheme.colors.project,
                    dashed = project?.start == com.example.data.model.Item.START_SOMEDAY,
                    modifier = Modifier.size(PROJECT_PROGRESS_ARC_SIZE)
                )
                Spacer(modifier = Modifier.width(TOP_APP_BAR_SPACING))
                Text(
                    text = project?.name ?: stringResource(R.string.fallback_project),
                    style = ThingsTheme.type.topAppBarTitle,
                    color = textPrimaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ActiveScreen.AREA_DETAIL -> {
                Icon(
                    imageVector = AppIcons.Area,
                    contentDescription = null,
                    tint = ThingsTheme.colors.area,
                    modifier = Modifier.size(AREA_ICON_SIZE)
                )
                Spacer(modifier = Modifier.width(TOP_APP_BAR_SPACING))
                Text(
                    text = area?.title ?: stringResource(R.string.fallback_area),
                    style = ThingsTheme.type.topAppBarTitle,
                    color = textPrimaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ActiveScreen.TAG_DETAIL -> {
                Icon(
                    imageVector = AppIcons.Tag,
                    contentDescription = null,
                    tint = ThingsTheme.colors.area,
                    modifier = Modifier.size(AREA_ICON_SIZE)
                )
                Spacer(modifier = Modifier.width(TOP_APP_BAR_SPACING))
                Text(
                    text = tag?.title ?: stringResource(R.string.fallback_tag),
                    style = ThingsTheme.type.topAppBarTitle,
                    color = textPrimaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ActiveScreen.SEARCH -> {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = textPrimaryColor,
                    modifier = Modifier.size(TOP_APP_BAR_ICON_SIZE)
                )
                Spacer(modifier = Modifier.width(TOP_APP_BAR_SPACING))
                Text(
                    text = stringResource(R.string.cd_search),
                    style = ThingsTheme.type.topAppBarTitle,
                    color = textPrimaryColor
                )
            }
            else -> {}
        }
    }
}
