package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Самый мягкий отклик: короткий слабый импульс вместо системного CLOCK_TICK.
 * На тестовом Xiaomi CLOCK_TICK играет заготовку TEXTURE_TICK средней силы, примитивов (LOW_TICK) нет,
 * но мотор умеет регулировать силу — поэтому импульс задаём сами: [SOFT_TICK_MS] мс на [SOFT_TICK_AMPLITUDE] из 255.
 * Категория TOUCH: система гасит/ослабляет его вместе с остальным откликом на касания.
 * Где сила не регулируется или Android старше 13 — обычный CLOCK_TICK.
 */
fun View.performSoftTick() {
    if (!isHapticFeedbackEnabled) return
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) context.vibrator() else null
    if (vibrator == null || !vibrator.hasAmplitudeControl()) {
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        return
    }
    vibrator.vibrate(
        VibrationEffect.createOneShot(SOFT_TICK_MS, SOFT_TICK_AMPLITUDE),
        VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
    )
}

private fun Context.vibrator(): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }?.takeIf { it.hasVibrator() }

private const val SOFT_TICK_MS = 10L
private const val SOFT_TICK_AMPLITUDE = 60
