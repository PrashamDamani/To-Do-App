package com.example.c030_prashamdamani_assignment2.util

import com.example.c030_prashamdamani_assignment2.data.model.Importance
import com.example.c030_prashamdamani_assignment2.data.model.Priority
import com.example.c030_prashamdamani_assignment2.data.model.Recurrence
import com.example.c030_prashamdamani_assignment2.data.model.Task
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class SortMode(val label: String) {
    DEFAULT("Smart (priority + deadline)"),
    PRIORITY("Priority"),
    DEADLINE("Deadline"),
    CREATED("Creation date"),
    ALPHA("Alphabetical")
}

enum class StatusFilter(val label: String) { ACTIVE("Active"), COMPLETED("Completed"), ALL("All") }
enum class DateFilter(val label: String) { ANY("Any date"), TODAY("Today"), THIS_WEEK("Next 7 days") }
enum class EntryKind { ALL_DAY, BLOCK, DEADLINE }

data class TaskFilter(
    val status: StatusFilter = StatusFilter.ACTIVE,
    val priority: Priority? = null,
    val importance: Importance? = null,
    val category: String? = null,
    val date: DateFilter = DateFilter.ANY,
    val overdueOnly: Boolean = false,
    val query: String = "",
    val sort: SortMode = SortMode.DEFAULT
) : Serializable {
    /** Number of non-default advanced filters (for the badge on the filter button). */
    val advancedCount: Int
        get() = listOf(priority != null, importance != null, category != null, date != DateFilter.ANY, overdueOnly).count { it }
}

data class Stats(
    val total: Int,
    val active: Int,
    val completed: Int,
    val highActive: Int,
    val dueToday: Int,
    val overdue: Int,
    val completedToday: Int,
    val completionPct: Int
)

data class CalendarEntry(val task: Task, val kind: EntryKind, val startMs: Long, val endMs: Long?)

object Validators {
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    fun isEmail(e: String) = EMAIL.matches(e.trim())
    fun passwordError(p: String): String? = if (p.length < 6) "Password must be at least 6 characters" else null
}

object TaskLogic {

    fun isOverdue(t: Task, now: Long): Boolean = !t.completed && t.dueAt != null && t.dueAt < now

    private fun effectiveTime(t: Task): Long? = t.dueAt ?: t.startAt

    /**
     * Default order: High > Medium > Low; inside a priority overdue first; then earliest
     * deadline/scheduled time; tasks without any date last; finally creation time.
     */
    fun defaultComparator(now: Long): Comparator<Task> = compareBy<Task>(
        { it.priority.rank },
        { if (isOverdue(it, now)) 0 else 1 },
        { effectiveTime(it) == null },
        { effectiveTime(it) },
        { it.startAt ?: Long.MAX_VALUE },
        { it.createdAt }
    )

    fun sort(tasks: List<Task>, mode: SortMode, now: Long): List<Task> = when (mode) {
        SortMode.DEFAULT -> tasks.sortedWith(defaultComparator(now))
        SortMode.PRIORITY -> tasks.sortedWith(compareBy<Task>({ it.priority.rank }, { it.title.lowercase() }))
        SortMode.DEADLINE -> tasks.sortedWith(compareBy<Task>({ it.dueAt == null }, { it.dueAt }, { it.priority.rank }))
        SortMode.CREATED -> tasks.sortedByDescending { it.createdAt }
        SortMode.ALPHA -> tasks.sortedBy { it.title.lowercase() }
    }

    fun isDueToday(t: Task, today: LocalDate, zone: ZoneId): Boolean =
        !t.completed && effectiveTime(t)?.toLocalDate(zone) == today

    fun applyFilter(tasks: List<Task>, f: TaskFilter, now: Long, zone: ZoneId = ZoneId.systemDefault()): List<Task> {
        val today = now.toLocalDate(zone)
        val weekEnd = today.plusDays(6)
        val q = f.query.trim().lowercase()
        val list = tasks.filter { t ->
            val d = effectiveTime(t)?.toLocalDate(zone)
            val dateOk = when (f.date) {
                DateFilter.ANY -> true
                DateFilter.TODAY -> d == today
                DateFilter.THIS_WEEK -> d != null && !d.isBefore(today) && !d.isAfter(weekEnd)
            }
            val statusOk = when (f.status) {
                StatusFilter.ACTIVE -> !t.completed
                StatusFilter.COMPLETED -> t.completed
                StatusFilter.ALL -> true
            }
            statusOk && dateOk &&
                (f.priority == null || t.priority == f.priority) &&
                (f.importance == null || t.importance == f.importance) &&
                (f.category == null || t.category == f.category) &&
                (!f.overdueOnly || isOverdue(t, now)) &&
                (q.isEmpty() || t.title.lowercase().contains(q) ||
                    t.description.lowercase().contains(q) || t.notes.lowercase().contains(q))
        }
        return if (f.status == StatusFilter.COMPLETED && f.sort == SortMode.DEFAULT)
            list.sortedByDescending { it.completedAt ?: it.updatedAt }
        else sort(list, f.sort, now)
    }

    /** Preset names used by dashboard cards and quick-filter chips. */
    fun presetFilter(name: String): TaskFilter = when (name) {
        "HIGH" -> TaskFilter(priority = Priority.HIGH)
        "TODAY" -> TaskFilter(date = DateFilter.TODAY)
        "OVERDUE" -> TaskFilter(overdueOnly = true)
        "COMPLETED" -> TaskFilter(status = StatusFilter.COMPLETED)
        "ALL" -> TaskFilter(status = StatusFilter.ALL)
        else -> TaskFilter()
    }

    fun stats(tasks: List<Task>, now: Long, zone: ZoneId = ZoneId.systemDefault()): Stats {
        val today = now.toLocalDate(zone)
        val total = tasks.size
        val completed = tasks.count { it.completed }
        return Stats(
            total = total,
            active = total - completed,
            completed = completed,
            highActive = tasks.count { !it.completed && it.priority == Priority.HIGH },
            dueToday = tasks.count { isDueToday(it, today, zone) },
            overdue = tasks.count { isOverdue(it, now) },
            completedToday = tasks.count { it.completed && it.completedAt?.toLocalDate(zone) == today },
            completionPct = if (total == 0) 0 else completed * 100 / total
        )
    }

    /** Completed-task counts for the last 7 days (oldest first, today last). */
    fun weeklyCompletion(tasks: List<Task>, now: Long, zone: ZoneId = ZoneId.systemDefault()): List<Pair<LocalDate, Int>> {
        val today = now.toLocalDate(zone)
        return (6 downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            d to tasks.count { it.completed && it.completedAt?.toLocalDate(zone) == d }
        }
    }

    fun greeting(hour: Int): String = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }

    fun computeReminderAt(offsetMinutes: Int, startAt: Long?, dueAt: Long?): Long? {
        if (offsetMinutes < 0) return null
        val base = startAt ?: dueAt ?: return null
        return base - offsetMinutes * 60_000L
    }

    /** Returns an error message or null when valid. */
    fun validate(
        title: String, startAt: Long?, dueAt: Long?, durationMin: Int?,
        reminderEnabled: Boolean, reminderAt: Long?, completed: Boolean, now: Long
    ): String? {
        if (title.isBlank()) return "Title is required"
        if (title.trim().length > 100) return "Title must be 100 characters or fewer"
        if (startAt != null && dueAt != null && dueAt < startAt) return "Due date cannot be before the start"
        if (durationMin != null && (durationMin <= 0 || durationMin > 1440)) return "Duration must be 1 to 1440 minutes"
        if (reminderEnabled) {
            if (reminderAt == null) return "Choose a reminder time"
            if (!completed && reminderAt <= now) return "Reminder time must be in the future"
        }
        return null
    }

    // ---------- Calendar ----------

    fun entriesForDate(tasks: List<Task>, date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<CalendarEntry> {
        val out = mutableListOf<CalendarEntry>()
        for (t in tasks) {
            val s = t.startAt
            val d = t.dueAt
            val startDate = s?.toLocalDate(zone)
            val dueDate = d?.toLocalDate(zone)
            if (s != null && startDate == date) {
                out += if (t.allDay) CalendarEntry(t, EntryKind.ALL_DAY, s, null)
                else CalendarEntry(t, EntryKind.BLOCK, s, t.endAt)
            }
            if (d != null && dueDate == date && !(t.allDay && startDate == dueDate)) {
                out += CalendarEntry(t, EntryKind.DEADLINE, d, null)
            }
        }
        return out.sortedWith(compareBy({ it.kind.ordinal }, { it.startMs }))
    }

    /** Ids of active tasks whose time blocks overlap another block in the list. */
    fun overlappingIds(entries: List<CalendarEntry>): Set<String> {
        val blocks = entries.filter { it.kind == EntryKind.BLOCK && it.endMs != null && !it.task.completed }
        val ids = mutableSetOf<String>()
        for (i in blocks.indices) for (j in i + 1 until blocks.size) {
            val a = blocks[i]
            val b = blocks[j]
            if (a.startMs < b.endMs!! && b.startMs < a.endMs!!) {
                ids += a.task.id
                ids += b.task.id
            }
        }
        return ids
    }

    /** Active tasks whose scheduled block overlaps [startAt, startAt+duration). */
    fun conflicts(tasks: List<Task>, startAt: Long?, durationMin: Int?, allDay: Boolean, excludeId: String?): List<Task> {
        if (startAt == null || durationMin == null || allDay) return emptyList()
        val end = startAt + durationMin * 60_000L
        return tasks.filter { t ->
            val s = t.startAt
            val e = t.endAt
            t.id != excludeId && !t.completed && s != null && e != null && startAt < e && s < end
        }
    }

    /** Moves start, due and reminder by the same delta. */
    fun shift(t: Task, deltaMs: Long): Task = t.copy(
        startAt = t.startAt?.plus(deltaMs),
        dueAt = t.dueAt?.plus(deltaMs),
        reminderAt = t.reminderAt?.plus(deltaMs)
    )

    // ---------- Recurrence ----------

    /**
     * Next occurrence of a recurring task, based on the due date (else start date).
     * Returns null when the task does not repeat or has no date to advance from.
     * The copy has an empty id; the caller assigns one.
     */
    fun nextOccurrence(t: Task, zone: ZoneId = ZoneId.systemDefault()): Task? {
        if (t.recurrence == Recurrence.NONE) return null
        val anchor = t.dueAt ?: t.startAt ?: return null
        val aDate = anchor.toLocalDate(zone)
        val next: LocalDate = when (t.recurrence) {
            Recurrence.DAILY -> aDate.plusDays(1)
            Recurrence.WEEKLY -> aDate.plusWeeks(1)
            Recurrence.MONTHLY -> aDate.plusMonths(1)
            Recurrence.CUSTOM -> {
                if (t.recurrenceDays.isEmpty()) return null
                (1L..7L).map { aDate.plusDays(it) }.first { it.dayOfWeek.value in t.recurrenceDays }
            }
            Recurrence.NONE -> return null
        }
        val days = ChronoUnit.DAYS.between(aDate, next)
        fun sh(ms: Long?): Long? = ms?.let { Instant.ofEpochMilli(it).atZone(zone).plusDays(days).toInstant().toEpochMilli() }
        return t.copy(
            id = "", completed = false, completedAt = null,
            startAt = sh(t.startAt), dueAt = sh(t.dueAt), reminderAt = sh(t.reminderAt)
        )
    }
}
