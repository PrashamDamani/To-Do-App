package com.example.c030_prashamdamani_assignment2.data.model

import com.google.firebase.database.DataSnapshot

enum class Priority(val label: String, val rank: Int) {
    HIGH("High", 0), MEDIUM("Medium", 1), LOW("Low", 2)
}

enum class Importance(val label: String) {
    CRITICAL("Critical"), IMPORTANT("Important"), NORMAL("Normal")
}

enum class Recurrence(val label: String) {
    NONE("Does not repeat"), DAILY("Daily"), WEEKLY("Weekly"), MONTHLY("Monthly"), CUSTOM("Custom days")
}

object Categories {
    val defaults = listOf("Academic", "Work/Career", "Personal", "Health", "Finance", "Household", "Other")
}

/** Reminder offsets in minutes before the base time (start, else due). -1 = custom date/time. */
object ReminderOffsets {
    val options: List<Pair<String, Int>> = listOf(
        "At scheduled time" to 0,
        "5 min before" to 5,
        "15 min before" to 15,
        "30 min before" to 30,
        "1 hour before" to 60,
        "1 day before" to 1440,
        "Custom" to -1
    )
}

/**
 * All timestamps are UTC epoch milliseconds. Null = not set.
 * recurrenceDays uses ISO day numbers: 1 = Monday ... 7 = Sunday.
 */
data class Task(
    val id: String = "",
    val ownerUid: String = "",
    val title: String = "",
    val description: String = "",
    val priority: Priority = Priority.MEDIUM,
    val importance: Importance = Importance.NORMAL,
    val category: String = "Other",
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val allDay: Boolean = false,
    val startAt: Long? = null,
    val dueAt: Long? = null,
    val durationMinutes: Int? = null,
    val reminderEnabled: Boolean = false,
    val reminderAt: Long? = null,
    val reminderOffsetMinutes: Int = 0,
    val recurrence: Recurrence = Recurrence.NONE,
    val recurrenceDays: List<Int> = emptyList(),
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    /** Estimated end = start + duration. Null when either is missing or task is all-day. */
    val endAt: Long?
        get() {
            val s = startAt
            val d = durationMinutes
            return if (s != null && d != null && !allDay) s + d * 60_000L else null
        }

    fun editableMap(): Map<String, Any?> = mapOf(
        "title" to title,
        "description" to description,
        "priority" to priority.name,
        "importance" to importance.name,
        "category" to category,
        "completed" to completed,
        "completedAt" to completedAt,
        "allDay" to allDay,
        "startAt" to startAt,
        "dueAt" to dueAt,
        "durationMinutes" to durationMinutes,
        "reminderEnabled" to reminderEnabled,
        "reminderAt" to reminderAt,
        "reminderOffsetMinutes" to reminderOffsetMinutes,
        "recurrence" to recurrence.name,
        "recurrenceDays" to recurrenceDays.joinToString(",").ifEmpty { null },
        "notes" to notes,
        "updatedAt" to updatedAt
    )

    fun toMap(): Map<String, Any?> =
        editableMap() + mapOf("id" to id, "ownerUid" to ownerUid, "createdAt" to createdAt)

    companion object {
        private fun <T> safe(block: () -> T?): T? = try { block() } catch (e: Exception) { null }

        private inline fun <reified E : Enum<E>> enumOr(value: String?, default: E): E =
            enumValues<E>().firstOrNull { it.name == value } ?: default

        /** Returns null for records that are too malformed to show (no key or no title). */
        fun fromSnapshot(s: DataSnapshot): Task? {
            val key = s.key ?: return null
            fun str(k: String) = safe { s.child(k).getValue(String::class.java) }
            fun lng(k: String) = safe { s.child(k).getValue(Long::class.java) }
            fun bool(k: String) = safe { s.child(k).getValue(Boolean::class.java) }
            val title = str("title")?.takeIf { it.isNotBlank() } ?: return null
            return Task(
                id = key,
                ownerUid = str("ownerUid").orEmpty(),
                title = title,
                description = str("description").orEmpty(),
                priority = enumOr(str("priority"), Priority.MEDIUM),
                importance = enumOr(str("importance"), Importance.NORMAL),
                category = str("category")?.takeIf { it.isNotBlank() } ?: "Other",
                completed = bool("completed") ?: false,
                completedAt = lng("completedAt"),
                allDay = bool("allDay") ?: false,
                startAt = lng("startAt"),
                dueAt = lng("dueAt"),
                durationMinutes = lng("durationMinutes")?.toInt(),
                reminderEnabled = bool("reminderEnabled") ?: false,
                reminderAt = lng("reminderAt"),
                reminderOffsetMinutes = lng("reminderOffsetMinutes")?.toInt() ?: 0,
                recurrence = enumOr(str("recurrence"), Recurrence.NONE),
                recurrenceDays = str("recurrenceDays").orEmpty().split(",")
                    .mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..7 },
                notes = str("notes").orEmpty(),
                createdAt = lng("createdAt") ?: 0L,
                updatedAt = lng("updatedAt") ?: 0L
            )
        }
    }
}
