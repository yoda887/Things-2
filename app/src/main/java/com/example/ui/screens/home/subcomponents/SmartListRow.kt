package com.example.ui.screens.home.subcomponents

import com.example.ui.theme.dimens
import com.example.ui.theme.ThingsSpacing
import com.example.ui.theme.ThingsIconSize
import com.example.ui.theme.ThingsAlpha
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

import com.example.ui.theme.ThingsTheme

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
            .clip(ThingsTheme.shapes.chipShape)
            .clickable { onClick() }
            .padding(vertical = ThingsSpacing.XS_PLUS, horizontal = ThingsSpacing.XS_PLUS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(ThingsIconSize.L))
        Spacer(modifier = Modifier.width(MaterialTheme.dimens.homeRowIconTextGap))
        Text(title, style = ThingsTheme.type.listTitle.copy(color = textPrimaryColor), modifier = Modifier.weight(1f))
        
        if (count > 0) {
            // [ИЗМЕНЕНИЕ]: Если флаг равен true (для Today и Inbox), рисуем цифру серого цвета и без фона
            if (isGrayCountAndNoBg) {
                Text(
                    count.toString(),
                    style = ThingsTheme.type.listTitle.copy(color = textSecondaryColor)
                )
            } else {
                Box(
                    modifier = Modifier
                        .clip(ThingsTheme.shapes.chipShape)
                        .background(iconColor.copy(alpha = ThingsAlpha.SUBTLE))
                        .padding(horizontal = ThingsSpacing.S, vertical = ThingsSpacing.XXS)
                ) {
                    Text(
                        count.toString(),
                        style = ThingsTheme.type.listTitle.copy(color = iconColor)
                    )
                }
            }
        }
    }
}
