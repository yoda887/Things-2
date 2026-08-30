package com.example.ui.screens.home.inlineeditor.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
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
        val todayStartMonday = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (startDate != null && section == TaskSection.UPCOMING) {
            val targetStartMonday = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                timeInMillis = startDate
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            var diffWeeks = 0
            val tempCal = todayStartMonday.clone() as Calendar
            if (tempCal.before(targetStartMonday)) {
                while (tempCal.before(targetStartMonday)) {
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
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF22242C)
            ),
            modifier = Modifier
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    alpha = alpha
                )
                .width(336.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Top bar: "When?" and Close "✕" Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier.weight(5f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "When?",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF33353E))
                                .clickable { onDismissRequest() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 1. Today Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onStartDateChange(System.currentTimeMillis())
                            onSectionChange(TaskSection.TODAY)
                            onIsTonightChange(false)
                            onShowCalendarHelperChange(true)
                            onDismissRequest()
                        }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = AppIcons.Today,
                            contentDescription = "Today",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Today",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.weight(5f)
                    )
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (isTodayActive) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active Today",
                                tint = ThingsBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 2. This Evening Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onStartDateChange(System.currentTimeMillis())
                            onSectionChange(TaskSection.TODAY)
                            onIsTonightChange(true)
                            onShowCalendarHelperChange(true)
                            onDismissRequest()
                        }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = AppIcons.Evening,
                            contentDescription = "This Evening",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "This Evening",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.weight(5f)
                    )
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (isThisEveningActive) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active This Evening",
                                tint = ThingsBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Calendar section (Weekdays + 4 rows of dates) with tight vertical spacing
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Weekday headers: Mon Tue Wed Thu Fri Sat Sun
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    weekdays.forEach { dayName ->
                        Text(
                            text = dayName,
                            style = TextStyle(
                                color = Color(0xFF6C6F7D),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Calendar cells
                val todayCal = Calendar.getInstance()
                val todayStartMonday = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.MONDAY
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val cells = remember(calendarWeekOffset) {
                    val list = mutableListOf<CalendarCell>()
                    val tempCal = (todayStartMonday.clone() as Calendar).apply {
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
                                            tint = Color(0xFF8E8E93),
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
                                            tint = Color(0xFF8E8E93),
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
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                    Text(
                                                        text = cell.day.toString(),
                                                        style = TextStyle(
                                                            color = Color.White,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    )
                                                }
                                            } else if (cell.isToday) {
                                                Icon(
                                                    imageVector = Icons.Filled.Star,
                                                    contentDescription = "Today",
                                                    tint = Color(0xFF5F6368),
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
                                                                color = Color(0xFF9E9EA6),
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        )
                                                        Text(
                                                            text = cell.day.toString(),
                                                            style = TextStyle(
                                                                color = Color.White,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Normal
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
            }

            // 3. Someday Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onStartDateChange(null)
                            onSectionChange(TaskSection.SOMEDAY)
                            onIsTonightChange(false)
                            onShowCalendarHelperChange(true)
                            onDismissRequest()
                        }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = AppIcons.Someday,
                            contentDescription = "Someday",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Someday",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.weight(5f)
                    )
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (isSomedayActive) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active Someday",
                                tint = ThingsBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 4. Add Reminder
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFF6C6F7D),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Add Reminder",
                        style = TextStyle(
                            color = Color(0xFF6C6F7D),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        modifier = Modifier.weight(5f)
                    )
                    Box(modifier = Modifier.weight(1f))
                }

                // Кнопка "Clear" для сброса даты в "Anytime" (форма полной пилюли, малиново-красный цвет)
                val hasTimeAssignment = startDate != null || section != TaskSection.ANYTIME
                if (hasTimeAssignment) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onStartDateChange(null)
                            onSectionChange(TaskSection.ANYTIME)
                            onIsTonightChange(false)
                            onShowCalendarHelperChange(false)
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE22D5A),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(percent = 50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
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

