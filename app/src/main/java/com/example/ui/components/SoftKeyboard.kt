package com.example.ui.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Insets
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsAnimationControlListener
import android.view.WindowInsetsAnimationController
import android.view.animation.PathInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOut
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/**
 * Прячет экранную клавиатуру немедленно — в том же кадре, не дожидаясь очереди команд Compose.
 *
 * Почему не `LocalSoftwareKeyboardController.hide()` вместе с `clearFocus()`:
 * Compose складывает команды ввода в очередь и выполняет их на следующем кадре, а снятие фокуса
 * добавляет туда же `restartInput`. Клавиатура, которая ещё на экране, получает переподключение
 * к пустому полю и перерисовывается в запасную раскладку (ряд цифр вместо строки подсказок),
 * и только потом уезжает. На Xiaomi/HyperOS с Gboard это видно как смена раскладки и полоска
 * клавиатуры, которая задерживается внизу, когда интерфейс уже закрылся.
 *
 * Фокус после вызова сразу не снимаем: он уйдёт вместе с полем ввода, когда поле покинет
 * композицию, и тогда же Compose вызовет `restartInput`. К этому моменту клавиатура должна быть
 * скрыта системой полностью, а не только визуально: штатная анимация скрытия на HyperOS идёт
 * ~530 мс (визуально клавиатура уходит за ~200 мс, дальше тянется невидимый хвост), а редактор
 * сворачивается за 300 мс. `restartInput` в этом хвосте перерисовывает Gboard в более высокую
 * раскладку, и её верх — панель Gboard — выглядывает из-под края экрана полоской.
 * Поэтому на API 30+ ведём анимацию скрытия сами и укладываемся в [IME_HIDE_DURATION_MS].
 *
 * Правило для всех полей ввода: поле с фокусом должно оставаться в композиции ещё
 * [SOFT_KEYBOARD_HIDE_SETTLE_MS] после вызова. Если оно и так уходит не быстрее (сворачивание,
 * выезд экрана, анимация закрытия окна) — достаточно этого вызова; если быстрее — см.
 * [holdForSoftKeyboardHide] и [hideSoftKeyboardThen].
 */
fun View.hideSoftKeyboardNow() {
    hideSoftKeyboard(onHidden = null)
}

/**
 * Прячет клавиатуру как [hideSoftKeyboardNow] и вызывает [onHidden], когда она скрыта полностью;
 * если клавиатуры на экране нет — сразу.
 *
 * Для полей и окон, которые убираются мгновенно, без анимации: переименование по «Готово»,
 * закрытие окна-диалога. Убрать их раньше нельзя — Compose вызовет `restartInput` при видимой
 * клавиатуре, а окно диалога к тому же унесёт с собой анимацию скрытия.
 */
fun View.hideSoftKeyboardThen(onHidden: () -> Unit) {
    hideSoftKeyboard(onHidden)
}

/**
 * Сколько поле с фокусом должно оставаться в композиции после [hideSoftKeyboardNow]: анимация
 * скрытия плюс задержка, с которой система отдаёт управление ею (на HyperOS ~30 мс), с запасом.
 */
const val SOFT_KEYBOARD_HIDE_SETTLE_MS = 350

/**
 * Добавка к exit-анимации `AnimatedVisibility`: держит содержимое в композиции
 * [SOFT_KEYBOARD_HIDE_SETTLE_MS], даже если видимая часть исчезает быстрее, — чтобы поле ввода
 * внутри ушло уже после полного скрытия клавиатуры.
 *
 * Сдвигает содержимое на 1 px в самом конце, когда оно уже невидимо: анимация, у которой начальное
 * и конечное значения совпадают, считается завершённой сразу и, несмотря на задержку, ничего не держит.
 */
fun holdForSoftKeyboardHide(): ExitTransition =
    slideOut(tween(durationMillis = 1, delayMillis = SOFT_KEYBOARD_HIDE_SETTLE_MS)) { IntOffset(0, 1) }

/** Маркер курсора и выделение прозрачного цвета — чтобы спрятать их, не снимая фокус с поля. */
private val HiddenTextSelectionColors = TextSelectionColors(
    handleColor = Color.Transparent,
    backgroundColor = Color.Transparent
)

/** «Окно не в фокусе» для полей внутри: Compose не создаёт им маркеры курсора и лупу. */
private val UnfocusedWindowInfo = object : WindowInfo {
    override val isWindowFocused: Boolean
        get() = false
}

/**
 * Убирает у полей внутри [content] маркер курсора («каплю»), выделение и мигающий курсор, не трогая
 * фокус и сессию ввода.
 *
 * Нужен, пока закрывается редактор или окно с полем в фокусе: снять фокус до скрытия клавиатуры
 * нельзя (см. [hideSoftKeyboardNow]), а маркер рисуется в отдельном всплывающем окне. Пока поле
 * уезжает, Compose двигает это окно системным вызовом на каждом кадре и пересоздаёт его, когда
 * курсор выходит из видимой области и возвращается, — на HyperOS это 10–17 мс за раз прямо
 * посреди анимации. Поэтому полям подставляется «окно не в фокусе»: `CoreTextField` показывает
 * маркеры только при `LocalWindowInfo.current.isWindowFocused` и тогда убирает окно маркера один
 * раз, в начале закрытия. Сессия ввода от этого флага не зависит (проверено по foundation 1.7.2).
 * Прозрачные цвета — запасной вариант на случай, если маркер всё же будет показан.
 *
 * Провайдер остаётся в композиции всегда, меняются только значения: иначе при переключении
 * [hidden] поле пересоздалось бы и потеряло фокус.
 */
@Composable
fun HideTextSelectionHandles(hidden: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalTextSelectionColors provides if (hidden) HiddenTextSelectionColors else LocalTextSelectionColors.current,
        LocalWindowInfo provides if (hidden) UnfocusedWindowInfo else LocalWindowInfo.current,
        content = content
    )
}

/** Должна закончиться раньше, чем свернувшийся редактор (300 мс) покинет композицию. */
private const val IME_HIDE_DURATION_MS = 220L

/** Если система так и не сообщила об окончании анимации — считаем скрытие законченным. */
private const val IME_HIDE_TIMEOUT_MS = IME_HIDE_DURATION_MS * 4

private val imeHideInterpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)

// Состояние текущего управляемого скрытия. Только главный поток.
private val mainHandler = Handler(Looper.getMainLooper())
private var imeHideGeneration = 0
private var imeHideInFlight = false
private val pendingOnHidden = mutableListOf<() -> Unit>()
private val imeHideTimeout = Runnable { finishImeHide() }

private fun View.hideSoftKeyboard(onHidden: (() -> Unit)?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hideImeWithControlledAnimation(onHidden)) return
    if (mayHaveSoftKeyboard()) hideImeViaInputMethodManager()
    onHidden?.invoke()
}

/**
 * Может ли клавиатура быть на экране или вот-вот появиться. Если нет — вызов IMM не нужен: на HyperOS
 * даже холостой `hideSoftInputFromWindow` занимает ~4 мс главного потока, а зовётся он из обработчика
 * нажатия, с которого начинается сворачивание редактора.
 */
private fun View.mayHaveSoftKeyboard(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true
    if (rootWindowInsets?.isVisible(WindowInsets.Type.ime()) == true) return true
    // Поле с сессией ввода: клавиатуру могли запросить, но она ещё не начала появляться
    val imm = context.getSystemService(InputMethodManager::class.java) ?: return false
    return imm.isAcceptingText
}

private fun View.hideImeViaInputMethodManager() {
    val imm = context.getSystemService(InputMethodManager::class.java) ?: return
    imm.hideSoftInputFromWindow(windowToken, 0)
}

private fun finishImeHide() {
    mainHandler.removeCallbacks(imeHideTimeout)
    imeHideInFlight = false
    val callbacks = pendingOnHidden.toList()
    pendingOnHidden.clear()
    callbacks.forEach { it() }
}

@RequiresApi(Build.VERSION_CODES.R)
private fun View.hideImeWithControlledAnimation(onHidden: (() -> Unit)?): Boolean {
    // Тап вне редактора и начало сворачивания зовут скрытие дважды с разницей в пару кадров —
    // второй вызов не должен перехватывать уже идущую анимацию, только дождаться её конца.
    if (imeHideInFlight) {
        onHidden?.let(pendingOnHidden::add)
        return true
    }
    val insetsController = windowInsetsController ?: return false
    if (rootWindowInsets?.isVisible(WindowInsets.Type.ime()) != true) return false

    val generation = ++imeHideGeneration
    imeHideInFlight = true
    onHidden?.let(pendingOnHidden::add)
    mainHandler.postDelayed(imeHideTimeout, IME_HIDE_TIMEOUT_MS)
    insetsController.controlWindowInsetsAnimation(
        WindowInsets.Type.ime(),
        IME_HIDE_DURATION_MS,
        imeHideInterpolator,
        null,
        object : WindowInsetsAnimationControlListener {
            override fun onReady(controller: WindowInsetsAnimationController, types: Int) {
                val from = controller.currentInsets.bottom
                val to = controller.hiddenStateInsets.bottom
                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = IME_HIDE_DURATION_MS
                    interpolator = imeHideInterpolator
                    addUpdateListener { animator ->
                        if (!controller.isReady) {
                            animator.cancel()
                            return@addUpdateListener
                        }
                        val bottom = (from + (to - from) * (animator.animatedValue as Float)).roundToInt()
                        controller.setInsetsAndAlpha(Insets.of(0, 0, 0, bottom), 1f, animator.animatedFraction)
                    }
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            if (controller.isReady) controller.finish(false)
                        }
                    })
                    start()
                }
            }

            override fun onFinished(controller: WindowInsetsAnimationController) {
                if (generation == imeHideGeneration) finishImeHide()
            }

            override fun onCancelled(controller: WindowInsetsAnimationController?) {
                // Управление не выдали или его перехватили — прячем штатным путём,
                // иначе клавиатура могла бы вернуться в показанное состояние
                hideImeViaInputMethodManager()
                if (generation == imeHideGeneration) finishImeHide()
            }
        }
    )
    return true
}
