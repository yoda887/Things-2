package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.RequiresApi

/**
 * Самый мягкий отклик: короткий слабый импульс вместо системного CLOCK_TICK.
 * На тестовом Xiaomi CLOCK_TICK играет заготовку TEXTURE_TICK средней силы, примитивов (LOW_TICK) нет,
 * но мотор умеет регулировать силу — поэтому импульс задаём сами: [SOFT_TICK_MS] мс на [SOFT_TICK_AMPLITUDE] из 255.
 * Категория TOUCH: система гасит/ослабляет его вместе с остальным откликом на касания.
 * Где сила не регулируется или Android старше 13 — обычный CLOCK_TICK.
 */
fun View.performSoftTick() {
    if (!isHapticFeedbackEnabled) return
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || !softTick(context)) {
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
}

/** Импульс заданной силы в категории TOUCH; false — мотора нет или он не регулирует силу. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun softTick(context: Context): Boolean {
    val vibrator = (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    if (vibrator == null || !vibrator.hasVibrator() || !vibrator.hasAmplitudeControl()) return false
    vibrator.vibrate(
        VibrationEffect.createOneShot(SOFT_TICK_MS, SOFT_TICK_AMPLITUDE),
        VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
    )
    return true
}

private const val SOFT_TICK_MS = 10L
private const val SOFT_TICK_AMPLITUDE = 60
