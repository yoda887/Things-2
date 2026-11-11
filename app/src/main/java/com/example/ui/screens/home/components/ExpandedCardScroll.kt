package com.example.ui.screens.home.components

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.example.ui.screens.home.subcomponents.EXPANDED_BOTTOM_GAP
import com.example.ui.screens.home.subcomponents.EXPANDED_TOP_LIMIT
import com.example.ui.theme.dimens
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

/** Раскрытая карточка у точки остановки прокрутки: видимая её часть и наибольший заход пружиной за точку */
private val EXPANDED_VISIBLE_STRIP = 96.dp
// Меньше видимой части: при заходе край карточки остаётся в зоне, не прячется за статус-бар или панель
private val EXPANDED_OVERSCROLL_MAX = 72.dp
/** Возврат раскрытой карточки к точке остановки — мягкая пружина без отскока */
private const val EXPANDED_SPRING_STIFFNESS = Spring.StiffnessLow
private val EXPANDED_SPRING_BACK = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = EXPANDED_SPRING_STIFFNESS
)

/**
 * Прокрутка списка при раскрытой задаче: ограничитель [clamp] (в цепочке nestedScroll списка) и бросок [fling]
 * (flingBehavior списка, пока задача раскрыта; иначе — [defaultFling]).
 */
class ExpandedCardScroll(
    val clamp: NestedScrollConnection,
    val fling: FlingBehavior,
    val defaultFling: FlingBehavior
)

/**
 * Пока задача раскрыта, прокрутка не уводит её карточку из видимой зоны — той же, в которую карточку ставит
 * раскрытие: от тулбара до плашки действий (или клавиатуры). Точки остановки — у краёв зоны, с видимой частью
 * карточки [EXPANDED_VISIBLE_STRIP]: вверх карточка встаёт так, что у верха зоны видна её нижняя часть, вниз — что у
 * низа видна верхняя. За точку остановки её уводит пружина: пальцем — с растущим сопротивлением, броском — с точки
 * остановки её ведёт пружина с набранной скоростью. Дальше [EXPANDED_OVERSCROLL_MAX] карточка не уходит, потом
 * пружиной возвращается к точке остановки. Движение к зоне — всегда свободно.
 *
 * @param expandedTaskId раскрытая задача (ключ её строки в списке) или null
 * @param listCoordinates координаты списка на экране
 */
@Composable
fun rememberExpandedCardScroll(
    expandedTaskId: String?,
    lazyListState: LazyListState,
    listCoordinates: () -> LayoutCoordinates?
): ExpandedCardScroll {
    val density = LocalDensity.current
    val currentExpandedId by rememberUpdatedState(expandedTaskId)
    val currentListCoordinates by rememberUpdatedState(listCoordinates)
    val imeInsets = WindowInsets.ime
    val navBarInsets = WindowInsets.navigationBars
    val cardTopLimitPx = WindowInsets.statusBars.getTop(density) + with(density) { EXPANDED_TOP_LIMIT.toPx() }
    val cardBottomReservePx = with(density) {
        (MaterialTheme.dimens.floatingToolbarBottomPadding + MaterialTheme.dimens.floatingToolbarHeight + EXPANDED_BOTTOM_GAP).toPx()
    }
    val cardGapPx = with(density) { MaterialTheme.dimens.taskExpandedVerticalGap.toPx() }
    val currentCardLimits by rememberUpdatedState(Triple(cardTopLimitPx, cardBottomReservePx, cardGapPx))
    val cardStripPx = with(density) { EXPANDED_VISIBLE_STRIP.toPx() }
    val cardOverscrollMaxPx = with(density) { EXPANDED_OVERSCROLL_MAX.toPx() }

    /** Верх раскрытой карточки на экране и его точки остановки (lo — выше всего, hi — ниже всего); null — не видна. */
    fun expandedCardStops(): Triple<Float, Float, Float>? {
        val id = currentExpandedId ?: return null
        val coords = currentListCoordinates()?.takeIf { it.isAttached } ?: return null
        val layoutInfo = lazyListState.layoutInfo
        val info = layoutInfo.visibleItemsInfo.firstOrNull { it.key == id } ?: return null
        val (topLimit, bottomReserve, gap) = currentCardLimits
        val rootHeight = coords.findRootCoordinates().size.height.toFloat()
        // Карточка — строка без отступов раскрытия сверху и снизу, в координатах экрана
        val cardTop = coords.positionInRoot().y + (info.offset - layoutInfo.viewportStartOffset) + gap
        val cardHeight = (info.size - 2 * gap).toFloat()
        val zoneBottom = minOf(
            rootHeight - imeInsets.getBottom(density),
            rootHeight - navBarInsets.getBottom(density) - bottomReserve
        )
        val strip = minOf(cardStripPx, cardHeight)
        return Triple(cardTop, topLimit - cardHeight + strip, zoneBottom - strip)
    }

    /** Сколько прокрутить, чтобы карточка встала в точку остановки (0 — она между точками). */
    fun cardBackDelta(): Float {
        val (cardTop, lo, hi) = expandedCardStops() ?: return 0f
        return when {
            cardTop < lo -> cardTop - lo
            cardTop > hi -> cardTop - hi
            else -> 0f
        }
    }

    val clamp = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val (cardTop, lo, hi) = expandedCardStops() ?: return Offset.Zero
                val dy = available.y
                if (dy == 0f) return Offset.Zero
                // До точки остановки — свободно; дальше пальцем — с растущим сопротивлением, броском — как решит
                // fling; в любом случае не дальше EXPANDED_OVERSCROLL_MAX
                val toStop = if (dy > 0f) maxOf(0f, hi - cardTop) else maxOf(0f, cardTop - lo)
                val overshoot = if (dy > 0f) maxOf(0f, cardTop - hi) else maxOf(0f, lo - cardTop)
                val size = abs(dy)
                val free = minOf(size, toStop)
                val beyond = size - free
                val room = maxOf(0f, cardOverscrollMaxPx - overshoot)
                val extra = if (source == NestedScrollSource.UserInput) {
                    minOf(room, beyond * (1f - overshoot / cardOverscrollMaxPx).coerceIn(0f, 1f))
                } else minOf(room, beyond)
                val allowed = (free + extra) * sign(dy)
                return Offset(0f, dy - allowed)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // Запасной возврат: бросок закончился, а карточка осталась за точкой остановки
                val back = cardBackDelta()
                if (back != 0f) lazyListState.animateScrollBy(back, EXPANDED_SPRING_BACK)
                return Velocity.Zero
            }
        }
    }

    // Бросок при раскрытой задаче: до точки остановки — обычное затухание. Как только карточка пересекла точку
    // остановки, её ведёт одна пружина с набранной скоростью: по инерции карточка проходит немного дальше, плавно
    // тормозит и без паузы возвращается к точке остановки (дальше EXPANDED_OVERSCROLL_MAX её не пускает onPreScroll)
    val defaultFling = ScrollableDefaults.flingBehavior()
    val currentDefaultFling by rememberUpdatedState(defaultFling)
    val fling = remember {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                if (expandedCardStops() == null) return with(currentDefaultFling) { performFling(initialVelocity) }
                var last = 0f
                var velocity = initialVelocity
                // Затухание довело карточку до точки остановки с ненулевой скоростью
                var reachedStop = false
                if (abs(initialVelocity) > 1f && cardBackDelta() == 0f) {
                    AnimationState(0f, initialVelocity).animateDecay(exponentialDecay()) {
                        val delta = value - last
                        last = value
                        velocity = this.velocity
                        // Шаг — не дальше точки остановки: за ней карточку ведёт только пружина
                        val stops = expandedCardStops()
                        val toStop = when {
                            stops == null -> Float.POSITIVE_INFINITY
                            delta > 0f -> maxOf(0f, stops.first - stops.second)
                            else -> maxOf(0f, stops.third - stops.first)
                        }
                        val step = if (abs(delta) > toStop) toStop * sign(delta) else delta
                        val used = scrollBy(step)
                        // Дошли до точки остановки (или упёрлись в край списка) — дальше ведёт пружина
                        if (step != delta) reachedStop = true
                        if (step != delta || abs(used - step) > 0.5f) cancelAnimation()
                    }
                    if (!reachedStop && cardBackDelta() == 0f) return 0f
                }
                // Пружина к точке остановки с текущей скоростью: инерция гаснет плавно, затем возврат. Скорость входа
                // ограничена так, чтобы пружина (без отскока: наибольший заход v / (ω·e)) гасила её внутри
                // EXPANDED_OVERSCROLL_MAX и не упиралась в предел
                val back = cardBackDelta()
                if (back != 0f || reachedStop) {
                    val omega = sqrt(EXPANDED_SPRING_STIFFNESS)
                    // Запас — то, что осталось до предела с учётом захода, уже сделанного пальцем
                    val room = maxOf(0f, cardOverscrollMaxPx - abs(back))
                    val maxVelocity = 0.8f * room * omega * kotlin.math.E.toFloat()
                    // Скорость к точке остановки не ограничиваем — это уже возврат
                    val outward = back == 0f || sign(velocity) != sign(back)
                    val entry = if (outward) velocity.coerceIn(-maxVelocity, maxVelocity) else velocity
                    var prev = 0f
                    animate(0f, back, initialVelocity = entry, animationSpec = EXPANDED_SPRING_BACK) { v, _ ->
                        scrollBy(v - prev)
                        prev = v
                    }
                }
                return 0f
            }
        }
    }
    return remember(clamp, fling, defaultFling) { ExpandedCardScroll(clamp, fling, defaultFling) }
}
