@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.c030_prashamdamani_assignment2.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.c030_prashamdamani_assignment2.data.model.Recurrence
import com.example.c030_prashamdamani_assignment2.data.model.Task
import com.example.c030_prashamdamani_assignment2.ui.components.ConfirmDialog
import com.example.c030_prashamdamani_assignment2.ui.components.EmptyState
import com.example.c030_prashamdamani_assignment2.ui.components.PriorityBadge
import com.example.c030_prashamdamani_assignment2.util.DateFmt
import com.example.c030_prashamdamani_assignment2.util.TaskLogic
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TaskDetailScreen(
    task: Task?,
    onBack: () -> Unit,
    onEdit: (Task) -> Unit,
    onToggle: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onStartTimer: (Task) -> Unit
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Task details", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            actions = {
                if (task != null) {
                    IconButton(onClick = { onEdit(task) }) { Icon(Icons.Default.Edit, contentDescription = "Edit task") }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, contentDescription = "Delete task") }
                }
            }
        )
        if (task == null) {
            EmptyState(Icons.Default.SearchOff, "Task not found", "It may have been deleted.")
            return@Column
        }
        val now = System.currentTimeMillis()
        val overdue = TaskLogic.isOverdue(task, now)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                task.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PriorityBadge(task.priority)
                Text(
                    if (task.completed) "Completed" else if (overdue) "Overdue" else "Active",
                    color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp)
                )
            }
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Field("Description", task.description.ifBlank { "No description" })
                    Field("Importance", task.importance.label)
                    Field("Category", task.category)
                    Field("Start", task.startAt?.let { if (task.allDay) DateFmt.date(it) + " (all day)" else DateFmt.dateTime(it) } ?: "Not set")
                    Field("Due", task.dueAt?.let { if (task.allDay) DateFmt.date(it) + " (all day)" else DateFmt.dateTime(it) } ?: "Not set")
                    Field("Estimated duration", task.durationMinutes?.let { DateFmt.duration(it) } ?: "Not set")
                    Field("Calculated end", task.endAt?.let { DateFmt.dateTime(it) } ?: "Not available (needs start time and duration)")
                    Field("Reminder", DateFmt.reminderStatus(task, now))
                    Field("Repeat", recurrenceText(task))
                    Field("Notes", task.notes.ifBlank { "No notes" })
                    Field("Created", if (task.createdAt > 0) DateFmt.dateTime(task.createdAt) else "Unknown")
                    Field("Last updated", if (task.updatedAt > 0) DateFmt.dateTime(task.updatedAt) else "Unknown")
                    if (task.completed) Field("Completed at", task.completedAt?.let { DateFmt.dateTime(it) } ?: "Unknown")
                }
            }
            Button(onClick = { onToggle(task) }, modifier = Modifier.fillMaxWidth()) {
                Icon(if (task.completed) Icons.Default.Refresh else Icons.Default.CheckCircle, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (task.completed) "Reopen task" else "Mark complete")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onEdit(task) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Edit") }
                OutlinedButton(onClick = { onStartTimer(task) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Timer, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Start timer") }
            }
        }
        if (confirmDelete) {
            ConfirmDialog("Delete task?", "\"${task.title}\" will be removed. You can undo right after.", "Delete",
                onConfirm = { confirmDelete = false; onDelete(task) }, onDismiss = { confirmDelete = false })
        }
    }
}

private fun recurrenceText(t: Task): String = when (t.recurrence) {
    Recurrence.NONE -> "Does not repeat"
    Recurrence.CUSTOM -> "Every " + t.recurrenceDays.joinToString { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    else -> t.recurrence.label
}

@Composable
private fun Field(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
