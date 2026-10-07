package com.example.ui.screens.home.inlineeditor.components

import com.example.data.model.Item
import com.example.R
import androidx.compose.ui.res.stringResource
import com.example.ui.theme.ThingsTheme
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.Flag
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
import com.example.ui.theme.*

private val PRIORITY_ICON_SIZE = 14.dp

@Composable
fun InlinePriorityPanel(
    priority: Int,
    onPriorityChange: (Int) -> Unit,
    showPriorityHelper: Boolean,
    onShowPriorityHelperChange: (Boolean) -> Unit
) {
    val showPanel = showPriorityHelper || priority > 0

    val textPrimaryColor = ThingsTheme.colors.editorText // blackish font
    val textSecondaryColor = ThingsTheme.colors.textSecondary // dark grey font
    val helperBgColor = ThingsTheme.colors.searchField // panel background

    val priorityName = when (priority) {
        Item.PRIORITY_HIGH -> stringResource(R.string.priority_high)
        Item.PRIORITY_MEDIUM -> stringResource(R.string.priority_medium)
        Item.PRIORITY_LOW -> stringResource(R.string.priority_low)
        else -> stringResource(R.string.priority_none)
    }

    val priorityColor = when (priority) {
        Item.PRIORITY_HIGH -> ThingsTheme.colors.upcoming
        Item.PRIORITY_MEDIUM -> ThingsTheme.colors.today
        Item.PRIORITY_LOW -> ThingsTheme.colors.anytime
        else -> ThingsTheme.colors.textSecondary
    }

    AnimatedVisibility(
        visible = showPanel,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, top = 10.dp)
                .clip(ThingsTheme.shapes.smallShape)
                .background(helperBgColor)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (priority > 0) Icons.Filled.Flag else AppIcons.Deadline,
                contentDescription = stringResource(R.string.cd_priority),
                tint = priorityColor,
                modifier = Modifier.size(ThingsIconSize.XS)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = priorityName,
                style = ThingsTheme.type.caption.copy(color = textPrimaryColor),
                modifier = Modifier.weight(1f)
            )
            
            // Quick Action Buttons to choose priority level
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ui_no),
                    style = ThingsTheme.type.badge.copy(
                        color = if (priority == Item.PRIORITY_NONE) ThingsTheme.colors.accent else ThingsTheme.colors.someday,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .clickable { onPriorityChange(Item.PRIORITY_NONE) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
                Text(
                    text = stringResource(R.string.ui_low),
                    style = ThingsTheme.type.badge.copy(
                        color = if (priority == Item.PRIORITY_LOW) ThingsTheme.colors.anytime else ThingsTheme.colors.someday,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .clickable { onPriorityChange(Item.PRIORITY_LOW) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
                Text(
                    text = stringResource(R.string.ui_med),
                    style = ThingsTheme.type.badge.copy(
                        color = if (priority == Item.PRIORITY_MEDIUM) ThingsTheme.colors.today else ThingsTheme.colors.someday,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .clickable { onPriorityChange(Item.PRIORITY_MEDIUM) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
                Text(
                    text = stringResource(R.string.ui_high),
                    style = ThingsTheme.type.badge.copy(
                        color = if (priority == Item.PRIORITY_HIGH) ThingsTheme.colors.upcoming else ThingsTheme.colors.someday,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .clickable { onPriorityChange(Item.PRIORITY_HIGH) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.cd_close_priority),
                tint = textSecondaryColor.copy(alpha = ThingsAlpha.HINT),
                modifier = Modifier
                    .size(PRIORITY_ICON_SIZE)
                    .clickable {
                        onShowPriorityHelperChange(false)
                    }
            )
        }
    }
}
