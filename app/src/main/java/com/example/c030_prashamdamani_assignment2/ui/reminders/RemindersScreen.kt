@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.c030_prashamdamani_assignment2.ui.reminders

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.c030_prashamdamani_assignment2.data.model.Task
import com.example.c030_prashamdamani_assignment2.data.model.UserProfile
import com.example.c030_prashamdamani_assignment2.reminders.NotificationHelper
import com.example.c030_prashamdamani_assignment2.reminders.ReminderScheduler
import com.example.c030_prashamdamani_assignment2.ui.components.DateTimePickerDialog
import com.example.c030_prashamdamani_assignment2.ui.components.EmptyState
import com.example.c030_prashamdamani_assignment2.ui.components.LoadingBox
import com.example.c030_prashamdamani_assignment2.ui.components.OfflineBanner
import com.example.c030_prashamdamani_assignment2.ui.components.PriorityBadge
import com.example.c030_prashamdamani_assignment2.ui.components.SectionHeader
import com.example.c030_prashamdamani_assignment2.util.DateFmt
import com.example.c030_prashamdamani_assignment2.util.openExactAlarmSettings
import com.example.c030_prashamdamani_assignment2.util.openNotificationSettings
import com.example.c030_prashamdamani_assignment2.viewmodel.TaskUiState

@Composable
fun RemindersScreen(
    profile: UserProfile?,
    ui: TaskUiState,
    onSetReminder: (Task, Boolean, Long?, Int) -> Unit,
    onOpenTask: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    requestNotificationPermission: () -> Unit
) {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val notificationsOn = remember(tick) { NotificationHelper.enabled(ctx) }
    val exactOk = remember(tick) { ReminderScheduler(ctx).canScheduleExact() }
    var rescheduling by remember { mutableStateOf<Task?>(null) }
    val now = System.currentTimeMillis()
    val withReminder = ui.tasks.filter { !it.completed && it.reminderAt != null }
    val upcoming = withReminder.filter { it.reminderEnabled && it.reminderAt!! > now }.sortedBy { it.reminderAt }
    val missed = withReminder.filter { it.reminderEnabled && it.reminderAt!! <= now }.sortedByDescending { it.reminderAt }
    val off = withReminder.filter { !it.reminderEnabled }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("${profile?.possessive ?: "My"} Reminders", fontWeight = FontWeight.Bold) })
        if (!ui.online) OfflineBanner()
        if (ui.loading) { LoadingBox(); return@Column }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!notificationsOn) item {
                StatusCard("Notifications are blocked", "Reminders cannot be shown until you allow notifications.", "Allow") {
                    if (Build.VERSION.SDK_INT >= 33) requestNotificationPermission()
                    openNotificationSettings(ctx)
                }
            }
            if (!exactOk) item {
                StatusCard("Exact alarms not allowed", "Reminders still work but Android may deliver them a few minutes late.", "Settings") { openExactAlarmSettings(ctx) }
            }
            if (withReminder.isEmpty()) item {
                EmptyState(Icons.Default.NotificationsOff, "No reminders", "Turn on a reminder when adding or editing a task.")
            }
            if (upcoming.isNotEmpty()) {
                item { SectionHeader("Upcoming", upcoming.size) }
                items(upcoming, key = { "up" + it.id }) { ReminderRow(it, now, onSetReminder, onOpenTask, onEditTask) { t -> rescheduling = t } }
            }
            if (missed.isNotEmpty()) {
                item { SectionHeader("Time passed", missed.size) }
                items(missed, key = { "ms" + it.id }) { ReminderRow(it, now, onSetReminder, onOpenTask, onEditTask) { t -> rescheduling = t } }
            }
            if (off.isNotEmpty()) {
                item { SectionHeader("Turned off", off.size) }
                items(off, key = { "of" + it.id }) { ReminderRow(it, now, onSetReminder, onOpenTask, onEditTask) { t -> rescheduling = t } }
            }
        }
    }
    rescheduling?.let { t ->
        DateTimePickerDialog(t.reminderAt, true, "Reminder for ${t.title}", { rescheduling = null }) { at ->
            if (at <= System.currentTimeMillis()) onSetReminder(t, false, at, -1) // invalid: stored but off
            else onSetReminder(t, true, at, -1)
            rescheduling = null
        }
    }
}

@Composable
private fun StatusCard(title: String, text: String, action: String, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(text, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onClick) { Text(action) }
        }
    }
}

@Composable
private fun ReminderRow(
    t: Task, now: Long,
    onSetReminder: (Task, Boolean, Long?, Int) -> Unit,
    onOpen: (Task) -> Unit, onEdit: (Task) -> Unit, onReschedule: (Task) -> Unit
) {
    val future = (t.reminderAt ?: 0L) > now
    Card(onClick = { onOpen(t) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(DateFmt.reminderStatus(t, now), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                PriorityBadge(t.priority)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = t.reminderEnabled && future,
                    onCheckedChange = { on ->
                        if (!on) onSetReminder(t, false, t.reminderAt, t.reminderOffsetMinutes)
                        else if (future) onSetReminder(t, true, t.reminderAt, t.reminderOffsetMinutes)
                        else onReschedule(t)
                    }
                )
                Text(if (t.reminderEnabled && future) "On" else "Off", Modifier.padding(start = 8.dp).weight(1f), style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { onReschedule(t) }) { Icon(Icons.Default.Event, contentDescription = "Reschedule reminder for ${t.title}") }
                IconButton(onClick = { onEdit(t) }) { Icon(Icons.Default.Edit, contentDescription = "Edit ${t.title}") }
            }
        }
    }
}
