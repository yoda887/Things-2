package com.example.ui.screens.home.subcomponents

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ThingsBlue
import com.example.ui.theme.ThingsBackgroundDark
import com.example.ui.theme.ThingsBackgroundLight

private val BACK_ICON_SIZE = 28.dp
private val OPTIONS_BOX_SIZE = 22.dp
private val OPTIONS_ICON_SIZE = 14.dp
private const val BACK_CONTENT_DESC = "Back"
private const val OPTIONS_CONTENT_DESC = "Options"

/**
 * Верхняя панель навигации (TopAppBar) для экрана категории.
 * Предоставляет кнопку возврата и иконку суб-опций.
 *
 * @param isDark Флаг темной темы для установки цвета контейнера.
 * @param textSecondaryColor Цвет текста и границ элементов управления.
 * @param onBackClick Обработчик нажатия на кнопку "Назад".
 * @param modifier Модификатор для внешнего контейнера.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListTopAppBar(
    isDark: Boolean,
    textSecondaryColor: Color,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = BACK_CONTENT_DESC,
                    tint = ThingsBlue,
                    modifier = Modifier.size(BACK_ICON_SIZE)
                )
            }
        },
        actions = {
            IconButton(
                onClick = {}
            ) {
                Box(
                    modifier = Modifier
                        .size(OPTIONS_BOX_SIZE)
                        .border(1.dp, textSecondaryColor.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = OPTIONS_CONTENT_DESC,
                        tint = textSecondaryColor,
                        modifier = Modifier.size(OPTIONS_ICON_SIZE)
                    )
                }
            }
        },
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (isDark) ThingsBackgroundDark else ThingsBackgroundLight
        ),
        modifier = modifier
    )
}
