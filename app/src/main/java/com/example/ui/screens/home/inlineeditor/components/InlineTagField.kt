package com.example.ui.screens.home.inlineeditor.components

import com.example.ui.theme.ThingsIconSize
import com.example.ui.theme.ThingsAlpha
import com.example.R
import androidx.compose.ui.res.stringResource
import com.example.ui.theme.ThingsTheme
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import com.example.ui.theme.AppIcons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

private val TAG_FIELD_ICON_SIZE = 14.dp

@Composable
fun InlineTagField(
    tagInput: String,
    onTagInputChange: (String) -> Unit,
    showTagHelper: Boolean,
    onShowTagHelperChange: (Boolean) -> Unit
) {
    val showPanel = showTagHelper || tagInput.isNotEmpty()

    val textPrimaryColor = ThingsTheme.colors.editorText
    val textSecondaryColor = ThingsTheme.colors.textSecondary
    val helperBgColor = ThingsTheme.colors.searchField
    val helperHintColor = ThingsTheme.colors.textSecondary

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
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = AppIcons.Tag,
                contentDescription = stringResource(R.string.tag_dialog_title),
                tint = ThingsTheme.colors.anytime,
                modifier = Modifier.size(ThingsIconSize.XS)
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = tagInput,
                onValueChange = onTagInputChange,
                textStyle = ThingsTheme.type.bodyLarge.copy(color = textPrimaryColor),
                cursorBrush = SolidColor(ThingsTheme.colors.accent),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (tagInput.isEmpty()) {
                        Text(
                            stringResource(R.string.ui_tags_e_g_work_home),
                            style = ThingsTheme.type.bodyLarge.copy(color = helperHintColor)
                        )
                    }
                    innerTextField()
                }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.cd_close_tags),
                tint = textSecondaryColor.copy(alpha = ThingsAlpha.HINT),
                modifier = Modifier
                    .size(TAG_FIELD_ICON_SIZE)
                    .clickable {
                        onTagInputChange("")
                        onShowTagHelperChange(false)
                    }
            )
        }
    }
}
