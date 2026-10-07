package com.example.ui.screens.home.subcomponents

import com.example.ui.theme.ThingsAlpha
import com.example.R
import androidx.compose.ui.res.stringResource
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
import com.example.ui.theme.ThingsTheme

private val EMPTY_STATE_ICON_SIZE = 60.dp

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
                contentDescription = stringResource(R.string.cd_empty),
                tint = textSecondaryColor.copy(alpha = ThingsAlpha.LOW),
                modifier = Modifier.size(EMPTY_STATE_ICON_SIZE)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.ui_all_clear_here_enjoy_your_day),
                color = textSecondaryColor,
                style = ThingsTheme.type.bodyMedium
            )
        }
    }
}
