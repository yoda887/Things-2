package com.example.ui.screens.home.inlineeditor.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/** Перевод местного дня в полночь UTC календаря Material и обратно сохраняет календарный день. */
class DatePickerUtcTest {

    private val kyiv = TimeZone.getTimeZone("Europe/Kyiv")
    private val newYork = TimeZone.getTimeZone("America/New_York")
    private val utc = TimeZone.getTimeZone("UTC")

    private fun at(zone: TimeZone, y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0) =
        Calendar.getInstance(zone).apply { clear(); set(y, m, d, h, min) }.timeInMillis

    @Test
    fun nightInKyiv_staysTheSameDayInPicker() {
        // 9 октября, 1:25 по Киеву — в UTC ещё 8 октября; календарь должен отметить 9-е
        val night = at(kyiv, 2026, Calendar.OCTOBER, 9, 1, 25)
        assertEquals(at(utc, 2026, Calendar.OCTOBER, 9), localDayToUtcMidnight(night, kyiv))
    }

    @Test
    fun lateEveningInNewYork_staysTheSameDayInPicker() {
        // 9 октября, 22:00 в Нью-Йорке — в UTC уже 10 октября
        val evening = at(newYork, 2026, Calendar.OCTOBER, 9, 22)
        assertEquals(at(utc, 2026, Calendar.OCTOBER, 9), localDayToUtcMidnight(evening, newYork))
    }

    @Test
    fun pickedDay_becomesLocalMidnightOfThatDay() {
        val picked = at(utc, 2026, Calendar.OCTOBER, 20)
        assertEquals(at(kyiv, 2026, Calendar.OCTOBER, 20), utcMidnightToLocalDay(picked, kyiv))
        assertEquals(at(newYork, 2026, Calendar.OCTOBER, 20), utcMidnightToLocalDay(picked, newYork))
    }

    @Test
    fun storedDeadline_roundTripsToTheSameDay() {
        val stored = at(kyiv, 2026, Calendar.OCTOBER, 20)
        assertEquals(stored, utcMidnightToLocalDay(localDayToUtcMidnight(stored, kyiv), kyiv))
    }
}
