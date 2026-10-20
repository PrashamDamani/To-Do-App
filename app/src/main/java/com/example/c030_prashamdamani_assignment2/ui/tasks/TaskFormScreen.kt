@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.c030_prashamdamani_assignment2.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.c030_prashamdamani_assignment2.data.model.Importance
import com.example.c030_prashamdamani_assignment2.data.model.Priority
import com.example.c030_prashamdamani_assignment2.data.model.Recurrence
import com.example.c030_prashamdamani_assignment2.data.model.ReminderOffsets
import com.example.c030_prashamdamani_assignment2.data.model.Task
import com.example.c030_prashamdamani_assignment2.data.model.UserProfile
import com.example.c030_prashamdamani_assignment2.ui.components.DateTimePickerDialog
import com.example.c030_prashamdamani_assignment2.util.DateFmt
import com.example.c030_prashamdamani_assignment2.util.TaskLogic
import com.example.c030_prashamdamani_assignment2.util.startMillis
import com.example.c030_prashamdamani_assignment2.util.toLocalDate
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TaskFormScreen(
    existing: Task?,
    profile: UserProfile?,
    allTasks: List<Task>,
    categories: List<String>,
    onSave: (Task) -> Unit,
    onBack: () -> Unit,
    onAddCategory: (String) -> Unit,
    requestNotificationPermission: () -> Unit
) {
    val zone = ZoneId.systemDefault()
    val key = existing?.id ?: "new"
    var title by rememberSaveable(key) { mutableStateOf(existing?.title ?: "") }
    var description by rememberSaveable(key) { mutableStateOf(existing?.description ?: "") }
    var notes by rememberSaveable(key) { mutableStateOf(existing?.notes ?: "") }
    var priority by rememberSaveable(key) { mutableStateOf(existing?.priority ?: Priority.MEDIUM) }
    var importance by rememberSaveable(key) { mutableStateOf(existing?.importance ?: Importance.NORMAL) }
    var category by rememberSaveable(key) { mutableStateOf(existing?.category ?: "Other") }
    var allDay by rememberSaveable(key) { mutableStateOf(existing?.allDay ?: false) }
    var startAt by rememberSaveable(key) { mutableStateOf(existing?.startAt) }
    var dueAt by rememberSaveable(key) { mutableStateOf(existing?.dueAt) }
    var durationText by rememberSaveable(key) { mutableStateOf(existing?.durationMinutes?.toString() ?: "") }
    var reminderOn by rememberSaveable(key) { mutableStateOf(existing?.reminderEnabled ?: false) }
    var offset by rememberSaveable(key) { mutableStateOf(existing?.reminderOffsetMinutes ?: 0) }
    var customReminder by rememberSaveable(key) { mutableStateOf(if ((existing?.reminderOffsetMinutes ?: 0) == -1) existing?.reminderAt else null) }
    var recurrence by rememberSaveable(key) { mutableStateOf(existing?.recurrence ?: Recurrence.NONE) }
    var daysText by rememberSaveable(key) { mutableStateOf(existing?.recurrenceDays?.joinToString(",") ?: "") }
    var error by rememberSaveable(key) { mutableStateOf<String?>(null) }
    var picker by rememberSaveable { mutableStateOf("") } // start | due | custom
    var showCategoryDialog by rememberSaveable { mutableStateOf(false) }

    val customDays = daysText.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    val reminderAt: Long? = if (!reminderOn) null else if (offset == -1) customReminder else TaskLogic.computeReminderAt(offset, startAt, dueAt)
    val durationMin = durationText.trim().toIntOrNull()
    val conflicts = TaskLogic.conflicts(allTasks, startAt, durationMin, allDay, existing?.id)

    fun build(): Task? {
        if (durationText.isNotBlank() && durationMin == null) { error = "Duration must be a whole number of minutes"; return null }
        val s = if (allDay) startAt?.toLocalDate(zone)?.startMillis(zone) else startAt
        val d = if (allDay) dueAt?.toLocalDate(zone)?.startMillis(zone) else dueAt
        val dur = if (allDay) null else durationMin
        if (reminderOn && reminderAt == null) { error = "Set a start/due time first, or choose a custom reminder time"; return null }
        val msg = TaskLogic.validate(title, s, d, dur, reminderOn, reminderAt, existing?.completed ?: false, System.currentTimeMillis())
        if (msg != null) { error = msg; return null }
        if (recurrence != Recurrence.NONE && s == null && d == null) { error = "Repeating tasks need a start or due date"; return null }
        if (recurrence == Recurrence.CUSTOM && customDays.isEmpty()) { error = "Pick at least one repeat day"; return null }
        error = null
        return (existing ?: Task()).copy(
            title = title.trim(), description = description.trim(), notes = notes.trim(),
            priority = priority, importance = importance, category = category,
            allDay = allDay, startAt = s, dueAt = d, durationMinutes = dur,
            reminderEnabled = reminderOn, reminderAt = reminderAt, reminderOffsetMinutes = offset,
            recurrence = recurrence,
            recurrenceDays = if (recurrence == Recurrence.CUSTOM) customDays.sorted() else emptyList()
        )
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopAppBar(
            title = { Text(if (existing == null) "New task for ${profile?.firstName ?: "me"}" else "Edit task", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = title, onValueChange = { title = it; error = null }, label = { Text("Title *") }, singleLine = true,
                isError = error?.contains("Title") == true, supportingText = { Text("${title.length}/100") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, minLines = 2, modifier = Modifier.fillMaxWidth())

            Label("Priority")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.values().forEach { p -> FilterChip(priority == p, { priority = p }, { Text(p.label) }) }
            }
            Label("Importance")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Importance.values().forEach { i -> FilterChip(importance == i, { importance = i }, { Text(i.label) }) }
            }
            Label("Category")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { c -> FilterChip(category == c, { category = c }, { Text(c) }) }
                FilterChip(false, { showCategoryDialog = true }, { Text("New") }, leadingIcon = { Icon(Icons.Default.Add, contentDescription = "Add custom category") })
            }

            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("All-day (date only)", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Switch(checked = allDay, onCheckedChange = { allDay = it })
            }
            DateRow("Start", startAt, allDay, { picker = "start" }, { startAt = null })
            DateRow("Due / deadline", dueAt, allDay, { picker = "due" }, { dueAt = null })
            if (!allDay) {
                OutlinedTextField(
                    value = durationText, onValueChange = { durationText = it.filter { c -> c.isDigit() }.take(4); error = null },
                    label = { Text("Estimated duration (minutes)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        val end = startAt?.let { s -> durationMin?.let { s + it * 60_000L } }
                        Text(if (end != null) "Estimated end: ${DateFmt.dateTime(end)}" else "Set start + duration to get an end time")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (conflicts.isNotEmpty()) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error)
                    Text(
                        "  Overlaps with: " + conflicts.joinToString { it.title },
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Reminder", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Switch(checked = reminderOn, onCheckedChange = { reminderOn = it; if (it) requestNotificationPermission() })
            }
            if (reminderOn) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderOffsets.options.forEach { (label, min) ->
                        FilterChip(offset == min, { offset = min; if (min == -1) picker = "custom" }, { Text(label) })
                    }
                }
                if (offset == -1) DateRow("Reminder at", customReminder, false, { picker = "custom" }, { customReminder = null })
                Text(
                    reminderAt?.let { "Will remind at ${DateFmt.dateTime(it)}" } ?: "Set a start/due time (or a custom time) to calculate the reminder",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("A deadline alone never creates a notification; only this switch does.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider()
            Label("Repeat")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Recurrence.values().forEach { r -> FilterChip(recurrence == r, { recurrence = r }, { Text(r.label) }) }
            }
            if (recurrence == Recurrence.CUSTOM) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayOfWeek.values().forEach { dow ->
                        FilterChip(
                            selected = dow.value in customDays,
                            onClick = { daysText = (if (dow.value in customDays) customDays - dow.value else customDays + dow.value).sorted().joinToString(",") },
                            label = { Text(dow.getDisplayName(TextStyle.SHORT, Locale.getDefault())) }
                        )
                    }
                }
            }
            if (recurrence != Recurrence.NONE) {
                Text("When you complete this task, the next occurrence is created automatically.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            Button(onClick = { build()?.let(onSave) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(if (existing == null) "Add task" else "Save changes")
            }
            Row(Modifier.height(16.dp)) {}
        }
    }

    when (picker) {
        "start" -> DateTimePickerDialog(startAt, !allDay, "Start", { picker = "" }) { startAt = it; picker = ""; error = null }
        "due" -> DateTimePickerDialog(dueAt, !allDay, "Due", { picker = "" }) { dueAt = it; picker = ""; error = null }
        "custom" -> DateTimePickerDialog(customReminder, true, "Reminder", { picker = ""; if (customReminder == null) offset = 0 }) { customReminder = it; picker = ""; error = null }
    }
    if (showCategoryDialog) {
        var name by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("New category") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, label = { Text("Name") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { onAddCategory(name); category = name.trim().ifEmpty { category }; showCategoryDialog = false }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { showCategoryDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun Label(text: String) = Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))

@Composable
private fun DateRow(label: String, value: Long?, dateOnly: Boolean, onPick: () -> Unit, onClear: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth().clickable(onClick = onPick)) {
        Row(Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Event, contentDescription = null)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    value?.let { if (dateOnly) DateFmt.date(it) else DateFmt.dateTime(it) } ?: "Not set",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            if (value != null) IconButton(onClick = onClear) { Icon(Icons.Default.Close, contentDescription = "Clear $label") }
        }
    }
}
