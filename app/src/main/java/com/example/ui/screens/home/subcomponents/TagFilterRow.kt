package com.example.ui.screens.home.subcomponents

import com.example.ui.theme.ThingsStroke
import com.example.ui.theme.ThingsSpacing
import com.example.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ThingsTheme

private val TAG_CHIP_VERTICAL_PADDING = 5.dp

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
            .padding(vertical = ThingsSpacing.XS),
        horizontalArrangement = Arrangement.spacedBy(ThingsSpacing.XS_PLUS)
    ) {
        item {
            val isAllSelected = selectedTag == null
            Box(
                modifier = Modifier
                    .clip(ThingsTheme.shapes.tagFilterShape)
                    .background(if (isAllSelected) ThingsTheme.colors.accent else Color.Transparent)
                    .border(ThingsStroke.THIN, if (isAllSelected) ThingsTheme.colors.accent else dividerColor, ThingsTheme.shapes.tagFilterShape)
                    .clickable { onTagSelect(null) }
                    .padding(horizontal = ThingsSpacing.S_PLUS, vertical = TAG_CHIP_VERTICAL_PADDING)
            ) {
                Text(
                    text = stringResource(R.string.ui_all),
                    color = if (isAllSelected) ThingsTheme.colors.onAccent else textSecondaryColor,
                    style = ThingsTheme.type.captionStrong
                )
            }
        }

        items(allTags.toList()) { tag ->
            val isSelected = selectedTag == tag
            Box(
                modifier = Modifier
                    .clip(ThingsTheme.shapes.tagFilterShape)
                    .background(if (isSelected) ThingsTheme.colors.accent else Color.Transparent)
                    .border(ThingsStroke.THIN, if (isSelected) ThingsTheme.colors.accent else dividerColor, ThingsTheme.shapes.tagFilterShape)
                    .clickable { onTagSelect(if (isSelected) null else tag) }
                    .padding(horizontal = ThingsSpacing.S_PLUS, vertical = TAG_CHIP_VERTICAL_PADDING)
            ) {
                Text(
                    text = tag,
                    color = if (isSelected) ThingsTheme.colors.onAccent else textSecondaryColor,
                    style = ThingsTheme.type.caption
                )
            }
        }
    }
}
