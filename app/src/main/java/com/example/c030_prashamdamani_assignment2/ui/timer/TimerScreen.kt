@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.c030_prashamdamani_assignment2.ui.timer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.c030_prashamdamani_assignment2.data.model.Task
import com.example.c030_prashamdamani_assignment2.data.model.UserProfile
import com.example.c030_prashamdamani_assignment2.util.DateFmt
import com.example.c030_prashamdamani_assignment2.viewmodel.TimerState

@Composable
fun TimerScreen(
    profile: UserProfile?,
    state: TimerState,
    tasks: List<Task>,
    presetTask: Task?,
    onBack: () -> Unit,
    onConfigure: (Long) -> Unit,
    onLink: (Task?) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
) {
    LaunchedEffect(presetTask?.id) {
        if (presetTask != null && !state.running && state.taskId != presetTask.id) {
            val dur = presetTask.durationMinutes?.let { it * 60_000L } ?: state.totalMs
            onConfigure(dur)
            onLink(presetTask)
        }
    }
    var customMin by rememberSaveable { mutableStateOf("") }
    var linkDialog by rememberSaveable { mutableStateOf(false) }
    val progress by animateFloatAsState(
        if (state.totalMs == 0L) 0f else state.remainingMs.toFloat() / state.totalMs, label = "timerProgress"
    )
    val paused = !state.running && state.remainingMs in 1 until state.totalMs

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("${profile?.possessive ?: "My"} Timer", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
        )
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(240.dp), strokeWidth = 10.dp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(DateFmt.mmss(state.remainingMs), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                    Text(
                        when { state.finished -> "Time is up"; state.running -> "Running"; paused -> "Paused"; else -> "Ready" },
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                state.taskTitle?.let { "Linked task: $it" } ?: "Not linked to a task",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.running) {
                    Button(onClick = onPause) { Icon(Icons.Default.Pause, null); Spacer(Modifier.width(6.dp)); Text("Pause") }
                } else {
                    Button(onClick = onStart, enabled = state.remainingMs > 0) {
                        Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text(if (paused) "Resume" else "Start")
                    }
                }
                OutlinedButton(onClick = onReset) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Reset") }
            }
            if (!state.running) {
                Text("Duration", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(5, 10, 15, 25, 45, 60).forEach { m ->
                        FilterChip(state.totalMs == m * 60_000L, { onConfigure(m * 60_000L) }, { Text("$m min") })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customMin, onValueChange = { customMin = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Custom minutes") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = { customMin.toLongOrNull()?.takeIf { it in 1..1440 }?.let { onConfigure(it * 60_000L) } }) { Text("Set") }
                }
                OutlinedButton(onClick = { linkDialog = true }) { Text(if (state.taskId == null) "Link a task (optional)" else "Change linked task") }
                if (state.taskId != null) TextButton(onClick = { onLink(null) }) { Text("Unlink task") }
            }
            Text(
                "The timer is separate from task deadlines and reminders. A notification is posted when it finishes.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    if (linkDialog) {
        val active = tasks.filter { !it.completed }
        AlertDialog(
            onDismissRequest = { linkDialog = false },
            title = { Text("Link a task") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (active.isEmpty()) Text("No active tasks.")
                    active.forEach { t -> TextButton(onClick = { onLink(t); linkDialog = false }) { Text(t.title) } }
                }
            },
            confirmButton = { TextButton(onClick = { linkDialog = false }) { Text("Close") } }
        )
    }
}
