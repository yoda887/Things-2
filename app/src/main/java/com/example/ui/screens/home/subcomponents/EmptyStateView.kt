package com.example.ui.screens.home.subcomponents

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Подкомпонент для отображения пустого состояния списка задач.
 * Спроектирован с соблюдением стандартов Material Design 3 и адаптивности.
 *
 * @param textSecondaryColor Цвет второстепенного текста для иконки и описания.
 * @param modifier Модификатор для настройки визуального контейнера.
 */
@Composable
fun EmptyStateView(
    textSecondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp)
            .testTag("empty_state_view"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.AssignmentTurnedIn,
                contentDescription = "Empty",
                tint = textSecondaryColor.copy(alpha = 0.3f),
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "All clear here! Enjoy your day.",
                color = textSecondaryColor,
                fontSize = 14.sp
            )
        }
    }
}
