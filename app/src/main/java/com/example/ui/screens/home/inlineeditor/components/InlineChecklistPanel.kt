package com.example.ui.screens.home.inlineeditor.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import com.example.ui.components.hideSoftKeyboardNow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.model.ChecklistItem
import com.example.ui.theme.ThingsBlue
import com.example.ui.theme.taskEditorChecklist
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

// Цвета, пропорции и тайминги сняты с видео Things 3 (пункты чек-листа в карточке задачи).
// Размеры считаются от размера шрифта пункта (у Roboto высота заглавной ≈ 0,71 размера шрифта).
private val ChecklistCircleColor = Color(0xFF31539D)
private val ChecklistCheckColor = Color(0xFF888888)
private val ChecklistHandleColor = Color(0xFFB5B5B5)
private val ChecklistDeleteColor = Color(0xFFEE004E)
private val ChecklistDividerColor = Color(0xFFEBEBEB)
private val ChecklistHighlightColor = Color(0xFFF0F1F3)
private val ChecklistTextColor = Color(0xFF000000)
private val ChecklistCompletedTextColor = Color(0xFF777778)
private const val CHECKLIST_MARK_TO_FONT = 0.80f
private const val CHECKLIST_HANDLE_WIDTH_TO_FONT = 0.71f
// Отступ строки и толщина разделителя тоже в долях шрифта — шаг строк и линии держат пропорцию к буквам
private const val CHECKLIST_ROW_PADDING_TO_FONT = 0.406f
private const val CHECKLIST_DIVIDER_TO_FONT = 0.09f
private const val CHECKLIST_DELETE_COLLAPSE_MS = 250
// Отметка пункта: строка сразу заливается серым, держится и гаснет
private const val CHECKLIST_HIGHLIGHT_HOLD_MS = 170L
private const val CHECKLIST_HIGHLIGHT_FADE_MS = 130
// Смахивание влево: кнопка появляется, когда палец ушёл на это расстояние (в размерах шрифта),
// и дальше едет за пальцем на эту долю его хода
private const val CHECKLIST_DELETE_ARM_TO_FONT = 3.3f
private const val CHECKLIST_DELETE_FOLLOW = 0.29f

@Composable
fun InlineChecklistPanel(
    itemId: String = "",
    checklist: List<ChecklistItem>,
    onChecklistChange: (List<ChecklistItem>) -> Unit,
    showChecklistHelper: Boolean,
    onShowChecklistHelperChange: (Boolean) -> Unit
) {
    val showPanel = showChecklistHelper || checklist.isNotEmpty()

    val textPrimaryColor = Color(0xFF1C1C1E)
    val textSecondaryColor = Color(0xFF747679)
    val bodyFontSize = MaterialTheme.typography.taskEditorChecklist.fontSize
    val density = LocalDensity.current
    val fontDp = with(density) { bodyFontSize.toDp() }
    val fontPx = with(density) { bodyFontSize.toPx() }
    val markSize = fontDp * CHECKLIST_MARK_TO_FONT
    // Подсветка строки выходит на это поле левее кружка и правее ручки ≡
    val highlightMargin = fontDp * 0.35f
    // Разделитель начинается чуть левее кружка, а кончается ровно по правому краю ручки ≡
    val dividerLead = fontDp * 0.064f
    val dividerLeadModifier = Modifier.layout { measurable, constraints ->
        val lead = dividerLead.roundToPx()
        val width = constraints.maxWidth + lead
        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
        layout(constraints.maxWidth, placeable.height) { placeable.place(-lead, 0) }
    }

    var newChecklistItemTitle by remember { mutableStateOf("") }
    var isNewItemFieldFocused by remember { mutableStateOf(false) }
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    // Пункт, у которого смахиванием влево открыта кнопка удаления, и держит ли его ещё палец
    var deleteArmedId by remember { mutableStateOf<String?>(null) }
    var deleteHeld by remember { mutableStateOf(false) }
    var swipedInGesture by remember { mutableStateOf(false) }
    // Сдвиг кнопки «+» за пальцем, пока строку держат, и сдвиг в момент отпускания (от него кнопка докатывается)
    val deleteFollowOffset = remember { mutableFloatStateOf(0f) }
    val deleteReleaseOffset = remember { mutableFloatStateOf(0f) }
    val armDistancePx = fontPx * CHECKLIST_DELETE_ARM_TO_FONT
    val disarmDistancePx = with(density) { ARM_DISTANCE.toPx() }

    // Удалённые пункты, пока их строка сворачивается: id -> (позиция в списке, пункт).
    // Из чек-листа пункт убирается сразу — сворачивание редактора посреди анимации его не вернёт.
    var exiting by remember { mutableStateOf(emptyMap<String, Pair<Int, ChecklistItem>>()) }
    val rows = remember(checklist, exiting) {
        if (exiting.isEmpty()) checklist
        else {
            val list = checklist.filter { it.id !in exiting }.toMutableList()
            exiting.values.sortedBy { it.first }.forEach { (index, item) ->
                list.add(index.coerceAtMost(list.size), item)
            }
            list
        }
    }
    val latestRows by rememberUpdatedState(rows)
    val latestChecklist by rememberUpdatedState(checklist)
    val latestOnChecklistChange by rememberUpdatedState(onChecklistChange)
    val deleteItem: (ChecklistItem) -> Unit = { item ->
        val index = latestChecklist.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            exiting = exiting + (item.id to (index to item))
            deleteArmedId = null
            latestOnChecklistChange(latestChecklist.filterNot { it.id == item.id })
        }
    }

    // Перетаскивание за ≡. Пока палец держит строку, порядок строк живёт в dragOrder, а в чек-лист
    // уходит после отпускания. dragRawOffset — смещение пальца от места поднятой строки,
    // dragShownOffset — то же, но строка не заходит выше первого и ниже последнего места.
    var dragOrder by remember { mutableStateOf<List<String>?>(null) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    val dragRawOffset = remember { mutableFloatStateOf(0f) }
    val dragShownOffset = remember { mutableFloatStateOf(0f) }
    val rowHeights = remember { HashMap<String, Int>() }
    // Смещение строки от её места: соседи поднятой строки доезжают на новое место из старого
    val rowOffsets = remember { HashMap<String, Animatable<Float, AnimationVector1D>>() }
    val shiftRow: (String, Float) -> Unit = { id, delta ->
        rowOffsets[id]?.let { anim ->
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                anim.snapTo(anim.value + delta)
                anim.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
            }
        }
    }
    val startReorder: (String) -> Unit = { id ->
        deleteArmedId = null
        dragOrder = latestRows.filter { it.id !in exiting }.map { it.id }
        draggingId = id
        dragRawOffset.floatValue = 0f
        dragShownOffset.floatValue = 0f
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
    val dragReorder: (Float) -> Unit = { dy ->
        val order = dragOrder
        val id = draggingId
        if (order != null && id != null) {
            var list: List<String> = order
            var index = list.indexOf(id)
            var offset = dragRawOffset.floatValue + dy
            val draggedHeight = (rowHeights[id] ?: 0).toFloat()
            // Середина строки прошла половину соседа — меняемся местами; место поднятой строки
            // сдвигается на высоту соседа, поэтому смещение от места уменьшается на ту же высоту
            while (index < list.lastIndex) {
                val next = list[index + 1]
                val h = rowHeights[next] ?: break
                if (offset <= h / 2f) break
                list = list.toMutableList().apply { add(index + 1, removeAt(index)) }
                offset -= h
                shiftRow(next, draggedHeight)
                index++
            }
            while (index > 0) {
                val prev = list[index - 1]
                val h = rowHeights[prev] ?: break
                if (offset >= -h / 2f) break
                list = list.toMutableList().apply { add(index - 1, removeAt(index)) }
                offset += h
                shiftRow(prev, -draggedHeight)
                index--
            }
            if (list !== order) {
                dragOrder = list
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            dragRawOffset.floatValue = offset
            var shown = offset
            if (index == 0) shown = shown.coerceAtLeast(0f)
            if (index == list.lastIndex) shown = shown.coerceAtMost(0f)
            dragShownOffset.floatValue = shown
        }
    }
    val endReorder: () -> Unit = {
        val order = dragOrder
        val id = draggingId
        if (order != null && id != null) {
            // Строка садится на место из того положения, где её видно
            rowOffsets[id]?.let { anim ->
                val from = dragShownOffset.floatValue
                scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    anim.snapTo(from)
                    anim.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                }
            }
            val current = latestChecklist
            val reordered = order.mapNotNull { oid -> current.firstOrNull { it.id == oid } } +
                current.filter { it.id !in order }
            if (reordered.map { it.id } != current.map { it.id }) latestOnChecklistChange(reordered)
        }
        draggingId = null
        dragOrder = null
    }
    val displayRows = dragOrder?.mapNotNull { id -> rows.firstOrNull { it.id == id } } ?: rows

    val startPadding = 24.dp

    AnimatedVisibility(
        visible = showPanel,
        // Подсветка строки выходит правее ручки ≡ на highlightMargin, а эта AnimatedVisibility обрезает
        // содержимое по своим границам (expandVertically) — расширяем её вправо на это поле
        modifier = Modifier.layout { measurable, constraints ->
            val extra = if (constraints.hasBoundedWidth) highlightMargin.roundToPx() else 0
            val placeable = measurable.measure(
                constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra)
            )
            layout(placeable.width - extra, placeable.height) { placeable.place(0, 0) }
        },
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = startPadding - highlightMargin, top = 8.dp)
                // Любое касание панели, кроме смахивания и нажатия на саму кнопку, закрывает кнопку удаления
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val armedAtDown = deleteArmedId
                        swipedInGesture = false
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                        } while (event.changes.any { it.pressed })
                        if (!swipedInGesture && armedAtDown != null && deleteArmedId == armedAtDown) {
                            deleteArmedId = null
                        }
                    }
                }
        ) {
            if (displayRows.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = highlightMargin).then(dividerLeadModifier),
                    color = ChecklistDividerColor,
                    thickness = fontDp * CHECKLIST_DIVIDER_TO_FONT
                )
            }
            displayRows.forEach { item ->
                key(item.id) {
                    val isExiting = item.id in exiting
                    // Без условного вызова: условная группа перед AnimatedVisibility пересоздала бы его
                    // уже скрытым — строка исчезала бы в одном кадре, без сворачивания
                    LaunchedEffect(isExiting) {
                        if (isExiting) {
                            delay(CHECKLIST_DELETE_COLLAPSE_MS + 50L)
                            exiting = exiting - item.id
                        }
                    }
                    val placementOffset = remember { Animatable(0f) }
                    DisposableEffect(item.id) {
                        rowOffsets[item.id] = placementOffset
                        onDispose { rowOffsets.remove(item.id) }
                    }
                    val highlight = remember { Animatable(0f) }
                    val isDragged = draggingId == item.id
                    val lift by animateFloatAsState(
                        targetValue = if (isDragged) 1f else 0f,
                        animationSpec = tween(150),
                        label = "checklist_row_lift"
                    )
                    AnimatedVisibility(
                        visible = !isExiting,
                        modifier = Modifier
                            .zIndex(if (isDragged || lift > 0f) 1f else 0f)
                            .onSizeChanged { rowHeights[item.id] = it.height }
                            .graphicsLayer {
                                translationY = if (draggingId == item.id) dragShownOffset.floatValue
                                else placementOffset.value
                                shadowElevation = 6.dp.toPx() * lift
                                shape = RoundedCornerShape(4.dp)
                            }
                            .background(Color.White)
                            .drawBehind {
                                val alpha = highlight.value
                                if (alpha > 0f) drawRect(ChecklistHighlightColor.copy(alpha = alpha))
                            },
                        enter = EnterTransition.None,
                        exit = shrinkVertically(tween(CHECKLIST_DELETE_COLLAPSE_MS)) +
                            fadeOut(tween(CHECKLIST_DELETE_COLLAPSE_MS))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = highlightMargin)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .swipeToRevealDelete(
                                        key = item.id,
                                        enabled = { draggingId == null },
                                        onSwipeStart = { swipedInGesture = true },
                                        onSwipe = { dx ->
                                            when {
                                                dx <= -armDistancePx -> {
                                                    if (deleteArmedId != item.id) {
                                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                        deleteArmedId = item.id
                                                    }
                                                    deleteHeld = true
                                                    deleteFollowOffset.floatValue =
                                                        CHECKLIST_DELETE_FOLLOW * (dx + armDistancePx)
                                                }
                                                // Палец вернулся до отпускания — кнопка уходит вслед за ним
                                                deleteHeld && deleteArmedId == item.id -> {
                                                    deleteArmedId = null
                                                    deleteHeld = false
                                                }
                                                dx >= disarmDistancePx && deleteArmedId == item.id ->
                                                    deleteArmedId = null
                                            }
                                        },
                                        onRelease = {
                                            if (deleteHeld) deleteReleaseOffset.floatValue = deleteFollowOffset.floatValue
                                            deleteHeld = false
                                        }
                                    )
                                    .padding(vertical = fontDp * CHECKLIST_ROW_PADDING_TO_FONT),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ChecklistCheckMark(
                                    checked = item.isCompleted,
                                    markSize = markSize,
                                    onToggle = {
                                        scope.launch {
                                            highlight.snapTo(1f)
                                            delay(CHECKLIST_HIGHLIGHT_HOLD_MS)
                                            highlight.animateTo(0f, tween(CHECKLIST_HIGHLIGHT_FADE_MS, easing = LinearEasing))
                                        }
                                        onChecklistChange(checklist.map {
                                            if (it.id == item.id) it.copy(isCompleted = !it.isCompleted) else it
                                        })
                                    }
                                )

                                // Editable checklist item title inline
                                BasicTextField(
                                    value = item.title,
                                    onValueChange = { updatedTitle ->
                                        onChecklistChange(checklist.map {
                                            if (it.id == item.id) it.copy(title = updatedTitle) else it
                                        })
                                    },
                                    textStyle = TextStyle(
                                        fontSize = bodyFontSize,
                                        color = if (item.isCompleted) ChecklistCompletedTextColor else ChecklistTextColor
                                    ),
                                    cursorBrush = SolidColor(ThingsBlue),
                                    modifier = Modifier.weight(1f)
                                )

                                ChecklistRowAction(
                                    showDelete = deleteArmedId == item.id || isExiting,
                                    held = deleteHeld && deleteArmedId == item.id,
                                    followOffset = { deleteFollowOffset.floatValue },
                                    releaseOffset = { deleteReleaseOffset.floatValue },
                                    fontDp = fontDp,
                                    onDelete = { deleteItem(item) },
                                    handleModifier = Modifier.reorderHandle(
                                        key = item.id,
                                        onStart = { startReorder(item.id) },
                                        onDrag = dragReorder,
                                        onEnd = endReorder
                                    )
                                )
                            }
                            HorizontalDivider(
                                modifier = dividerLeadModifier,
                                color = ChecklistDividerColor,
                                thickness = fontDp * CHECKLIST_DIVIDER_TO_FONT
                            )
                        }
                    }
                }
            }

            // Add inline checklist item
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = highlightMargin)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Checklist",
                    tint = ThingsBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = newChecklistItemTitle,
                    onValueChange = { newChecklistItemTitle = it },
                    textStyle = TextStyle(fontSize = bodyFontSize, color = textPrimaryColor),
                    cursorBrush = SolidColor(ThingsBlue),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newChecklistItemTitle.isNotBlank()) {
                            val updated = checklist + ChecklistItem(itemId = itemId, title = newChecklistItemTitle.trim())
                            onChecklistChange(updated)
                            newChecklistItemTitle = ""
                        }
                    }),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { isNewItemFieldFocused = it.isFocused },
                    decorationBox = { innerTextField ->
                        if (newChecklistItemTitle.isEmpty()) {
                            Text(
                                "Add Checklist Item...",
                                style = TextStyle(fontSize = bodyFontSize, color = textSecondaryColor.copy(alpha = 0.4f))
                            )
                        }
                        innerTextField()
                    }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Checklist Helper",
                    tint = textSecondaryColor.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(14.dp)
                        .clickable {
                            // Пустая панель уезжает вместе с полем анимацией (~400 мс) — если клавиатура
                            // открыта для этого поля, прячем её сразу (см. hideSoftKeyboardNow)
                            if (checklist.isEmpty() && isNewItemFieldFocused) view.hideSoftKeyboardNow()
                            onShowChecklistHelperChange(false)
                        }
                )
            }
        }
    }
}

/**
 * Отметка пункта чек-листа, как в Things 3: невыполненный — синий кружок, выполненный — серая
 * галочка без рамки. Меняется сразу, без анимации (в Things 3 анимирована только подсветка строки).
 * Область нажатия шире кружка и включает отступ до текста.
 */
@Composable
private fun ChecklistCheckMark(checked: Boolean, markSize: Dp, onToggle: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val view = LocalView.current

    Box(
        modifier = Modifier
            // Ширина включает зазор до текста; обе величины в долях кружка, чтобы следовать за шрифтом
            .size(width = markSize * 1.6f, height = markSize * 1.52f)
            .clickable(interactionSource = interactionSource, indication = null) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onToggle()
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.size(markSize)) {
            val d = size.minDimension
            if (!checked) {
                // Толщина кольца — 13 % диаметра
                val ring = d * 0.13f
                drawCircle(
                    color = ChecklistCircleColor,
                    radius = (d - ring) / 2f,
                    style = Stroke(width = ring)
                )
            } else {
                val check = Path().apply {
                    moveTo(d * 0.1285f, d * 0.446f)
                    lineTo(d * 0.393f, d * 0.7635f)
                    lineTo(d * 0.871f, d * 0.2365f)
                }
                drawPath(
                    path = check,
                    color = ChecklistCheckColor,
                    style = Stroke(width = d * 0.159f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

/**
 * Правый край строки: ручка ≡ (за неё строку перетаскивают, [handleModifier]) или красная кнопка
 * удаления. Ручка прижата к правому краю строки — там же кончается разделитель; кнопка стоит по
 * центру ручки. Пока палец держит смахнутую строку, на кнопке «+» и она едет за пальцем
 * ([followOffset]); после отпускания докатывается от [releaseOffset] на место ручки с небольшим
 * перелётом и поворачивается в «×».
 */
@Composable
private fun ChecklistRowAction(
    showDelete: Boolean,
    held: Boolean,
    followOffset: () -> Float,
    releaseOffset: () -> Float,
    fontDp: Dp,
    onDelete: () -> Unit,
    handleModifier: Modifier
) {
    val reveal by animateFloatAsState(
        targetValue = if (showDelete) 1f else 0f,
        animationSpec = if (showDelete) snap() else tween(160),
        label = "checklist_delete_reveal"
    )
    val settle by animateFloatAsState(
        targetValue = if (showDelete && !held) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "checklist_delete_settle"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(fontDp * 1.6f)
            .then(
                if (showDelete) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onDelete
                ) else handleModifier
            )
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val font = fontDp.toPx()
            val halfWidth = font * CHECKLIST_HANDLE_WIDTH_TO_FONT / 2f
            val center = Offset(size.width - halfWidth, size.height / 2f)
            if (reveal < 1f) {
                val pitch = font * 0.167f
                val stroke = font * 0.058f
                for (i in -1..1) {
                    val y = center.y + i * pitch
                    drawLine(
                        color = ChecklistHandleColor.copy(alpha = 1f - reveal),
                        start = Offset(center.x - halfWidth, y),
                        end = Offset(center.x + halfWidth, y),
                        strokeWidth = stroke
                    )
                }
            }
            if (reveal > 0f) {
                val radius = font * 0.51f
                val shift = (if (held) followOffset() else releaseOffset()) * (1f - settle)
                translate(left = shift) {
                    rotate(degrees = 45f * settle, pivot = center) {
                        drawCircle(color = ChecklistDeleteColor.copy(alpha = reveal), radius = radius, center = center)
                        val arm = radius * 0.5f
                        val stroke = radius * 0.2f
                        val white = Color.White.copy(alpha = reveal)
                        drawLine(white, Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), stroke, StrokeCap.Round)
                        drawLine(white, Offset(center.x, center.y - arm), Offset(center.x, center.y + arm), stroke, StrokeCap.Round)
                    }
                }
            }
        }
    }
}

/**
 * Горизонтальное смахивание строки чек-листа. Пока палец ведёт строку, [onSwipe] получает его ход
 * по горизонтали (влево — отрицательный): что показывать, решает панель. Жест перехватывается в
 * Initial-проходе, как только движение по горизонтали превысило порог касания: поле ввода не
 * получает его как нажатие, а список — как прокрутку. Вертикальное движение жест отдаёт списку.
 * Пока [enabled] ложно (строку тащат за ≡), смахивание не начинается.
 */
private val ARM_DISTANCE = 24.dp

private fun Modifier.swipeToRevealDelete(
    key: Any,
    enabled: () -> Boolean,
    onSwipeStart: () -> Unit,
    onSwipe: (Float) -> Unit,
    onRelease: () -> Unit
): Modifier = pointerInput(key) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val slop = viewConfiguration.touchSlop
        var total = Offset.Zero
        var swiping = false
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            total += change.positionChange()
            if (!swiping) {
                if (!enabled()) return@awaitEachGesture
                if (abs(total.y) > slop && abs(total.y) >= abs(total.x)) return@awaitEachGesture
                if (abs(total.x) > slop && abs(total.x) > abs(total.y)) {
                    swiping = true
                    onSwipeStart()
                }
            }
            if (swiping) {
                change.consume()
                onSwipe(total.x)
            }
        }
        if (swiping) onRelease()
    }
}

/**
 * Ручка ≡: строка поднимается при касании и идёт за пальцем по вертикали. Все события жеста
 * поглощаются — список под редактором не прокручивается, смахивание строки не начинается.
 */
private fun Modifier.reorderHandle(
    key: Any,
    onStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onEnd: () -> Unit
): Modifier = pointerInput(key) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        down.consume()
        onStart()
        try {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    change.consume()
                    break
                }
                val dy = change.positionChange().y
                change.consume()
                if (dy != 0f) onDrag(dy)
            }
        } finally {
            onEnd()
        }
    }
}
