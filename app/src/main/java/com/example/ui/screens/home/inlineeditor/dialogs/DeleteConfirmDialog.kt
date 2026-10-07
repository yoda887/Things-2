package com.example.ui.screens.home.inlineeditor.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.components.ThingsConfirmDialog

/**
 * Диалог подтверждения удаления задачи — общий тёмный диалог [ThingsConfirmDialog].
 */
@Composable
fun DeleteConfirmDialog(
    onDismissRequest: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    ThingsConfirmDialog(
        title = stringResource(id = R.string.delete_task_confirm_title),
        message = stringResource(id = R.string.delete_task_confirm_message),
        confirmText = stringResource(id = R.string.delete_task_confirm_delete),
        dismissText = stringResource(id = R.string.delete_task_confirm_cancel),
        onConfirm = onConfirmDelete,
        onDismiss = onDismissRequest
    )
}
