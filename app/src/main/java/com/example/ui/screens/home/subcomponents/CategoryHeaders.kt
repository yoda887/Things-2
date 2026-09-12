package com.example.ui.screens.home.subcomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.ui.components.ProjectProgressArc
import com.example.ui.screens.home.components.TaskListKeys
import com.example.ui.screens.home.ActiveScreen
import com.example.ui.theme.*

/**
 * Компонент основного заголовка категории (Inbox, Today, Upcoming и т.д.).
 */
@Composable
fun MainCategoryHeader(
    screen: ActiveScreen,
    project: Item?,
    tasks: List<ItemWithChecklist>,
    area: Area?,
    scaleFactor: Float,
    textPrimaryColor: Color,
    globalDimAlpha: Float,
    onDeleteProject: (Item) -> Unit = {},
    onDeleteArea: (Area) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val headerEmojiFontSize = MaterialTheme.typography.displayMedium.fontSize
    val headerTitleFontSize = MaterialTheme.typography.displayLarge.fontSize

    var showOptionsMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var showAreaDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmDialog && project != null) {
        val taskCountInProj = tasks.count { it.item.projectId == project.id }
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Удалить проект?", fontWeight = FontWeight.Bold, color = textPrimaryColor) },
            text = {
                Text(
                    if (taskCountInProj > 0) {
                        "Вы действительно хотите удалить проект \"${project.name}\"? Проект удалится вместе с $taskCountInProj задачами."
                    } else {
                        "Вы действительно хотите удалить проект \"${project.name}\"?"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteProject(project)
                    }
                ) {
                    Text("Удалить", color = ThingsUpcomingRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Отмена", color = textPrimaryColor)
                }
            }
        )
    }

    if (showAreaDeleteConfirmDialog && area != null) {
        AlertDialog(
            onDismissRequest = { showAreaDeleteConfirmDialog = false },
            title = { Text("Удалить область?", fontWeight = FontWeight.Bold, color = textPrimaryColor) },
            text = {
                Text(
                    "Вы действительно хотите удалить область \"${area.title}\"?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAreaDeleteConfirmDialog = false
                        onDeleteArea(area)
                    }
                ) {
                    Text("Удалить", color = ThingsUpcomingRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAreaDeleteConfirmDialog = false }) {
                    Text("Отмена", color = textPrimaryColor)
                }
            }
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.dimens.mainHeaderPaddingTop, bottom = MaterialTheme.dimens.mainHeaderPaddingBottom)
            .graphicsLayer { alpha = globalDimAlpha },
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (screen) {
            ActiveScreen.TODAY -> {
                // Используем кастомную иконку AppIcons.Today вместо обычного эмодзи звезды
                Icon(
                    imageVector = AppIcons.Today,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size((32 * scaleFactor).dp)
                )
                Spacer(modifier = Modifier.width((10 * scaleFactor).dp))
                Text(
                    text = "Today",
                    style = TextStyle(
                        fontSize = headerTitleFontSize,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor
                    )
                )
            }
            ActiveScreen.INBOX -> {
                Icon(
                    // Используем кастомную иконку AppIcons.Inbox
                    imageVector = AppIcons.Inbox,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size((32 * scaleFactor).dp)
                )
                Spacer(modifier = Modifier.width((10 * scaleFactor).dp))
                Text(
                    text = "Inbox",
                    style = TextStyle(
                        fontSize = headerTitleFontSize,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor
                    )
                )
            }
            ActiveScreen.UPCOMING -> {
                Icon(
                    // Используем кастомную иконку AppIcons.Upcoming
                    imageVector = AppIcons.Upcoming,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size((32 * scaleFactor).dp)
                )
                Spacer(modifier = Modifier.width((10 * scaleFactor).dp))
                Text(
                    text = "Upcoming",
                    style = TextStyle(
                        fontSize = headerTitleFontSize,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor
                    )
                )
            }
            ActiveScreen.ANYTIME -> {
                Icon(
                    // Используем кастомную иконку AppIcons.Anytime
                    imageVector = AppIcons.Anytime,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size((32 * scaleFactor).dp)
                )
                Spacer(modifier = Modifier.width((10 * scaleFactor).dp))
                Text(
                    text = "Anytime",
                    style = TextStyle(
                        fontSize = headerTitleFontSize,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor
                    )
                )
            }
            ActiveScreen.SOMEDAY -> {
                Icon(
                    // Используем кастомную иконку AppIcons.Someday
                    imageVector = AppIcons.Someday,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size((32 * scaleFactor).dp)
                )
                Spacer(modifier = Modifier.width((10 * scaleFactor).dp))
                Text(
                    text = "Someday",
                    style = TextStyle(
                        fontSize = headerTitleFontSize,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor
                    )
                )
            }
            ActiveScreen.LOGBOOK -> {
                Icon(
                    // Используем кастомную иконку AppIcons.Logbook
                    imageVector = AppIcons.Logbook,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size((32 * scaleFactor).dp)
                )
                Spacer(modifier = Modifier.width((10 * scaleFactor).dp))
                Text(
                    text = "Logbook",
                    style = TextStyle(
                        fontSize = headerTitleFontSize,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor
                    )
                )
            }
            ActiveScreen.PROJECT_DETAIL -> {
                val completedCount = tasks.count { it.item.projectId == project?.id && it.item.isCompleted }
                val totalCount = tasks.count { it.item.projectId == project?.id }
                // Иконка проекта (ProgressArc) окрашена в синий цвет и выровнена по верхнему краю заголовка с компенсационным отступом
                ProjectProgressArc(
                    completed = completedCount,
                    total = totalCount,
                    color = ThingsBlue,
                    modifier = Modifier
                        .size((26 * scaleFactor).dp)
                        .align(Alignment.Top)
                        .padding(top = (4 * scaleFactor).dp)
                )
                Spacer(modifier = Modifier.width(12.dp))

                val projectNameText = project?.name ?: "Project"
                val projectTitleAnnotated = remember(projectNameText) {
                    buildAnnotatedString {
                        append(projectNameText)
                        append(" ")
                        appendInlineContent("project_options_icon", "[options]")
                    }
                }

                val inlineContentMap = mapOf(
                    "project_options_icon" to InlineTextContent(
                        Placeholder(
                            width = (headerTitleFontSize.value * 1.1f).sp,
                            height = headerTitleFontSize,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showOptionsMenu = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "Project Options",
                                tint = textPrimaryColor.copy(alpha = 0.45f),
                                modifier = Modifier.size((headerTitleFontSize.value * 0.88f).dp)
                            )

                            DropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = { showOptionsMenu = false },
                                modifier = Modifier.background(Color(0xFF22242C))
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = ThingsUpcomingRed,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = "Delete Project",
                                            color = ThingsUpcomingRed,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 17.sp
                                        )
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        showDeleteConfirmDialog = true
                                    }
                                )
                            }
                        }
                    }
                )

                Text(
                    text = projectTitleAnnotated,
                    inlineContent = inlineContentMap,
                    style = TextStyle(
                        fontSize = headerTitleFontSize,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor
                    ),
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            ActiveScreen.AREA_DETAIL -> {
                Icon(
                    imageVector = AppIcons.Area,
                    contentDescription = null,
                    tint = ThingsLogbookGreen,
                    modifier = Modifier
                        .size((30 * scaleFactor).dp)
                        .align(Alignment.Top)
                        .padding(top = (2 * scaleFactor).dp)
                )
                Spacer(modifier = Modifier.width(12.dp))

                val areaTitleText = area?.title ?: "Responsibility Area"
                val areaTitleAnnotated = remember(areaTitleText) {
                    buildAnnotatedString {
                        append(areaTitleText)
                        append(" ")
                        appendInlineContent("area_options_icon", "[options]")
                    }
                }

                var showAreaOptionsMenu by remember { mutableStateOf(false) }

                val areaInlineContentMap = mapOf(
                    "area_options_icon" to InlineTextContent(
                        Placeholder(
                            width = (headerTitleFontSize.value * 1.1f).sp,
                            height = headerTitleFontSize,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showAreaOptionsMenu = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "Area Options",
                                tint = textPrimaryColor.copy(alpha = 0.45f),
                                modifier = Modifier.size((headerTitleFontSize.value * 0.88f).dp)
                            )

                            DropdownMenu(
                                expanded = showAreaOptionsMenu,
                                onDismissRequest = { showAreaOptionsMenu = false },
                                modifier = Modifier.background(Color(0xFF22242C))
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = ThingsUpcomingRed,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = "Delete Area",
                                            color = ThingsUpcomingRed,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 17.sp
                                        )
                                    },
                                    onClick = {
                                        showAreaOptionsMenu = false
                                        showAreaDeleteConfirmDialog = true
                                    }
                                )
                            }
                        }
                    }
                )

                Text(
                    text = areaTitleAnnotated,
                    inlineContent = areaInlineContentMap,
                    style = TextStyle(
                        fontSize = headerTitleFontSize,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor
                    ),
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            else -> {}
        }
    }
}

/**
 * Вспомогательный заголовок для подразделов ("This Evening", "TASKS", "PROJECTS").
 */
@Composable
fun SubCategoryHeader(
    headerText: String,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    dividerColor: Color,
    dimAlpha: Float,
    modifier: Modifier = Modifier
) {
    if (headerText == TaskListKeys.PROJECTS_HEADING) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = dimAlpha }
                .padding(top = 22.dp, bottom = 10.dp)
        ) {
            Text(
                text = "PROJECTS",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondaryColor.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(dividerColor)
            )
        }
    } else if (headerText == TaskListKeys.TASKS_HEADING) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = dimAlpha }
                .padding(top = 22.dp, bottom = 10.dp)
        ) {
            Text(
                text = "TASKS",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondaryColor.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(dividerColor)
            )
        }
    } else {
        Column(
            modifier = modifier
                .graphicsLayer { alpha = dimAlpha }
        ) {
            // Свободное пространство сверху увеличено на 16.dp по запросу пользователя.
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.eveningSectionSpacing + 16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Иконка "Вечер" с автоматическим подбором цвета и размера
                Icon(
                    imageVector = AppIcons.Evening,
                    contentDescription = "This Evening",
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(20.dp)
                )
                // Увеличенный размер шрифта для заголовка "This Evening" до 20.sp по запросу пользователя.
                Text(
                    "This Evening",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor
                    )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(dividerColor)
                    .padding(bottom = 6.dp)
            )
            // Свободное пространство в 8.dp снизу от разделительной линии по запросу пользователя.
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Заголовок с датой на экране предстоящих событий (Upcoming). Renders tomorrow, day labels, divider line.
 */
@Composable
fun UpcomingDateHeader(
    dayOfMonth: String,
    dayOfWeekLabel: String,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    dividerColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 36.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = dayOfMonth,
            style = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimaryColor
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.Bottom)
                .padding(bottom = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.6.dp)
                    .background(dividerColor)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dayOfWeekLabel,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondaryColor.copy(alpha = 0.5f)
                )
            )
        }
    }
}

/**
 * Заголовок месяца на экране «Предстоящие» (Upcoming) для событий и задач позже 14 дней.
 */
@Composable
fun UpcomingMonthHeader(
    monthLabel: String,
    textPrimaryColor: Color,
    dividerColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.6.dp)
                .background(dividerColor)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = monthLabel,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimaryColor
            )
        )
    }
}
