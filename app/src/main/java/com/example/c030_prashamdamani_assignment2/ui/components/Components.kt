@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.c030_prashamdamani_assignment2.ui.components

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.c030_prashamdamani_assignment2.data.model.Priority
import com.example.c030_prashamdamani_assignment2.data.model.Task
import com.example.c030_prashamdamani_assignment2.util.DateFmt
import com.example.c030_prashamdamani_assignment2.util.TaskLogic
import com.example.c030_prashamdamani_assignment2.util.combineDateTime
import com.example.c030_prashamdamani_assignment2.util.toLocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import androidx.compose.ui.graphics.vector.ImageVector

val LocalReduceMotion = compositionLocalOf { false }

fun priorityColor(p: Priority): Color = when (p) {
    Priority.HIGH -> Color(0xFFC62828)
    Priority.MEDIUM -> Color(0xFFB45309)
    Priority.LOW -> Color(0xFF2E7D32)
}

data class TaskActions(
    val onToggle: (Task) -> Unit,
    val onDelete: (Task) -> Unit,
    val onOpen: (Task) -> Unit,
    val onEdit: (Task) -> Unit
)

@Composable
fun PriorityBadge(priority: Priority, modifier: Modifier = Modifier) {
    val c by animateColorAsState(priorityColor(priority), label = "priorityColor")
    Surface(color = c.copy(alpha = 0.12f), shape = RoundedCornerShape(50), modifier = modifier) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Flag, contentDescription = null, tint = c, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(priority.label, color = c, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun AnimatedCheck(checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val scale by animateFloatAsState(
        if (checked) 1f else 0f,
        if (reduce) snap() else spring(Spring.DampingRatioMediumBouncy), label = "checkScale"
    )
    val fill by animateColorAsState(if (checked) MaterialTheme.colorScheme.primary else Color.Transparent, label = "checkFill")
    val border by animateColorAsState(if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, label = "checkBorder")
    Box(
        modifier
            .size(48.dp)
            .clip(CircleShape)
            .semantics { contentDescription = if (checked) "Completed. Tap to reopen" else "Not completed. Tap to complete" }
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() }),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(24.dp).border(2.dp, border, CircleShape).background(fill, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp).scale(scale))
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    actions: TaskActions,
    modifier: Modifier = Modifier,
    overlapping: Boolean = false
) {
    var expanded by rememberSaveable(task.id) { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val overdue = TaskLogic.isOverdue(task, now)
    val alpha by animateFloatAsState(if (task.completed) 0.6f else 1f, label = "cardAlpha")
    val reduce = LocalReduceMotion.current
    Card(
        onClick = { actions.onOpen(task) },
        modifier = modifier.fillMaxWidth().then(if (reduce) Modifier else Modifier.animateContentSize()),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp).alpha(alpha)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedCheck(task.completed, { actions.onToggle(task) })
                Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Text(
                        task.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text(
                        (if (overdue) "Overdue - " else "") + DateFmt.when_(task),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (overlapping) {
                        Text("Overlaps another scheduled task", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                PriorityBadge(task.priority)
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse details" else "Expand details"
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = if (reduce) fadeIn(snap()) else expandVertically() + fadeIn(),
                exit = if (reduce) fadeOut(snap()) else shrinkVertically() + fadeOut()
            ) {
                Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)) {
                    if (task.description.isNotBlank()) {
                        Text(task.description, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(6.dp))
                    }
                    Text("${task.category} | ${task.importance.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(DateFmt.reminderStatus(task, now), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { actions.onEdit(task) }) {
                            Icon(Icons.Default.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Edit")
                        }
                        TextButton(onClick = { actions.onDelete(task) }) {
                            Icon(Icons.Default.Delete, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

/** Swipe right = complete/reopen, swipe left = delete (with undo snackbar from the ViewModel). */
@Composable
fun SwipeableTaskCard(task: Task, actions: TaskActions, modifier: Modifier = Modifier, overlapping: Boolean = false) {
    val currentTask by rememberUpdatedState(task)
    val currentActions by rememberUpdatedState(actions)
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> { currentActions.onDelete(currentTask); true }
                SwipeToDismissBoxValue.StartToEnd -> { currentActions.onToggle(currentTask); false }
                else -> false
            }
        }
    )
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        backgroundContent = {
            val dir = state.dismissDirection
            val color = when (dir) {
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF2E7D32)
                else -> Color.Transparent
            }
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(color).padding(horizontal = 20.dp),
                contentAlignment = if (dir == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                if (dir != SwipeToDismissBoxValue.Settled) {
                    Icon(
                        if (dir == SwipeToDismissBoxValue.EndToStart) Icons.Default.Delete else Icons.Default.Check,
                        contentDescription = if (dir == SwipeToDismissBoxValue.EndToStart) "Delete task" else "Toggle complete",
                        tint = Color.White
                    )
                }
            }
        }
    ) {
        TaskCard(task, actions, overlapping = overlapping)
    }
}

@Composable
fun SectionHeader(title: String, count: Int? = null, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (count != null) "$title ($count)" else title,
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        action?.invoke()
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

@Composable
fun ErrorState(message: String, modifier: Modifier = Modifier) {
    EmptyState(Icons.Default.ErrorOutline, "Something went wrong", message, modifier)
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun OfflineBanner() {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Offline - showing saved data. Changes sync when you reconnect.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Two-step date (and optional time) picker. Returns UTC epoch millis in the device time zone. */
@Composable
fun DateTimePickerDialog(
    initial: Long?, withTime: Boolean, title: String,
    onDismiss: () -> Unit, onConfirm: (Long) -> Unit
) {
    val zone = ZoneId.systemDefault()
    val init = Instant.ofEpochMilli(initial ?: System.currentTimeMillis()).atZone(zone)
    var step by remember { mutableIntStateOf(0) }
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = init.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    val timeState = rememberTimePickerState(
        initialHour = init.hour, initialMinute = init.minute,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    )
    if (step == 0) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { d ->
                        if (withTime) step = 1 else onConfirm(combineDateTime(d, 0, 0))
                    }
                }) { Text(if (withTime) "Next" else "OK") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        ) { DatePicker(state = dateState, title = { Text(title, Modifier.padding(start = 24.dp, top = 16.dp)) }) }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { d -> onConfirm(combineDateTime(d, timeState.hour, timeState.minute)) }
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text("Back") } }
        )
    }
}

@Composable
fun ConfirmDialog(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
