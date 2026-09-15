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
 */
@Composable
fun ClearFocusOnImeHidden() {
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
            delay(120)
            if (imeInsets.getBottom(density) == 0) focusManager.clearFocus()
        }
    }
}
