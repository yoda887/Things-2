package com.example.ui.screens.home.inlineeditor.dialogs

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.TaskSection
import com.example.ui.theme.ThingsBlue
import com.example.ui.theme.ThingsUpcomingRed
import com.example.ui.theme.AppIcons
import com.example.ui.screens.home.inlineeditor.utils.isPastDate
import com.example.ui.screens.home.inlineeditor.utils.isTodayDate
import com.example.ui.screens.home.inlineeditor.utils.isTodayDateOrPast
import com.example.ui.screens.home.inlineeditor.utils.isSameDay
import java.text.SimpleDateFormat
import java.util.*

sealed class CalendarCell {
    object Empty : CalendarCell()
    object PrevMonth : CalendarCell()
    object NextMonth : CalendarCell()
    data class Day(
        val day: Int,
        val isToday: Boolean,
        val monthLabel: String?,
        val timestamp: Long
    ) : CalendarCell()
}

@Composable
fun ThingsWhenDialog(
    startDate: Long?,
    onStartDateChange: (Long?) -> Unit,
    section: TaskSection,
    onSectionChange: (TaskSection) -> Unit,
    isTonight: Boolean,
    onIsTonightChange: (Boolean) -> Unit,
    onShowCalendarHelperChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit
) {
    var calendarWeekOffset by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val todayStartSunday = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.SUNDAY
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (startDate != null && section == TaskSection.UPCOMING) {
            val targetStartSunday = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.SUNDAY
                timeInMillis = startDate
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            var diffWeeks = 0
            val tempCal = todayStartSunday.clone() as Calendar
            if (tempCal.before(targetStartSunday)) {
                while (tempCal.before(targetStartSunday)) {
                    tempCal.add(Calendar.WEEK_OF_YEAR, 1)
                    diffWeeks++
                }
            }
            calendarWeekOffset = maxOf(0, diffWeeks)
        } else {
            calendarWeekOffset = 0
        }
    }

    val isTodayActive = (startDate == null || isTodayDateOrPast(startDate)) && section == TaskSection.TODAY && !isTonight
    val isThisEveningActive = (startDate == null || isTodayDateOrPast(startDate)) && section == TaskSection.TODAY && isTonight
    val isSomedayActive = startDate == null && section == TaskSection.SOMEDAY

    val titleFontSize = MaterialTheme.typography.headlineSmall.fontSize
    val notesFontSize = MaterialTheme.typography.titleSmall.fontSize
    val smallFontSize = MaterialTheme.typography.labelMedium.fontSize

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow
        ),
        label = "alpha"
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF22242C)
            ),
            modifier = Modifier
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    alpha = alpha
                )
                .width(320.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top bar: "When?" and "Cancel"
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "When?",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Text(
                        text = "Cancel",
                        style = TextStyle(
                            color = Color(0xFF8E8E93),
                            fontSize = notesFontSize,
                            fontWeight = FontWeight.Normal
                        ),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clickable { onDismissRequest() }
                    )
                }

                // 1. Today Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            // [ИЗМЕНЕНИЕ]: Нажатие назначает дату "Today" без сброса в "Anytime". Сброс даты теперь производится через кнопку "Clear".
                            onStartDateChange(System.currentTimeMillis())
                            onSectionChange(TaskSection.TODAY)
                            onIsTonightChange(false)
                            onShowCalendarHelperChange(true)
                            onDismissRequest()
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AppIcons.Today,
                        contentDescription = "Today",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(24.dp)
                    )
                    Text(
                        text = "Today",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = MaterialTheme.typography.titleLarge.fontSize,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (isTodayActive) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active Today",
                            tint = ThingsBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 2. This Evening Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            // [ИЗМЕНЕНИЕ]: Нажатие назначает категорию "This Evening" без сброса в "Anytime". Сброс даты производится через кнопку "Clear".
                            onStartDateChange(System.currentTimeMillis())
                            onSectionChange(TaskSection.TODAY)
                            onIsTonightChange(true)
                            onShowCalendarHelperChange(true)
                            onDismissRequest()
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Иконка "Вечер" с автоматическим подбором цвета и размера
                    Icon(
                        imageVector = AppIcons.Evening,
                        contentDescription = "This Evening",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(24.dp)
                    )
                    Text(
                        text = "This Evening",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = MaterialTheme.typography.titleLarge.fontSize,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (isThisEveningActive) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active This Evening",
                            tint = ThingsBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Weekday headers: "Sun Mon Tue Wed Thu Fri Sat"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    weekdays.forEach { dayName ->
                        Text(
                            text = dayName,
                            style = TextStyle(
                                color = Color(0xFF5F6368),
                                fontSize = smallFontSize,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Calendar cells
                val todayCal = Calendar.getInstance()
                val todayStartSunday = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.SUNDAY
                    set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val cells = remember(calendarWeekOffset) {
                    val list = mutableListOf<CalendarCell>()
                    val tempCal = (todayStartSunday.clone() as Calendar).apply {
                        add(Calendar.WEEK_OF_YEAR, calendarWeekOffset)
                    }
                    
                    for (i in 0 until 28) {
                        val dNum = tempCal.get(Calendar.DAY_OF_MONTH)
                        val cMonth = tempCal.get(Calendar.MONTH)
                        val cYear = tempCal.get(Calendar.YEAR)
                        
                        val isTod = (cYear == todayCal.get(Calendar.YEAR) &&
                                     cMonth == todayCal.get(Calendar.MONTH) &&
                                     dNum == todayCal.get(Calendar.DAY_OF_MONTH))
                                     
                        val mLabel = if (dNum == 1 || (i == 0 && calendarWeekOffset == 0) || (i == 1 && calendarWeekOffset > 0)) {
                            SimpleDateFormat("MMM", Locale.US).format(tempCal.time)
                        } else null
                        
                        val cTime = tempCal.timeInMillis
                        val isPast = isPastDate(cYear, cMonth, dNum, todayCal)
                        
                        if (i == 0 && calendarWeekOffset > 0) {
                            list.add(CalendarCell.PrevMonth)
                        } else if (i == 27) {
                            list.add(CalendarCell.NextMonth)
                        } else if (isPast) {
                            list.add(CalendarCell.Empty)
                        } else {
                            list.add(
                                CalendarCell.Day(
                                    day = dNum,
                                    isToday = isTod,
                                    monthLabel = mLabel,
                                    timestamp = cTime
                                )
                            )
                        }
                        tempCal.add(Calendar.DATE, 1)
                    }
                    list
                }

                // Display standard 4 rows of 7 items
                for (rowIdx in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (colIdx in 0 until 7) {
                            val cellIdx = rowIdx * 7 + colIdx
                            val cell = cells.getOrNull(cellIdx) ?: CalendarCell.Empty
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                when (cell) {
                                    is CalendarCell.Empty -> {
                                        // Empty past day
                                    }
                                    is CalendarCell.PrevMonth -> {
                                        Icon(
                                            imageVector = Icons.Default.ChevronLeft,
                                            contentDescription = "Previous Month",
                                            tint = ThingsBlue,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable {
                                                    calendarWeekOffset = maxOf(0, calendarWeekOffset - 3)
                                                }
                                        )
                                    }
                                    is CalendarCell.NextMonth -> {
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Next Month",
                                            tint = ThingsBlue,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable {
                                                    calendarWeekOffset += 3
                                                }
                                        )
                                    }
                                    is CalendarCell.Day -> {
                                        val isSelected = isSameDay(startDate, cell.timestamp) && section == TaskSection.UPCOMING
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(6.dp))
                                                .then(
                                                    if (isSelected) {
                                                        Modifier.border(1.5.dp, ThingsBlue, RoundedCornerShape(6.dp))
                                                    } else {
                                                        Modifier
                                                    }
                                                )
                                                .clickable {
                                                    // [ИЗМЕНЕНИЕ]: Выбор даты в календаре устанавливает дату без переключения в "Anytime". Сброс даты производится через кнопку "Clear".
                                                    onStartDateChange(cell.timestamp)
                                                    onSectionChange(if (isTodayDate(cell.timestamp)) TaskSection.TODAY else TaskSection.UPCOMING)
                                                    onIsTonightChange(false)
                                                    onShowCalendarHelperChange(true)
                                                    onDismissRequest()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    val monthLabel = SimpleDateFormat("MMM", Locale.US).format(Date(cell.timestamp))
                                                    Text(
                                                        text = monthLabel,
                                                        style = TextStyle(
                                                            color = ThingsBlue,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                    Text(
                                                        text = cell.day.toString(),
                                                        style = TextStyle(
                                                            color = Color.White,
                                                            fontSize = 18.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    )
                                                }
                                            } else if (cell.isToday) {
                                                Icon(
                                                    imageVector = Icons.Filled.Star,
                                                    contentDescription = "Today",
                                                    tint = Color(0xFF7E8494),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            } else {
                                                if (cell.monthLabel != null) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            text = cell.monthLabel,
                                                            style = TextStyle(
                                                                color = ThingsBlue,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        )
                                                        Text(
                                                            text = cell.day.toString(),
                                                            style = TextStyle(
                                                                color = Color.White,
                                                                fontSize = 18.sp,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        )
                                                    }
                                                } else {
                                                    Text(
                                                        text = cell.day.toString(),
                                                        style = TextStyle(
                                                            color = Color.White,
                                                            fontSize = 18.sp,
                                                            fontWeight = FontWeight.Normal
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Someday Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            // [ИЗМЕНЕНИЕ]: Выбор "Someday" устанавливает категорию "Someday" без переключения в "Anytime". Сброс даты производится через кнопку "Clear".
                            onStartDateChange(null)
                            onSectionChange(TaskSection.SOMEDAY)
                            onIsTonightChange(false)
                            onShowCalendarHelperChange(true)
                            onDismissRequest()
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AppIcons.Someday,
                        contentDescription = "Someday",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(24.dp)
                    )
                    Text(
                        text = "Someday",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (isSomedayActive) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active Someday",
                            tint = ThingsBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 4. Add Reminder
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF5F6368),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Add Reminder",
                        style = TextStyle(
                            color = Color(0xFF5F6368),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }

                // [ИЗМЕНЕНИЕ]: Если назначена дата или другая временная категория (Today, This Evening, Someday, Upcoming),
                // выводим широкую кнопку "Clear" в самом низу диалога для обнуления/сброса даты в "Anytime".
                val hasTimeAssignment = startDate != null || section != TaskSection.ANYTIME
                if (hasTimeAssignment) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            onStartDateChange(null)
                            onSectionChange(TaskSection.ANYTIME)
                            onIsTonightChange(false)
                            onShowCalendarHelperChange(false)
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThingsUpcomingRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Clear",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
