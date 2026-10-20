@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.c030_prashamdamani_assignment2.ui.calendar

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.c030_prashamdamani_assignment2.data.model.Task
import com.example.c030_prashamdamani_assignment2.data.model.UserProfile
import com.example.c030_prashamdamani_assignment2.ui.components.DateTimePickerDialog
import com.example.c030_prashamdamani_assignment2.ui.components.EmptyState
import com.example.c030_prashamdamani_assignment2.ui.components.LocalReduceMotion
import com.example.c030_prashamdamani_assignment2.ui.components.OfflineBanner
import com.example.c030_prashamdamani_assignment2.ui.components.SectionHeader
import com.example.c030_prashamdamani_assignment2.ui.components.priorityColor
import com.example.c030_prashamdamani_assignment2.util.CalendarEntry
import com.example.c030_prashamdamani_assignment2.util.DateFmt
import com.example.c030_prashamdamani_assignment2.util.EntryKind
import com.example.c030_prashamdamani_assignment2.util.TaskLogic
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

private enum class CalMode(val label: String) { MONTH("Month"), WEEK("Week"), DAY("Agenda") }

@Composable
fun CalendarScreen(
    profile: UserProfile?,
    tasks: List<Task>,
    online: Boolean,
    onOpenTask: (Task) -> Unit,
    onReschedule: (Task, Long) -> Unit
) {
    val zone = ZoneId.systemDefault()
    val reduce = LocalReduceMotion.current
    var mode by rememberSaveable { mutableStateOf(CalMode.MONTH) }
    var selectedEpoch by rememberSaveable { mutableStateOf(LocalDate.now(zone).toEpochDay()) }
    var showUnscheduled by rememberSaveable { mutableStateOf(true) }
    var reschedule by remember { mutableStateOf<CalendarEntry?>(null) }
    val selected = LocalDate.ofEpochDay(selectedEpoch)
    val today = LocalDate.now(zone)
    val firstDow = WeekFields.of(Locale.getDefault()).firstDayOfWeek

    fun move(dir: Long) {
        selectedEpoch = when (mode) {
            CalMode.MONTH -> selected.plusMonths(dir)
            CalMode.WEEK -> selected.plusWeeks(dir)
            CalMode.DAY -> selected.plusDays(dir)
        }.toEpochDay()
    }

    val dayEntries = remember(tasks, selectedEpoch) { TaskLogic.entriesForDate(tasks, selected, zone) }
    val overlapIds = remember(dayEntries) { TaskLogic.overlappingIds(dayEntries) }
    val unscheduled = tasks.filter { !it.completed && it.startAt == null && it.dueAt == null }
    val title = profile?.scheduleHeading ?: "My Schedule"

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            actions = {
                IconButton(onClick = { selectedEpoch = today.toEpochDay() }) { Icon(Icons.Default.Today, contentDescription = "Jump to today") }
            }
        )
        if (!online) OfflineBanner()
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CalMode.values().forEach { m -> FilterChip(mode == m, { mode = m }, { Text(m.label) }) }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { move(-1) }) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous ${mode.label.lowercase()}") }
                    Text(
                        when (mode) {
                            CalMode.MONTH -> DateFmt.month(selected)
                            CalMode.WEEK -> {
                                val start = selected.with(java.time.temporal.TemporalAdjusters.previousOrSame(firstDow))
                                "${start.dayOfMonth} ${start.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} - ${start.plusDays(6).let { "${it.dayOfMonth} ${it.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}" }}"
                            }
                            CalMode.DAY -> DateFmt.day(selected)
                        },
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { move(1) }) { Icon(Icons.Default.ChevronRight, contentDescription = "Next ${mode.label.lowercase()}") }
                }
            }
            if (mode != CalMode.DAY) {
                item {
                    Column {
                        Row(Modifier.fillMaxWidth()) {
                            for (i in 0..6) {
                                Text(
                                    firstDow.plus(i.toLong()).getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                    Modifier.weight(1f), textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Crossfade(
                            targetState = if (mode == CalMode.MONTH) YearMonth.from(selected).atDay(1) else selected.with(java.time.temporal.TemporalAdjusters.previousOrSame(firstDow)),
                            animationSpec = if (reduce) snap() else tween(200), label = "calendarGrid"
                        ) { anchor ->
                            if (mode == CalMode.MONTH) MonthGrid(anchor, firstDow, selected, today, tasks, zone) { selectedEpoch = it.toEpochDay() }
                            else WeekRow(anchor, selected, today, tasks, zone) { selectedEpoch = it.toEpochDay() }
                        }
                    }
                }
            }
            item { SectionHeader("Schedule for ${DateFmt.day(selected)}") }
            if (overlapIds.isNotEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error)
                            Text("  ${overlapIds.size} tasks have overlapping time blocks on this day.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            val allDay = dayEntries.filter { it.kind == EntryKind.ALL_DAY }
            val timed = dayEntries.filter { it.kind == EntryKind.BLOCK }
            val deadlines = dayEntries.filter { it.kind == EntryKind.DEADLINE }
            if (dayEntries.isEmpty()) {
                item { EmptyState(Icons.Default.EventBusy, "Nothing scheduled", "No tasks start or are due on this date.") }
            }
            if (allDay.isNotEmpty()) {
                item { Text("All-day", style = MaterialTheme.typography.titleSmall) }
                items(allDay, key = { "a" + it.task.id }) { EntryRow(it, it.task.id in overlapIds, onOpenTask) { e -> reschedule = e } }
            }
            if (timed.isNotEmpty()) {
                item { Text("Scheduled time blocks", style = MaterialTheme.typography.titleSmall) }
                items(timed, key = { "b" + it.task.id }) { EntryRow(it, it.task.id in overlapIds, onOpenTask) { e -> reschedule = e } }
            }
            if (deadlines.isNotEmpty()) {
                item { Text("Deadlines", style = MaterialTheme.typography.titleSmall) }
                items(deadlines, key = { "d" + it.task.id }) { EntryRow(it, false, onOpenTask) { e -> reschedule = e } }
            }
            item {
                SectionHeader("Unscheduled", unscheduled.size) {
                    IconButton(onClick = { showUnscheduled = !showUnscheduled }) {
                        Icon(if (showUnscheduled) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = if (showUnscheduled) "Hide unscheduled tasks" else "Show unscheduled tasks")
                    }
                }
            }
            if (showUnscheduled) {
                if (unscheduled.isEmpty()) item { Text("All active tasks have a date.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(unscheduled, key = { "u" + it.id }) { t ->
                    Card(onClick = { onOpenTask(t) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(priorityColor(t.priority), CircleShape))
                            Text(t.title, Modifier.padding(start = 12.dp).weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("No date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    reschedule?.let { entry ->
        DateTimePickerDialog(
            initial = entry.startMs,
            withTime = entry.kind != EntryKind.ALL_DAY,
            title = "Reschedule: ${entry.task.title}",
            onDismiss = { reschedule = null },
            onConfirm = { newMs -> onReschedule(entry.task, newMs - entry.startMs); reschedule = null }
        )
    }
}

@Composable
private fun MonthGrid(
    first: LocalDate, firstDow: DayOfWeek, selected: LocalDate, today: LocalDate,
    tasks: List<Task>, zone: ZoneId, onSelect: (LocalDate) -> Unit
) {
    val offset = (first.dayOfWeek.value - firstDow.value + 7) % 7
    val len = first.lengthOfMonth()
    val rows = (offset + len + 6) / 7
    Column {
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0..6) {
                    val dayNum = r * 7 + c - offset + 1
                    if (dayNum in 1..len) DayCell(first.withDayOfMonth(dayNum), selected, today, tasks, zone, Modifier.weight(1f), onSelect)
                    else Spacer(Modifier.weight(1f).height(52.dp))
                }
            }
        }
    }
}

@Composable
private fun WeekRow(start: LocalDate, selected: LocalDate, today: LocalDate, tasks: List<Task>, zone: ZoneId, onSelect: (LocalDate) -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        for (i in 0..6) DayCell(start.plusDays(i.toLong()), selected, today, tasks, zone, Modifier.weight(1f), onSelect)
    }
}

@Composable
private fun DayCell(date: LocalDate, selected: LocalDate, today: LocalDate, tasks: List<Task>, zone: ZoneId, modifier: Modifier, onSelect: (LocalDate) -> Unit) {
    val entries = remember(tasks, date) { TaskLogic.entriesForDate(tasks, date, zone) }
    val isSel = date == selected
    val isToday = date == today
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier.height(52.dp).padding(2.dp)
            .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, shape)
            .then(if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .clickable(onClickLabel = "Select ${DateFmt.day(date)}") { onSelect(date) },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Text("${date.dayOfMonth}", style = MaterialTheme.typography.bodyMedium, fontWeight = if (isToday || isSel) FontWeight.Bold else FontWeight.Normal)
        Row(Modifier.height(8.dp).padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            entries.map { it.task }.distinctBy { it.id }.sortedBy { it.priority.rank }.take(3).forEach { t ->
                Box(Modifier.size(6.dp).background(if (t.completed) Color.Gray else priorityColor(t.priority), CircleShape))
            }
        }
    }
}

@Composable
private fun EntryRow(entry: CalendarEntry, overlapping: Boolean, onOpen: (Task) -> Unit, onReschedule: (CalendarEntry) -> Unit) {
    val t = entry.task
    val timeLabel = when (entry.kind) {
        EntryKind.ALL_DAY -> "All day"
        EntryKind.BLOCK -> DateFmt.time(entry.startMs) + (entry.endMs?.let { " - ${DateFmt.time(it)}" } ?: "") + (t.durationMinutes?.let { " | ${DateFmt.duration(it)}" } ?: "")
        EntryKind.DEADLINE -> "Deadline " + if (t.allDay) "(all day)" else DateFmt.time(entry.startMs)
    }
    Card(
        onClick = { onOpen(t) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().alpha(if (t.completed) 0.6f else 1f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(6.dp).height(64.dp).background(priorityColor(t.priority)))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(t.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textDecoration = if (t.completed) TextDecoration.LineThrough else TextDecoration.None)
                Text(timeLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (entry.kind == EntryKind.BLOCK && entry.endMs == null) {
                    Text("No duration set, so no end time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (overlapping) Text("Overlaps another task", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
            IconButton(onClick = { onReschedule(entry) }) { Icon(Icons.Default.Event, contentDescription = "Reschedule ${t.title}") }
        }
    }
}
