package com.example.ui.screens.home.subcomponents

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.ui.screens.home.ActiveScreen
import com.example.ui.theme.AppIcons
import com.example.ui.theme.ThingsBlue
import com.example.ui.theme.ThingsLogbookGreen
import com.example.ui.theme.ThingsBackgroundDark
import com.example.ui.theme.ThingsBackgroundLight
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
 * @param isDark Флаг темной темы для установки цвета контейнера.
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
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListTopAppBar(
    screen: ActiveScreen,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    textPrimaryColor: Color = Color.Unspecified,
    textSecondaryColor: Color = Color.Unspecified,
    project: Item? = null,
    area: Area? = null,
    tasks: List<ItemWithChecklist> = emptyList(),
    backgroundProgress: Float = 0f,
    titleProgress: Float = 0f,
    selectionState: TopAppBarSelectionState = TopAppBarSelectionState(),
    hasTags: Boolean = false,
    isTagsFilterVisible: Boolean = false,
    onToggleTagsFilter: () -> Unit = {}
) {
    // Фон и разделитель проявляются вместе с прокруткой, синхронно с растворением заголовка экрана,
    // а не включаются скачком по порогу
    val dividerAlpha = backgroundProgress * 0.2f
    val baseColor = if (isDark) ThingsBackgroundDark else ThingsBackgroundLight
    val containerColor = baseColor.copy(alpha = backgroundProgress)
    var isOptionsMenuExpanded by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        title = {
            // Компактный заголовок проступает и выезжает снизу вместе с прокруткой, подхватывая заголовок
            // экрана в тот момент, когда тот уже почти растворился
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = titleProgress
                    translationY = (1f - titleProgress) * size.height
                }
            ) {
                CategoryTopAppBarTitleContent(
                    screen = screen,
                    project = project,
                    area = area,
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

                DropdownMenu(
                    expanded = isOptionsMenuExpanded,
                    onDismissRequest = { isOptionsMenuExpanded = false }
                ) {
                    if (selectionState.isSelectionMode) {
                        if (selectionState.isAllSelected) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.deselect_all)) },
                                onClick = {
                                    isOptionsMenuExpanded = false
                                    selectionState.onDeselectAllClick()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.select_all)) },
                                onClick = {
                                    isOptionsMenuExpanded = false
                                    selectionState.onSelectAllClick()
                                }
                            )
                        }
                    } else {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.select_items)) },
                            onClick = {
                                isOptionsMenuExpanded = false
                                selectionState.onEnterSelectionMode()
                            }
                        )
                        if (hasTags) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            if (isTagsFilterVisible) R.string.hide_tags else R.string.show_tags
                                        )
                                    )
                                },
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
                    val strokeWidth = 1.dp.toPx()
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
                    style = MaterialTheme.typography.topAppBarTitle,
                    color = textPrimaryColor,
                    fontWeight = FontWeight.SemiBold
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
                    style = MaterialTheme.typography.topAppBarTitle,
                    color = textPrimaryColor,
                    fontWeight = FontWeight.SemiBold
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
                    style = MaterialTheme.typography.topAppBarTitle,
                    color = textPrimaryColor,
                    fontWeight = FontWeight.SemiBold
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
                    style = MaterialTheme.typography.topAppBarTitle,
                    color = textPrimaryColor,
                    fontWeight = FontWeight.SemiBold
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
                    style = MaterialTheme.typography.topAppBarTitle,
                    color = textPrimaryColor,
                    fontWeight = FontWeight.SemiBold
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
                    style = MaterialTheme.typography.topAppBarTitle,
                    color = textPrimaryColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
            ActiveScreen.PROJECT_DETAIL -> {
                val completedCount = tasks.count { it.item.projectId == project?.id && it.item.isCompleted }
                val totalCount = tasks.count { it.item.projectId == project?.id }
                com.example.ui.components.ProjectProgressArc(
                    completed = completedCount,
                    total = totalCount,
                    color = ThingsBlue,
                    modifier = Modifier.size(PROJECT_PROGRESS_ARC_SIZE)
                )
                Spacer(modifier = Modifier.width(TOP_APP_BAR_SPACING))
                Text(
                    text = project?.name ?: "Project",
                    style = MaterialTheme.typography.topAppBarTitle,
                    color = textPrimaryColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ActiveScreen.AREA_DETAIL -> {
                Icon(
                    imageVector = AppIcons.Area,
                    contentDescription = null,
                    tint = ThingsLogbookGreen,
                    modifier = Modifier.size(AREA_ICON_SIZE)
                )
                Spacer(modifier = Modifier.width(TOP_APP_BAR_SPACING))
                Text(
                    text = area?.title ?: "Responsibility Area",
                    style = MaterialTheme.typography.topAppBarTitle,
                    color = textPrimaryColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            else -> {}
        }
    }
}
