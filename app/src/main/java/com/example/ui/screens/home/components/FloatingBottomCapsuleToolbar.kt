package com.example.ui.screens.home.components

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
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.compose.ui.unit.sp
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
                .clip(RoundedCornerShape(MaterialTheme.dimens.floatingToolbarCornerRadius))
                .background(Color(0xFF232329))
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
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onMoveClick()
                        }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Move icon",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Move",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
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
                        tint = Color.White,
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
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = expandedDotsMenu,
                        onDismissRequest = { expandedDotsMenu = false },
                        modifier = Modifier.background(Color(0xFF22242C))
                    ) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Duplicate icon",
                                    tint = Color.White
                                )
                            },
                            text = { Text("Duplicate", color = Color.White, fontWeight = FontWeight.Normal) },
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
                                    tint = Color.Gray
                                )
                            },
                            text = { Text("Repeat", color = Color.Gray, fontWeight = FontWeight.Normal) },
                            onClick = {
                                expandedDotsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Transform,
                                    contentDescription = "Convert icon",
                                    tint = Color.Gray
                                )
                            },
                            text = { Text("Convert", color = Color.Gray, fontWeight = FontWeight.Normal) },
                            onClick = {
                                expandedDotsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share icon",
                                    tint = Color.Gray
                                )
                            },
                            text = { Text("Share", color = Color.Gray, fontWeight = FontWeight.Normal) },
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
