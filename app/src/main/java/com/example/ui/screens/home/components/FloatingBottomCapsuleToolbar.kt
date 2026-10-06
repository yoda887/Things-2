package com.example.ui.screens.home.components

import com.example.ui.theme.ThingsTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import com.example.ui.theme.dimens

/**
 * Плавающий тулбар в виде капсулы, появляющийся плавно при раскрытии задачи.
 * Содержит быстрые действия: перемещение задачи, удаление и дополнительное меню.
 *
 * @param visible определяет видимость тулбара на экране.
 * @param onMoveClick обработчик события для перемещения задачи.
 * @param onDeleteClick обработчик события для удаления задачи.
 * @param onDuplicateClick обработчик события для дублирования задачи.
 * @param modifier модификатор макета для кастомизации расположения.
 */
@Composable
fun FloatingBottomCapsuleToolbar(
    visible: Boolean,
    onMoveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedDotsMenu by remember { mutableStateOf(false) }
    val view = LocalView.current

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
            .padding(bottom = MaterialTheme.dimens.floatingToolbarBottomPadding)
    ) {
        Box(
            modifier = Modifier
                .height(MaterialTheme.dimens.floatingToolbarHeight)
                .clip(ThingsTheme.shapes.toolbarShape)
                .background(ThingsTheme.colors.overlaySurface)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Кнопка перемещения
                Row(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(ThingsTheme.shapes.rowShape)
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onMoveClick()
                        }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Move icon",
                        tint = ThingsTheme.colors.overlayContent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Move",
                        color = ThingsTheme.colors.overlayContent,
                        style = ThingsTheme.type.dialogButton.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // 2. Кнопка удаления (Trash)
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(CircleShape)
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onDeleteClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete task",
                        tint = ThingsTheme.colors.overlayContent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 3. Кнопка "Еще" с выпадающим меню дополнительных опций
                Box {
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(CircleShape)
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                expandedDotsMenu = !expandedDotsMenu
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "More options",
                            tint = ThingsTheme.colors.overlayContent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = expandedDotsMenu,
                        onDismissRequest = { expandedDotsMenu = false },
                        modifier = Modifier.background(ThingsTheme.colors.overlaySurface)
                    ) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Duplicate icon",
                                    tint = ThingsTheme.colors.overlayContent
                                )
                            },
                            text = { Text("Duplicate", color = ThingsTheme.colors.overlayContent, fontWeight = FontWeight.Normal, style = ThingsTheme.type.menuItem) },
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onDuplicateClick()
                                expandedDotsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Repeat icon",
                                    tint = ThingsTheme.colors.overlayContentSecondary
                                )
                            },
                            text = { Text("Repeat", color = ThingsTheme.colors.overlayContentSecondary, fontWeight = FontWeight.Normal, style = ThingsTheme.type.menuItem) },
                            onClick = {
                                expandedDotsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Transform,
                                    contentDescription = "Convert icon",
                                    tint = ThingsTheme.colors.overlayContentSecondary
                                )
                            },
                            text = { Text("Convert", color = ThingsTheme.colors.overlayContentSecondary, fontWeight = FontWeight.Normal, style = ThingsTheme.type.menuItem) },
                            onClick = {
                                expandedDotsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share icon",
                                    tint = ThingsTheme.colors.overlayContentSecondary
                                )
                            },
                            text = { Text("Share", color = ThingsTheme.colors.overlayContentSecondary, fontWeight = FontWeight.Normal, style = ThingsTheme.type.menuItem) },
                            onClick = {
                                expandedDotsMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}
