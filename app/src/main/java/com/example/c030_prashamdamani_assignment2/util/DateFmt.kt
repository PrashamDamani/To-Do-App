package com.example.c030_prashamdamani_assignment2.util

import com.example.c030_prashamdamani_assignment2.data.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

fun LocalDate.startMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    atStartOfDay(zone).toInstant().toEpochMilli()

/** Combines a Material date picker value (UTC midnight millis) with a local time. */
fun combineDateTime(pickerUtcMillis: Long, hour: Int, minute: Int, zone: ZoneId = ZoneId.systemDefault()): Long {
    val d = Instant.ofEpochMilli(pickerUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return d.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
}

object DateFmt {
    private fun z(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())

    fun date(ms: Long): String =
        DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault()).format(z(ms))

    fun time(ms: Long): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault()).format(z(ms))

    fun dateTime(ms: Long): String = "${date(ms)}, ${time(ms)}"

    fun day(d: LocalDate): String =
        DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault()).format(d)

    fun month(d: LocalDate): String =
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()).format(d)

    fun duration(min: Int): String {
        val h = min / 60
        val m = min % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    fun mmss(ms: Long): String {
        val total = (ms + 999) / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
        else String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }

    fun when_(t: Task): String = when {
        t.dueAt != null -> "Due " + if (t.allDay) date(t.dueAt) else dateTime(t.dueAt)
        t.startAt != null -> "Starts " + if (t.allDay) date(t.startAt) else dateTime(t.startAt)
        else -> "No date set"
    }

    fun reminderStatus(t: Task, now: Long): String = when {
        !t.reminderEnabled -> "Reminder off"
        t.completed -> "Cancelled (task completed)"
        t.reminderAt == null -> "Reminder time not set"
        t.reminderAt <= now -> "Time passed: " + dateTime(t.reminderAt)
        else -> "Scheduled: " + dateTime(t.reminderAt)
    }
}
