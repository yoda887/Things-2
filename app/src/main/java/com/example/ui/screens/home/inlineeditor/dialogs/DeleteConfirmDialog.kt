package com.example.ui.screens.home.inlineeditor.dialogs

import com.example.ui.theme.ThingsTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R

/**
 * Диалог подтверждения удаления задачи.
 * Вынесен из ThingsCategoryListPanel для соблюдения принципов декомпозиции и чистого кода.
 */
@Composable
fun DeleteConfirmDialog(
    onDismissRequest: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = ThingsTheme.shapes.dialogShape,
            colors = CardDefaults.cardColors(containerColor = ThingsTheme.colors.overlaySurface),
            modifier = Modifier
                .width(300.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.delete_task_confirm_title),
                    color = ThingsTheme.colors.overlayContent,
                    style = ThingsTheme.type.dialogTitle,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(id = R.string.delete_task_confirm_message),
                    color = ThingsTheme.colors.overlayContentSecondary,
                    style = ThingsTheme.type.dialogBody,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onDismissRequest
                    ) {
                        Text(
                            text = stringResource(id = R.string.delete_task_confirm_cancel),
                            color = ThingsTheme.colors.overlayContent,
                            style = ThingsTheme.type.dialogButton
                        )
                    }
                    Button(
                        onClick = onConfirmDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = ThingsTheme.colors.danger)
                    ) {
                        Text(
                            text = stringResource(id = R.string.delete_task_confirm_delete),
                            color = ThingsTheme.colors.overlayContent,
                            style = ThingsTheme.type.dialogButton
                        )
                    }
                }
            }
        }
    }
}
