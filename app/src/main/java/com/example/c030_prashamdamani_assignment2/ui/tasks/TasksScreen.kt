@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.c030_prashamdamani_assignment2.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.c030_prashamdamani_assignment2.data.model.Importance
import com.example.c030_prashamdamani_assignment2.data.model.Priority
import com.example.c030_prashamdamani_assignment2.data.model.UserProfile
import com.example.c030_prashamdamani_assignment2.ui.components.EmptyState
import com.example.c030_prashamdamani_assignment2.ui.components.ErrorState
import com.example.c030_prashamdamani_assignment2.ui.components.LoadingBox
import com.example.c030_prashamdamani_assignment2.ui.components.OfflineBanner
import com.example.c030_prashamdamani_assignment2.ui.components.SectionHeader
import com.example.c030_prashamdamani_assignment2.ui.components.SwipeableTaskCard
import com.example.c030_prashamdamani_assignment2.ui.components.TaskActions
import com.example.c030_prashamdamani_assignment2.util.DateFilter
import com.example.c030_prashamdamani_assignment2.util.SortMode
import com.example.c030_prashamdamani_assignment2.util.StatusFilter
import com.example.c030_prashamdamani_assignment2.util.TaskFilter
import com.example.c030_prashamdamani_assignment2.util.TaskLogic
import com.example.c030_prashamdamani_assignment2.viewmodel.TaskUiState

@Composable
fun TasksScreen(
    profile: UserProfile?,
    ui: TaskUiState,
    categories: List<String>,
    initialFilter: String,
    defaultSort: SortMode,
    actions: TaskActions,
    onAddTask: () -> Unit
) {
    var filter by rememberSaveable(initialFilter) {
        mutableStateOf(TaskLogic.presetFilter(initialFilter).let { if (it.sort == SortMode.DEFAULT) it.copy(sort = defaultSort) else it })
    }
    var searching by rememberSaveable { mutableStateOf(false) }
    var sortMenu by rememberSaveable { mutableStateOf(false) }
    var sheet by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("${profile?.possessive ?: "My"} Tasks", fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(onClick = { searching = !searching; if (!searching) filter = filter.copy(query = "") }) {
                    Icon(if (searching) Icons.Default.Close else Icons.Default.Search, contentDescription = if (searching) "Close search" else "Search tasks")
                }
                IconButton(onClick = { sheet = true }) {
                    BadgedBox(badge = { if (filter.advancedCount > 0) Badge { Text("${filter.advancedCount}") } }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter tasks")
                    }
                }
                IconButton(onClick = { sortMenu = true }) { Icon(Icons.Default.Sort, contentDescription = "Sort tasks") }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    SortMode.values().forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m.label + if (filter.sort == m) "  (selected)" else "") },
                            onClick = { filter = filter.copy(sort = m); sortMenu = false }
                        )
                    }
                }
            }
        )
        if (!ui.online) OfflineBanner()
        if (searching) {
            OutlinedTextField(
                value = filter.query, onValueChange = { filter = filter.copy(query = it) },
                label = { Text("Search title, description, notes") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusFilter.values().forEach { s ->
                FilterChip(selected = filter.status == s, onClick = { filter = filter.copy(status = s) }, label = { Text(s.label) })
            }
        }
        val now = System.currentTimeMillis()
        when {
            ui.loading -> LoadingBox()
            ui.error != null -> ErrorState(ui.error)
            else -> {
                val list = TaskLogic.applyFilter(ui.tasks, filter, now)
                val overdue = list.filter { TaskLogic.isOverdue(it, now) }
                val rest = list.filterNot { TaskLogic.isOverdue(it, now) }
                if (list.isEmpty()) {
                    if (ui.tasks.isEmpty()) {
                        EmptyState(Icons.Default.Checklist, "No tasks yet", "Tap + to add your first task.")
                    } else {
                        EmptyState(Icons.Default.SearchOff, "No matching tasks", "Try changing the filters or search.") {
                            TextButton(onClick = { filter = TaskFilter(sort = defaultSort); searching = false }) { Text("Clear filters") }
                        }
                    }
                } else {
                    val overlapIds = remember(ui.tasks) { overlapAll(ui.tasks) }
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (overdue.isNotEmpty()) {
                            item(key = "hdr_overdue") { SectionHeader("Overdue", overdue.size) }
                            items(overdue, key = { it.id }) { t ->
                                SwipeableTaskCard(t, actions, Modifier.animateItem(), t.id in overlapIds)
                            }
                        }
                        if (rest.isNotEmpty()) {
                            item(key = "hdr_rest") {
                                SectionHeader(if (filter.status == StatusFilter.COMPLETED) "Completed" else if (overdue.isEmpty()) "Tasks" else "Other tasks", rest.size)
                            }
                            items(rest, key = { it.id }) { t ->
                                SwipeableTaskCard(t, actions, Modifier.animateItem(), t.id in overlapIds)
                            }
                        }
                    }
                }
            }
        }
    }

    if (sheet) {
        ModalBottomSheet(onDismissRequest = { sheet = false }, sheetState = rememberModalBottomSheetState()) {
            FilterSheet(filter, categories, onChange = { filter = it }, onReset = { filter = TaskFilter(sort = defaultSort) }, onClose = { sheet = false })
        }
    }
}

private fun overlapAll(tasks: List<com.example.c030_prashamdamani_assignment2.data.model.Task>): Set<String> {
    val zone = java.time.ZoneId.systemDefault()
    val dates = tasks.filter { !it.completed && it.startAt != null }.mapNotNull { it.startAt?.let { s -> java.time.Instant.ofEpochMilli(s).atZone(zone).toLocalDate() } }.distinct()
    return dates.flatMap { TaskLogic.overlappingIds(TaskLogic.entriesForDate(tasks, it, zone)) }.toSet()
}

@Composable
private fun FilterSheet(filter: TaskFilter, categories: List<String>, onChange: (TaskFilter) -> Unit, onReset: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Filters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Priority", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(filter.priority == null, { onChange(filter.copy(priority = null)) }, { Text("Any") })
            Priority.values().forEach { p -> FilterChip(filter.priority == p, { onChange(filter.copy(priority = p)) }, { Text(p.label) }) }
        }
        Text("Importance", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(filter.importance == null, { onChange(filter.copy(importance = null)) }, { Text("Any") })
            Importance.values().forEach { i -> FilterChip(filter.importance == i, { onChange(filter.copy(importance = i)) }, { Text(i.label) }) }
        }
        Text("Category", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(filter.category == null, { onChange(filter.copy(category = null)) }, { Text("Any") })
            categories.forEach { c -> FilterChip(filter.category == c, { onChange(filter.copy(category = c)) }, { Text(c) }) }
        }
        Text("Date", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateFilter.values().forEach { d -> FilterChip(filter.date == d, { onChange(filter.copy(date = d)) }, { Text(d.label) }) }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Overdue only", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Switch(checked = filter.overdueOnly, onCheckedChange = { onChange(filter.copy(overdueOnly = it)) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onReset) { Text("Reset") }
            TextButton(onClick = onClose) { Text("Done") }
        }
    }
}
