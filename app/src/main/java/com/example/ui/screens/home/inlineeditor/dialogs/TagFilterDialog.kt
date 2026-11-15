package com.example.ui.screens.home.inlineeditor.dialogs

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.domain.tag.TagFilter
import com.example.ui.theme.AppIcons
import com.example.ui.theme.ThingsIconSize
import com.example.ui.theme.ThingsSpacing
import com.example.ui.theme.ThingsStroke
import com.example.ui.theme.ThingsTheme
import com.example.ui.theme.dimens

/** Ширина окна — как у «Когда?» и «Термін» */
private val TAG_FILTER_DIALOG_WIDTH = 336.dp
/** Высота окна — постоянная, как в Things: короткий список не сжимает его, длинный прокручивается */
private val TAG_FILTER_DIALOG_HEIGHT = 470.dp
private val TAG_FILTER_DIALOG_VERTICAL_PADDING = 18.dp
/** «Очистити» — как в окне «Термін» */
private val TAG_FILTER_CLEAR_HEIGHT = 40.dp
/** Окно появляется, вырастая из этой доли размера */
private const val TAG_FILTER_ENTER_SCALE = 0.85f

/**
 * Окно «Теги» фильтра списка, как «Filter by Tag» в Things: тёмная карточка с заголовком и ✕, теги этого списка
 * ([tags]) и «Без тегу». Нажатие отмечает или снимает тег — фильтр меняется сразу ([onFilterChange]), окно
 * остаётся открытым; «Без тегу» и теги взаимоисключающие ([TagFilter]). «Очистити» — только когда фильтр есть:
 * снимает его и закрывает окно.
 */
@Composable
fun TagFilterDialog(
    tags: List<String>,
    filter: TagFilter,
    onFilterChange: (TagFilter) -> Unit,
    onDismissRequest: () -> Unit
) {
    val view = LocalView.current
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else TAG_FILTER_ENTER_SCALE,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "tagFilterScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "tagFilterAlpha"
    )
    fun change(next: TagFilter) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        onFilterChange(next)
    }
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(
                shape = ThingsTheme.shapes.dialogShape,
                colors = CardDefaults.cardColors(containerColor = ThingsTheme.colors.overlaySurface),
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .width(TAG_FILTER_DIALOG_WIDTH)
                    .height(TAG_FILTER_DIALOG_HEIGHT)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = ThingsSpacing.L, vertical = TAG_FILTER_DIALOG_VERTICAL_PADDING)
                ) {
                    TagFilterHeader(onClose = onDismissRequest)
                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.dialogTitleGap))
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(ThingsSpacing.XS)
                    ) {
                        items(tags, key = { it }) { title ->
                            TagFilterRow(
                                title = title,
                                icon = AppIcons.Tag,
                                selectedIcon = AppIcons.TagFilled,
                                selected = filter.has(title),
                                onClick = { change(filter.toggle(title)) }
                            )
                        }
                        item(key = "no_tag") {
                            TagFilterRow(
                                title = stringResource(R.string.tag_filter_no_tag),
                                icon = AppIcons.Tag,
                                selectedIcon = AppIcons.TagFilled,
                                selected = filter.untagged,
                                crossed = true,
                                onClick = { change(filter.toggleUntagged()) }
                            )
                        }
                    }
                    if (filter.isActive) {
                        Spacer(modifier = Modifier.height(ThingsSpacing.M))
                        // Серая капсула, как «Clear» в Things: снять фильтр — не опасное действие
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(TAG_FILTER_CLEAR_HEIGHT)
                                .clip(ThingsTheme.shapes.capsuleShape)
                                .background(ThingsTheme.colors.overlayControl)
                                .clickable {
                                    change(TagFilter.NONE)
                                    onDismissRequest()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.cd_clear),
                                style = ThingsTheme.type.dialogButton.copy(color = ThingsTheme.colors.overlayContent)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Заголовок «Теги» по центру и ✕ справа — как у «Термін» */
@Composable
private fun TagFilterHeader(onClose: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.weight(5f), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.tag_filter_title),
                style = ThingsTheme.type.dialogTitle.copy(color = ThingsTheme.colors.overlayContent)
            )
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(MaterialTheme.dimens.dialogHeaderButtonSize)
                    .clip(CircleShape)
                    .background(ThingsTheme.colors.overlayControl)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_close),
                    tint = ThingsTheme.colors.overlayContent,
                    modifier = Modifier.size(ThingsIconSize.S)
                )
            }
        }
    }
}

/**
 * Строка тега — как в окне тегов задачи: отмеченный — значок залит цветом текста и синяя галочка справа.
 * [crossed] — значок перечёркнут («Без тегу»).
 */
@Composable
private fun TagFilterRow(
    title: String,
    icon: ImageVector,
    selectedIcon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    crossed: Boolean = false
) {
    val iconColor = if (selected) ThingsTheme.colors.overlayContent else ThingsTheme.colors.overlayContentSecondary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ThingsTheme.shapes.capsuleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = ThingsSpacing.XS, vertical = ThingsSpacing.S_PLUS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier
                .size(ThingsIconSize.S)
                .then(
                    if (crossed) Modifier.drawWithContent {
                        drawContent()
                        // Перечёркнутая бирка — «без тега»
                        drawLine(
                            color = iconColor,
                            start = Offset(size.width * 0.1f, size.height * 0.9f),
                            end = Offset(size.width * 0.9f, size.height * 0.1f),
                            strokeWidth = ThingsStroke.FOCUS.toPx()
                        )
                    } else Modifier
                )
        )
        Spacer(modifier = Modifier.width(ThingsSpacing.M))
        Text(
            text = title,
            style = ThingsTheme.type.dialogRow.copy(color = ThingsTheme.colors.overlayContent),
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.cd_selected),
                tint = ThingsTheme.colors.accent,
                modifier = Modifier.size(ThingsIconSize.S)
            )
        }
    }
}
