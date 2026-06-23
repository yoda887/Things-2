package com.example.ui.screens.home.subcomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * TSmartListRow: Supports localized smart category row item metrics with flexible 
 * counter badging.
 */

@Composable
fun SmartListRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    count: Int,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    isGrayCountAndNoBg: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(7.dp))
        Text(title, style = MaterialTheme.typography.displaySmall.copy(color = textPrimaryColor), modifier = Modifier.weight(1f))
        
        if (count > 0) {
            // [ИЗМЕНЕНИЕ]: Если флаг равен true (для Today и Inbox), рисуем цифру серого цвета и без фона
            if (isGrayCountAndNoBg) {
                Text(
                    count.toString(),
                    style = MaterialTheme.typography.displaySmall.copy(color = textSecondaryColor)
                )
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        count.toString(),
                        style = MaterialTheme.typography.displaySmall.copy(color = iconColor)
                    )
                }
            }
        }
    }
}
