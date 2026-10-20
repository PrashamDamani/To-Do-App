@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.c030_prashamdamani_assignment2.ui.insights

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.c030_prashamdamani_assignment2.data.model.Priority
import com.example.c030_prashamdamani_assignment2.data.model.UserProfile
import com.example.c030_prashamdamani_assignment2.ui.components.EmptyState
import com.example.c030_prashamdamani_assignment2.ui.components.LoadingBox
import com.example.c030_prashamdamani_assignment2.ui.components.priorityColor
import com.example.c030_prashamdamani_assignment2.util.TaskLogic
import com.example.c030_prashamdamani_assignment2.viewmodel.TaskUiState
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun InsightsScreen(profile: UserProfile?, ui: TaskUiState, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("${profile?.possessive ?: "My"} Insights", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
        )
        if (ui.loading) { LoadingBox(); return@Column }
        if (ui.tasks.isEmpty()) {
            EmptyState(Icons.Default.Insights, "Not enough data", "Add and complete some tasks to see insights.")
            return@Column
        }
        val now = System.currentTimeMillis()
        val s = TaskLogic.stats(ui.tasks, now)
        val weekly = TaskLogic.weeklyCompletion(ui.tasks, now)
        val maxWeek = (weekly.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
        val byCategory = ui.tasks.groupingBy { it.category }.eachCount().toList().sortedByDescending { it.second }
        val progress by animateFloatAsState(s.completionPct / 100f, label = "completion")

        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Completion: ${s.completionPct}%", style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(8.dp))
                    Text("Total ${s.total}  |  Active ${s.active}  |  Completed ${s.completed}", style = MaterialTheme.typography.bodyMedium)
                    Text("Overdue ${s.overdue}  |  Completed today ${s.completedToday}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tasks by priority", style = MaterialTheme.typography.titleMedium)
                    Priority.values().forEach { p ->
                        val n = ui.tasks.count { it.priority == p }
                        BarRow(p.label, n, ui.tasks.size, priorityColor(p))
                    }
                }
            }
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tasks by category", style = MaterialTheme.typography.titleMedium)
                    byCategory.forEach { (c, n) -> BarRow(c, n, ui.tasks.size, MaterialTheme.colorScheme.secondary) }
                }
            }
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Completed in the last 7 days", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth().height(140.dp).padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                        weekly.forEach { (d, n) ->
                            val frac by animateFloatAsState(n.toFloat() / maxWeek, label = "bar")
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxHeight()) {
                                Text("$n", style = MaterialTheme.typography.labelSmall)
                                Box(Modifier.width(24.dp).height((80 * frac + 2).dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.primary))
                                Text(d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarRow(label: String, count: Int, total: Int, color: Color) {
    val frac by animateFloatAsState(if (total == 0) 0f else count.toFloat() / total, label = "barRow")
    Column {
        Row { Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium); Text("$count", style = MaterialTheme.typography.bodyMedium) }
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(color))
        }
    }
}
