package com.example.ui.screens.home.inlineeditor.dialogs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Поле поиска в окне, вытягиваемое тягой списка вниз, как в Things (окна «Переместить» и «Теги»).
 *
 * [pull] — общий вытянутый путь (px). Сначала под заголовком раскрывается место поля (до его полной высоты
 * [fullPx]) и поле проявляется на своём месте; дальше поле стоит, а список уходит ниже со всё большим
 * сопротивлением — в открывшемся промежутке стрелка вниз. Протянули ещё на [armPx] — поле и стрелка синеют
 * ([armed]): отпустили — поле остаётся и получает курсор. Не дотянули — поле уезжает обратно.
 */
@Stable
internal class DialogSearchPull(
    private val scope: CoroutineScope,
    private val overPullMaxPx: Float,
    val armPx: Float,
    private val pinned: () -> Boolean,
    private val onArmedRelease: () -> Unit
) {
    val pull = Animatable(0f)
    var fullPx by mutableIntStateOf(0)

    /** Отпущенный список возвращается резко, без раскачки */
    val springBack = spring<Float>(stiffness = Spring.StiffnessMedium)

    /** Протянуто дальше поля на [armPx] — можно отпускать: поле и стрелка залиты акцентом */
    val armed: Boolean get() = fullPx > 0 && pull.value >= fullPx + armPx && !pinned()

    /** Насколько список ушёл ниже полностью раскрытого поля */
    val overPull: Float get() = (pull.value - fullPx).coerceAtLeast(0f)

    /** Ниже этого поле не прячется: пока в нём курсор или текст, оно видно целиком */
    private fun floor() = if (pinned()) fullPx.toFloat() else 0f

    val connection: NestedScrollConnection = object : NestedScrollConnection {
        // Палец вверх: сначала уходит перетяжка списка и пустое поле, потом листается список
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            val low = floor()
            if (delta >= 0f || pull.value <= low) return Offset.Zero
            val next = (pull.value + delta).coerceAtLeast(low)
            val consumed = next - pull.value
            scope.launch { pull.snapTo(next) }
            return Offset(0f, consumed)
        }

        // Палец вниз, список уже наверху: вытягивается поле — пружиной вполовину хода пальца, а за его
        // полной высотой список идёт дальше всё туже, до предела
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            if (source != NestedScrollSource.UserInput || delta <= 0f || fullPx == 0) return Offset.Zero
            val full = fullPx.toFloat()
            val value = pull.value
            val factor = if (value < full) SEARCH_PULL_RESISTANCE
            else SEARCH_PULL_RESISTANCE * (1f - (value - full) / overPullMaxPx).coerceAtLeast(0f)
            val next = (value + delta * factor).coerceAtMost(full + overPullMaxPx)
            scope.launch { pull.snapTo(next) }
            return Offset(0f, delta)
        }

        // Отпустили синее поле — оно остаётся и получает курсор, список пружиной возвращается под него;
        // не дотянули до синего — поле уезжает обратно
        override suspend fun onPreFling(available: Velocity): Velocity {
            val full = fullPx.toFloat()
            val value = pull.value
            if (full <= 0f || value <= floor() && value <= full) return Velocity.Zero
            val opened = value >= full + armPx || pinned()
            if (opened && !pinned()) onArmedRelease()
            pull.animateTo(if (opened) full else 0f, springBack)
            return available
        }
    }
}

/** [DialogSearchPull] окна: [pinned] — в поле курсор или текст, [onArmedRelease] — отпустили вытянутое до конца поле */
@Composable
internal fun rememberDialogSearchPull(pinned: Boolean, onArmedRelease: () -> Unit): DialogSearchPull {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val currentPinned by rememberUpdatedState(pinned)
    val currentOnArmedRelease by rememberUpdatedState(onArmedRelease)
    return remember(density) {
        DialogSearchPull(
            scope = scope,
            overPullMaxPx = with(density) { SEARCH_OVERPULL_MAX.toPx() },
            armPx = with(density) { SEARCH_ARM_DISTANCE.toPx() },
            pinned = { currentPinned },
            onArmedRelease = { currentOnArmedRelease() }
        )
    }
}

/** Тяга списка вниз вытягивает поле поиска вполовину хода пальца — пружиной, как поиск на главном экране */
private const val SEARCH_PULL_RESISTANCE = 0.5f

/** Список протянут ниже раскрытого поля на столько — поле и стрелка синеют, отпускание открывает поиск */
private val SEARCH_ARM_DISTANCE = 24.dp

/** Поле проявляется, когда место под него раскрыто на эту долю */
internal const val SEARCH_FADE_START = 0.5f

/** Насколько список можно протянуть ниже вытянутого поля — сопротивление растёт к этому пределу */
private val SEARCH_OVERPULL_MAX = 120.dp
