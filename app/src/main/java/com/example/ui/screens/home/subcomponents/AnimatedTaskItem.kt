package com.example.ui.screens.home.subcomponents

import com.example.ui.theme.ThingsIconSize
import com.example.ui.theme.ThingsMotion
import com.example.ui.theme.ThingsTheme
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.lerp
import androidx.compose.foundation.layout.PaddingValues
import com.example.ui.components.progressPadding
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.Tag
import com.example.data.model.TaskSection
import com.example.data.model.toStartVal
import com.example.data.model.ChecklistItem
import com.example.ui.screens.home.ActiveScreen
import com.example.ui.screens.home.components.ThingsCategoryListEvent
import com.example.ui.components.dragdrop.GenericDragDropState
import com.example.ui.screens.home.components.taskDragAndDrop
import com.example.ui.screens.home.components.UpcomingDay
import com.example.ui.screens.home.inlineeditor.ThingsTaskInlineEditor
import com.example.ui.screens.home.inlineeditor.utils.EditorOutsideTouch
import com.example.ui.theme.dimens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.AppIcons
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.sp
import com.example.ui.components.ProjectProgressArc
import com.example.ui.components.hideSoftKeyboardNow
import androidx.compose.ui.platform.LocalView
import com.example.ui.screens.home.components.ProjectProgress
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatListBulleted
import com.example.ui.components.swipe.SwipeableTaskContainer

// Раскрытие новой задачи из кнопки «+»
private const val FAB_REVEAL_MS = 160

private const val DELETE_ANIMATION_DELAY_MS = 300L

/**
 * Анимированный и интерактивный контейнер задачи.
 * Объединяет логику анимаций перетаскивания (Drag-and-Drop), затемнения (Dimming),
 * отображения встроенного редактора задачи (ThingsTaskInlineEditor) и стандартной строки.
 */
@Composable
fun AnimatedTaskItem(
    taskWrapper: ItemWithChecklist,
    dragDropState: GenericDragDropState,
    inlineExpandedTaskId: String?,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    dividerColor: Color,
    highlightedTaskId: String?,
    projects: List<Item>,
    areas: List<com.example.data.model.Area> = emptyList(),
    allSavedTags: List<String>,
    allSavedTagObjects: List<Tag>,
    deletedTaskIds: List<String>,
    onDeletedTaskIdAdd: (String) -> Unit,
    onEvent: (ThingsCategoryListEvent) -> Unit,
    coroutineScope: CoroutineScope,
    screen: ActiveScreen,
    upcomingDays: List<UpcomingDay>,
    // Заголовки экрана проекта по порядку: перенос задачи меняет и её заголовок
    projectHeadingIds: List<String> = emptyList(),
    localTasksList: List<ItemWithChecklist>,
    displayTasks: List<ItemWithChecklist>,
    allTasks: List<ItemWithChecklist> = emptyList(),
    projectProgressMap: Map<String, ProjectProgress> = emptyMap(),
    onLocalTasksListChange: (List<ItemWithChecklist>) -> Unit,
    lazyListState: LazyListState,
    onWhenDialogVisibilityChange: (Boolean) -> Unit = {},
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    isDragSelecting: Boolean = false,
    onToggleSelect: () -> Unit = {},
    selectedTaskIds: Set<String> = emptySet(),
    onExitSelectionMode: () -> Unit = {},
    dateBadge: String? = null,
    editorOutsideTouch: EditorOutsideTouch? = null,
    modifier: Modifier = Modifier
) {
    val task = taskWrapper.item
    val view = LocalView.current
    val currentProject = remember(task.projectId, projects) {
        projects.firstOrNull { it.id == task.projectId }
    }
    val currentArea = remember(task.areaId, areas) {
        areas.firstOrNull { it.id == task.areaId }
    }
    val isDragTask = dragDropState.draggedItemKey == task.id
    // На время перетаскивания вид строки замораживается: задача и метка даты — такие, какими были,
    // когда её подняли. Во время жеста бизнес-логика меняет задаче дату и раздел (в Upcoming у задач
    // за 14-дневным горизонтом есть метка даты, у задач в днях — нет), и карточка под пальцем мигала
    // меткой на каждом заголовке. Куда встанет задача, показывает слот; новый вид строка получит
    // после отпускания. Сам жест работает с живыми данными (taskWrapper).
    val dragAppearance = remember(isDragTask) {
        if (isDragTask) taskWrapper.item to dateBadge else null
    }
    val rowTask = dragAppearance?.first ?: taskWrapper.item
    val rowDateBadge = if (dragAppearance != null) dragAppearance.second else dateBadge
    val isExpanded = inlineExpandedTaskId == task.id
    val shouldDim = inlineExpandedTaskId != null && !isExpanded

    // Новая задача после сброса кнопки «+»: место под неё уже раздвинуто, и она раскрывается
    // из центра этого места. Строка сразу полной высоты — соседи второй раз не двигаются.
    val fabController = com.example.ui.components.fabdrag.LocalFabDragController.current
    val isFreshFromFab = fabController?.freshTaskId == task.id
    // Решается один раз, когда строка появилась: freshTaskId сбросится после раскрытия. Только на первое
    // раскрытие — строка остаётся в композиции, и повторное открытие тапом должно идти как обычно
    var focusTitleOnOpen by remember(task.id) { mutableStateOf(isFreshFromFab) }
    // Сбрасывается, только когда редактор ушёл совсем: пока он открыт или сворачивается, настройки поля
    // (кнопка «Done» у клавиатуры) не должны меняться — иначе Compose перезапускает ввод и снова
    // показывает клавиатуру прямо во время её скрытия
    var wasExpandedOnce by remember(task.id) { mutableStateOf(false) }
    LaunchedEffect(isExpanded) {
        if (isExpanded) wasExpandedOnce = true
    }
    val reveal = remember(task.id) {
        androidx.compose.animation.core.Animatable(if (isFreshFromFab) 0f else 1f)
    }
    LaunchedEffect(task.id, isFreshFromFab) {
        if (isFreshFromFab && reveal.value < 1f) {
            reveal.animateTo(
                1f,
                androidx.compose.animation.core.tween(
                    durationMillis = FAB_REVEAL_MS,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
            )
            if (fabController?.freshTaskId == task.id) fabController.freshTaskId = null
        }
    }

    // Прогресс раскрытия читается только в лямбдах раскладки и отрисовки (layout, graphicsLayer,
    // drawBehind) и в snapshotFlow: иначе строка и весь встроенный редактор пересобирались бы
    // на каждом кадре анимации.
    val expansionProgressState = animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = com.example.ui.theme.AnimationConstants.TASK_EXPANSION_DURATION_MS.toInt(),
            easing = FastOutSlowInEasing
        ),
        label = "expansionProgress_${task.id}"
    )
    val expansionProgress: () -> Float = remember(expansionProgressState) { { expansionProgressState.value } }
    // Меняется только в начале раскрытия и в конце сворачивания — композиция узнаёт лишь об этих переходах.
    val isExpansionVisible by remember(expansionProgressState) {
        derivedStateOf { expansionProgressState.value > 0f }
    }
    val showEditor = isExpanded || isExpansionVisible
    LaunchedEffect(showEditor) {
        if (!showEditor && wasExpandedOnce) focusTitleOnOpen = false
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val verticalGapLimit = MaterialTheme.dimens.taskExpandedVerticalGap
    val verticalGapLimitPx = with(density) { verticalGapLimit.toPx() }
    val extraTopPaddingPx = with(density) {
        (MaterialTheme.dimens.taskExpandedTopPadding - MaterialTheme.dimens.taskCollapsedTopPadding).toPx()
    }
    val extraBottomPaddingPx = with(density) {
        (MaterialTheme.dimens.taskExpandedBottomPadding - MaterialTheme.dimens.taskCollapsedBottomPadding).toPx()
    }
    val totalCompensationPx = verticalGapLimitPx + extraTopPaddingPx
    var prevPaddingPx by remember(task.id) { mutableStateOf(0f) }

    // Раскрытая задача должна помещаться между тулбаром и плашкой действий внизу экрана.
    // Геометрию пишет раскладка карточки, читает только цикл ниже — без рекомпозиций.
    val cardGeometry = remember(task.id) { ExpandedCardGeometry() }
    // На сколько список уже подтянут ради этой задачи (его пишут раскрытие и выезд клавиатуры)
    val liftHolder = remember(task.id) { LiftHolder() }
    // Центр строки на экране — точка, из которой вырастает диалог When при свайпе вправо.
    // Держатель, а не состояние: его пишет раскладка и читает обработчик жеста, рекомпозиция не нужна.
    val rowCenter = remember(task.id) { RowCenter() }
    val currentIsExpanded by rememberUpdatedState(isExpanded)
    val topLimitPx = WindowInsets.statusBars.getTop(density) + with(density) { EXPANDED_TOP_LIMIT.toPx() }
    val bottomReservePx = WindowInsets.navigationBars.getBottom(density) + with(density) {
        (MaterialTheme.dimens.floatingToolbarBottomPadding + MaterialTheme.dimens.floatingToolbarHeight +
            EXPANDED_BOTTOM_GAP).toPx()
    }

    val imeInsets = WindowInsets.ime
    LaunchedEffect(focusTitleOnOpen) {
        if (!focusTitleOnOpen) return@LaunchedEffect
        // Новая задача появилась уже раскрытой, подтягивать по прогрессу нечего. Зато выезжает клавиатура:
        // список едет вместе с ней, чтобы карточка осталась над клавиатурой, а не ушла под неё.
        val gapPx = with(density) { 12.dp.toPx() }
        val startNanos = withFrameNanos { it }
        // Тот же ход, что у обычного раскрытия задачи по тапу: ту же длительность и кривую
        // имеет подтяжка списка. Время идёт от первого кадра с замеренной карточкой.
        // Чуть быстрее обычного раскрытия: здесь всё остальное (раздвигание, раскрытие карточки) тоже ускорено
        val durationNs = 220_000_000L
        var timelineStart = -1L
        var prevProgress = 0f
        while (currentIsExpanded) {
            val now = withFrameNanos { it }
            if (now - startNanos > 1_800_000_000L) break
            val g = cardGeometry
            if (!g.measured || g.editorFullHeight <= 0 || g.rootHeight <= 0) continue
            if (timelineStart < 0L) timelineStart = now
            val progress = FastOutSlowInEasing.transform(((now - timelineStart).toFloat() / durationNs).coerceIn(0f, 1f))
            val imeTop = g.rootHeight - imeInsets.getBottom(density)
            val limit = minOf(imeTop.toFloat(), g.rootHeight - bottomReservePx)
            val overflow = g.top + g.editorFullHeight - (limit - gapPx)
            val room = g.top - topLimitPx
            val pending = liftHolder.pendingComp
            // Подтяжка списка складывается из отложенной компенсации отступов и подъёма над клавиатурой;
            // компенсация сама сдвинет карточку вверх, поэтому подъём считается уже за её вычетом
            val remaining = pending + minOf(overflow - pending, room - pending).coerceAtLeast(0f)
            val span = 1f - prevProgress
            // Остаток раскладывается на остаток хода — одно движение, как при обычном раскрытии;
            // клавиатура растёт позже начала хода, поэтому остаток может прибавляться по пути
            val step = if (span <= 0f) remaining.coerceAtMost(48f)
            else remaining * ((progress - prevProgress) / span).coerceIn(0f, 1f)
            prevProgress = progress
            if (step > 0f) {
                lazyListState.dispatchRawDelta(step)
                val compPart = minOf(step, pending)
                liftHolder.pendingComp = pending - compPart
                liftHolder.liftPx += step - compPart
                liftHolder.liftAtCollapseStart = liftHolder.liftPx
            }
        }
    }

    LaunchedEffect(lazyListState, verticalGapLimitPx) {
        // Сколько список уже подтянут ради этой задачи, и сколько было к началу сворачивания
        var liftPx by liftHolder::liftPx
        var liftAtCollapseStart by liftHolder::liftAtCollapseStart
        // Прогресс предыдущего кадра: по нему считается, какую часть остатка проехать сейчас
        var prevProgress = 0f
        snapshotFlow { expansionProgressState.value }.collect { progress ->
            val currentPaddingPx = verticalGapLimitPx * progress
            val delta = currentPaddingPx - prevPaddingPx
            if (delta != 0f) {
                if (focusTitleOnOpen && !liftHolder.compDeferred && prevPaddingPx == 0f && delta > 0f) {
                    // Новая задача сразу полной высоты: компенсация отступов не должна выполняться рывком
                    // в первом кадре — её плавно отрабатывает пружина вместе с подъёмом над клавиатурой
                    liftHolder.compDeferred = true
                    liftHolder.pendingComp = delta
                } else {
                    lazyListState.dispatchRawDelta(delta)
                }
            }
            prevPaddingPx = currentPaddingPx

            if (currentIsExpanded) {
                // Раскрытие: список едет вместе с ростом карточки, а не после того, как она упрётся
                // в плашку действий. На каждом кадре считается, сколько ещё надо подтянуть, и этот
                // остаток раскладывается на остаток анимации — получается одно движение, которое
                // заканчивается вместе с раскрытием. Задача, которая помещается целиком, не сдвигается
                // вовсе; верх карточки не поднимается выше тулбара, даже если ради этого низ не влезет.
                val g = cardGeometry
                // У новой задачи, созданной перетаскиванием «+», подъём ведёт пружина выше (вместе с клавиатурой)
                if (!focusTitleOnOpen && g.measured && g.editorFullHeight > 0 && g.rootHeight > 0) {
                    // Замеренная высота редактора — с отступами этого кадра: они тоже едут по прогрессу
                    // (progressPadding), поэтому к концу раскрытия карточка станет выше на их остаток
                    val finalHeight = g.editorFullHeight +
                        (extraTopPaddingPx + extraBottomPaddingPx) * (1f - progress)
                    // Где окажется верх карточки к концу раскрытия, если больше не подтягивать список:
                    // компенсация верхнего отступа сдвинет его вниз на остаток своего хода
                    val finalTop = g.top - extraTopPaddingPx * (1f - progress)
                    val overflow = finalTop + finalHeight - (g.rootHeight - bottomReservePx)
                    val room = finalTop - topLimitPx
                    val remaining = minOf(overflow, room).coerceAtLeast(0f)
                    val span = 1f - prevProgress
                    val step = if (span <= 0f) remaining
                    else remaining * ((progress - prevProgress) / span).coerceIn(0f, 1f)
                    if (step > 0f) {
                        lazyListState.dispatchRawDelta(step)
                        liftPx += step
                    }
                }
                liftAtCollapseStart = liftPx
            } else if (liftPx > 0f) {
                // Сворачивание: подтяжка возвращается вместе с прогрессом, задача встаёт на прежнее место
                val target = liftAtCollapseStart * progress
                val back = liftPx - target
                if (back > 0f) {
                    lazyListState.dispatchRawDelta(-back)
                    liftPx = target
                }
            }
            if (progress == 0f) {
                liftPx = 0f
                liftAtCollapseStart = 0f
            }
            prevProgress = progress
        }
    }

    val isBeingDeleted by remember(task.id) {
        derivedStateOf { deletedTaskIds.contains(task.id) }
    }

    val dimAlpha by animateFloatAsState(
        targetValue = if (shouldDim) 0.3f else 1f,
        label = "dimAlpha_${task.id}"
    )
    val dragScale by animateFloatAsState(
        targetValue = if (isDragTask) 1.04f else 1.0f,
        label = "dragScale_${task.id}"
    )
    // Высота тени тоже читается только при отрисовке слоя (см. graphicsLayer карточки ниже).
    val dragElevationState = animateDpAsState(
        targetValue = if (isDragTask || showEditor) 8.dp else 0.dp,
        animationSpec = tween(
            durationMillis = com.example.ui.theme.AnimationConstants.TASK_EXPANSION_DURATION_MS.toInt(),
            easing = FastOutSlowInEasing
        ),
        label = "dragElev_${task.id}"
    )
    val hasElevation by remember(dragElevationState) {
        derivedStateOf { dragElevationState.value > 0.dp }
    }
    val zIndexValToUse = if (isDragTask) 100f else (if (showEditor) 1f else 0f)

    val containerBgColor = if (showEditor || isDragTask || hasElevation) ThingsTheme.colors.background else Color.Transparent

    val collapsedRadius = ThingsTheme.shapes.row
    val expandedRadius = ThingsTheme.shapes.row
    // Текущий радиус скругления карточки — только для чтения в лямбдах раскладки и отрисовки.
    val cornerRadius: () -> Dp = { lerp(collapsedRadius, expandedRadius, expansionProgress()) }
    // Форма для слоёв перетаскивания: вне перетаскивания прогресс в композиции не читается,
    // а во время перетаскивания он не меняется.
    val dragCornerShape = if (isDragTask) RoundedCornerShape(cornerRadius()) else RectangleShape

    val isSecondaryBatchItem = dragDropState.stackedDragKeys.isNotEmpty()
        && dragDropState.stackedDragKeys.contains(task.id)
        && task.id != dragDropState.draggedItemKey

    Box(
        modifier = modifier
            .zIndex(zIndexValToUse)
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = !isSecondaryBatchItem,
            enter = androidx.compose.animation.expandVertically(
                animationSpec = tween(
                    durationMillis = ThingsMotion.MEDIUM,
                    easing = FastOutSlowInEasing
                )
            ) + androidx.compose.animation.fadeIn(animationSpec = tween(ThingsMotion.BASE)),
            exit = androidx.compose.animation.shrinkVertically(
                animationSpec = tween(
                    durationMillis = ThingsMotion.MEDIUM,
                    easing = FastOutSlowInEasing
                )
            ) + androidx.compose.animation.fadeOut(animationSpec = tween(ThingsMotion.BASE))
        ) {
            Box {
                // Каскадный эффект стопки карточек под ведущей задачей при групповом перетаскивании
                if (isDragTask && dragDropState.hasStackedItems) {
                val stackCardBg = ThingsTheme.colors.stackCard
                val stackBorderColor = ThingsTheme.colors.hairline

                // 3-й слой стопки (если в пачке 3 или более задач)
                if (dragDropState.stackedDragKeys.size >= 3) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                val currentTranslationY = if (isDragTask) dragDropState.visualDragOffsetY(task.id) else 0f
                                val currentTranslationX = if (isDragTask) dragDropState.dragAccumulatedX else 0f
                                translationX = currentTranslationX + 8.dp.toPx()
                                translationY = currentTranslationY + 8.dp.toPx()
                                scaleX = dragScale
                                scaleY = dragScale
                                rotationZ = 3.2f
                                shadowElevation = 4.dp.toPx()
                                shape = dragCornerShape
                                clip = true
                            }
                            .background(stackCardBg, dragCornerShape)
                            .border(0.5.dp, stackBorderColor, dragCornerShape)
                    )
                }

                // 2-й слой стопки
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            val currentTranslationY = if (isDragTask) dragDropState.visualDragOffsetY(task.id) else 0f
                            val currentTranslationX = if (isDragTask) dragDropState.dragAccumulatedX else 0f
                            translationX = currentTranslationX + 4.dp.toPx()
                            translationY = currentTranslationY + 4.dp.toPx()
                            scaleX = dragScale
                            scaleY = dragScale
                            rotationZ = 1.6f
                            shadowElevation = 6.dp.toPx()
                            shape = dragCornerShape
                            clip = true
                        }
                        .background(stackCardBg, dragCornerShape)
                        .border(0.5.dp, stackBorderColor, dragCornerShape)
                )
            }

            if (isDragTask) {
                val placeholderBgColor = ThingsTheme.colors.dropPlaceholder
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0.5f) // Полупрозрачный
                        .zIndex(-1f) // Уровнем ниже всех задач в списке (в рамках контекста элемента)
                        .background(placeholderBgColor, dragCornerShape)
                )
            }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = if (isDragTask) dragDropState.dragAccumulatedX else 0f
                    translationY = if (isDragTask) dragDropState.visualDragOffsetY(task.id) else 0f
                    val r = reveal.value
                    scaleX = dragScale * (0.3f + 0.7f * r)
                    scaleY = dragScale * r
                    alpha = dimAlpha * (r * 3f).coerceAtMost(1f)
                }
                .progressPadding(
                    progress = expansionProgress,
                    collapsed = PaddingValues(0.dp),
                    expanded = PaddingValues(vertical = verticalGapLimit)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        // Мгновенная компенсация ВСЕГО вертикального смещения при раскрытии:
                        // totalCompensationPx * progress = полное смещение (внешний зазор + внутренний top-padding)
                        // prevPaddingPx = сколько уже скомпенсировано скроллом (dispatchRawDelta)
                        // Разница компенсируется graphicsLayer мгновенно, в том же кадре отрисовки.
                        // По мере того как dispatchRawDelta догоняет, graphicsLayer плавно уменьшает компенсацию.
                        translationY = -(totalCompensationPx * expansionProgress() - prevPaddingPx)
                    }
                    .onGloballyPositioned { coordinates ->
                        // Видимый верх карточки (с учётом сдвига слоя выше) и высота окна — для подтяжки списка
                        if (showEditor) {
                            cardGeometry.top = coordinates.positionInRoot().y
                            cardGeometry.rootHeight = coordinates.findRootCoordinates().size.height
                            cardGeometry.measured = true
                        } else {
                            cardGeometry.measured = false
                        }
                    }
                    .layout { measurable, constraints ->
                        val extraPaddingPx = (10.dp * expansionProgress()).roundToPx()
                        val extendedConstraints = constraints.copy(
                            minWidth = (constraints.minWidth + extraPaddingPx * 2).coerceAtMost(constraints.maxWidth + extraPaddingPx * 2),
                            maxWidth = (constraints.maxWidth + extraPaddingPx * 2)
                        )
                        val placeable = measurable.measure(extendedConstraints)
                        layout(placeable.width - extraPaddingPx * 2, placeable.height) {
                            placeable.place(-extraPaddingPx, 0)
                        }
                    }
                    .graphicsLayer {
                        // То же, что shadow(dragElevation, shape): тень и обрезка по скруглению,
                        // но высота и радиус читаются при отрисовке слоя, а не в композиции.
                        val elevation = dragElevationState.value
                        shadowElevation = elevation.toPx()
                        shape = RoundedCornerShape(cornerRadius())
                        clip = elevation > 0.dp
                    }
                    .drawBehind {
                        // То же, что background(containerBgColor, shape) со скруглением текущего радиуса.
                        if (containerBgColor != Color.Transparent) {
                            val radius = cornerRadius().toPx()
                            drawRoundRect(containerBgColor, cornerRadius = CornerRadius(radius, radius))
                        }
                    }
            ) {
                if (showEditor) {
                    ThingsTaskInlineEditor(
                        task = taskWrapper,
                        projects = projects,
                        allSavedTags = allSavedTags,
                        allSavedTagObjects = allSavedTagObjects,
                        onNewTagCreated = { title, parentId -> onEvent(ThingsCategoryListEvent.CreateTag(title, parentId)) },
                        onDeleteTag = { tag -> onEvent(ThingsCategoryListEvent.DeleteTag(tag)) },
                        onUpdateTag = { tag -> onEvent(ThingsCategoryListEvent.UpdateTag(tag)) },
                        onUpdateTagsOrder = { tags -> onEvent(ThingsCategoryListEvent.UpdateTagsOrder(tags)) },
                        isDeletedExternally = { deletedTaskIds.contains(task.id) },
                        isExpanded = isExpanded,
                        autoFocusTitle = focusTitleOnOpen,
                        outsideTouch = editorOutsideTouch,
                        expansionProgress = expansionProgress,
                        onFullHeightMeasured = { cardGeometry.editorFullHeight = it },
                        screen = screen,
                        onToggle = { currentTitle, currentNotes, currentSection, currentIsTonight, currentStartDate, currentDueDate, currentTags, currentProjectId, currentChecklist, currentPriority ->
                            val updatedItem = taskWrapper.item.copy(
                                title = currentTitle,
                                notes = currentNotes,
                                start = currentSection.toStartVal(),
                                isTonight = currentIsTonight,
                                startDate = currentStartDate,
                                dueDate = currentDueDate,
                                cachedTags = com.example.domain.tag.TagTitles.join(currentTags),
                                projectId = currentProjectId,
                                priority = currentPriority
                            )
                            val updatedWrapper = taskWrapper.copy(
                                item = updatedItem,
                                checklist = currentChecklist
                            )
                            onEvent(ThingsCategoryListEvent.ToggleTask(updatedWrapper))
                        },
                        onSave = { title, notes, section, isTonight, startDate, dueDate, tags, projectId, checklist, priority ->
                            val hasPositionChange = (projectId != task.projectId) ||
                                    (isTonight != task.isTonight) ||
                                    (startDate != task.startDate) ||
                                    (dueDate != task.dueDate) ||
                                    (section != task.section)

                            if (hasPositionChange) {
                                // 1. Начинаем плавное сворачивание редактора (300 мс).
                                // Редактор в процессе сворачивания продолжает отображать НОВЫЙ заголовок и НОВУЮ дату.
                                onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                                // 2. Применяем новые свойства во ViewModel ровно через 300 мс (после полного сворачивания редактора)
                                coroutineScope.launch {
                                    delay(com.example.ui.theme.AnimationConstants.TASK_EXPANSION_DURATION_MS)
                                    onEvent(
                                        ThingsCategoryListEvent.SaveTask(
                                            taskWrapper = taskWrapper,
                                            title = title,
                                            notes = notes,
                                            section = section,
                                            isTonight = isTonight,
                                            startDate = startDate,
                                            dueDate = dueDate,
                                            tags = tags,
                                            projectId = projectId,
                                            priority = priority,
                                            checklist = checklist
                                        )
                                    )
                                }
                            } else {
                                // Если свойства позиции не менялись — обновляем всё мгновенно
                                onEvent(
                                    ThingsCategoryListEvent.SaveTask(
                                        taskWrapper = taskWrapper,
                                        title = title,
                                        notes = notes,
                                        section = section,
                                        isTonight = isTonight,
                                        startDate = startDate,
                                        dueDate = dueDate,
                                        tags = tags,
                                        projectId = projectId,
                                        priority = priority,
                                        checklist = checklist
                                    )
                                )
                                onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                            }
                        },
                        onDelete = {
                            onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                            coroutineScope.launch {
                                // 1. Сначала сворачиваем открытый редактор в обычную белую строку (300 мс)
                                delay(com.example.ui.theme.AnimationConstants.TASK_EXPANSION_DURATION_MS)
                                // 2. Включаем серое закрашивание серым кругом от чекбокса и серость текста (как при чекбоксе)
                                onDeletedTaskIdAdd(task.id)
                                // 3. Ждём 500 мс (стандартный таймер выполнения чекбокса)
                                delay(ThingsMotion.LONG.toLong())
                                // 4. Удаляем задачу из ViewModel -> animateItem растворяет серую карточку и подтягивает задачи
                                onEvent(ThingsCategoryListEvent.DeleteTask(taskWrapper))
                            }
                        },
                        onDone = {
                            onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                        },
                        onWhenDialogVisibilityChange = onWhenDialogVisibilityChange,
                        areas = areas,
                        onNavigateToProject = { project ->
                            onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                            onEvent(ThingsCategoryListEvent.ClickProject(project))
                        },
                        onNavigateToArea = { area ->
                            onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                            onEvent(ThingsCategoryListEvent.ClickArea(area))
                        }
                    )
                } else {
                    SwipeableTaskContainer(
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                // Смещение центра строки от центра экрана: из него диалог When вырастает
                                val root = coordinates.findRootCoordinates()
                                rowCenter.value = coordinates.boundsInRoot().center -
                                    Offset(root.size.width / 2f, root.size.height / 2f)
                            }
                            // В результатах поиска порядок задаёт выдача, перетаскивание отключено
                            .then(
                                if (screen == ActiveScreen.SEARCH) Modifier
                                else Modifier.taskDragAndDrop(
                                    state = dragDropState,
                                    taskWrapper = taskWrapper,
                                    screen = screen,
                                    upcomingDays = upcomingDays,
                                    projectHeadingIds = projectHeadingIds,
                                    localTasksList = localTasksList,
                                    filteredTasks = displayTasks,
                                    onLocalTasksListChange = onLocalTasksListChange,
                                    onTasksReordered = { items ->
                                        onEvent(ThingsCategoryListEvent.ReorderTasks(items))
                                    },
                                    selectedTaskIds = selectedTaskIds,
                                    onExitSelectionMode = onExitSelectionMode
                                )
                            ),
                        onSwipeLeft = { onEvent(ThingsCategoryListEvent.SwipeTaskLeft(taskWrapper)) },
                        onSwipeRight = {
                            onEvent(ThingsCategoryListEvent.SwipeTaskRight(taskWrapper, rowCenter.value))
                        },
                        enabled = inlineExpandedTaskId == null
                                && dragDropState.draggedItemKey == null
                                && !task.id.startsWith("cal_"),
                        // В режиме мультивыбора свайп вправо не вызывает диалог When
                        swipeRightEnabled = !isSelectionMode
                    ) {
                        TaskItemRow(
                            modifier = Modifier,
                            task = rowTask,
                            textPrimaryColor = textPrimaryColor,
                            textSecondaryColor = textSecondaryColor,
                            dividerColor = dividerColor,
                            onToggle = { onEvent(ThingsCategoryListEvent.ToggleTask(taskWrapper)) },
                            onClick = {
                                if (isSelectionMode) {
                                    onToggleSelect()
                                } else if (inlineExpandedTaskId != null) {
                                    editorOutsideTouch?.onCollapseRequested()
                                    view.hideSoftKeyboardNow()
                                    onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                                } else {
                                    onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(task.id))
                                }
                            },
                            projects = projects,
                            areas = areas,
                            showTodayIndicator = screen == ActiveScreen.TODAY && !task.isTonight,
                            isDragging = false,
                            dragOffsetY = 0f,
                            dragModifier = Modifier,
                            isFound = task.id == highlightedTaskId,
                            isDimmed = shouldDim,
                            isBeingDeleted = isBeingDeleted,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            isDragSelecting = isDragSelecting,
                            onToggleSelect = onToggleSelect,
                            screen = screen,
                            dateBadge = rowDateBadge
                        )
                    }
                }
            }

        // Project/Area indicator row (drawn under/outside the Card)
        if (showEditor) {
            if (currentProject != null || currentArea != null) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isExpanded,
                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                ) {
                    val progress = if (currentProject != null) projectProgressMap[currentProject.id] else null
                    val completedCount = progress?.completed ?: 0
                    val totalCount = progress?.total ?: 0

                    val pillContentColor = ThingsTheme.colors.textSecondary
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .layout { measurable, constraints ->
                                val extraPaddingPx = (10.dp * expansionProgress()).roundToPx()
                                val extendedConstraints = constraints.copy(
                                    minWidth = (constraints.minWidth + extraPaddingPx * 2).coerceAtMost(constraints.maxWidth + extraPaddingPx * 2),
                                    maxWidth = (constraints.maxWidth + extraPaddingPx * 2)
                                )
                                val placeable = measurable.measure(extendedConstraints)
                                layout(placeable.width - extraPaddingPx * 2, placeable.height) {
                                    placeable.place(-extraPaddingPx, 0)
                                }
                            }
                            .padding(top = 10.dp, bottom = 4.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(ThingsTheme.shapes.smallShape)
                                .clickable {
                                    // Save task automatically and navigate
                                    editorOutsideTouch?.onCollapseRequested()
                                    onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                                    if (currentProject != null) {
                                        onEvent(ThingsCategoryListEvent.ClickProject(currentProject))
                                    } else if (currentArea != null) {
                                        onEvent(ThingsCategoryListEvent.ClickArea(currentArea))
                                    }
                                }
                                .padding(start = 8.dp, top = 4.dp, end = 0.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentProject != null) {
                                ProjectProgressArc(
                                    completed = completedCount,
                                    total = totalCount,
                                    color = pillContentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = AppIcons.Area,
                                    contentDescription = null,
                                    tint = pillContentColor,
                                    modifier = Modifier.size(ThingsIconSize.XS)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentProject?.title ?: currentArea?.title ?: "",
                                style = ThingsTheme.type.editorChecklist.copy(
                                    color = pillContentColor,
                                    fontWeight = FontWeight.Normal
                                )
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = pillContentColor,
                                modifier = Modifier.size(ThingsIconSize.M)
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

/** Раскрытая задача не поднимается выше этой границы: высота тулбара и небольшой зазор под ним */
private val EXPANDED_TOP_LIMIT = 64.dp

/** Зазор между низом раскрытой задачи и плашкой действий внизу экрана */
private val EXPANDED_BOTTOM_GAP = 12.dp

/**
 * Геометрия карточки раскрываемой задачи. Её пишут раскладка карточки и редактора,
 * а читает только цикл подтяжки списка, поэтому это не snapshot-состояние: запись не вызывает рекомпозиций.
 */
/** Смещение центра строки от центра экрана — точка, из которой вырастает диалог When */
private class RowCenter {
    var value: Offset? = null
}

private class LiftHolder {
    var liftPx = 0f
    var liftAtCollapseStart = 0f
    // Компенсация отступов новой задачи, которую ещё предстоит плавно отработать
    var pendingComp = 0f
    var compDeferred = false
}

private class ExpandedCardGeometry {
    var top = 0f
    var rootHeight = 0
    var editorFullHeight = 0
    var measured = false
}
