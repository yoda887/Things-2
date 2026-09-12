package com.example.data.local

import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import com.example.data.model.Item
import java.util.Calendar

/**
 * Локальный источник данных для взаимодействия с календарем устройства.
 * Изолирует работу с [android.content.ContentResolver], [CalendarContract] и проверками
 * runtime-разрешений от репозитория и остальных архитектурных слоев.
 *
 * @property context Контекст приложения для доступа к ContentResolver и системным службам
 */
class DeviceCalendarDataSource(private val context: Context) {

    /**
     * Запрашивает события системного календаря за текущий день и на 1 год вперед (для отображения в днях и месяцах Upcoming).
     * Если разрешение не предоставлено или возникает ошибка, возвращает резервные/тестовые данные.
     *
     * @return [Result] со списком обнаруженных событий в виде объектов [Item]
     */
    suspend fun fetchLocalCalendarEvents(): Result<List<Item>> {
        // [ИЗМЕНЕНИЕ]: Выполняем проверку разрешений доступа к чтению календаря
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CALENDAR
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("DeviceCalendarDataSource", "Calendar access permission not granted.")
            return Result.success(getMockCalendarEvents())
        }
        try {
            val events = mutableListOf<Item>()
            
            // Вычисляем начало сегодняшнего дня в миллисекундах
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startDay = cal.timeInMillis
            
            // Вычисляем конец периода (1 год вперед для отображения событий во всех месяцах Upcoming)
            val calEnd = Calendar.getInstance()
            calEnd.add(Calendar.YEAR, 1)
            calEnd.set(Calendar.HOUR_OF_DAY, 23)
            calEnd.set(Calendar.MINUTE, 59)
            calEnd.set(Calendar.SECOND, 59)
            calEnd.set(Calendar.MILLISECOND, 999)
            val endDay = calEnd.timeInMillis

            Log.d("DeviceCalendarDataSource", "Fetching local calendar events between $startDay and $endDay")

            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            android.content.ContentUris.appendId(builder, startDay)
            android.content.ContentUris.appendId(builder, endDay)
            val uri = builder.build()

            val projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Events.ALL_DAY,
                CalendarContract.Calendars.CALENDAR_COLOR,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
            )

            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val startIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
                val allDayIdx = cursor.getColumnIndex(CalendarContract.Events.ALL_DAY)
                val calendarColorIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)
                val calendarDisplayNameIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)

                Log.d("DeviceCalendarDataSource", "Calendar query executed, cursor count: ${cursor.count}")

                while (cursor.moveToNext()) {
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else -1L
                    val title = if (titleIdx >= 0) cursor.getString(titleIdx) ?: "Event" else "Event"
                    val start = if (startIdx >= 0) cursor.getLong(startIdx) else 0L
                    val end = if (endIdx >= 0) cursor.getLong(endIdx) else 0L
                    val allDay = if (allDayIdx >= 0) cursor.getInt(allDayIdx) == 1 else false
                    val calendarColor = if (calendarColorIdx >= 0) cursor.getInt(calendarColorIdx) else null
                    val calendarDisplayName = if (calendarDisplayNameIdx >= 0) cursor.getString(calendarDisplayNameIdx) else null

                    Log.d("DeviceCalendarDataSource", "Found calendar event: ID=$id, Title='$title', Start=$start")

                    events.add(
                        Item(
                            id = "cal_$id",
                            type = 0,
                            title = title,
                            notes = "Local Calendar Event",
                            start = 1, // mapping onto today/upcoming smart list
                            status = 0,
                            cachedTags = "Calendar",
                            calendarColor = calendarColor,
                            calendarDisplayName = calendarDisplayName,
                            eventStartMillis = start,
                            isAllDay = allDay
                        )
                    )
                }
            } ?: Log.e("DeviceCalendarDataSource", "ContentResolver.query returned null for Calendar Instances")

            // Если список событий пуст, генерируем демонстрационные резервные данные
            if (events.isEmpty()) {
                events.addAll(getMockCalendarEvents())
            }

            Log.d("DeviceCalendarDataSource", "Fetched total ${events.size} local calendar events")
            return Result.success(events)
        } catch (e: SecurityException) {
            Log.w("DeviceCalendarDataSource", "SecurityException: Calendar access permission not granted.")
            return Result.success(getMockCalendarEvents())
        } catch (e: Exception) {
            Log.e("DeviceCalendarDataSource", "Failed to fetch local calendar events", e)
            return Result.failure(e)
        }
    }

    /**
     * Создает фиксированный набор тестовых (mock) событий для демонстрации в случае отсутствия прав доступа к календарю.
     */
    private fun getMockCalendarEvents(): List<Item> {
        val events = mutableListOf<Item>()
        val tom = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        
        val lydiaStart = Calendar.getInstance().apply {
            timeInMillis = tom.timeInMillis
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        events.add(
            Item(
                id = "cal_mock_lydia",
                type = 0,
                title = "Interview with Lydia",
                notes = "Local Calendar Event",
                start = 1,
                status = 0,
                cachedTags = "Calendar",
                calendarColor = android.graphics.Color.parseColor("#4CD964"),
                calendarDisplayName = "Work",
                eventStartMillis = lydiaStart,
                isAllDay = false
            )
        )

        val benefitsStart = Calendar.getInstance().apply {
            timeInMillis = tom.timeInMillis
            set(Calendar.HOUR_OF_DAY, 13)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        events.add(
            Item(
                id = "cal_mock_benefits",
                type = 0,
                title = "Benefits presentation",
                notes = "Local Calendar Event",
                start = 1,
                status = 0,
                cachedTags = "Calendar",
                calendarColor = android.graphics.Color.parseColor("#4CD964"),
                calendarDisplayName = "Work",
                eventStartMillis = benefitsStart,
                isAllDay = false
            )
        )

        val thur = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }

        val workHomeStart = Calendar.getInstance().apply {
            timeInMillis = thur.timeInMillis
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        events.add(
            Item(
                id = "cal_mock_work_home",
                type = 0,
                title = "Work from home",
                notes = "Local Calendar Event",
                start = 1,
                status = 0,
                cachedTags = "Calendar",
                calendarColor = android.graphics.Color.parseColor("#4CD964"),
                calendarDisplayName = "Personal",
                eventStartMillis = workHomeStart,
                isAllDay = true
            )
        )

        val confStart = Calendar.getInstance().apply {
            timeInMillis = thur.timeInMillis
            set(Calendar.HOUR_OF_DAY, 13)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        events.add(
            Item(
                id = "cal_mock_conf",
                type = 0,
                title = "Monthly conference call",
                notes = "Local Calendar Event",
                start = 1,
                status = 0,
                cachedTags = "Calendar",
                calendarColor = android.graphics.Color.parseColor("#4CD964"),
                calendarDisplayName = "Work",
                eventStartMillis = confStart,
                isAllDay = false
            )
        )

        val nextMonthCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 25) }
        val nextMonthEventStart = Calendar.getInstance().apply {
            timeInMillis = nextMonthCal.timeInMillis
            set(Calendar.HOUR_OF_DAY, 11)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        events.add(
            Item(
                id = "cal_mock_future_plan",
                type = 0,
                title = "Quarterly Strategy Review",
                notes = "Local Calendar Event",
                start = 1,
                status = 0,
                cachedTags = "Calendar",
                calendarColor = android.graphics.Color.parseColor("#4CD964"),
                calendarDisplayName = "Work",
                eventStartMillis = nextMonthEventStart,
                isAllDay = false
            )
        )

        return events
    }
}
