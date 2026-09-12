package com.example.ui.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Insets
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsAnimationControlListener
import android.view.WindowInsetsAnimationController
import android.view.animation.PathInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
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
 * Фокус после вызова сразу не снимаем: он уйдёт вместе с полем ввода, когда редактор покинет
 * композицию, и тогда же Compose вызовет `restartInput`. К этому моменту клавиатура должна быть
 * скрыта системой полностью, а не только визуально: штатная анимация скрытия на HyperOS идёт
 * ~530 мс (визуально клавиатура уходит за ~200 мс, дальше тянется невидимый хвост), а редактор
 * сворачивается за 300 мс. `restartInput` в этом хвосте перерисовывает Gboard в более высокую
 * раскладку, и её верх — панель Gboard — выглядывает из-под края экрана полоской.
 * Поэтому на API 30+ ведём анимацию скрытия сами и укладываемся в [IME_HIDE_DURATION_MS].
 */
fun View.hideSoftKeyboardNow() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hideImeWithControlledAnimation()) return
    hideImeViaInputMethodManager()
}

/** Должна закончиться раньше, чем свернувшийся редактор (300 мс) покинет композицию. */
private const val IME_HIDE_DURATION_MS = 220L

private val imeHideInterpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)

/** Момент запуска текущего управляемого скрытия, 0 — скрытие не идёт. Только главный поток. */
private var imeHideStartedAt = 0L

private fun View.hideImeViaInputMethodManager() {
    val imm = context.getSystemService(InputMethodManager::class.java) ?: return
    imm.hideSoftInputFromWindow(windowToken, 0)
}

@RequiresApi(Build.VERSION_CODES.R)
private fun View.hideImeWithControlledAnimation(): Boolean {
    val now = SystemClock.uptimeMillis()
    // Тап вне редактора и начало сворачивания зовут скрытие дважды с разницей в пару кадров —
    // второй вызов не должен перехватывать уже идущую анимацию. Метка устаревает сама на случай,
    // если система не вызвала ни onFinished, ни onCancelled.
    if (imeHideStartedAt != 0L && now - imeHideStartedAt < IME_HIDE_DURATION_MS * 4) return true
    val insetsController = windowInsetsController ?: return false
    if (rootWindowInsets?.isVisible(WindowInsets.Type.ime()) != true) return false

    imeHideStartedAt = now
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
                imeHideStartedAt = 0L
            }

            override fun onCancelled(controller: WindowInsetsAnimationController?) {
                imeHideStartedAt = 0L
                // Управление не выдали или его перехватили — прячем штатным путём,
                // иначе клавиатура могла бы вернуться в показанное состояние
                hideImeViaInputMethodManager()
            }
        }
    )
    return true
}
