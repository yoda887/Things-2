package com.example.ui.screens.home.inlineeditor.dialogs

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
import com.example.ui.theme.ThingsUpcomingRed
import com.example.R

private val DialogBackgroundColor = Color(0xFF22242C)

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
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DialogBackgroundColor),
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
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(id = R.string.delete_task_confirm_message),
                    color = Color.LightGray,
                    fontSize = 14.sp,
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
                            color = Color.White
                        )
                    }
                    Button(
                        onClick = onConfirmDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = ThingsUpcomingRed)
                    ) {
                        Text(
                            text = stringResource(id = R.string.delete_task_confirm_delete),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
