package com.example.domain.repeat

import com.example.data.model.ChecklistItem
import com.example.data.model.Item
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/** Повторяющиеся задачи: правило, даты повторов, какие копии создать и как они выглядят. */
/** Часовой пояс тестов: «Europe/Kiev» — тот же пояс, что Europe/Kyiv, но есть и в старых базах поясов (Java десктопа) */
private const val TEST_ZONE = "Europe/Kiev"

class RepeatTest {

    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)
    private val wed = d(2026, 10, 14)  // среда

    private fun dates(rule: RepeatRule, from: LocalDate, n: Int): List<LocalDate> {
        val out = mutableListOf<LocalDate>()
        var cur: LocalDate? = from
        while (out.size < n) {
            cur = RepeatDates.nextOccurrence(rule, cur!!) ?: break
            out += cur
            cur = cur.plusDays(1)
        }
        return out
    }

    @Test
    fun encodeDecode_roundTrip() {
        val rules = listOf(
            RepeatRule(afterCompletion = true, interval = 2, unit = RepeatUnit.WEEK, start = wed),
            RepeatRule(
                afterCompletion = false, interval = 1, unit = RepeatUnit.YEAR, start = wed,
                on = listOf(RepeatOn(RepeatDay.OfMonth(14), 2), RepeatOn(RepeatDay.Weekday(1, 1), 11), RepeatOn(RepeatDay.OfMonth(RepeatDay.LAST))),
                endDate = d(2027, 12, 31), endCount = 5, deadlineDaysBefore = 2
            )
        )
        rules.forEach { assertEquals(it, RepeatRule.decode(it.encode())) }
        assertNull(RepeatRule.decode("garbage"))
    }

    @Test
    fun daily_everyTwoDays_fromStart() {
        val rule = RepeatRule(afterCompletion = false, interval = 2, unit = RepeatUnit.DAY, start = wed)
        assertEquals(listOf(d(2026, 10, 14), d(2026, 10, 16), d(2026, 10, 18)), dates(rule, d(2026, 10, 1), 3))
    }

    @Test
    fun weekly_selectedDays_everyOtherWeek() {
        val days = listOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY).map { RepeatOn(RepeatDay.Weekday(it.value)) }
        val weekly = RepeatRule(afterCompletion = false, unit = RepeatUnit.WEEK, start = wed, on = days)
        assertEquals(listOf(d(2026, 10, 14), d(2026, 10, 17), d(2026, 10, 20), d(2026, 10, 21)), dates(weekly, wed, 4))
        // Каждые 2 недели: неделя с 19-го пропускается целиком
        val biweekly = weekly.copy(interval = 2)
        assertEquals(listOf(d(2026, 10, 14), d(2026, 10, 17), d(2026, 10, 27), d(2026, 10, 28)), dates(biweekly, wed, 4))
        // Без дней — тот же день недели, что у начала
        val plain = RepeatRule(afterCompletion = false, unit = RepeatUnit.WEEK, start = wed)
        assertEquals(listOf(d(2026, 10, 14), d(2026, 10, 21)), dates(plain, wed, 2))
    }

    @Test
    fun monthly_31st_isLastDayInShortMonths() {
        val rule = RepeatRule(afterCompletion = false, unit = RepeatUnit.MONTH, start = d(2027, 1, 31))
        assertEquals(listOf(d(2027, 1, 31), d(2027, 2, 28), d(2027, 3, 31), d(2027, 4, 30)), dates(rule, d(2027, 1, 1), 4))
        val leap = RepeatRule(afterCompletion = false, unit = RepeatUnit.MONTH, start = d(2028, 1, 1), on = listOf(RepeatOn(RepeatDay.OfMonth(RepeatDay.LAST))))
        assertEquals(listOf(d(2028, 1, 31), d(2028, 2, 29)), dates(leap, d(2028, 1, 1), 2))
    }

    @Test
    fun monthly_nthAndLastWeekday() {
        val firstMonday = RepeatRule(afterCompletion = false, unit = RepeatUnit.MONTH, start = wed, on = listOf(RepeatOn(RepeatDay.Weekday(1, 1))))
        assertEquals(listOf(d(2026, 11, 2), d(2026, 12, 7)), dates(firstMonday, wed, 2))
        val lastFriday = RepeatRule(afterCompletion = false, unit = RepeatUnit.MONTH, start = wed, on = listOf(RepeatOn(RepeatDay.Weekday(5, RepeatDay.LAST))))
        assertEquals(listOf(d(2026, 10, 30), d(2026, 11, 27)), dates(lastFriday, wed, 2))
        // Каждые 3 месяца: октябрь, январь
        val quarterly = RepeatRule(afterCompletion = false, interval = 3, unit = RepeatUnit.MONTH, start = wed)
        assertEquals(listOf(d(2026, 10, 14), d(2027, 1, 14)), dates(quarterly, wed, 2))
    }

    @Test
    fun yearly_severalDays() {
        val rule = RepeatRule(
            afterCompletion = false, unit = RepeatUnit.YEAR, start = wed,
            on = listOf(RepeatOn(RepeatDay.OfMonth(14), 2), RepeatOn(RepeatDay.Weekday(1, 1), 11))
        )
        assertEquals(listOf(d(2026, 11, 2), d(2027, 2, 14), d(2027, 11, 1)), dates(rule, wed, 3))
    }

    @Test
    fun endDate_stopsRepeats() {
        val rule = RepeatRule(afterCompletion = false, unit = RepeatUnit.DAY, start = wed, endDate = d(2026, 10, 15))
        assertEquals(listOf(d(2026, 10, 14), d(2026, 10, 15)), dates(rule, wed, 5))
    }

    @Test
    fun due_regular_stacksMissedDays_andHonorsCount() {
        val rule = RepeatRule(afterCompletion = false, unit = RepeatUnit.DAY, start = wed, endCount = 3)
        val state = RepeatPlan.start(rule, wed)
        assertEquals(wed, state.nextOccurrence)
        // Приложение не открывали 4 дня: подошедшие копии копятся, но не больше числа повторов
        val (created, after) = RepeatPlan.due(state, d(2026, 10, 18))
        assertEquals(listOf(d(2026, 10, 14), d(2026, 10, 15), d(2026, 10, 16)), created)
        assertEquals(3, after.generated)
        assertNull(after.nextOccurrence)
        assertTrue(RepeatPlan.due(after, d(2026, 10, 30)).first.isEmpty())
    }

    @Test
    fun due_catchUpIsLimited() {
        val rule = RepeatRule(afterCompletion = false, unit = RepeatUnit.DAY, start = wed)
        val (created, after) = RepeatPlan.due(RepeatPlan.start(rule, wed), wed.plusDays(100))
        assertEquals(RepeatPlan.MAX_CATCH_UP, created.size)
        assertEquals(wed.plusDays(RepeatPlan.MAX_CATCH_UP.toLong()), after.nextOccurrence)
    }

    @Test
    fun due_pausedCreatesNothing() {
        val rule = RepeatRule(afterCompletion = false, unit = RepeatUnit.DAY, start = wed)
        val paused = RepeatPlan.start(rule, wed).copy(active = false)
        assertTrue(RepeatPlan.due(paused, wed.plusDays(3)).first.isEmpty())
    }

    @Test
    fun afterCompletion_firstCopyToday_nextAfterCheckingOff() {
        val rule = RepeatRule(afterCompletion = true, interval = 1, unit = RepeatUnit.WEEK, start = wed)
        val (first, waiting) = RepeatPlan.due(RepeatPlan.start(rule, wed), wed)
        assertEquals(listOf(wed), first)
        assertNull(waiting.nextOccurrence)
        // Отметили в субботу — следующая копия через неделю, в следующую субботу
        val next = RepeatPlan.completed(waiting, d(2026, 10, 17))
        assertEquals(d(2026, 10, 24), next.nextOccurrence)
        assertTrue(RepeatPlan.due(next, d(2026, 10, 23)).first.isEmpty())
        assertEquals(listOf(d(2026, 10, 24)), RepeatPlan.due(next, d(2026, 10, 24)).first)
    }

    @Test
    fun deadline_copyAppearsEarlier_withDueOnRepeatDay() {
        val rule = RepeatRule(afterCompletion = false, unit = RepeatUnit.MONTH, start = d(2026, 10, 15), deadlineDaysBefore = 5)
        val state = RepeatPlan.start(rule, d(2026, 10, 1))
        assertTrue(RepeatPlan.due(state, d(2026, 10, 9)).first.isEmpty())
        assertEquals(listOf(d(2026, 10, 15)), RepeatPlan.due(state, d(2026, 10, 10)).first)

        val zone = ZoneId.of(TEST_ZONE)
        val template = Item(id = "tpl", type = Item.TYPE_TEMPLATE, title = "Bill", notes = "n", cachedTags = "home", projectId = "p", reminderTime = 9 * 60)
        val checklist = listOf(ChecklistItem(id = "c", itemId = "tpl", title = "a", isCompleted = true))
        var n = 0
        val (copy, copyChecklist) = RepeatPlan.copyOf(template, checklist, rule, d(2026, 10, 15), zone, now = 1L) { "id${n++}" }
        assertEquals(Item.TYPE_TASK, copy.type)
        assertEquals("tpl", copy.repeatTemplateId)
        assertEquals("Bill", copy.title)
        assertEquals("p", copy.projectId)
        assertEquals(9 * 60, copy.reminderTime)
        assertEquals(d(2026, 10, 10).atStartOfDay(zone).toInstant().toEpochMilli(), copy.startDate)
        assertEquals(d(2026, 10, 15).atStartOfDay(zone).toInstant().toEpochMilli(), copy.dueDate)
        assertEquals(copy.id, copyChecklist.single().itemId)
        assertFalse(copyChecklist.single().isCompleted)
    }

    @Test
    fun copyIds_sameOnEveryDevice_differentPerOccurrence() {
        // Два устройства создают копию одного повтора независимо — id совпадают, синхронизация сводит их в одну запись
        val rule = RepeatRule(afterCompletion = false, unit = RepeatUnit.WEEK, start = d(2026, 10, 15))
        val template = Item(id = "tpl", type = Item.TYPE_TEMPLATE, title = "Bill")
        val checklist = listOf(ChecklistItem(id = "c", itemId = "tpl", title = "a"))
        val (onPhone, phoneChecklist) = RepeatPlan.copyOf(template, checklist, rule, d(2026, 10, 15), now = 1L)
        val (onDesktop, desktopChecklist) = RepeatPlan.copyOf(template, checklist, rule, d(2026, 10, 15), now = 2L)
        assertEquals(onPhone.id, onDesktop.id)
        assertEquals(phoneChecklist.single().id, desktopChecklist.single().id)
        assertTrue(onPhone.id != phoneChecklist.single().id)
        val (nextWeek, _) = RepeatPlan.copyOf(template, checklist, rule, d(2026, 10, 22), now = 1L)
        assertTrue(nextWeek.id != onPhone.id)
        assertTrue(RepeatIds.of("other", d(2026, 10, 15)) != onPhone.id)
    }

    @Test
    fun copyIds_dayAlreadyHasCopy_randomIdsSoOldCopyIsKept() {
        // Повтор включили заново или перенесли на день, где копия уже есть, — новая копия её не затирает
        val day = d(2026, 10, 15)
        val seeded = RepeatIds.generator("tpl", day, taken = false)
        assertEquals(RepeatIds.of("tpl", day), seeded(""))
        val fresh = RepeatIds.generator("tpl", day, taken = true)
        val first = fresh("")
        assertTrue(first != RepeatIds.of("tpl", day))
        assertTrue(first != fresh(""))
    }

    @Test
    fun setup_taskBecomesTemplate_copiesOnlyFromRegularTasks() {
        val task = Item(id = "t", title = "Water", start = Item.START_TODAY, startDate = 5L, dueDate = 7L, isTonight = true, cachedTags = "home")
        assertTrue(RepeatSetup.canRepeat(task))
        assertFalse(RepeatSetup.canRepeat(task.copy(repeatTemplateId = "tpl")))
        assertFalse(RepeatSetup.canRepeat(task.copy(type = Item.TYPE_PROJECT)))
        assertFalse(RepeatSetup.canRepeat(task.copy(status = Item.STATUS_COMPLETED)))
        val template = RepeatSetup.template(task, reminderTime = 9 * 60, now = 1L)
        assertEquals(Item.TYPE_TEMPLATE, template.type)
        assertEquals("t", template.id)
        assertEquals("Water", template.title)
        assertEquals("home", template.cachedTags)
        assertNull(template.startDate)
        assertNull(template.dueDate)
        assertFalse(template.isTonight)
        assertEquals(9 * 60, template.reminderTime)
    }

    @Test
    fun control_pauseResume_skipsMissedDays() {
        val rule = RepeatRule(afterCompletion = false, unit = RepeatUnit.DAY, start = wed)
        val paused = RepeatControl.paused(RepeatPlan.start(rule, wed))
        assertFalse(paused.active)
        val resumed = RepeatControl.resumed(paused, d(2026, 10, 20))
        assertTrue(resumed.active)
        assertEquals(d(2026, 10, 20), resumed.nextOccurrence)
    }

    @Test
    fun control_nextCopy_aheadOfTime() {
        val weekly = RepeatRule(afterCompletion = false, unit = RepeatUnit.WEEK, start = wed, endCount = 2)
        val (first, s1) = RepeatControl.nextCopy(RepeatPlan.start(weekly, wed), wed)!!
        assertEquals(wed, first)
        assertEquals(d(2026, 10, 21), s1.nextOccurrence)
        val (second, s2) = RepeatControl.nextCopy(s1, wed)!!
        assertEquals(d(2026, 10, 21), second)
        assertNull(s2.nextOccurrence)
        assertNull(RepeatControl.nextCopy(s2, wed))
        // «После выполнения» в ожидании — на интервал от сегодня
        val after = RepeatRule(afterCompletion = true, interval = 3, unit = RepeatUnit.DAY, start = wed)
        assertEquals(d(2026, 10, 17), RepeatControl.nextCopy(RepeatState(after, null, 1), wed)!!.first)
    }

    @Test
    fun control_stopAndNewRule() {
        val template = Item(id = "t", type = Item.TYPE_TEMPLATE, title = "x")
        assertEquals(Item.TYPE_TASK, RepeatControl.stopped(template).type)
        val old = RepeatState(RepeatRule(afterCompletion = false, unit = RepeatUnit.DAY, start = wed), wed, generated = 3)
        val monthly = RepeatRule(afterCompletion = false, unit = RepeatUnit.MONTH, start = d(2026, 11, 1))
        val changed = RepeatControl.withRule(old, monthly, wed, hasOpenCopy = true)
        assertEquals(d(2026, 11, 1), changed.nextOccurrence)
        assertEquals(3, changed.generated)
        val after = RepeatRule(afterCompletion = true, unit = RepeatUnit.WEEK, start = wed)
        assertNull(RepeatControl.withRule(old, after, wed, hasOpenCopy = true).nextOccurrence)
        assertEquals(wed, RepeatControl.withRule(old, after, wed, hasOpenCopy = false).nextOccurrence)
    }

    @Test
    fun repeatingList_soonestFirst_waitingLast() {
        fun t(id: String) = com.example.data.model.ItemWithChecklist(Item(id = id, type = Item.TYPE_TEMPLATE))
        fun r(id: String, next: Long, active: Boolean = true) = com.example.data.model.RecurrenceRule(templateId = id, rrule = "", nextGenerationDate = next, isActive = active)
        val rules = listOf(r("late", 300), r("soon", 100), r("paused", 50, active = false), r("ended", 0)).associateBy { it.templateId }
        val order = com.example.domain.lists.RepeatingList.order(listOf(t("late"), t("paused"), t("ended"), t("soon")), rules, todayStart = 0L, endOfToday = 0L).map { it.item.id }
        assertEquals(listOf("soon", "late"), order.take(2))
        assertEquals(setOf("paused", "ended"), order.drop(2).toSet())
        assertTrue(com.example.domain.lists.RepeatingList.includes(Item(id = "x", type = Item.TYPE_TEMPLATE)))
        assertFalse(com.example.domain.lists.RepeatingList.includes(Item(id = "y")))
    }

    @Test
    fun upcomingPreview_templateOnDayOfNextCopy() {
        val zone = ZoneId.of(TEST_ZONE)
        fun ms(date: LocalDate) = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val rule = RepeatRule(afterCompletion = false, unit = RepeatUnit.WEEK, start = wed, deadlineDaysBefore = 2)
        val template = com.example.data.model.ItemWithChecklist(Item(id = "t", type = Item.TYPE_TEMPLATE))
        fun stored(next: LocalDate, active: Boolean = true) =
            mapOf("t" to com.example.data.model.RecurrenceRule(templateId = "t", rrule = rule.encode(), nextGenerationDate = ms(next), isActive = active))
        val endOfToday = ms(wed.plusDays(1)) - 1
        // Повтор 21-го со сроком за 2 дня — в «Предстоящих» 19-го
        val preview = com.example.domain.lists.RepeatingList.upcomingPreviews(listOf(template), stored(d(2026, 10, 21)), endOfToday, zone)
        assertEquals(ms(d(2026, 10, 19)), preview.single().item.startDate)
        // На паузе и уже сегодня — не показывается
        assertTrue(com.example.domain.lists.RepeatingList.upcomingPreviews(listOf(template), stored(d(2026, 10, 21), active = false), endOfToday, zone).isEmpty())
        assertTrue(com.example.domain.lists.RepeatingList.upcomingPreviews(listOf(template), stored(d(2026, 10, 16)), endOfToday, zone).isEmpty())
    }

    // ---------------------------------------------------------------- повторяющиеся проекты

    private val project = Item(id = "p", type = Item.TYPE_PROJECT, title = "Уборка", startDate = 1L, dueDate = 2L)
    private val heading = Item(id = "h", type = Item.TYPE_HEADING, projectId = "p", title = "Кухня")
    private val openTask = Item(id = "a", projectId = "p", headingId = "h", title = "Пол", startDate = 5L)
    private val doneTask = Item(id = "b", projectId = "p", title = "Окна", status = Item.STATUS_COMPLETED)

    @Test
    fun projectTemplate_projectAndOpenTasksBecomeTemplate() {
        assertTrue(RepeatProjects.canRepeat(project))
        assertFalse(RepeatProjects.canRepeat(project.copy(repeatTemplateId = "t")))
        val changed = RepeatProjects.template(project, listOf(heading, openTask, doneTask), reminderTime = 540, now = 9L).associateBy { it.id }
        val template = changed.getValue("p")
        assertEquals(Item.TYPE_PROJECT_TEMPLATE, template.type)
        assertNull(template.startDate)
        assertNull(template.dueDate)
        assertEquals(540, template.reminderTime)
        assertTrue(template.isRepeatTemplate)
        val part = changed.getValue("a")
        assertTrue(part.isTemplatePart)
        assertEquals("p", part.repeatTemplateId)
        assertNull(part.startDate)
        // Закрытая задача и заголовок не меняются
        assertFalse("b" in changed)
        assertFalse("h" in changed)
    }

    @Test
    fun projectCopy_copiesHeadingsAndPartsUnderNewIds() {
        val template = project.copy(type = Item.TYPE_PROJECT_TEMPLATE, startDate = null, dueDate = null)
        val part = com.example.data.model.ItemWithChecklist(
            openTask.copy(type = Item.TYPE_TEMPLATE, repeatTemplateId = "p", startDate = null),
            listOf(ChecklistItem(id = "c", itemId = "a", title = "Швабра", isCompleted = true))
        )
        val zone = ZoneId.of(TEST_ZONE)
        var n = 0
        val rule = RepeatRule(afterCompletion = false, unit = RepeatUnit.WEEK, start = wed, deadlineDaysBefore = 1)
        val copy = RepeatProjects.copyOf(template, listOf(heading), listOf(part), rule, wed, zone, now = 7L) { "n${n++}" }
        assertEquals(Item.TYPE_PROJECT, copy.project.type)
        assertEquals("p", copy.project.repeatTemplateId)
        assertEquals(wed.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), copy.project.startDate)
        assertEquals(wed.atStartOfDay(zone).toInstant().toEpochMilli(), copy.project.dueDate)
        val newHeading = copy.headings.single()
        assertEquals(copy.project.id, newHeading.projectId)
        val (task, checklist) = copy.tasks.single()
        assertEquals(Item.TYPE_TASK, task.type)
        assertNull(task.repeatTemplateId)
        assertEquals(copy.project.id, task.projectId)
        assertEquals(newHeading.id, task.headingId)
        assertFalse(checklist.single().isCompleted)
        assertEquals(task.id, checklist.single().itemId)
    }

    @Test
    fun projectTemplate_normalizeAndStop() {
        val template = project.copy(type = Item.TYPE_PROJECT_TEMPLATE)
        val added = Item(id = "x", projectId = "p", title = "Новая")
        val movedOut = Item(id = "y", type = Item.TYPE_TEMPLATE, repeatTemplateId = "p", projectId = "other")
        val stayed = Item(id = "z", type = Item.TYPE_TEMPLATE, repeatTemplateId = "p", projectId = "p")
        val fixes = RepeatProjects.normalize(listOf(template, added, movedOut, stayed)).associateBy { it.id }
        assertTrue(fixes.getValue("x").isTemplatePart)
        assertEquals(Item.TYPE_TASK, fixes.getValue("y").type)
        assertNull(fixes.getValue("y").repeatTemplateId)
        assertFalse("z" in fixes)
        // Остановить: снова проект, части — его задачи
        val stopped = RepeatProjects.stopped(template, listOf(stayed, doneTask)).associateBy { it.id }
        assertEquals(Item.TYPE_PROJECT, stopped.getValue("p").type)
        assertEquals(Item.TYPE_TASK, stopped.getValue("z").type)
        assertNull(stopped.getValue("z").repeatTemplateId)
        assertFalse("b" in stopped)
    }

    @Test
    fun repeatingList_projectTemplatesAreProjectRows() {
        val template = project.copy(type = Item.TYPE_PROJECT_TEMPLATE)
        val part = Item(id = "z", type = Item.TYPE_TEMPLATE, repeatTemplateId = "p", projectId = "p")
        assertFalse(com.example.domain.lists.RepeatingList.includes(part))
        assertFalse(com.example.domain.lists.RepeatingList.includes(template))
        assertTrue(com.example.domain.lists.RepeatingList.includesProject(template))
        val groups = com.example.domain.lists.RepeatingList.groups(emptyList(), emptyMap(), emptyList(), emptyList(), listOf(template, part))
        assertEquals(listOf("p"), groups.flatMap { it.projectRows }.map { it.id })
    }

    @Test
    fun projectCopy_keepsTaskOrderOfTemplate() {
        val template = project.copy(type = Item.TYPE_PROJECT_TEMPLATE)
        fun part(id: String, created: Long) = com.example.data.model.ItemWithChecklist(
            Item(id = id, type = Item.TYPE_TEMPLATE, repeatTemplateId = "p", projectId = "p", title = id, creationDate = created)
        )
        // В шаблоне новые выше: t2 (создана позже), затем t1
        val rule = RepeatRule(afterCompletion = false, unit = RepeatUnit.WEEK, start = wed)
        val copy = RepeatProjects.copyOf(template, emptyList(), listOf(part("t1", 1L), part("t2", 2L)), rule, wed, now = 100L)
        val shown = copy.tasks.map { it.first }.sortedWith(compareBy<Item> { it.sortOrder }.thenByDescending { it.creationDate })
        assertEquals(listOf("t2", "t1"), shown.map { it.title })
    }

    // ---------------------------------------------------------------- выполнить заранее и перенос

    @Test
    fun completedEarly_fixedTakesNextOccurrence_afterCompletionCountsFromToday() {
        val weekly = RepeatRule(afterCompletion = false, unit = RepeatUnit.WEEK, start = wed)
        val (occurrence, next) = RepeatControl.completedEarly(RepeatState(weekly, d(2026, 10, 21)), wed)!!
        assertEquals(d(2026, 10, 21), occurrence)
        assertEquals(d(2026, 10, 28), next.nextOccurrence)
        assertEquals(1, next.generated)

        val after = RepeatRule(afterCompletion = true, interval = 3, unit = RepeatUnit.DAY, start = wed)
        val (afterOccurrence, afterNext) = RepeatControl.completedEarly(RepeatState(after, d(2026, 10, 20)), wed)!!
        assertEquals(d(2026, 10, 20), afterOccurrence)
        assertEquals(d(2026, 10, 17), afterNext.nextOccurrence)
        // Повторов больше нет — отмечать нечего
        assertNull(RepeatControl.completedEarly(RepeatState(after.copy(endCount = 1), null, generated = 1), wed))
    }

    @Test
    fun ruleMovedTo_weekdayDayOfMonthAndNthWeekday() {
        // Каждый понедельник → вторник: «каждый вторник»; «пн и чт», пн → вт: «вт и чт»
        val mon = d(2026, 10, 19)
        val weekly = RepeatRule(afterCompletion = false, unit = RepeatUnit.WEEK, start = d(2026, 10, 5), on = listOf(RepeatOn(RepeatDay.Weekday(1))))
        val moved = RepeatControl.ruleMovedTo(weekly, mon, mon.plusDays(1))
        assertEquals(listOf(RepeatOn(RepeatDay.Weekday(2))), moved.on)
        assertEquals(mon.plusDays(1), RepeatDates.nextOccurrence(moved, mon))
        assertEquals(mon.plusDays(8), RepeatDates.nextOccurrence(moved, mon.plusDays(2)))
        val twice = weekly.copy(on = listOf(RepeatOn(RepeatDay.Weekday(1)), RepeatOn(RepeatDay.Weekday(4))))
        assertEquals(listOf(2, 4), RepeatControl.ruleMovedTo(twice, mon, mon.plusDays(1)).on.map { (it.day as RepeatDay.Weekday).dayOfWeek })

        // 14-го каждого месяца → 16-го
        val monthly = RepeatRule(afterCompletion = false, unit = RepeatUnit.MONTH, start = wed, on = listOf(RepeatOn(RepeatDay.OfMonth(14))))
        val movedMonthly = RepeatControl.ruleMovedTo(monthly, d(2026, 11, 14), d(2026, 11, 16))
        assertEquals(listOf(RepeatOn(RepeatDay.OfMonth(16))), movedMonthly.on)
        assertEquals(d(2026, 12, 16), RepeatDates.nextOccurrence(movedMonthly, d(2026, 11, 17)))

        // 2-й понедельник → 2-й вторник (10 ноября 2026 — вторник)
        val nth = monthly.copy(on = listOf(RepeatOn(RepeatDay.Weekday(1, 2))))
        val movedNth = RepeatControl.ruleMovedTo(nth, d(2026, 11, 9), d(2026, 11, 10))
        assertEquals(listOf(RepeatOn(RepeatDay.Weekday(2, 2))), movedNth.on)
    }

    @Test
    fun repeatingList_allOpenCopiesByDate_undatedThenWaitingLast() {
        val rl = com.example.domain.lists.RepeatingList
        fun w(item: Item) = com.example.data.model.ItemWithChecklist(item)
        fun r(id: String, next: Long, active: Boolean = true) = com.example.data.model.RecurrenceRule(templateId = id, rrule = "", nextGenerationDate = next, isActive = active)
        val template = Item(id = "t", type = Item.TYPE_TEMPLATE)
        val waiting = Item(id = "w", type = Item.TYPE_TEMPLATE)
        val today = Item(id = "c0", repeatTemplateId = "t", startDate = 500L, isTonight = true)
        val later = Item(id = "c2", repeatTemplateId = "t", startDate = 3_000L)
        val someday = Item(id = "cs", repeatTemplateId = "t", start = Item.START_SOMEDAY)
        // Копии видны с любой датой — сегодня, вечером, когда-нибудь; закрытые — нет
        assertTrue(listOf(today, later, someday).all { rl.includesCopy(it) })
        assertFalse(rl.includesCopy(later.copy(status = Item.STATUS_COMPLETED)))
        val rules = listOf(r("t", 2_000L), r("w", 0L)).associateBy { it.templateId }
        val ordered = rl.order(listOf(waiting, someday, later, template, today).map(::w), rules, todayStart = 0L, endOfToday = 0L).map { it.item.id }
        assertEquals(listOf("c0", "t", "c2", "cs", "w"), ordered)
    }

    @Test
    fun repeatingList_tasksThenProjects_eachByDate() {
        val rl = com.example.domain.lists.RepeatingList
        fun r(id: String, next: Long, active: Boolean = true) = com.example.data.model.RecurrenceRule(templateId = id, rrule = "", nextGenerationDate = next, isActive = active)
        val projectTemplate = Item(id = "p", type = Item.TYPE_PROJECT_TEMPLATE)
        val projectCopy = Item(id = "pc", type = Item.TYPE_PROJECT, repeatTemplateId = "p", startDate = 500L)
        val taskTemplate = com.example.data.model.ItemWithChecklist(Item(id = "t", type = Item.TYPE_TEMPLATE))
        val plain = Item(id = "x", type = Item.TYPE_PROJECT, startDate = 100L)
        val rules = listOf(r("p", 3_000L), r("t", 1_000L)).associateBy { it.templateId }
        val group = rl.groups(listOf(taskTemplate), rules, emptyList(), emptyList(), listOf(projectTemplate, projectCopy, plain)).single()
        // Задачи и проекты — отдельно: сначала задачи, потом проекты, каждые по дате
        assertEquals(listOf("t", "pc", "p"), rl.rows(group, rules, todayStart = 0L, endOfToday = 0L).map { if (it is Item) it.id else (it as com.example.data.model.ItemWithChecklist).item.id })
    }

    @Test
    fun search_findsRepeatingTasksAndCopies() {
        fun wrap(item: Item) = com.example.data.model.ItemWithChecklist(item)
        val template = Item(id = "t", type = Item.TYPE_TEMPLATE, title = "Полить цветы")
        val copy = Item(id = "c", repeatTemplateId = "t", title = "Полить цветы")
        val part = Item(id = "p", type = Item.TYPE_TEMPLATE, repeatTemplateId = "proj", title = "Полить цветы")
        assertTrue(com.example.domain.lists.SearchList.includes(wrap(template), "цвет"))
        assertTrue(com.example.domain.lists.SearchList.includes(wrap(copy), "цвет"))
        // Задача внутри шаблона проекта — не отдельная повторяющаяся задача
        assertFalse(com.example.domain.lists.SearchList.includes(wrap(part), "цвет"))
    }

    @Test
    fun afterCompletion_deletedCopyStartsNextFromToday() {
        val zone = ZoneId.of(TEST_ZONE)
        val rule = RepeatRule(afterCompletion = true, interval = 3, unit = RepeatUnit.DAY, start = wed)
        val waiting = RepeatState(rule, nextOccurrence = null, generated = 1)
        // Копию удалили насовсем — её нет: следующий повтор через 3 дня от сегодня
        assertEquals(wed.plusDays(3), RepeatControl.afterCopiesClosed(waiting, emptyList(), wed, zone).nextOccurrence)
        // Открытая копия есть — ждёт
        val open = Item(id = "c", repeatTemplateId = "t")
        assertNull(RepeatControl.afterCopiesClosed(waiting, listOf(open), wed, zone).nextOccurrence)
        // Выполнили вчера — от вчера
        val yesterday = wed.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val done = open.copy(status = Item.STATUS_COMPLETED, stopDate = yesterday)
        assertEquals(wed.plusDays(2), RepeatControl.afterCopiesClosed(waiting, listOf(done), wed, zone).nextOccurrence)
        // Ещё ничего не создано — ждать нечего
        assertNull(RepeatControl.afterCopiesClosed(waiting.copy(generated = 0), emptyList(), wed, zone).nextOccurrence)
    }

    @Test
    fun orphanCopies_unlinkedWhenTemplateDeleted() {
        val template = Item(id = "t", type = Item.TYPE_TEMPLATE)
        val trashedTemplate = Item(id = "z", type = Item.TYPE_TEMPLATE, trashed = true)
        val live = Item(id = "a", repeatTemplateId = "t")
        val ofTrashed = Item(id = "b", repeatTemplateId = "z")
        val orphan = Item(id = "c", repeatTemplateId = "gone")
        val orphanProject = Item(id = "d", type = Item.TYPE_PROJECT, repeatTemplateId = "gone")
        val fixed = RepeatControl.orphanCopies(listOf(template, trashedTemplate, live, ofTrashed, orphan, orphanProject), now = 5L)
        assertEquals(listOf("c", "d"), fixed.map { it.id })
        assertTrue(fixed.all { it.repeatTemplateId == null })
        assertEquals(Item.TYPE_PROJECT, fixed.last().type)
    }

    @Test
    fun logbookDeletions_doNotTouchActiveRepeats() {
        val zone = ZoneId.of(TEST_ZONE)
        val after = RepeatRule(afterCompletion = true, unit = RepeatUnit.WEEK, start = wed)
        // Создано 5 копий, в «Журнале» остались не все (часть удалили), текущая копия открыта — повтор ждёт её дальше
        val open = Item(id = "c5", repeatTemplateId = "t")
        val waiting = RepeatState(after, nextOccurrence = null, generated = 5)
        assertEquals(waiting, RepeatControl.afterCopiesClosed(waiting, listOf(open), wed, zone))
        // Следующий повтор уже назначен — удаление старых копий его не меняет
        val scheduled = RepeatState(after, nextOccurrence = wed.plusDays(7), generated = 5)
        assertEquals(scheduled, RepeatControl.afterCopiesClosed(scheduled, emptyList(), wed, zone))
        // Регулярный повтор от копий не зависит вовсе
        val fixed = RepeatState(after.copy(afterCompletion = false), nextOccurrence = wed.plusDays(7), generated = 5)
        assertEquals(fixed, RepeatControl.afterCopiesClosed(fixed, emptyList(), wed, zone))
        // Шаблон на месте — его копии не отвязываются
        assertTrue(RepeatControl.orphanCopies(listOf(Item(id = "t", type = Item.TYPE_TEMPLATE), open)).isEmpty())
    }

    @Test
    fun repeatingList_todayBeforeTonight() {
        val day = 500L
        val evening = com.example.data.model.ItemWithChecklist(Item(id = "e", repeatTemplateId = "t", startDate = day, isTonight = true, sortOrder = 0))
        val today = com.example.data.model.ItemWithChecklist(Item(id = "d", repeatTemplateId = "t", startDate = day, sortOrder = 5))
        assertEquals(listOf("d", "e"), com.example.domain.lists.RepeatingList.order(listOf(evening, today), emptyMap(), todayStart = 0L, endOfToday = 0L).map { it.item.id })
        // Вчерашняя вечерняя копия (просрочена) — всё равно ниже сегодняшней дневной
        val rl = com.example.domain.lists.RepeatingList
        val yesterdayEvening = Item(id = "ye", repeatTemplateId = "t", startDate = 100L, isTonight = true)
        val todayDay = Item(id = "td", repeatTemplateId = "t", startDate = 900L)
        assertEquals(listOf("td", "ye"), listOf(yesterdayEvening, todayDay).sortedWith(rl.comparator(emptyMap(), todayStart = 800L, endOfToday = 999L)).map { it.id })
    }

    @Test
    fun upcoming_twoDates_ellipsisOnlyWhenMore() {
        val d1 = LocalDate.of(2026, 10, 20)
        val daily = RepeatRule(afterCompletion = false, interval = 1, unit = RepeatUnit.DAY, start = d1)
        assertEquals(listOf(d1, d1.plusDays(1)) to true, RepeatPlan.upcoming(RepeatState(daily, d1), 2))

        // Правило кончается после трёх повторов, два уже созданы — впереди один, троеточия нет
        val counted = daily.copy(endCount = 3)
        assertEquals(listOf(d1) to false, RepeatPlan.upcoming(RepeatState(counted, d1, generated = 2), 2))

        // Окончание по дате — ровно два повтора впереди, троеточия нет
        val until = daily.copy(endDate = d1.plusDays(1))
        assertEquals(listOf(d1, d1.plusDays(1)) to false, RepeatPlan.upcoming(RepeatState(until, d1), 2))

        // «После выполнения» — известен только ближайший; дальше повторы есть
        val after = RepeatRule(afterCompletion = true, interval = 1, unit = RepeatUnit.WEEK, start = d1)
        assertEquals(listOf(d1) to true, RepeatPlan.upcoming(RepeatState(after, d1), 2))

        // Ждёт отметки копии — дат нет
        assertEquals(emptyList<LocalDate>() to false, RepeatPlan.upcoming(RepeatState(after, null), 2))
    }
}
