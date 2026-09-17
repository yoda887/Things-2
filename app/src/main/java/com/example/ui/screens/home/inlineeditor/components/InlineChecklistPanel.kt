package com.example.ui.screens.home.inlineeditor.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
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
// Строка, которую правят (текст в фокусе), и строка, которую тащат за ≡ (видео 5-checklists-iphone)
private val ChecklistFocusColor = Color(0xFFF7F7F7)
private val ChecklistDragColor = Color(0xFFD7E6FD)
private const val CHECKLIST_MARK_TO_FONT = 0.80f
private const val CHECKLIST_HANDLE_WIDTH_TO_FONT = 0.71f
// Отступ строки и толщина разделителя тоже в долях шрифта — шаг строк и линии держат пропорцию к буквам
private const val CHECKLIST_ROW_PADDING_TO_FONT = 0.406f
private const val CHECKLIST_DIVIDER_TO_FONT = 0.09f
private const val CHECKLIST_DELETE_COLLAPSE_MS = 250
// Отметка пункта: строка сразу заливается серым, держится и гаснет; сама строка на миг сжимается к центру
private const val CHECKLIST_HIGHLIGHT_HOLD_MS = 170L
private const val CHECKLIST_HIGHLIGHT_FADE_MS = 130
private const val CHECKLIST_PRESS_SCALE = 0.99f
private const val CHECKLIST_PRESS_DOWN_MS = 50
private const val CHECKLIST_PRESS_UP_MS = 250
// Кнопка удаления: радиус (в размерах шрифта); при смахивании она выкатывается из-под правого края
// строки и едет за пальцем на эту долю его хода, поворачиваясь, как катящееся колесо (доля от
// поворота без проскальзывания — так в эталоне)
private const val CHECKLIST_DELETE_RADIUS_TO_FONT = 0.51f
private const val CHECKLIST_DELETE_FOLLOW = 0.29f
private const val CHECKLIST_DELETE_ROLL = 0.92f
// Правый край, из-под которого выкатывается кнопка, — дальше ручки ≡ на это расстояние (в размерах шрифта),
// как край карточки в эталоне
private const val CHECKLIST_CLIP_END_TO_FONT = 0.84f

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
    // Справа строка шире: там, за ручкой ≡, прячется кнопка удаления до того, как выкатиться
    val clipEnd = fontDp * CHECKLIST_CLIP_END_TO_FONT
    val clipExtraPx = with(density) { (clipEnd - highlightMargin).toPx() }
    // Подсветка, фон и тень поднятой строки — без этого запаса справа
    val rowShape = remember(clipExtraPx, density) {
        val corner = with(density) { 4.dp.toPx() }
        GenericShape { size, _ ->
            addRoundRect(RoundRect(0f, 0f, size.width - clipExtraPx, size.height, CornerRadius(corner)))
        }
    }
    // Разделитель начинается чуть левее кружка, а кончается ровно по правому краю ручки ≡
    val dividerLead = fontDp * 0.064f
    val dividerLeadModifier = Modifier.layout { measurable, constraints ->
        val lead = dividerLead.roundToPx()
        val width = constraints.maxWidth + lead
        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
        layout(constraints.maxWidth, placeable.height) { placeable.place(-lead, 0) }
    }

    val focusManager = LocalFocusManager.current
    // Правка пунктов, как в Things 3: строка с текстом в фокусе подсвечена серым; pendingFocus — пункт,
    // в который перевести фокус после добавления или удаления соседнего, и где поставить курсор
    var focusedId by remember { mutableStateOf<String?>(null) }
    var pendingFocus by remember { mutableStateOf<Pair<String, Int>?>(null) }
    // Клавиатура закрылась — курсор с пункта снимает общий ClearFocusOnImeHidden в корне приложения,
    // а дальше срабатывает onFocusChanged: возвращаются подсветка и разделители, пустой пункт удаляется
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    // Кнопка удаления. deleteRowId — строка, у которой она видна; deleteOffset — смещение её центра от
    // центра ручки ≡ (px, вправо — плюс; deleteHiddenOffset — кнопка целиком за правым краем строки).
    // deleteArmed — кнопка стоит на месте ручки и нажимается; deleteSwiping — строку ведёт палец.
    val deleteRadiusPx = fontPx * CHECKLIST_DELETE_RADIUS_TO_FONT
    val deleteHiddenOffset = 2f * deleteRadiusPx
    // Укатываясь, кнопка уходит целиком за край, из-под которого выкатывалась, и только потом пропадает
    val deleteGoneOffset = fontPx * CHECKLIST_HANDLE_WIDTH_TO_FONT / 2f +
        with(density) { clipEnd.toPx() } + deleteRadiusPx
    var deleteRowId by remember { mutableStateOf<String?>(null) }
    var deleteArmed by remember { mutableStateOf(false) }
    var deleteSwiping by remember { mutableStateOf(false) }
    val deleteOffset = remember { mutableFloatStateOf(0f) }
    val deleteSwipeBase = remember { mutableFloatStateOf(0f) }
    // Жест начался на строке без кнопки: ждём, поведёт ли палец строку влево
    val deleteGesturePending = remember { mutableStateOf(false) }
    // Кнопка выкатывается из-за края карточки: расстояние от центра ручки ≡ до правого края окна
    // (карточка редактора — во всю ширину; где карточка уже, кнопку прячет её край). Меряется при раскладке.
    val deleteEdgeDistance = remember { mutableFloatStateOf(0f) }
    // Жест вывел кнопку из-за края (а не покатил уже стоящую на месте ручки)
    val deleteFromEdge = remember { mutableStateOf(false) }
    // От края до места ручки кнопка доходит за этот ход пальца, дальше едет на CHECKLIST_DELETE_FOLLOW хода
    val deleteArmTravel = deleteHiddenOffset / CHECKLIST_DELETE_FOLLOW
    fun deleteStartOffset(): Float =
        (if (deleteEdgeDistance.floatValue > 0f) deleteEdgeDistance.floatValue else deleteGoneOffset - deleteRadiusPx) +
            deleteRadiusPx
    val deleteJob = remember { mutableStateOf<Job?>(null) }
    var swipedInGesture by remember { mutableStateOf(false) }
    // Кнопка докатывается до цели той же пружиной, что в эталоне (с небольшим перелётом)
    fun rollDeleteTo(target: Float, onEnd: () -> Unit = {}) {
        deleteJob.value?.cancel()
        val from = deleteOffset.floatValue
        deleteJob.value = scope.launch {
            animate(from, target, animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)) { v, _ ->
                deleteOffset.floatValue = v
            }
            onEnd()
        }
    }
    val rollOutDelete: () -> Unit = {
        if (deleteRowId != null && !deleteSwiping) {
            deleteArmed = false
            rollDeleteTo(deleteStartOffset()) { if (!deleteArmed && !deleteSwiping) deleteRowId = null }
        }
    }

    // Удалённые пункты, пока их строка сворачивается: id -> (позиция в списке, пункт).
    // Из чек-листа пункт убирается сразу — сворачивание редактора посреди анимации его не вернёт.
    var exiting by remember { mutableStateOf(emptyMap<String, Pair<Int, ChecklistItem>>()) }
    // Из исчезающих — удалённые кнопкой удаления: только у них при сворачивании остаётся видна кнопка
    var exitingViaDelete by remember { mutableStateOf(emptySet<String>()) }
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
            exitingViaDelete = exitingViaDelete + item.id
            deleteArmed = false
            latestOnChecklistChange(latestChecklist.filterNot { it.id == item.id })
        }
    }
    // Пустой пункт (Backspace, Return в пустом пункте, потеря фокуса) сворачивается так же, как удалённый
    // смахиванием. Раньше он пропадал в одном кадре: карточка резко уменьшалась на высоту строки, а затем,
    // если пункт был последним, подрастала вместе с заметкой, возвращающей себе высоту без чек-листа.
    // При необходимости курсор уходит в конец предыдущего пункта.
    val removeEmptyItem: (String, Boolean) -> Unit = { id, focusPrevious ->
        val current = latestChecklist
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            if (focusPrevious && index > 0) current[index - 1].let { pendingFocus = it.id to it.title.length }
            exiting = exiting + (id to (index to current[index]))
            latestOnChecklistChange(current.filterNot { it.id == id })
        }
    }
    // Кнопка «Checklists» в тулбаре: как в Things 3 — сразу первый пустой пункт с курсором
    LaunchedEffect(showChecklistHelper) {
        if (showChecklistHelper) {
            if (latestChecklist.isEmpty()) {
                val first = ChecklistItem(itemId = itemId, title = "")
                pendingFocus = first.id to 0
                latestOnChecklistChange(listOf(first))
            }
            onShowChecklistHelperChange(false)
        }
    }
    // Серая (правка) и голубая (перетаскивание) подсветка — по строке: чуть левее кружка и чуть правее ручки ≡
    val cornerPx = with(density) { 4.dp.toPx() }

    // Перетаскивание за ≡. Пока палец держит строку, порядок строк живёт в dragOrder, а в чек-лист
    // уходит после отпускания. dragRawOffset — смещение пальца от места поднятой строки,
    // dragShownOffset — то же, но строка не заходит выше первого и ниже последнего места.
    var dragOrder by remember { mutableStateOf<List<String>?>(null) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    // Строка, которую только что отпустили: пока гаснет голубой, разделители у неё ещё скрыты
    var settlingId by remember { mutableStateOf<String?>(null) }
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
        deleteJob.value?.cancel()
        deleteRowId = null
        deleteArmed = false
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
            settlingId = id
            scope.launch {
                delay(160)
                if (settlingId == id) settlingId = null
            }
        }
        draggingId = null
        dragOrder = null
    }
    // У строки, которую правят или тащат за ≡, не видно разделителей ни сверху, ни снизу (как в Things 3);
    // место под них остаётся — строки не сдвигаются
    fun linesHiddenAround(id: String?): Boolean =
        id != null && (id == focusedId || id == draggingId || id == settlingId)
    val displayRows = dragOrder?.mapNotNull { id -> rows.firstOrNull { it.id == id } } ?: rows

    val startPadding = 24.dp

    AnimatedVisibility(
        visible = showPanel,
        // Строки выходят правее ручки ≡ на clipEnd (там прячется кнопка удаления), а эта AnimatedVisibility
        // обрезает содержимое по своим границам (expandVertically) — расширяем её вправо на это поле
        modifier = Modifier.layout { measurable, constraints ->
            val extra = if (constraints.hasBoundedWidth) clipEnd.roundToPx() else 0
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
                // Любое касание панели, кроме смахивания и нажатия на саму кнопку, укатывает кнопку удаления
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val armedAtDown = if (deleteArmed) deleteRowId else null
                        swipedInGesture = false
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                        } while (event.changes.any { it.pressed })
                        if (!swipedInGesture && armedAtDown != null && deleteArmed && deleteRowId == armedAtDown) {
                            rollOutDelete()
                        }
                    }
                }
        ) {
            if (displayRows.isNotEmpty()) {
                // Над строкой, которую правят, разделителя не видно (как в Things 3); место под него остаётся
                HorizontalDivider(
                    modifier = Modifier.padding(start = highlightMargin, end = clipEnd).then(dividerLeadModifier),
                    color = if (linesHiddenAround(displayRows.first().id)) Color.Transparent else ChecklistDividerColor,
                    thickness = fontDp * CHECKLIST_DIVIDER_TO_FONT
                )
            }
            displayRows.forEachIndexed { rowIndex, item ->
                key(item.id) {
                    val isExiting = item.id in exiting
                    val isExitingViaDelete = item.id in exitingViaDelete
                    val nextId = displayRows.getOrNull(rowIndex + 1)?.id
                    // Без условного вызова: условная группа перед AnimatedVisibility пересоздала бы его
                    // уже скрытым — строка исчезала бы в одном кадре, без сворачивания
                    LaunchedEffect(isExiting) {
                        if (isExiting) {
                            delay(CHECKLIST_DELETE_COLLAPSE_MS + 50L)
                            exiting = exiting - item.id
                            exitingViaDelete = exitingViaDelete - item.id
                        }
                    }
                    val placementOffset = remember { Animatable(0f) }
                    DisposableEffect(item.id) {
                        rowOffsets[item.id] = placementOffset
                        onDispose { rowOffsets.remove(item.id) }
                    }
                    val highlight = remember { Animatable(0f) }
                    val press = remember { Animatable(1f) }
                    val isDragged = draggingId == item.id
                    val lift by animateFloatAsState(
                        targetValue = if (isDragged) 1f else 0f,
                        animationSpec = tween(130),
                        label = "checklist_row_lift"
                    )
                    // Поле пункта: TextFieldValue — чтобы ставить курсор при переводе фокуса
                    val focusRequester = remember { FocusRequester() }
                    var fieldValue by remember { mutableStateOf(TextFieldValue(item.title, TextRange(item.title.length))) }
                    var wasFocused by remember { mutableStateOf(false) }
                    LaunchedEffect(item.title) {
                        if (fieldValue.text != item.title) fieldValue = TextFieldValue(item.title, TextRange(item.title.length))
                    }
                    val focusTarget = pendingFocus
                    LaunchedEffect(focusTarget) {
                        if (focusTarget != null && focusTarget.first == item.id) {
                            fieldValue = fieldValue.copy(selection = TextRange(focusTarget.second.coerceIn(0, fieldValue.text.length)))
                            focusRequester.requestFocus()
                            pendingFocus = null
                        }
                    }
                    AnimatedVisibility(
                        visible = !isExiting,
                        modifier = Modifier
                            .zIndex(if (isDragged || lift > 0f) 1f else 0f)
                            .onSizeChanged { rowHeights[item.id] = it.height }
                            .graphicsLayer {
                                translationY = if (draggingId == item.id) dragShownOffset.floatValue
                                else placementOffset.value
                                shadowElevation = 6.dp.toPx() * lift
                                shape = rowShape
                                // Сжатие при отметке — к середине строки (без запаса справа)
                                scaleX = press.value
                                scaleY = press.value
                                transformOrigin = TransformOrigin(
                                    pivotFractionX = if (size.width > 0f) (size.width - clipExtraPx) / 2f / size.width else 0.5f,
                                    pivotFractionY = 0.5f
                                )
                            }
                            .drawBehind {
                                drawRoundRect(Color.White, size = Size(size.width - clipExtraPx, size.height), cornerRadius = CornerRadius(cornerPx))
                                val band = Size(size.width - clipExtraPx, size.height)
                                if (focusedId == item.id) drawRect(ChecklistFocusColor, size = band)
                                if (lift > 0f) drawRoundRect(ChecklistDragColor.copy(alpha = lift), size = band, cornerRadius = CornerRadius(cornerPx))
                                val alpha = highlight.value
                                if (alpha > 0f) {
                                    drawRect(
                                        ChecklistHighlightColor.copy(alpha = alpha),
                                        size = Size(size.width - clipExtraPx, size.height)
                                    )
                                }
                            },
                        enter = EnterTransition.None,
                        exit = shrinkVertically(tween(CHECKLIST_DELETE_COLLAPSE_MS)) +
                            fadeOut(tween(CHECKLIST_DELETE_COLLAPSE_MS))
                    ) {
                        Column(modifier = Modifier.padding(start = highlightMargin, end = clipEnd)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .swipeToRevealDelete(
                                        key = item.id,
                                        enabled = { draggingId == null },
                                        onSwipeStart = {
                                            swipedInGesture = true
                                            if (deleteRowId == item.id && deleteArmed) {
                                                // Кнопка стоит на месте ручки — катится от него за пальцем в любую сторону
                                                deleteJob.value?.cancel()
                                                deleteSwipeBase.floatValue = deleteOffset.floatValue
                                                deleteArmed = false
                                                deleteSwiping = true
                                                deleteFromEdge.value = false
                                                deleteGesturePending.value = false
                                            } else {
                                                // Кнопки у строки нет — она появится, только если палец поведёт строку влево
                                                deleteGesturePending.value = true
                                            }
                                        },
                                        onSwipe = { dx ->
                                            if (deleteGesturePending.value && dx < 0f) {
                                                // Всегда выкатывается из-за края карточки, а не с того места, где её застал жест
                                                deleteGesturePending.value = false
                                                deleteJob.value?.cancel()
                                                deleteRowId = item.id
                                                deleteArmed = false
                                                deleteFromEdge.value = true
                                                deleteOffset.floatValue = deleteStartOffset()
                                                deleteSwiping = true
                                            }
                                            if (deleteSwiping && deleteRowId == item.id) {
                                                val offset = if (deleteFromEdge.value) {
                                                    // От края до места ручки — за deleteArmTravel хода пальца, дальше — на долю хода
                                                    val travel = -dx
                                                    if (travel <= deleteArmTravel) deleteStartOffset() * (1f - travel / deleteArmTravel)
                                                    else -CHECKLIST_DELETE_FOLLOW * (travel - deleteArmTravel)
                                                } else {
                                                    deleteSwipeBase.floatValue + CHECKLIST_DELETE_FOLLOW * dx
                                                }
                                                if (offset <= 0f && deleteOffset.floatValue > 0f) {
                                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                }
                                                deleteOffset.floatValue = offset
                                            }
                                        },
                                        onRelease = {
                                            deleteGesturePending.value = false
                                            if (deleteSwiping && deleteRowId == item.id) {
                                                deleteSwiping = false
                                                if (deleteOffset.floatValue <= 0f) {
                                                    // Кнопка прошла место ручки — докатывается на него и встаёт «×»
                                                    deleteArmed = true
                                                    rollDeleteTo(0f)
                                                } else {
                                                    deleteArmed = false
                                                    rollDeleteTo(deleteStartOffset()) {
                                                        if (!deleteArmed && !deleteSwiping) deleteRowId = null
                                                    }
                                                }
                                            }
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
                                        scope.launch {
                                            press.animateTo(CHECKLIST_PRESS_SCALE, tween(CHECKLIST_PRESS_DOWN_MS))
                                            press.animateTo(1f, tween(CHECKLIST_PRESS_UP_MS))
                                        }
                                        onChecklistChange(checklist.map {
                                            if (it.id == item.id) it.copy(isCompleted = !it.isCompleted) else it
                                        })
                                    }
                                )

                                // Текст пункта правится прямо в строке. Return (перевод строки) делит пункт:
                                // текст до курсора остаётся, после — уходит в новый пункт ниже, и курсор
                                // переходит туда (как в Things 3). Return в пустом пункте убирает его и
                                // заканчивает правку; Backspace в пустом — убирает и возвращает курсор в
                                // конец предыдущего; пустой пункт, из которого ушёл фокус, удаляется.
                                BasicTextField(
                                    value = fieldValue,
                                    onValueChange = { value ->
                                        val newline = value.text.indexOf('\n')
                                        if (newline < 0) {
                                            fieldValue = value
                                            if (value.text != item.title) {
                                                latestOnChecklistChange(latestChecklist.map {
                                                    if (it.id == item.id) it.copy(title = value.text) else it
                                                })
                                            }
                                        } else {
                                            val before = value.text.substring(0, newline)
                                            val after = value.text.substring(newline + 1).replace("\n", "")
                                            val current = latestChecklist
                                            val index = current.indexOfFirst { it.id == item.id }
                                            when {
                                                index < 0 -> Unit
                                                before.isBlank() && after.isBlank() -> {
                                                    removeEmptyItem(item.id, false)
                                                    focusManager.clearFocus()
                                                    view.hideSoftKeyboardNow()
                                                }
                                                before.isBlank() -> {
                                                    // Return в начале пункта: над ним появляется пустой, курсор остаётся в этом
                                                    fieldValue = TextFieldValue(after, TextRange(0))
                                                    val list = current.map { if (it.id == item.id) it.copy(title = after) else it }
                                                        .toMutableList().apply { add(index, ChecklistItem(itemId = itemId, title = "")) }
                                                    latestOnChecklistChange(list)
                                                }
                                                else -> {
                                                    fieldValue = TextFieldValue(before, TextRange(before.length))
                                                    val next = ChecklistItem(itemId = itemId, title = after)
                                                    val list = current.map { if (it.id == item.id) it.copy(title = before) else it }
                                                        .toMutableList().apply { add(index + 1, next) }
                                                    pendingFocus = next.id to 0
                                                    latestOnChecklistChange(list)
                                                }
                                            }
                                        }
                                    },
                                    textStyle = TextStyle(
                                        fontSize = bodyFontSize,
                                        color = if (item.isCompleted) ChecklistCompletedTextColor else ChecklistTextColor
                                    ),
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                    cursorBrush = SolidColor(ThingsBlue),
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(focusRequester)
                                        .onFocusChanged { state ->
                                            if (state.isFocused) {
                                                wasFocused = true
                                                focusedId = item.id
                                            } else if (wasFocused) {
                                                wasFocused = false
                                                if (focusedId == item.id) focusedId = null
                                                if (latestChecklist.firstOrNull { it.id == item.id }?.title?.isBlank() == true) {
                                                    removeEmptyItem(item.id, false)
                                                }
                                            }
                                        }
                                        .onPreviewKeyEvent { event ->
                                            if (event.type == KeyEventType.KeyDown && event.key == Key.Backspace && fieldValue.text.isEmpty()) {
                                                removeEmptyItem(item.id, true)
                                                true
                                            } else false
                                        }
                                )

                                ChecklistRowAction(
                                    showDelete = (deleteRowId == item.id && !isExiting) || isExitingViaDelete,
                                    deleteClickable = deleteArmed && deleteRowId == item.id && !isExiting,
                                    deleteOffset = { if (isExitingViaDelete) 0f else deleteOffset.floatValue },
                                    fontDp = fontDp,
                                    onEdgeDistance = { deleteEdgeDistance.floatValue = it },
                                    onDelete = { deleteItem(item) },
                                    handleModifier = Modifier.reorderHandle(
                                        key = item.id,
                                        onStart = { startReorder(item.id) },
                                        onDrag = dragReorder,
                                        onEnd = endReorder
                                    )
                                )
                            }
                            // Разделитель под строкой: не виден, если правят или тащат эту строку или следующую
                            HorizontalDivider(
                                modifier = dividerLeadModifier,
                                color = if (linesHiddenAround(item.id) || linesHiddenAround(nextId)) Color.Transparent
                                else ChecklistDividerColor,
                                thickness = fontDp * CHECKLIST_DIVIDER_TO_FONT
                            )
                        }
                    }
                }
            }

            // Отдельной строки «добавить пункт» нет, как в Things 3: новый пункт — Return в пункте или кнопка
            // «Checklists» в тулбаре
        }
    }
}

/**
 * Отметка пункта чек-листа, как в Things 3: невыполненный — синий кружок, выполненный — серая
 * галочка без рамки. Меняется сразу, без анимации (в Things 3 анимированы подсветка и сжатие строки).
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
 * Правый край строки: ручка ≡ (за неё строку перетаскивают, [handleModifier]) и красная кнопка
 * удаления. Ручка прижата к правому краю строки — там же кончается разделитель. Кнопка рисуется со
 * смещением [deleteOffset] от центра ручки и катится, как колесо: угол поворота привязан к смещению,
 * на месте ручки она стоит «×» (45°). Ручка пропадает, как только кнопка её накрывает. Нажимается
 * кнопка, только когда стоит на месте ([deleteClickable]).
 */
@Composable
private fun ChecklistRowAction(
    showDelete: Boolean,
    deleteClickable: Boolean,
    deleteOffset: () -> Float,
    fontDp: Dp,
    // Сообщает расстояние от центра ручки до правого края окна — из-за него выкатывается кнопка
    onEdgeDistance: (Float) -> Unit,
    onDelete: () -> Unit,
    handleModifier: Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    val radiusPx = with(density) { (fontDp * CHECKLIST_DELETE_RADIUS_TO_FONT).toPx() }
    val halfWidthPx = with(density) { (fontDp * CHECKLIST_HANDLE_WIDTH_TO_FONT / 2f).toPx() }
    // Кнопка накрывает ручку — ручка немного отъезжает влево и гаснет; укатывается кнопка — ручка возвращается.
    // derivedStateOf: пересборка только в момент смены «накрыта / не накрыта», а не на каждом кадре качения
    val handleCovered by remember(showDelete) {
        derivedStateOf { showDelete && deleteOffset() - radiusPx < halfWidthPx }
    }
    val handleHide by animateFloatAsState(
        targetValue = if (handleCovered) 1f else 0f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "checklist_handle_hide"
    )

    Box(
        modifier = Modifier
            .size(fontDp * 1.6f)
            .onGloballyPositioned { coords ->
                val root = coords.findRootCoordinates()
                val handleCenterX = coords.localToRoot(Offset(coords.size.width.toFloat(), 0f)).x - halfWidthPx
                onEdgeDistance(root.size.width - handleCenterX)
            }
            .then(
                when {
                    deleteClickable -> Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onDelete
                    )
                    showDelete -> Modifier
                    else -> handleModifier
                }
            )
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val font = fontDp.toPx()
            val halfWidth = font * CHECKLIST_HANDLE_WIDTH_TO_FONT / 2f
            val handleCenter = Offset(size.width - halfWidth, size.height / 2f)
            val radius = font * CHECKLIST_DELETE_RADIUS_TO_FONT
            val offset = if (showDelete) deleteOffset() else 0f
            if (handleHide < 1f) {
                val pitch = font * 0.167f
                val stroke = font * 0.058f
                val shift = -font * 0.5f * handleHide
                for (i in -1..1) {
                    val y = handleCenter.y + i * pitch
                    drawLine(
                        color = ChecklistHandleColor.copy(alpha = 1f - handleHide),
                        start = Offset(handleCenter.x - halfWidth + shift, y),
                        end = Offset(handleCenter.x + halfWidth + shift, y),
                        strokeWidth = stroke
                    )
                }
            }
            if (showDelete) {
                val center = Offset(handleCenter.x + offset, handleCenter.y)
                val degrees = 45f + offset / radius * CHECKLIST_DELETE_ROLL * (180f / PI.toFloat())
                // Не обрезается здесь: кнопку прячет край карточки редактора
                rotate(degrees = degrees, pivot = center) {
                    drawCircle(color = ChecklistDeleteColor, radius = radius, center = center)
                    val arm = radius * 0.5f
                    val stroke = radius * 0.2f
                    drawLine(Color.White, Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), stroke, StrokeCap.Round)
                    drawLine(Color.White, Offset(center.x, center.y - arm), Offset(center.x, center.y + arm), stroke, StrokeCap.Round)
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
