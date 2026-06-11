package com.example.ui.screens.home.subcomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ThingsBlue

/**
 * Строка фильтрации по тегам.
 */
@Composable
fun TagFilterRow(
    allTags: Set<String>,
    selectedTag: String?,
    textSecondaryColor: Color,
    dividerColor: Color,
    onTagSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            val isAllSelected = selectedTag == null
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isAllSelected) ThingsBlue else Color.Transparent)
                    .border(1.dp, if (isAllSelected) ThingsBlue else dividerColor, RoundedCornerShape(12.dp))
                    .clickable { onTagSelect(null) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "All",
                    color = if (isAllSelected) Color.White else textSecondaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        items(allTags.toList()) { tag ->
            val isSelected = selectedTag == tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) ThingsBlue else Color.Transparent)
                    .border(1.dp, if (isSelected) ThingsBlue else dividerColor, RoundedCornerShape(12.dp))
                    .clickable { onTagSelect(if (isSelected) null else tag) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = tag,
                    color = if (isSelected) Color.White else textSecondaryColor,
                    fontSize = 12.sp
                )
            }
        }
    }
}
