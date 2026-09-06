package com.example.ui.screens.home.subcomponents

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AppIcons
import com.example.ui.theme.dimens

/**
 * Плавающая нижняя панель действий (BatchActionToolbar) в стиле Things 3.
 * По стилю, анимации и тактильному отклику полностью унифицирована с FloatingBottomCapsuleToolbar.
 *
 * @param isVisible Флаг видимости панели.
 * @param selectedCount Количество выбранных элементов.
 * @param onWhenClick Обработчик нажатия на действие «When» (планирование даты).
 * @param onMoveClick Обработчик нажатия на действие «Move» (перемещение в проект/область).
 * @param onDeleteClick Обработчик нажатия на действие «Delete» (удаление выбранных задач).
 * @param onCompleteClick Обработчик нажатия на действие «Mark as Completed».
 * @param onSetTagsClick Обработчик нажатия на действие «Set Tags».
 * @param onSetDeadlineClick Обработчик нажатия на действие «Set Deadline».
 * @param onDuplicateClick Обработчик нажатия на действие «Duplicate».
 * @param onShareClick Обработчик нажатия на действие «Share».
 * @param modifier Модификатор для позиционирования тулбара.
 */
@Composable
fun BatchActionToolbar(
    isVisible: Boolean,
    selectedCount: Int,
    onWhenClick: () -> Unit,
    onMoveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCompleteClick: () -> Unit,
    onSetTagsClick: () -> Unit,
    onSetDeadlineClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMoreMenuExpanded by remember { mutableStateOf(false) }
    val isEnabled = selectedCount > 0
    val view = LocalView.current

    AnimatedVisibility(
        visible = isVisible,
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
                // 1. When (Календарь)
                CapsuleToolbarIconButton(
                    icon = AppIcons.Upcoming,
                    contentDescription = stringResource(R.string.batch_action_when),
                    enabled = isEnabled,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onWhenClick()
                    }
                )

                // 2. Move (Перемещение)
                CapsuleToolbarIconButton(
                    icon = Icons.Default.ArrowForward,
                    contentDescription = stringResource(R.string.batch_action_move),
                    enabled = isEnabled,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onMoveClick()
                    }
                )

                // 3. Delete (Корзина)
                CapsuleToolbarIconButton(
                    icon = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.batch_action_delete),
                    enabled = isEnabled,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onDeleteClick()
                    }
                )

                // 4. More (Три точки с выпадающим меню)
                Box {
                    CapsuleToolbarIconButton(
                        icon = Icons.Default.MoreHoriz,
                        contentDescription = stringResource(R.string.batch_action_more),
                        enabled = isEnabled,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            isMoreMenuExpanded = !isMoreMenuExpanded
                        }
                    )

                    DropdownMenu(
                        expanded = isMoreMenuExpanded,
                        onDismissRequest = { isMoreMenuExpanded = false },
                        modifier = Modifier.background(Color(0xFF22242C))
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.batch_action_complete), color = Color.White) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircleOutline,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                isMoreMenuExpanded = false
                                onCompleteClick()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.batch_action_set_tags), color = Color.White) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.LocalOffer,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                isMoreMenuExpanded = false
                                onSetTagsClick()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.batch_action_set_deadline), color = Color.White) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Flag,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                isMoreMenuExpanded = false
                                onSetDeadlineClick()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.batch_action_duplicate), color = Color.White) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                isMoreMenuExpanded = false
                                onDuplicateClick()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.batch_action_share), color = Color.White) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                isMoreMenuExpanded = false
                                onShareClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Круглая кнопка действия на капсульной панели тулбара.
 * Соответствует размеру и эффекту нажатия из FloatingBottomCapsuleToolbar.
 */
@Composable
private fun CapsuleToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false, radius = 24.dp)
            ) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(24.dp)
        )
    }
}
