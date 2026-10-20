package com.example.c030_prashamdamani_assignment2

import com.example.c030_prashamdamani_assignment2.data.model.Priority
import com.example.c030_prashamdamani_assignment2.data.model.Recurrence
import com.example.c030_prashamdamani_assignment2.data.model.Task
import com.example.c030_prashamdamani_assignment2.data.model.UserProfile
import com.example.c030_prashamdamani_assignment2.util.DateFilter
import com.example.c030_prashamdamani_assignment2.util.EntryKind
import com.example.c030_prashamdamani_assignment2.util.SortMode
import com.example.c030_prashamdamani_assignment2.util.StatusFilter
import com.example.c030_prashamdamani_assignment2.util.TaskFilter
import com.example.c030_prashamdamani_assignment2.util.TaskLogic
import com.example.c030_prashamdamani_assignment2.util.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TaskLogicTest {
    private val zone = ZoneId.of("UTC")
    private val day = LocalDate.of(2026, 10, 19)
    private val now = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private val hour = 3_600_000L

    private fun t(id: String, p: Priority = Priority.MEDIUM, due: Long? = null, start: Long? = null, created: Long = 0, done: Boolean = false, dur: Int? = null) =
        Task(id = id, title = "Task $id", priority = p, dueAt = due, startAt = start, createdAt = created, completed = done, durationMinutes = dur)

    @Test fun priorityOrderIsHighMediumLow() {
        val sorted = TaskLogic.sort(listOf(t("l", Priority.LOW), t("h", Priority.HIGH), t("m", Priority.MEDIUM)), SortMode.DEFAULT, now)
        assertEquals(listOf("h", "m", "l"), sorted.map { it.id })
    }

    @Test fun overdueFirstInsideSamePriority() {
        val sorted = TaskLogic.sort(listOf(t("future", Priority.HIGH, due = now + hour), t("late", Priority.HIGH, due = now - hour)), SortMode.DEFAULT, now)
        assertEquals("late", sorted.first().id)
    }

    @Test fun overdueDoesNotJumpAbovePriority() {
        val sorted = TaskLogic.sort(listOf(t("lowLate", Priority.LOW, due = now - hour), t("highFuture", Priority.HIGH, due = now + hour)), SortMode.DEFAULT, now)
        assertEquals("highFuture", sorted.first().id)
    }

    @Test fun earlierDeadlineFirstAndUndatedLast() {
        val sorted = TaskLogic.sort(listOf(t("none"), t("later", due = now + 5 * hour), t("soon", due = now + hour)), SortMode.DEFAULT, now)
        assertEquals(listOf("soon", "later", "none"), sorted.map { it.id })
    }

    @Test fun scheduledBeforeUndatedAndCreationTieBreak() {
        val sorted = TaskLogic.sort(listOf(t("b", created = 2), t("a", created = 1), t("s", start = now + hour)), SortMode.DEFAULT, now)
        assertEquals(listOf("s", "a", "b"), sorted.map { it.id })
    }

    @Test fun deadlineSortPutsNullsLast() {
        val sorted = TaskLogic.sort(listOf(t("n"), t("d", due = now)), SortMode.DEADLINE, now)
        assertEquals("d", sorted.first().id)
    }

    @Test fun alphaSort() {
        val sorted = TaskLogic.sort(listOf(t("b"), t("a")), SortMode.ALPHA, now)
        assertEquals("a", sorted.first().id)
    }

    @Test fun overdueRules() {
        assertTrue(TaskLogic.isOverdue(t("x", due = now - 1), now))
        assertFalse(TaskLogic.isOverdue(t("x", due = now + 1), now))
        assertFalse(TaskLogic.isOverdue(t("x", due = now - 1, done = true), now))
        assertFalse(TaskLogic.isOverdue(t("x"), now))
    }

    @Test fun filterByStatusPriorityAndQuery() {
        val tasks = listOf(t("a", Priority.HIGH), t("b", Priority.LOW, done = true), t("c", Priority.HIGH).copy(title = "Buy milk"))
        assertEquals(2, TaskLogic.applyFilter(tasks, TaskFilter(), now, zone).size)
        assertEquals(1, TaskLogic.applyFilter(tasks, TaskFilter(status = StatusFilter.COMPLETED), now, zone).size)
        assertEquals(2, TaskLogic.applyFilter(tasks, TaskFilter(priority = Priority.HIGH), now, zone).size)
        assertEquals("c", TaskLogic.applyFilter(tasks, TaskFilter(query = "milk"), now, zone).single().id)
    }

    @Test fun todayFilterAndStats() {
        val tasks = listOf(
            t("today", due = now + hour), t("tomorrow", due = now + 30 * hour),
            t("late", Priority.HIGH, due = now - 2 * hour), t("done", done = true).copy(completedAt = now)
        )
        val today = TaskLogic.applyFilter(tasks, TaskFilter(date = DateFilter.TODAY), now, zone)
        assertEquals(setOf("today", "late"), today.map { it.id }.toSet())
        val s = TaskLogic.stats(tasks, now, zone)
        assertEquals(4, s.total); assertEquals(3, s.active); assertEquals(1, s.completed)
        assertEquals(1, s.overdue); assertEquals(1, s.highActive); assertEquals(1, s.completedToday); assertEquals(25, s.completionPct)
    }

    @Test fun emptyStatsHaveZeroPercent() {
        assertEquals(0, TaskLogic.stats(emptyList(), now, zone).completionPct)
    }

    @Test fun validation() {
        assertNotNull(TaskLogic.validate("", null, null, null, false, null, false, now))
        assertNotNull(TaskLogic.validate("x".repeat(101), null, null, null, false, null, false, now))
        assertNotNull(TaskLogic.validate("a", now, now - 1, null, false, null, false, now))
        assertNotNull(TaskLogic.validate("a", null, null, 0, false, null, false, now))
        assertNotNull(TaskLogic.validate("a", null, null, 2000, false, null, false, now))
        assertNotNull(TaskLogic.validate("a", null, null, null, true, null, false, now))
        assertNotNull(TaskLogic.validate("a", null, null, null, true, now - 1, false, now))
        assertNull(TaskLogic.validate("a", now, now + hour, 30, true, now + 1, false, now))
        assertNull(TaskLogic.validate("a", null, null, null, false, null, false, now))
    }

    @Test fun reminderComputationUsesStartThenDue() {
        assertEquals(now - 15 * 60_000L, TaskLogic.computeReminderAt(15, now, now + hour))
        assertEquals(now + hour, TaskLogic.computeReminderAt(0, null, now + hour))
        assertNull(TaskLogic.computeReminderAt(15, null, null))
        assertNull(TaskLogic.computeReminderAt(-1, now, now))
    }

    @Test fun deadlineAloneIsNotAReminder() {
        assertFalse(t("x", due = now + hour).reminderEnabled)
    }

    @Test fun endTimeCalculation() {
        assertEquals(now + 90 * 60_000L, t("x", start = now, dur = 90).endAt)
        assertNull(t("x", start = now).endAt)
        assertNull(t("x", dur = 30).endAt)
    }

    @Test fun overlapDetection() {
        val a = t("a", start = now, dur = 60)
        val b = t("b", start = now + 30 * 60_000L, dur = 60)
        val c = t("c", start = now + 2 * hour, dur = 30)
        val entries = TaskLogic.entriesForDate(listOf(a, b, c), day, zone)
        assertEquals(setOf("a", "b"), TaskLogic.overlappingIds(entries))
        assertTrue(TaskLogic.conflicts(listOf(a), now + 10 * 60_000L, 20, false, null).isNotEmpty())
        assertTrue(TaskLogic.conflicts(listOf(a), now + hour, 20, false, null).isEmpty())
    }

    @Test fun calendarEntriesDistinguishKinds() {
        val block = t("blk", start = now, dur = 30)
        val deadline = t("dl", due = now + hour)
        val allDay = t("ad", start = day.atStartOfDay(zone).toInstant().toEpochMilli()).copy(allDay = true)
        val unscheduled = t("none")
        val kinds = TaskLogic.entriesForDate(listOf(block, deadline, allDay, unscheduled), day, zone).associate { it.task.id to it.kind }
        assertEquals(EntryKind.BLOCK, kinds["blk"]); assertEquals(EntryKind.DEADLINE, kinds["dl"]); assertEquals(EntryKind.ALL_DAY, kinds["ad"])
        assertFalse(kinds.containsKey("none"))
    }

    @Test fun shiftMovesAllTimes() {
        val s = TaskLogic.shift(t("x", start = 1000, due = 2000).copy(reminderAt = 500), 100)
        assertEquals(1100L, s.startAt); assertEquals(2100L, s.dueAt); assertEquals(600L, s.reminderAt)
    }

    @Test fun recurrence() {
        val daily = t("d", due = now).copy(recurrence = Recurrence.DAILY)
        assertEquals(now + 24 * hour, TaskLogic.nextOccurrence(daily, zone)!!.dueAt)
        assertNull(TaskLogic.nextOccurrence(t("n", due = now), zone))
        assertNull(TaskLogic.nextOccurrence(t("nodate").copy(recurrence = Recurrence.DAILY), zone))
        val weekly = t("w", due = now).copy(recurrence = Recurrence.WEEKLY)
        assertEquals(now + 7 * 24 * hour, TaskLogic.nextOccurrence(weekly, zone)!!.dueAt)
        // 2026-10-19 is a Monday (1). Custom Wed(3)/Fri(5) -> Wednesday, two days later.
        val custom = t("c", due = now).copy(recurrence = Recurrence.CUSTOM, recurrenceDays = listOf(3, 5))
        assertEquals(now + 48 * hour, TaskLogic.nextOccurrence(custom, zone)!!.dueAt)
        assertFalse(TaskLogic.nextOccurrence(daily, zone)!!.completed)
    }

    @Test fun weeklyTrendCountsCompletionsPerDay() {
        val done = t("d", done = true).copy(completedAt = now)
        val trend = TaskLogic.weeklyCompletion(listOf(done), now, zone)
        assertEquals(7, trend.size); assertEquals(1, trend.last().second); assertEquals(0, trend.first().second)
    }

    @Test fun greetingByHour() {
        assertEquals("Good morning", TaskLogic.greeting(8))
        assertEquals("Good afternoon", TaskLogic.greeting(14))
        assertEquals("Good evening", TaskLogic.greeting(20))
    }

    @Test fun emailAndPasswordValidation() {
        assertTrue(Validators.isEmail("prasham@example.com"))
        assertFalse(Validators.isEmail("not-an-email"))
        assertNotNull(Validators.passwordError("123"))
        assertNull(Validators.passwordError("123456"))
    }

    @Test fun profileHeadings() {
        val p = UserProfile(fullName = "Prasham Damani", rollNumber = "C030")
        assertEquals("C030 - Prasham Damani Schedule", p.scheduleHeading)
        assertEquals("Prasham's", p.possessive)
        val other = UserProfile(fullName = "Asha Rao")
        assertEquals("Asha Rao's Schedule", other.scheduleHeading)
        assertEquals("Asha Rao", other.identity)
    }
}
