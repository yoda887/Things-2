package com.example.ui.components

import com.example.ui.theme.ThingsSpacing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.ThingsTheme

private val CONFIRM_DIALOG_WIDTH = 300.dp

/**
 * Единый диалог подтверждения (ConfirmDialog в брендбуке) — светлый, по теме приложения: фон `background`
 * (в тёмной теме — тёмный), радиус `dialogShape`, заголовок `dialogTitle` цветом `textPrimary`, пояснение
 * `dialogBody` цветом `textSecondary`, слева текстовая кнопка отмены, справа — капсула действия
 * (`danger` для удаления, иначе `accent`).
 *
 * Стандартный Material `AlertDialog` в приложении не используется — все подтверждения идут через этот диалог.
 */
@Composable
fun ThingsConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = ThingsTheme.shapes.dialogShape,
            colors = CardDefaults.cardColors(containerColor = ThingsTheme.colors.background),
            modifier = Modifier
                .width(CONFIRM_DIALOG_WIDTH)
                .padding(ThingsSpacing.L)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ThingsSpacing.L),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    color = ThingsTheme.colors.textPrimary,
                    style = ThingsTheme.type.dialogTitle,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(ThingsSpacing.M))
                Text(
                    text = message,
                    color = ThingsTheme.colors.textSecondary,
                    style = ThingsTheme.type.dialogBody,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(ThingsSpacing.L_PLUS))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = dismissText,
                            color = ThingsTheme.colors.textPrimary,
                            style = ThingsTheme.type.dialogButton
                        )
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (destructive) ThingsTheme.colors.danger else ThingsTheme.colors.accent
                        )
                    ) {
                        Text(
                            text = confirmText,
                            color = ThingsTheme.colors.onAccent,
                            style = ThingsTheme.type.dialogButton
                        )
                    }
                }
            }
        }
    }
}
