package com.example.ui.screens.home.inlineeditor.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.LocalOffer
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
import com.example.ui.theme.ThingsAnytimeTeal
import com.example.ui.theme.ThingsBlue

@Composable
fun InlineTagField(
    tagInput: String,
    onTagInputChange: (String) -> Unit,
    showTagHelper: Boolean,
    onShowTagHelperChange: (Boolean) -> Unit
) {
    val showPanel = showTagHelper || tagInput.isNotEmpty()

    val textPrimaryColor = Color(0xFF1C1C1E)
    val textSecondaryColor = Color(0xFF747679)
    val helperBgColor = Color(0xFFF2F2F7)
    val helperHintColor = Color(0xFF8E8E93)

    val tagFontSize = MaterialTheme.typography.bodyMedium.fontSize

    AnimatedVisibility(
        visible = showPanel,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, top = 10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(helperBgColor)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.LocalOffer,
                contentDescription = "Tags",
                tint = ThingsAnytimeTeal,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = tagInput,
                onValueChange = onTagInputChange,
                textStyle = TextStyle(fontSize = tagFontSize, color = textPrimaryColor),
                cursorBrush = SolidColor(ThingsBlue),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (tagInput.isEmpty()) {
                        Text(
                            "Tags (e.g., Work, Home)",
                            style = TextStyle(fontSize = tagFontSize, color = helperHintColor)
                        )
                    }
                    innerTextField()
                }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Tags",
                tint = textSecondaryColor.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(14.dp)
                    .clickable {
                        onTagInputChange("")
                        onShowTagHelperChange(false)
                    }
            )
        }
    }
}
