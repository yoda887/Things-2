package com.example.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.delay

/**
 * Свернули клавиатуру — курсор уходит из поля, в котором стоял.
 *
 * Правка идёт до конца вместе с клавиатурой: пока курсор виден, поле считается редактируемым
 * (подсветка строки, скрытые разделители, незавершённый ввод), и оставлять его после того,
 * как печатать уже негде, неправильно.
 *
 * Вызывается один раз на окно: в корне приложения — на все поля основного экрана, и отдельно
 * внутри диалогов, потому что у них своё окно со своими отступами и своим фокусом.
 *
 * @param settleMillis сколько выждать после того, как клавиатура визуально ушла. В окне диалога
 *   она уходит штатной анимацией с невидимым хвостом (см. SOFT_KEYBOARD_SYSTEM_HIDE_SETTLE_MS):
 *   снятый в хвосте фокус вызывает restartInput, и Gboard, перерисовываясь, выглядывает из-под
 *   края экрана верхней полоской. Для диалогов — [IME_FOCUS_CLEAR_DIALOG_SETTLE_MS].
 */
@Composable
fun ClearFocusOnImeHidden(settleMillis: Long = IME_FOCUS_CLEAR_SETTLE_MS) {
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val imeInsets = WindowInsets.ime
    val imeVisible by remember(imeInsets, density) { derivedStateOf { imeInsets.getBottom(density) > 0 } }
    var imeWasVisible by remember { mutableStateOf(false) }

    LaunchedEffect(imeVisible) {
        if (imeVisible) {
            imeWasVisible = true
        } else if (imeWasVisible) {
            imeWasVisible = false
            // При переходе между полями клавиатура на мгновение скрывается — выжидаем,
            // чтобы не снять курсор на ровном месте
            delay(settleMillis)
            if (imeInsets.getBottom(density) == 0) focusManager.clearFocus()
        }
    }
}

/** Пауза перед снятием фокуса в окне приложения: клавиатуру там прячет управляемая анимация без хвоста. */
private const val IME_FOCUS_CLEAR_SETTLE_MS = 120L

/**
 * Пауза перед снятием фокуса в окне диалога: от конца видимой части штатной анимации скрытия
 * до её полного завершения (~530 мс от начала), с запасом.
 */
const val IME_FOCUS_CLEAR_DIALOG_SETTLE_MS = 450L
