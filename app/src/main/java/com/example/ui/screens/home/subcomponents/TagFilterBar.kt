package com.example.ui.screens.home.subcomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.R
import com.example.domain.tag.TagFilter
import com.example.ui.theme.AppIcons
import com.example.ui.theme.ThingsIconSize
import com.example.ui.theme.ThingsSpacing
import com.example.ui.theme.ThingsTheme
import com.example.ui.theme.ThingsAlpha
import com.example.ui.theme.dimens
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.height

/**
 * Полоса включённого фильтра по тегам над списком, как в Things: зелёная плашка цвета бирки тега высотой и
 * скруглением с карточку задачи (чуть уже неё — отступ задаёт место вызова) — значок, выбранные теги через
 * запятую жирным (не вместились — многоточие) и ✕ в светлом круге, снимающий фильтр ([onClear]). Нажатие на
 * плашку снова открывает окно «Теги» ([onClick]).
 */
@Composable
fun TagFilterBar(
    filter: TagFilter,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = ThingsTheme.colors.tagChipText
    val label = if (filter.untagged) stringResource(R.string.tag_filter_no_tag) else filter.tags.joinToString(", ")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MaterialTheme.dimens.rowHeight)
            .clip(ThingsTheme.shapes.rowShape)
            .background(ThingsTheme.colors.tagChipBackground)
            .clickable(onClick = onClick)
            .padding(start = ThingsSpacing.M, end = ThingsSpacing.XS_PLUS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = AppIcons.Tag,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(ThingsIconSize.XS)
        )
        Spacer(modifier = Modifier.width(ThingsSpacing.S))
        Text(
            text = label,
            style = ThingsTheme.type.button.copy(color = textColor),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(ThingsSpacing.S))
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.cd_clear_tag_filter),
            tint = textColor,
            modifier = Modifier
                .clip(CircleShape)
                // Светлый круг под ✕, как в Things
                .background(ThingsTheme.colors.searchCard.copy(alpha = ThingsAlpha.HALF))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClear
                )
                .padding(ThingsSpacing.XS_PLUS)
                .size(ThingsIconSize.S)
        )
    }
}
