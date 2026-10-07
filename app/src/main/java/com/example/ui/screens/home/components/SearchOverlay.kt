package com.example.ui.screens.home.components

import com.example.ui.theme.ThingsTheme
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.Tag
import com.example.ui.screens.home.ActiveScreen
import com.example.ui.theme.AppIcons
import com.example.ui.screens.home.subcomponents.TaskItemRow
import com.example.ui.components.ProjectProgressArc
import com.example.ui.components.HideTextSelectionHandles
import com.example.ui.components.hideSoftKeyboardNow
import com.example.ui.theme.*

// Номинальная длительность раскрытия формы карточки Quick Find (0.22 с)
private const val MORPH_TRANSFORM_DURATION_MS = 220
// Номинальная длительность проявления контента Quick Find (0.154 с = 220 * 0.7, functionByExpanding: 0.7)
private const val MORPH_CONTENT_DURATION_MS = 154

// Запасной источник, если прямоугольник поля/иконки неизвестен: доля карточки у её верхнего края
private const val MORPH_FALLBACK_SCALE = 0.55f
// Карточка стартует полупрозрачной и становится непрозрачной с самого начала роста
private const val MORPH_START_ALPHA = 0.55f
private const val MORPH_FADE_IN_FRACTION = 0.6f

/**
 * Точная кривая пружины затухания, соответствующая оригинальному
 * MSInterpolationFunctions.spring(withDampingRatio: 0.8, duration: 0.22).
 * Окно нормировано к [0, 1] с естественным пиком перелёта ~1.5% (микро-overshoot).
 */
class ThingsSpringEasing(
    private val zeta: Float = 0.8f
) : Easing {
    override fun transform(fraction: Float): Float {
        if (fraction <= 0f) return 0f
        if (fraction >= 1f) return 1f
        val z = zeta.coerceIn(1e-4f, 0.9999f)
        val w0 = 2f * Math.PI.toFloat()
        val wd = w0 * kotlin.math.sqrt(1f - z * z)
        val e = kotlin.math.exp(-z * w0 * fraction)
        val raw = 1f - e * (kotlin.math.cos(wd * fraction) + (z * w0 / wd) * kotlin.math.sin(wd * fraction))
        val endVal = 1f - kotlin.math.exp(-z * w0)
        return (raw / endVal).coerceAtLeast(0f)
    }
}

/**
 * Подсвечивает совпадения поискового запроса [searchQuery] в строке [text] фоновым цветом [highlightColor].
 */
@Composable
fun rememberHighlightedText(
    text: String,
    query: String,
    highlightColor: Color = ThingsTheme.colors.searchMatchHighlight
): AnnotatedString {
    return remember(text, query, highlightColor) {
        if (query.isBlank() || !text.contains(query, ignoreCase = true)) {
            AnnotatedString(text)
        } else {
            buildAnnotatedString {
                var currentIndex = 0
                val lowerText = text.lowercase()
                val lowerQuery = query.trim().lowercase()
                val queryLength = lowerQuery.length

                while (currentIndex < text.length) {
                    val matchIndex = lowerText.indexOf(lowerQuery, currentIndex)
                    if (matchIndex < 0) {
                        append(text.substring(currentIndex))
                        break
                    } else {
                        if (matchIndex > currentIndex) {
                            append(text.substring(currentIndex, matchIndex))
                        }
                        val matchEnd = matchIndex + queryLength
                        val matchText = text.substring(matchIndex, matchEnd)
                        pushStyle(SpanStyle(background = highlightColor))
                        append(matchText)
                        pop()
                        currentIndex = matchEnd
                    }
                }
            }
        }
    }
}

/**
 * Форма карточки во время морфинга: скруглённый прямоугольник [rect] в локальных координатах
 * карточки. Радиусы по осям задаются раздельно — при неравномерном масштабе слоя это
 * сохраняет углы круглыми на экране.
 */
private class MorphCardShape(
    private val rect: Rect,
    private val radiusX: Float,
    private val radiusY: Float
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(RoundRect(rect, CornerRadius(radiusX, radiusY)))
}

private fun lerpF(start: Float, stop: Float, fraction: Float) = start + (stop - start) * fraction

/**
 * Оверлей полноэкранного поиска, точно стилизованный под Things 3 Quick Find.
 * Включает раздел "Recent" при пустом вводе, раздельные кнопки закрытия,
 * кастомные иконки прогресса проектов и визуальные эффекты подсветки.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThingsSearchOverlay(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    allTasks: List<ItemWithChecklist>,
    projects: List<Item>,
    areas: List<Area>,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    cardSurfaceColor: Color,
    dividerColor: Color,
    onTaskClick: (ItemWithChecklist) -> Unit,
    onProjectClick: (Item) -> Unit,
    onAreaClick: (Area) -> Unit,
    onSmartListClick: (ActiveScreen) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    allSavedTagObjects: List<Tag> = emptyList(),
    onTagClick: (Tag) -> Unit = {},
    // [ИЗМЕНЕНИЕ]: Список недавно искавшихся/выбранных в поиске объектов
    recentSearchItems: List<SearchResultItem> = emptyList(),
    onContinueSearchClick: () -> Unit = {},
    onTaskToggle: (ItemWithChecklist) -> Unit = {},
    // Оверлей уже закрывается (идёт exit-анимация), но поле поиска ещё в композиции
    isClosing: Boolean = false,
    wasPulled: Boolean = false,
    // Прямоугольник источника морфинга в координатах корня: поле поиска на стартовом экране
    // или круглая иконка оттяжки на экранах списков. Окно растягивается из него вместе с содержимым
    morphSource: Rect? = null,
    // Источник — поле поиска стартового экрана: шапка окна (поле ввода и ✕) не сжимается
    // и с первого кадра стоит на месте поля, отступы поля и ✕ нарастают вокруг него
    morphFromWideField: Boolean = false,
    // Карточка измерена и со следующего кадра рисуется на месте источника: только теперь
    // можно прятать поле стартового экрана, иначе между ними остаётся пустой кадр
    onMorphReady: () -> Unit = {},
    currentScreen: ActiveScreen = ActiveScreen.HOME,
    currentProject: Item? = null,
    currentArea: Area? = null,
    // [ИЗМЕНЕНИЕ]: Прогресс синхронного появления капсулы поиска (0f..1f)
    onDismissProgress: ((Float) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val transformProgress = remember { Animatable(0f) }
    val opacityProgress = remember { Animatable(0f) }
    val dismissProgress = remember { Animatable(0f) }
    var isMorphClosing by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val isReduceMotion = remember {
        try {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        } catch (e: Exception) {
            false
        }
    }

    // Итоговый прямоугольник карточки нужен до старта роста: морфинг идёт от источника к нему
    var cardBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    val isMorphMeasured = cardBoundsInRoot != null

    // Фокус и клавиатура
    val focusRequester = remember { FocusRequester() }

    // [ИЗМЕНЕНИЕ]: Физическая пружина Jetpack Compose (dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow).
    // Клавиатура запрашивается на середине хода (~110 мс) — её системный подъем происходит
    // одновременно с приземлением карточки, устраняя задержку появления.
    LaunchedEffect(isMorphMeasured) {
        if (!isMorphMeasured) return@LaunchedEffect
        if (isReduceMotion) {
            transformProgress.snapTo(1f)
            opacityProgress.animateTo(1f, tween(100))
            focusRequester.requestFocus()
        } else {
            coroutineScope.launch {
                delay(110)
                focusRequester.requestFocus()
            }
            coroutineScope.launch {
                opacityProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            coroutineScope.launch {
                transformProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }
    }

    val view = LocalView.current

    val handleClose: () -> Unit = remember(isMorphClosing, isReduceMotion, onDismissProgress) {
        {
            if (!isMorphClosing) {
                isMorphClosing = true
                view.hideSoftKeyboardNow()
                coroutineScope.launch {
                    if (isReduceMotion) {
                        dismissProgress.snapTo(1f)
                        onDismissProgress?.invoke(1f)
                    } else {
                        dismissProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 250,
                                easing = FastOutSlowInEasing
                            )
                        ) {
                            // Синхронный cross-fade: капсула начинает мягко проявляться с 50% пути съезда окна
                            val capsuleAlpha = ((value - 0.50f) / 0.50f).coerceIn(0f, 1f)
                            onDismissProgress?.invoke(capsuleAlpha)
                        }
                    }
                    onClose()
                }
            }
        }
    }

    BackHandler(enabled = !isMorphClosing) {
        handleClose()
    }

    LaunchedEffect(isClosing) {
        if (isClosing && !isMorphClosing) {
            handleClose()
        }
    }



    // Алгоритм умного поиска (Quick Find)
    val searchResults = remember(searchQuery, allTasks, projects, areas, allSavedTagObjects) {
        if (searchQuery.isBlank()) {
            emptyList<SearchResultItem>()
        } else {
            val results = mutableListOf<SearchResultItem>()
            val query = searchQuery.trim()

            // 1. Поиск по смарт-спискам
            val smartLists = listOf(
                Pair("Today", ActiveScreen.TODAY),
                Pair("Inbox", ActiveScreen.INBOX),
                Pair("Upcoming", ActiveScreen.UPCOMING),
                Pair("Anytime", ActiveScreen.ANYTIME),
                Pair("Someday", ActiveScreen.SOMEDAY),
                Pair("Logbook", ActiveScreen.LOGBOOK)
            )
            for ((name, screen) in smartLists) {
                if (name.contains(query, ignoreCase = true)) {
                    results.add(SearchResultItem.SmartListResult(name, screen))
                }
            }

            // 2. Поиск по областям ответственности (только активные, неудаленные)
            val matchedAreas = areas.filter { !it.trashed && it.title.contains(query, ignoreCase = true) }
            results.addAll(matchedAreas.map { SearchResultItem.AreaResult(it) })

            // 2.1 Поиск по тегам (отображаются карточкой, аналогичной области)
            val matchedTags = allSavedTagObjects.filter {
                it.title.isNotBlank() && it.title.contains(query, ignoreCase = true)
            }
            results.addAll(matchedTags.map { SearchResultItem.TagResult(it) })

            // 3. Поиск по проектам (только активные по названию, заметкам; исключая удаленные и завершенные/отмененные)
            val matchedProjects = projects.filter {
                it.type == 1 && !it.trashed && !it.isCompleted && it.status != 2 && (
                    it.title.contains(query, ignoreCase = true) ||
                    it.notes.contains(query, ignoreCase = true)
                )
            }
            results.addAll(matchedProjects.map { SearchResultItem.ProjectResult(it) })

            // 4. Поиск по задачам (только активные по названию, заметкам, чеклистам; исключая удаленные и завершенные/отмененные)
            val matchedTasks = allTasks.filter { wrapper ->
                wrapper.item.type == 0 && !wrapper.item.trashed && !wrapper.item.isCompleted && wrapper.item.status != 2 && (
                    wrapper.item.title.contains(query, ignoreCase = true) ||
                    wrapper.item.notes.contains(query, ignoreCase = true) ||
                    wrapper.checklist.any { it.title.contains(query, ignoreCase = true) }
                )
            }
            results.addAll(matchedTasks.map { SearchResultItem.TaskResult(it) })

            results
        }
    }

    // В эталоне Things 3 кнопка "Continue Search" отображается всегда при наличии любого поискового запроса
    val showContinueSearch = searchQuery.isNotBlank()

    // Имя первого доступного проекта для отображения в "Recent"
    val recentProjectName = remember(projects) {
        projects.firstOrNull { it.type == 1 && !it.trashed && !it.isCompleted && it.status != 2 }?.title ?: "Vacation in Rome"
    }
    val recentProject = remember(projects) {
        projects.firstOrNull { it.type == 1 && !it.trashed && !it.isCompleted && it.status != 2 }
    }

    // Ход анимации формы (220 мс, пружина ζ = 0.8) и проявления контента (154 мс, functionByExpanding: 0.7)
    val morphRaw = transformProgress.value
    val progress = morphRaw.coerceIn(0f, 1f)
    val contentAlpha = opacityProgress.value.coerceIn(0f, 1f)

    val cardBackground = ThingsTheme.colors.searchCard
    val textPrimary = textPrimaryColor
    val textSecondary = textSecondaryColor
    val inputNormalBackground = ThingsTheme.colors.searchField
    // Карточка и поле ввода всегда имеют свой итоговый цвет с первого кадра (белая карточка, серый инпут)
    val currentCardBg = cardBackground
    val currentInputBg = inputNormalBackground
    val closeButtonBackground = ThingsTheme.colors.searchCloseButton

    // Карточка всегда раскладывается в своём итоговом месте, путь от источника к нему
    // целиком задаётся преобразованием слоя
    val currentHorizontalMargin = 14.dp
    val density = LocalDensity.current
    // Радиус плавающей карточки — общий с Quick Add
    val morphFinalCornerRadius = ThingsTheme.shapes.floatingCard
    val finalRadiusPx = with(density) { morphFinalCornerRadius.toPx() }

    // Высота шапки (поле ввода и ✕) в раскладке — нужна, чтобы содержимое шло сразу под несжатой шапкой
    var headerHeightPx by remember { mutableStateOf(0) }
    val headerTopPaddingPx = with(density) { 20.dp.toPx() }
    var layerTranslationX = 0f
    var layerTranslationY = 0f
    var layerScaleX = 1f
    var layerScaleY = 1f
    var cardShape: Shape = ThingsTheme.shapes.floatingCardShape
    val dst = cardBoundsInRoot
    if (dst != null && dst.width > 0f && dst.height > 0f) {
        val src = morphSource ?: Rect(
            left = dst.center.x - dst.width * MORPH_FALLBACK_SCALE / 2f,
            top = dst.top,
            right = dst.center.x + dst.width * MORPH_FALLBACK_SCALE / 2f,
            bottom = dst.top + dst.height * MORPH_FALLBACK_SCALE
        )
        // Текущий прямоугольник на экране: от источника к карточке по пружине
        val cur = Rect(
            left = lerpF(src.left, dst.left, morphRaw),
            top = lerpF(src.top, dst.top, morphRaw),
            right = lerpF(src.right, dst.right, morphRaw),
            bottom = lerpF(src.bottom, dst.bottom, morphRaw)
        )
        val sourceRadius = minOf(src.width, src.height) / 2f
        val radius = lerpF(sourceRadius, finalRadiusPx, progress)
            .coerceAtMost(minOf(cur.width, cur.height) / 2f)
        // Карточка растягивается из источника вниз и в стороны вместе с содержимым
        layerScaleX = (cur.width / dst.width).coerceAtLeast(0.01f)
        layerScaleY = (cur.height / dst.height).coerceAtLeast(0.01f)
        layerTranslationX = cur.left - dst.left
        layerTranslationY = cur.top - dst.top
        cardShape = MorphCardShape(
            rect = Rect(0f, 0f, dst.width, dst.height),
            radiusX = radius / layerScaleX,
            radiusY = radius / layerScaleY
        )
    }
    val dismissSlidePx = with(density) { 110.dp.toPx() }
    val dismissVal = dismissProgress.value.coerceIn(0f, 1f)
    val finalTranslationX = if (isMorphClosing) 0f else layerTranslationX
    val finalTranslationY = if (isMorphClosing) dismissVal * dismissSlidePx else layerTranslationY
    val finalScaleX = if (isMorphClosing) 1f else layerScaleX
    val finalScaleY = if (isMorphClosing) 1f else layerScaleY
    val finalCardShape = if (isMorphClosing) ThingsTheme.shapes.floatingCardShape else cardShape
    val finalCardAlpha = if (!isMorphMeasured) 0f
        else if (isMorphClosing) {
            // Вариант А (Порог задержки): первые 50% пути смещения вниз карточка остаётся 100% непрозрачной (alpha = 1.0f),
            // а растворение начинается только после того, как карточка набрала ход вниз и прошла 50% пути
            if (dismissVal <= 0.50f) {
                1f
            } else {
                val fadeProgress = ((dismissVal - 0.50f) / 0.50f).coerceIn(0f, 1f)
                (1f - fadeProgress).coerceIn(0f, 1f)
            }
        }
        else lerpF(MORPH_START_ALPHA, 1f, (progress / MORPH_FADE_IN_FRACTION).coerceIn(0f, 1f))
    val currentElevation = 12.dp

    // Из поля стартового экрана поле окна в первом кадре занимает всю капсулу:
    // отступы от краёв карточки нарастают от 0.
    val fieldMorph = if (isMorphClosing) 1f else morphRaw.coerceAtLeast(0f)
    val innerHorizontalPadding = if (morphFromWideField && !isMorphClosing) {
        androidx.compose.ui.unit.lerp(0.dp, 14.dp, fieldMorph.coerceAtMost(1f))
    } else {
        14.dp
    }
    val innerTopPadding = 20.dp

    val backdropAlpha = if (isMorphClosing) ((1f - dismissVal) * 0.45f).coerceIn(0f, 0.45f)
        else (morphRaw * 0.45f).coerceIn(0f, 0.45f)
    val closeButtonAlpha = if (isMorphClosing) (1f - dismissVal).coerceIn(0f, 1f)
        else (opacityProgress.value / 0.7f).coerceIn(0f, 1f)
    // Из поля стартового экрана ✕ раздвигается и отодвигает правый край поля
    val closeButtonWidth = if (morphFromWideField && !isMorphClosing) androidx.compose.ui.unit.lerp(0.dp, 44.dp, fieldMorph.coerceAtMost(1f)) else 44.dp
    val closeButtonSpacer = if (morphFromWideField && !isMorphClosing) androidx.compose.ui.unit.lerp(0.dp, 12.dp, fieldMorph.coerceAtMost(1f)) else 12.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ThingsTheme.colors.scrim.copy(alpha = backdropAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = handleClose
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        // [ИЗМЕНЕНИЕ]: Контейнер диалога поиска с бесшовным морфингом при открытии и обратным схлопыванием при закрытии
        Card(
            colors = CardDefaults.cardColors(containerColor = currentCardBg),
            shape = finalCardShape,
            elevation = CardDefaults.cardElevation(defaultElevation = currentElevation),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = currentHorizontalMargin, end = currentHorizontalMargin, top = 20.dp, bottom = 20.dp)
                .widthIn(max = 480.dp)
                // Координаты до слоя — итоговое место карточки без учёта морфинга
                .onGloballyPositioned {
                    val wasMeasured = cardBoundsInRoot != null
                    cardBoundsInRoot = Rect(it.positionInRoot(), it.size.toSize())
                    if (!wasMeasured) onMorphReady()
                }
                .graphicsLayer {
                    alpha = finalCardAlpha
                    translationX = finalTranslationX
                    translationY = finalTranslationY
                    scaleX = finalScaleX
                    scaleY = finalScaleY
                    transformOrigin = TransformOrigin(pivotFractionX = 0f, pivotFractionY = 0f)
                }
                .clip(finalCardShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Игнорировать клики внутри карты
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                // Верхний заголовок со строкой поиска и кнопкой закрытия
                // Из поля стартового экрана шапка раскладывается в текущую ширину карточки на экране
                // и не сжимается вместе с ней: иначе лупа и «Quick Find» в первом кадре сдвигаются влево
                val headerCounterScale = morphFromWideField && !isMorphClosing && dst != null && dst.width > 0f
                val headerWidthModifier = if (headerCounterScale) {
                    Modifier.width(with(density) { (dst!!.width * layerScaleX).toDp() })
                } else {
                    Modifier.fillMaxWidth()
                }
                Column(
                    modifier = headerWidthModifier
                        .onSizeChanged { headerHeightPx = it.height }
                        .graphicsLayer {
                            if (morphFromWideField) {
                                // Шапка не сжимается вместе с карточкой: обратный масштаб.
                                // Верхний отступ до поля нарастает от 0, чтобы поле стартовало ровно
                                // на месте капсулы; сдвиг задан в координатах сжатой карточки
                                transformOrigin = TransformOrigin(pivotFractionX = 0f, pivotFractionY = 0f)
                                if (headerCounterScale) scaleX = 1f / layerScaleX
                                scaleY = 1f / layerScaleY
                                translationY = -headerTopPaddingPx * (1f - fieldMorph) / layerScaleY
                            }
                        }
                        .padding(horizontal = innerHorizontalPadding)
                        .padding(top = innerTopPadding)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Капсула поискового ввода
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(ThingsTheme.shapes.capsuleShape)
                                .background(currentInputBg)
                                .padding(horizontal = 14.dp)
                        ) {
                            val currentIconTint = textSecondary
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = currentIconTint,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    val currentTextTint = textSecondary.copy(alpha = 0.6f)
                                    Text(
                                        text = "Quick Find",
                                        color = currentTextTint,
                                        style = ThingsTheme.type.bodyLarge
                                    )
                                }
                                // Пока оверлей закрывается, курсор и его маркер не рисуем: фокус остаётся
                                // до полного скрытия клавиатуры
                                HideTextSelectionHandles(hidden = isMorphClosing || isClosing) {
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = onSearchQueryChange,
                                        textStyle = ThingsTheme.type.bodyLarge.copy(
                                            color = textPrimary
                                        ),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        cursorBrush = SolidColor(if (isMorphClosing || isClosing) Color.Unspecified else ThingsTheme.colors.accent),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(focusRequester)
                                            .testTag("overlay_search_input")
                                    )
                                }
                            }
                            
                            // Кнопка очистки текста внутри инпута (iOS cut-out badge)
                            if (searchQuery.isNotEmpty()) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(textSecondary.copy(alpha = 0.5f))
                                        .clickable { onSearchQueryChange("") }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = currentInputBg,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }

                        // Standalone круглая кнопка "X" закрытия с плавным Fade-in
                        if (closeButtonAlpha > 0f) {
                            Spacer(modifier = Modifier.width(closeButtonSpacer))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(closeButtonWidth, 44.dp)
                                    .graphicsLayer {
                                        alpha = closeButtonAlpha
                                    }
                                    .clip(CircleShape)
                                    .background(closeButtonBackground)
                                    .clickable(onClick = handleClose)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Контент: Либо "Recent", либо "Результаты поиска" (с плавным проявлением и микросдвигом)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = if (isMorphClosing) 1f else contentAlpha
                            if (morphFromWideField && !isMorphClosing) {
                                // Содержимое растягивается вместе с карточкой, но начинается сразу
                                // под несжатой шапкой, а не наезжает на неё
                                val shiftOnScreen = headerHeightPx * (1f - layerScaleY) -
                                    headerTopPaddingPx * (1f - fieldMorph)
                                translationY = shiftOnScreen / layerScaleY
                            }
                        }
                        .padding(horizontal = 14.dp)
                ) {
                    if (searchQuery.isBlank()) {
                        // Валидация списка Recent: исключаем удаленные/помещенные в корзину задачи, проекты, области и теги,
                        // а также подтягиваем актуальное состояние сущностей
                        val validRecentItems = remember(recentSearchItems, projects, areas, allSavedTagObjects, allTasks) {
                            recentSearchItems.mapNotNull { item: SearchResultItem ->
                                when (item) {
                                    is SearchResultItem.TaskResult -> {
                                        val currentTask = allTasks.find { it.item.id == item.taskWrapper.item.id }
                                        if (currentTask != null && !currentTask.item.trashed) {
                                            SearchResultItem.TaskResult(currentTask)
                                        } else null
                                    }
                                    is SearchResultItem.ProjectResult -> {
                                        val currentProject = projects.find { it.id == item.project.id && !it.trashed }
                                        if (currentProject != null) {
                                            SearchResultItem.ProjectResult(currentProject)
                                        } else null
                                    }
                                    is SearchResultItem.AreaResult -> {
                                        val currentArea = areas.find { it.id == item.area.id && !it.trashed }
                                        if (currentArea != null) {
                                            SearchResultItem.AreaResult(currentArea)
                                        } else null
                                    }
                                    is SearchResultItem.TagResult -> {
                                        val currentTag = allSavedTagObjects.find { it.id == item.tag.id }
                                        if (currentTag != null) {
                                            SearchResultItem.TagResult(currentTag)
                                        } else null
                                    }
                                    is SearchResultItem.SmartListResult -> item
                                    is SearchResultItem.SpecialResult -> item
                                }
                            }
                        }

                        // РЕЖИМ 1: Стартовый экран (без запроса)
                        if (validRecentItems.isNotEmpty()) {
                            Text(
                                text = "Recent",
                                color = textSecondary,
                                style = ThingsTheme.type.subheadMedium,
                                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                            )
                            HorizontalDivider(
                                color = dividerColor,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                validRecentItems.take(5).forEach { result ->
                                    val isCurrent = when (result) {
                                        is SearchResultItem.SmartListResult -> currentScreen == result.screen
                                        is SearchResultItem.ProjectResult -> currentScreen == ActiveScreen.PROJECT_DETAIL && currentProject?.id == result.project.id
                                        is SearchResultItem.AreaResult -> currentScreen == ActiveScreen.AREA_DETAIL && currentArea?.id == result.area.id
                                        is SearchResultItem.TagResult -> currentScreen == ActiveScreen.TAG_DETAIL
                                        else -> false
                                    }
                                    SearchResultRow(
                                        result = result,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        allTasks = allTasks,
                                        projects = projects,
                                        areas = areas,
                                        searchQuery = searchQuery,
                                        isHighlighted = false,
                                        isChecked = isCurrent,
                                        onTaskClick = onTaskClick,
                                        onProjectClick = onProjectClick,
                                        onAreaClick = onAreaClick,
                                        onTagClick = onTagClick,
                                        onSmartListClick = onSmartListClick,
                                        onTaskToggle = onTaskToggle
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        } else {
                            // При пустой истории даем аккуратный отступ сверху
                            Spacer(modifier = Modifier.height(18.dp))
                        }

                        // Подпись внизу
                        Text(
                            text = "Quickly switch lists, find to-dos,\nsearch for tags...",
                            color = textSecondary.copy(alpha = 0.8f),
                            style = ThingsTheme.type.subhead,
                            fontStyle = FontStyle.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp)
                        )
                    } else {
                        // РЕЖИМ 2: Экран результатов поиска
                        if (searchResults.isEmpty()) {
                            // Если нет прямых результатов в списках/проектах, сразу предлагаем глубокий поиск
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                ContinueSearchTaskRow(
                                    onClick = onContinueSearchClick,
                                    textPrimary = textPrimary,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            // Список отфильтрованных пунктов
                            // Строки результатов вплотную друг к другу, как в Things 3
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 350.dp)
                                    .padding(bottom = 12.dp)
                            ) {
                                val visibleResults = searchResults.take(15)
                                itemsIndexed(visibleResults) { index, result ->
                                    val isFirstTask = result is SearchResultItem.TaskResult &&
                                            index > 0 &&
                                            visibleResults[index - 1] !is SearchResultItem.TaskResult // [ИЗМЕНЕНИЕ]: Проверяем только прошлый элемент, чтобы избежать разделителей между задачами
                                    
                                    if (isFirstTask) {
                                        HorizontalDivider(
                                            color = dividerColor,
                                            thickness = 0.5.dp,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                    }

                                    SearchResultRow(
                                        result = result,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        allTasks = allTasks,
                                        projects = projects,
                                        areas = areas,
                                        searchQuery = searchQuery,
                                        isHighlighted = (index == 0),
                                        isChecked = false,
                                        onTaskClick = onTaskClick,
                                        onProjectClick = onProjectClick,
                                        onAreaClick = onAreaClick,
                                        onTagClick = onTagClick,
                                        onSmartListClick = onSmartListClick,
                                        onTaskToggle = onTaskToggle
                                    )
                                }
                                
                                // [ИЗМЕНЕНИЕ]: Отображаем кнопку "Continue Search" в конце прокручиваемого списка результатов поиска в качестве элемента, аналогичного задаче, если showContinueSearch — true
                                if (showContinueSearch) {
                                    item {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        ContinueSearchTaskRow(
                                            onClick = onContinueSearchClick,
                                            textPrimary = textPrimary,
                                            modifier = Modifier.fillMaxWidth()
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

/**
 * [ИЗМЕНЕНИЕ]: Компонент строки "Continue Search" (высота 44dp, капсула 22dp, лупа 22dp в колонке 28dp, вес SemiBold)
 */
@Composable
fun ContinueSearchTaskRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textPrimary: Color = ThingsTheme.colors.textPrimary
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(ThingsTheme.shapes.capsuleShape)
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 4.dp)
    ) {
        Box(
            modifier = Modifier.size(MaterialTheme.dimens.searchLeftColumnWidth),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = textPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(MaterialTheme.dimens.searchSpacingToText))
        Text(
            text = "Continue Search",
            style = ThingsTheme.type.taskTitle.copy(
                color = textPrimary,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun RecentRow(
    title: String,
    icon: @Composable () -> Unit,
    textPrimary: Color,
    isChecked: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(ThingsTheme.shapes.capsuleShape)
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 12.dp)
    ) {
        Box(
            modifier = Modifier.size(MaterialTheme.dimens.searchLeftColumnWidth),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.width(MaterialTheme.dimens.searchSpacingToText))
        Text(
            text = title,
            style = ThingsTheme.type.taskTitle.copy(
                color = textPrimary,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (isChecked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Current screen",
                tint = ThingsTheme.colors.accent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Запечатанный класс для единообразного отображения разных результатов (Quick Find)
 */
sealed class SearchResultItem {
    data class SmartListResult(val title: String, val screen: ActiveScreen) : SearchResultItem()
    data class SpecialResult(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val iconColor: Color) : SearchResultItem()
    data class AreaResult(val area: Area) : SearchResultItem()
    data class TagResult(val tag: Tag) : SearchResultItem()
    data class ProjectResult(val project: Item) : SearchResultItem()
    data class TaskResult(val taskWrapper: ItemWithChecklist) : SearchResultItem()
}

@Composable
fun SearchResultRow(
    result: SearchResultItem,
    textPrimary: Color,
    textSecondary: Color,
    allTasks: List<ItemWithChecklist>,
    projects: List<Item>,
    areas: List<Area> = emptyList(),
    searchQuery: String = "",
    isHighlighted: Boolean = false,
    isChecked: Boolean = false,
    onTaskClick: (ItemWithChecklist) -> Unit,
    onProjectClick: (Item) -> Unit,
    onAreaClick: (Area) -> Unit,
    onTagClick: (Tag) -> Unit = {},
    onSmartListClick: (ActiveScreen) -> Unit,
    onTaskToggle: (ItemWithChecklist) -> Unit = {}
) {
    val accentColor = ThingsTheme.colors.accent
    when (result) {
        is SearchResultItem.TaskResult -> {
            TaskItemRow(
                task = result.taskWrapper.item,
                textPrimaryColor = textPrimary,
                textSecondaryColor = textSecondary,
                dividerColor = Color.Transparent,
                isHighlighted = isHighlighted,
                onToggle = { onTaskToggle(result.taskWrapper) },
                onClick = { onTaskClick(result.taskWrapper) },
                projects = projects,
                areas = areas,
                leftColumnWidth = MaterialTheme.dimens.searchLeftColumnWidth,
                spacingToText = MaterialTheme.dimens.searchSpacingToText
            )
        }
        is SearchResultItem.ProjectResult -> {
            val totalCount = allTasks.count { it.item.projectId == result.project.id && it.item.type == 0 }
            val completedCount = allTasks.count { it.item.projectId == result.project.id && it.item.type == 0 && it.item.isCompleted }
            val isSomeday = result.project.isSomeday
            val baseArcColor = if (isSomeday) ThingsTheme.colors.someday else ThingsTheme.colors.project
            val arcColor = if (isHighlighted) androidx.compose.ui.graphics.lerp(baseArcColor, accentColor, 0.45f) else baseArcColor
            val rowBg = if (isHighlighted) accentColor.copy(alpha = 0.3f) else Color.Transparent

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(ThingsTheme.shapes.capsuleShape)
                    .background(rowBg)
                    .clickable { onProjectClick(result.project) }
                    .padding(start = 8.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(MaterialTheme.dimens.searchLeftColumnWidth),
                    contentAlignment = Alignment.Center
                ) {
                    ProjectProgressArc(
                        completed = completedCount,
                        total = totalCount,
                        color = arcColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(MaterialTheme.dimens.searchSpacingToText))
                Text(
                    text = result.project.title,
                    style = ThingsTheme.type.taskTitle.copy(
                        color = textPrimary,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isChecked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Current screen",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        else -> {
            val rowBg = if (isHighlighted) accentColor.copy(alpha = 0.3f) else Color.Transparent

            val title = when (result) {
                is SearchResultItem.SmartListResult -> result.title
                is SearchResultItem.SpecialResult -> result.title
                is SearchResultItem.AreaResult -> result.area.title
                is SearchResultItem.TagResult -> result.tag.title
                else -> ""
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(ThingsTheme.shapes.capsuleShape)
                    .background(rowBg)
                    .clickable {
                        when (result) {
                            is SearchResultItem.SmartListResult -> onSmartListClick(result.screen)
                            is SearchResultItem.SpecialResult -> onSmartListClick(ActiveScreen.INBOX)
                            is SearchResultItem.AreaResult -> onAreaClick(result.area)
                            is SearchResultItem.TagResult -> onTagClick(result.tag)
                            else -> {}
                        }
                    }
                    .padding(start = 8.dp, end = 12.dp)
            ) {
                Box(
                    modifier = Modifier.size(MaterialTheme.dimens.searchLeftColumnWidth),
                    contentAlignment = Alignment.Center
                ) {
                    val iconModifier = Modifier
                        .then(
                            if (isHighlighted) {
                                Modifier
                                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(accentColor.copy(alpha = 0.45f), blendMode = BlendMode.SrcAtop)
                                    }
                            } else Modifier
                        )

                    when (result) {
                        is SearchResultItem.SmartListResult -> {
                            val info = when (result.screen) {
                                ActiveScreen.TODAY -> Pair(AppIcons.Today, Color.Unspecified)
                                ActiveScreen.INBOX -> Pair(AppIcons.Inbox, Color.Unspecified)
                                ActiveScreen.UPCOMING -> Pair(AppIcons.Upcoming, Color.Unspecified)
                                ActiveScreen.ANYTIME -> Pair(AppIcons.Anytime, Color.Unspecified)
                                ActiveScreen.SOMEDAY -> Pair(AppIcons.Someday, Color.Unspecified)
                                ActiveScreen.LOGBOOK -> Pair(AppIcons.Logbook, Color.Unspecified)
                                else -> Pair(Icons.Default.Layers, ThingsTheme.colors.someday)
                            }
                            Icon(
                                imageVector = info.first,
                                contentDescription = null,
                                tint = info.second,
                                modifier = iconModifier.size(19.dp)
                            )
                        }
                        is SearchResultItem.SpecialResult -> {
                            Icon(
                                imageVector = result.icon,
                                contentDescription = null,
                                tint = result.iconColor,
                                modifier = iconModifier.size(20.dp)
                            )
                        }
                        is SearchResultItem.AreaResult -> {
                            Icon(
                                imageVector = AppIcons.Area,
                                contentDescription = null,
                                tint = ThingsTheme.colors.area,
                                modifier = iconModifier.size(20.dp)
                            )
                        }
                        is SearchResultItem.TagResult -> {
                            Icon(
                                imageVector = AppIcons.Tag,
                                contentDescription = null,
                                tint = ThingsTheme.colors.someday,
                                modifier = iconModifier.size(20.dp)
                            )
                        }
                        else -> {}
                    }
                }

                Spacer(modifier = Modifier.width(MaterialTheme.dimens.searchSpacingToText))

                Text(
                    text = title,
                    style = ThingsTheme.type.taskTitle.copy(
                        color = textPrimary,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isChecked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Current screen",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

