package com.example.ui.screens.home.subcomponents

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.ui.screens.home.ActiveScreen
import com.example.ui.theme.AppIcons
import com.example.ui.theme.ThingsBlue
import com.example.ui.theme.ThingsBackgroundDark
import com.example.ui.theme.ThingsBackgroundLight
import com.example.ui.theme.topAppBarTitle

private val BACK_ICON_SIZE = 28.dp
private val OPTIONS_BOX_SIZE = 22.dp
private val OPTIONS_ICON_SIZE = 14.dp
private const val BACK_CONTENT_DESC = "Back"
private const val OPTIONS_CONTENT_DESC = "Options"

// [ИЗМЕНЕНИЕ]: Константы анимации, размеров и отступов для исключения хардкода во всем компоненте
private val SCROLLED_ELEVATION = 4.dp
private val UNSCROLLED_ELEVATION = 0.dp
private const val ANIMATION_DURATION_MS = 100
private val TOP_APP_BAR_ICON_SIZE = 22.dp
private val PROJECT_PROGRESS_ARC_SIZE = 20.dp
private val AREA_ICON_SIZE = 20.dp
private val TOP_APP_BAR_SPACING = 8.dp

/**
 * Верхняя панель навигации (TopAppBar) для экрана категории.
 * Предоставляет кнопку возврата и иконку суб-опций.
 *
 * @param isDark Флаг темной темы для установки цвета контейнера.
 * @param textPrimaryColor Основной цвет текста названия экрана.
 * @param textSecondaryColor Цвет текста и границ элементов управления.
 * @param onBackClick Обработчик нажатия на кнопку "Назад".
 * @param screen Текущий активный экран для вывода заголовка/иконки.
 * @param project Объект проекта (для экрана PROJECT_DETAIL).
 * @param area Объект области ответственности (для экрана AREA_DETAIL).
 * @param tasks Список задач проекта (для вычисления прогресса проекта).
 * @param isScrolled Флаг, указывающий на скролл списка дальше порогового значения.
 * @param modifier Модификатор для внешнего контейнера.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListTopAppBar(
    isDark: Boolean,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    onBackClick: () -> Unit,
    screen: ActiveScreen,
    project: Item?,
    area: Area?,
    tasks: List<ItemWithChecklist>,
    isScrolled: Boolean,
    modifier: Modifier = Modifier
) {
    // [ИЗМЕНЕНИЕ]: Анимация прогресса прокрутки (плавный переход за ANIMATION_DURATION_MS / 10 для сверхбыстрого переключения прозрачности)
    val scrollProgress by animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0f,
        animationSpec = tween(durationMillis = ANIMATION_DURATION_MS / 10),
        label = "scrollProgress"
    )
    val dividerAlpha = scrollProgress * 0.2f
    val baseColor = if (isDark) ThingsBackgroundDark else ThingsBackgroundLight
    val containerColor = baseColor.copy(alpha = scrollProgress)

    CenterAlignedTopAppBar(
        title = {
            // [ИЗМЕНЕНИЕ]: Анимированное появление заголовка и иконки при скрытии основного заголовка (выполняется slide в направлении снизу вверх)
            AnimatedVisibility(
                visible = isScrolled,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = ANIMATION_DURATION_MS)
                ) + fadeIn(animationSpec = tween(durationMillis = ANIMATION_DURATION_MS)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = ANIMATION_DURATION_MS)
                ) + fadeOut(animationSpec = tween(durationMillis = ANIMATION_DURATION_MS))
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
                                text = "Today",
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
                                text = "Inbox",
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
                                text = "Upcoming",
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
                                text = "Anytime",
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
                                text = "Someday",
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
                                text = "Logbook",
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
                                imageVector = Icons.Outlined.Layers,
                                contentDescription = null,
                                tint = Color(0xFF1B80FA),
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
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = BACK_CONTENT_DESC,
                    tint = ThingsBlue,
                    modifier = Modifier.size(BACK_ICON_SIZE)
                )
            }
        },
        actions = {
            IconButton(
                onClick = {}
            ) {
                Box(
                    modifier = Modifier
                        .size(OPTIONS_BOX_SIZE)
                        .border(1.dp, textSecondaryColor.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = OPTIONS_CONTENT_DESC,
                        tint = textSecondaryColor,
                        modifier = Modifier.size(OPTIONS_ICON_SIZE)
                    )
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
