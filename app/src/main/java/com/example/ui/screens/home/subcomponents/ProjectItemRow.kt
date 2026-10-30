package com.example.ui.screens.home.subcomponents

import com.example.ui.viewmodel.ProjectProgress
import com.example.ui.theme.ThingsIconSize
import com.example.ui.theme.ThingsAlpha
import com.example.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import com.example.ui.theme.ThingsTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.ui.components.ClosedProjectIcon
import com.example.ui.components.ProjectProgressArc
import com.example.ui.components.selectionTint
import com.example.ui.components.swipe.SwipeableTaskContainer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.ui.theme.dimens

// Название → значок заметок → шеврон
private val NOTES_ICON_GAP = 5.dp
private val CHEVRON_GAP = 3.dp

private val NOTES_ICON_SIZE = 14.dp
/** Значок повтора перед названием шаблона повторяющегося проекта — как у шаблона задачи */
private val ROW_REPEAT_ICON_SIZE = 15.dp
// Новое в «Сегодня» — жёлтая точка перед названием, как у задачи
private val TODAY_NEW_DOT_SIZE = 8.dp

/**
 * Строка проекта в списке проектов области.
 * Отображает круглый индикатор прогресса ThingsTheme.colors.accent, название проекта,
 * опциональную иконку заметок и встроенный шеврон перехода > согласно эталону Things 3.
 */
@Composable
fun ProjectItemRow(
    project: Item,
    tasks: List<ItemWithChecklist> = emptyList(),
    projectProgressMap: Map<String, ProjectProgress> = emptyMap(),
    textPrimaryColor: Color,
    inlineExpandedTaskId: String?,
    onProjectClick: (Item) -> Unit,
    // Дата закрытия перед названием — в «Журнале», в той же колонке, что у закрытых задач
    closedAt: Long? = null,
    // Плашка с датой старта перед названием — в месячных разделах «Предстоящих», как у задач
    dateBadge: String? = null,
    // Вторая строка под названием — область проекта (у закрытых проектов, как в Logbook Things)
    subtitle: String? = null,
    // Проект на сегодня — звезда, на вечер — луна перед названием вместо плашки, как у задач («Повторяющиеся»)
    todayIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    todayIconTint: Color = Color.Unspecified,
    // Новое в «Сегодня» — жёлтая точка перед названием, до «OK» на плашке
    isNew: Boolean = false,
    // Множественный выбор проектов (свайп влево, как у задач): нажатие отмечает, справа — кружок выбора
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    // Свайпы по проекту, как на главном экране: вправо — «Когда», влево — выбор; null — строка без свайпов
    swipe: ProjectRowSwipe? = null,
    modifier: Modifier = Modifier
) {
    val progress = projectProgressMap[project.id]
    val completedCount = progress?.completed ?: tasks.filter { it.item.projectId == project.id }.count { it.item.isClosed }
    val totalCount = progress?.total ?: tasks.count { it.item.projectId == project.id }
    // Выполненный или отменённый проект («Журнал», Logbook поиска) — кольцо с галочкой или крестиком
    val isDone = project.isCompleted || project.status == Item.STATUS_CANCELLED
    val shouldDim = inlineExpandedTaskId != null
    val dimAlpha by animateFloatAsState(
        targetValue = if (shouldDim) 0.3f else 1f,
        label = "dimAlpha_proj_${project.id}"
    )

    val rowContent: @Composable (Modifier) -> Unit = { rowModifier ->
    Row(
        modifier = rowModifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.dimens.rowHeight)
            .clip(ThingsTheme.shapes.rowShape)
            .selectionTint(if (isSelected) ThingsTheme.colors.accentSelection else Color.Transparent)
            .clickable { if (isSelectionMode) onToggleSelect() else onProjectClick(project) }
            .graphicsLayer { alpha = dimAlpha }
            .padding(
                start = MaterialTheme.dimens.taskRowStartPadding,
                end = MaterialTheme.dimens.taskRowEndPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Колонка той же ширины, что под чекбоксом задачи: кольцо центрируется по оси чекбоксов,
        // а название начинается с того же края, что и названия задач. Кольцо крупнее колонки
        // и выступает из неё поровну с обеих сторон
        Box(
            modifier = Modifier.size(MaterialTheme.dimens.taskLeftColumnWidthDefault),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                ClosedProjectIcon(
                    cancelled = project.status == Item.STATUS_CANCELLED,
                    modifier = Modifier.requiredSize(ThingsIconSize.M)
                )
            } else {
                ProjectProgressArc(
                    completed = completedCount,
                    total = totalCount,
                    // «Когда-нибудь» — серым пунктиром
                    color = if (project.start == Item.START_SOMEDAY) ThingsTheme.colors.someday else ThingsTheme.colors.project,
                    dashed = project.start == Item.START_SOMEDAY,
                    modifier = Modifier.requiredSize(ThingsIconSize.M)
                )
            }
        }
        Spacer(modifier = Modifier.width(MaterialTheme.dimens.taskSpacingToTextDefault))
        if (closedAt != null) {
            ClosedDateLabel(closedAt = closedAt, inLogbook = true)
            Spacer(modifier = Modifier.width(com.example.ui.theme.ThingsSpacing.S))
        }
        if (todayIcon != null) {
            Icon(todayIcon, contentDescription = null, tint = todayIconTint, modifier = Modifier.size(ThingsIconSize.XS))
            Spacer(modifier = Modifier.width(com.example.ui.theme.ThingsSpacing.XS_PLUS))
        } else if (dateBadge != null) {
            DateBadge(text = dateBadge)
            Spacer(modifier = Modifier.width(com.example.ui.theme.ThingsSpacing.XS_PLUS))
        }
        Column(modifier = Modifier.weight(1f)) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Шаблон повторяющегося проекта — как шаблон задачи: кольцо проекта на месте, синий значок повтора перед названием
            if (project.type == Item.TYPE_PROJECT_TEMPLATE) {
                com.example.ui.components.RepeatTemplateIcon(modifier = Modifier.size(ROW_REPEAT_ICON_SIZE))
                Spacer(modifier = Modifier.width(com.example.ui.theme.ThingsSpacing.XS_PLUS))
            }
            if (isNew) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(TODAY_NEW_DOT_SIZE)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(ThingsTheme.colors.todayNew)
                )
                Spacer(modifier = Modifier.width(com.example.ui.theme.ThingsSpacing.XS_PLUS))
            }
            Text(
                text = project.title,
                style = ThingsTheme.type.listTitle.copy(
                    color = textPrimaryColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            // Копия повторяющегося проекта — серая круговая стрелка справа от названия, как у копий задач
            if (project.type == Item.TYPE_PROJECT && project.repeatTemplateId != null) {
                Spacer(modifier = Modifier.width(NOTES_ICON_GAP))
                Icon(
                    imageVector = com.example.ui.theme.AppIcons.RepeatCopy,
                    contentDescription = stringResource(R.string.cd_repeat_copy),
                    tint = ThingsTheme.colors.textSecondary.copy(alpha = ThingsAlpha.MUTED),
                    modifier = Modifier.size(NOTES_ICON_SIZE)
                )
            }
            // Свойства проекта, как у задачи: напоминание, заметки, теги, дедлайн — между названием и шевроном
            val metaTint = ThingsTheme.colors.textSecondary.copy(alpha = ThingsAlpha.MUTED)
            if (project.reminderTime != null && !isDone) {
                Spacer(modifier = Modifier.width(NOTES_ICON_GAP))
                Icon(
                    imageVector = com.example.ui.theme.AppIcons.Bell,
                    contentDescription = stringResource(R.string.reminder_title),
                    tint = metaTint,
                    modifier = Modifier.size(NOTES_ICON_SIZE)
                )
            }
            if (project.notes.isNotBlank()) {
                Spacer(modifier = Modifier.width(NOTES_ICON_GAP))
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = stringResource(R.string.cd_has_notes),
                    tint = metaTint,
                    modifier = Modifier.size(NOTES_ICON_SIZE)
                )
            }
            if (project.cachedTags.isNotBlank()) {
                Spacer(modifier = Modifier.width(NOTES_ICON_GAP))
                Icon(
                    imageVector = com.example.ui.theme.AppIcons.Tag,
                    contentDescription = stringResource(R.string.cd_has_tags),
                    tint = metaTint,
                    modifier = Modifier.size(NOTES_ICON_SIZE)
                )
            }
            if (project.dueDate != null && !isDone) {
                // Дедлайн сегодня или просрочен — красным, как у задачи
                val dueSoon = project.dueDate <= com.example.data.model.DayBounds.now().endOfToday
                Spacer(modifier = Modifier.width(NOTES_ICON_GAP))
                Icon(
                    imageVector = com.example.ui.theme.AppIcons.Deadline,
                    contentDescription = stringResource(R.string.cd_deadline_flag),
                    tint = if (dueSoon) ThingsTheme.colors.danger else metaTint,
                    modifier = Modifier.size(NOTES_ICON_SIZE)
                )
            }
            Spacer(modifier = Modifier.width(CHEVRON_GAP))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = ThingsTheme.colors.textSecondary.copy(alpha = ThingsAlpha.MUTED),
                modifier = Modifier.size(ThingsIconSize.XS)
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = ThingsTheme.type.taskSubtitle.copy(color = ThingsTheme.colors.textSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = isSelectionMode,
            enter = androidx.compose.animation.scaleIn() + androidx.compose.animation.fadeIn() +
                androidx.compose.animation.expandHorizontally(),
            exit = androidx.compose.animation.scaleOut() + androidx.compose.animation.fadeOut() +
                androidx.compose.animation.shrinkHorizontally()
        ) {
            SelectionCircle(isSelected = isSelected, isDragSelecting = false, onToggle = onToggleSelect)
        }
    }
    }
    if (swipe == null) {
        rowContent(modifier)
    } else {
        // Смещение центра строки от центра экрана: из него вырастает диалог «Когда», как у задач.
        // Перетаскивание ([modifier]) — на внешнем контейнере свайпа, иначе долгое нажатие перехватывает свайп
        var rowCenter by remember { mutableStateOf<Offset?>(null) }
        SwipeableTaskContainer(
            modifier = modifier.onGloballyPositioned { coordinates ->
                val root = coordinates.findRootCoordinates()
                rowCenter = coordinates.boundsInRoot().center - Offset(root.size.width / 2f, root.size.height / 2f)
            },
            onSwipeLeft = swipe.onSwipeLeft,
            onSwipeRight = { swipe.onSwipeRight(rowCenter) },
            enabled = swipe.enabled,
            // В режиме выбора свайп вправо «Когда» не вызывает — как у задач
            swipeRightEnabled = !isSelectionMode
        ) {
            rowContent(Modifier.background(ThingsTheme.colors.background))
        }
    }
}

/** Свайпы строки проекта: влево — отметить (выбор), вправо — «Когда» из точки [Offset] (центр строки от центра экрана) */
class ProjectRowSwipe(
    val onSwipeLeft: () -> Unit,
    val onSwipeRight: (Offset?) -> Unit,
    val enabled: Boolean = true
)
