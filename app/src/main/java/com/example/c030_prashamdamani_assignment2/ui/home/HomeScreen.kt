@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.c030_prashamdamani_assignment2.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.c030_prashamdamani_assignment2.data.model.Priority
import com.example.c030_prashamdamani_assignment2.data.model.Task
import com.example.c030_prashamdamani_assignment2.data.model.UserProfile
import com.example.c030_prashamdamani_assignment2.ui.components.EmptyState
import com.example.c030_prashamdamani_assignment2.ui.components.ErrorState
import com.example.c030_prashamdamani_assignment2.ui.components.LoadingBox
import com.example.c030_prashamdamani_assignment2.ui.components.OfflineBanner
import com.example.c030_prashamdamani_assignment2.ui.components.SectionHeader
import com.example.c030_prashamdamani_assignment2.ui.components.TaskActions
import com.example.c030_prashamdamani_assignment2.ui.components.TaskCard
import com.example.c030_prashamdamani_assignment2.ui.components.priorityColor
import com.example.c030_prashamdamani_assignment2.util.DateFmt
import com.example.c030_prashamdamani_assignment2.util.TaskLogic
import com.example.c030_prashamdamani_assignment2.viewmodel.TaskUiState
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun HomeScreen(
    profile: UserProfile?,
    ui: TaskUiState,
    actions: TaskActions,
    onOpenFilter: (String) -> Unit,
    onOpenReminders: () -> Unit,
    onOpenInsights: () -> Unit,
    onQuickAdd: (String, Priority) -> Unit,
    onAddTask: () -> Unit
) {
    var showQuick by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("To Do app", fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(onClick = onOpenInsights) { Icon(Icons.Default.Insights, contentDescription = "Open insights") }
                IconButton(onClick = onOpenReminders) { Icon(Icons.Default.Notifications, contentDescription = "Open reminders") }
            }
        )
        if (!ui.online) OfflineBanner()
        when {
            ui.loading -> LoadingBox()
            ui.error != null -> ErrorState(ui.error)
            else -> HomeContent(profile, ui.tasks, actions, onOpenFilter, { showQuick = true }, onAddTask, onOpenReminders)
        }
    }
    if (showQuick) QuickAddDialog(onDismiss = { showQuick = false }, onAdd = { t, p -> onQuickAdd(t, p); showQuick = false })
}

@Composable
private fun HomeContent(
    profile: UserProfile?, tasks: List<Task>, actions: TaskActions,
    onOpenFilter: (String) -> Unit, onQuick: () -> Unit, onAddTask: () -> Unit, onOpenReminders: () -> Unit
) {
    val zone = ZoneId.systemDefault()
    val now = System.currentTimeMillis()
    val today = LocalDate.now(zone)
    val stats = TaskLogic.stats(tasks, now, zone)
    val hour = java.time.LocalTime.now(zone).hour
    val name = profile?.fullName ?: "there"
    val owner = profile?.possessive ?: "My"
    val active = tasks.filter { !it.completed }
    val todayTasks = TaskLogic.sort(active.filter { TaskLogic.isDueToday(it, today, zone) }, com.example.c030_prashamdamani_assignment2.util.SortMode.DEFAULT, now)
    val overdue = TaskLogic.sort(tasks.filter { TaskLogic.isOverdue(it, now) }, com.example.c030_prashamdamani_assignment2.util.SortMode.DEFAULT, now)
    val upcoming = active.filter { it.dueAt != null && it.dueAt >= now && !TaskLogic.isDueToday(it, today, zone) }
        .sortedBy { it.dueAt }.take(5)
    val recent = tasks.sortedByDescending { it.createdAt }.take(5)
    val progress by animateFloatAsState(stats.completionPct / 100f, label = "progress")

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("${TaskLogic.greeting(hour)}, $name", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text(DateFmt.day(today), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                    if (!profile?.rollNumber.isNullOrBlank()) {
                        Text(profile!!.identity + " | Assignment 2", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.9f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Welcome back! You have ${stats.active} active task${if (stats.active == 1) "" else "s"}, ${stats.dueToday} due today.",
                        style = MaterialTheme.typography.bodyMedium, color = Color.White
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = onQuick) {
                            Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Quick add")
                        }
                        FilledTonalButton(onClick = onOpenReminders) {
                            Icon(Icons.Default.Notifications, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Reminders")
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Active tasks", stats.active, Icons.Default.Checklist, MaterialTheme.colorScheme.primary, Modifier.weight(1f)) { onOpenFilter("ACTIVE") }
                    StatCard("High priority", stats.highActive, Icons.Default.Flag, priorityColor(Priority.HIGH), Modifier.weight(1f)) { onOpenFilter("HIGH") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Due today", stats.dueToday, Icons.Default.Schedule, Color(0xFFB45309), Modifier.weight(1f)) { onOpenFilter("TODAY") }
                    StatCard("Completed", stats.completed, Icons.Default.CheckCircle, Color(0xFF2E7D32), Modifier.weight(1f)) { onOpenFilter("COMPLETED") }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("$owner progress: ${stats.completionPct}% complete", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp), strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                    Text("${stats.completed} of ${stats.total} tasks done", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("ACTIVE" to "All active", "HIGH" to "High priority", "TODAY" to "Due today", "OVERDUE" to "Overdue", "COMPLETED" to "Completed")) { (key, label) ->
                    FilterChip(selected = false, onClick = { onOpenFilter(key) }, label = { Text(label) })
                }
            }
        }
        if (tasks.isEmpty()) {
            item {
                EmptyState(Icons.Default.Checklist, "No tasks yet", "Add your first task to see your schedule and insights here.") {
                    Button(onClick = onAddTask) { Text("Add a task") }
                }
            }
        } else {
            if (overdue.isNotEmpty()) {
                item { SectionHeader("Overdue", overdue.size) }
                items(overdue, key = { "o" + it.id }) { TaskCard(it, actions) }
            }
            item { SectionHeader("$owner Today", todayTasks.size) }
            if (todayTasks.isEmpty()) item { Text("Nothing scheduled for today.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(todayTasks, key = { "t" + it.id }) { TaskCard(it, actions) }
            if (upcoming.isNotEmpty()) {
                item { SectionHeader("Upcoming deadlines", upcoming.size) }
                items(upcoming, key = { "u" + it.id }) { TaskCard(it, actions) }
            }
            item { SectionHeader("Recent tasks") }
            items(recent, key = { "r" + it.id }) { TaskCard(it, actions) }
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int, icon: ImageVector, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(Modifier.height(8.dp))
            Text("$value", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickAddDialog(onDismiss: () -> Unit, onAdd: (String, Priority) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf(Priority.MEDIUM) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick add task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Priority.values().forEach { p ->
                        FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.label) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (title.isNotBlank()) onAdd(title, priority) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
