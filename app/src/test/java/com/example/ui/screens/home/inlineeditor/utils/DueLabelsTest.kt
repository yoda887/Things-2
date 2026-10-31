package com.example.ui.screens.home.inlineeditor.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

/** Подпись дедлайна в списках: позже, чем через месяц, — датой, ближе — днями. */
class DueLabelsTest {

    private val today = LocalDate.of(2026, 10, 31)

    @Test
    fun showsDate_afterSameDayNextMonth() {
        // 31 октября + месяц = 30 ноября: до него включительно — днями, после — датой
        assertFalse(DueLabels.showsDate(today, LocalDate.of(2026, 11, 10)))
        assertFalse(DueLabels.showsDate(today, LocalDate.of(2026, 11, 30)))
        assertTrue(DueLabels.showsDate(today, LocalDate.of(2026, 12, 1)))
        assertFalse(DueLabels.showsDate(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 11, 15)))
        assertTrue(DueLabels.showsDate(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 11, 16)))
        // Просроченный — днями («просрочено»)
        assertFalse(DueLabels.showsDate(today, LocalDate.of(2026, 9, 1)))
    }

    @Test
    fun dateText_monthName_orDigits_yearIfOther() {
        assertEquals("5 Dec", DueLabels.dateText(LocalDate.of(2026, 12, 5), today, monthName = true, locale = Locale.ENGLISH))
        assertEquals("5 Jan 2027", DueLabels.dateText(LocalDate.of(2027, 1, 5), today, monthName = true, locale = Locale.ENGLISH))
        assertEquals("5.12.", DueLabels.dateText(LocalDate.of(2026, 12, 5), today, monthName = false, locale = Locale.ENGLISH))
        assertEquals("5.1.27", DueLabels.dateText(LocalDate.of(2027, 1, 5), today, monthName = false, locale = Locale.ENGLISH))
    }

    @Test
    fun yearOnly_afterFiveMonths() {
        // 31 октября + 5 месяцев = 31 марта: до него включительно — датой, после — только годом
        assertFalse(DueLabels.showsYearOnly(today, LocalDate.of(2027, 3, 31)))
        assertTrue(DueLabels.showsYearOnly(today, LocalDate.of(2027, 4, 1)))
    }

    @Test
    fun editorDate_weekdayThisYear_yearOtherYear() {
        assertEquals("Thu 10 Dec", DueLabels.editorDateText(LocalDate.of(2026, 12, 10), today, Locale.ENGLISH))
        assertEquals("1 Jan 2027", DueLabels.editorDateText(LocalDate.of(2027, 1, 1), today, Locale.ENGLISH))
    }
}
