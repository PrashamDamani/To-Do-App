package com.example.c030_prashamdamani_assignment2.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.c030_prashamdamani_assignment2.data.model.Categories
import com.example.c030_prashamdamani_assignment2.data.model.Recurrence
import com.example.c030_prashamdamani_assignment2.data.model.Task
import com.example.c030_prashamdamani_assignment2.data.repository.TaskRepository
import com.example.c030_prashamdamani_assignment2.reminders.ReminderScheduler
import com.example.c030_prashamdamani_assignment2.util.TaskLogic
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskUiState(
    val loading: Boolean = true,
    val tasks: List<Task> = emptyList(),
    val error: String? = null,
    val online: Boolean = true
)

data class UiEvent(val text: String, val undoable: Boolean = false)

class TaskViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TaskRepository()
    private val scheduler = ReminderScheduler(app)

    private val _ui = MutableStateFlow(TaskUiState())
    val ui: StateFlow<TaskUiState> = _ui.asStateFlow()

    private val _categories = MutableStateFlow(Categories.defaults)
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private var uid: String? = null
    private var jobs: List<Job> = emptyList()
    private var lastDeleted: Task? = null

    /** Called whenever the authenticated user changes. Cancels old listeners (no duplicates). */
    fun setUser(newUid: String?) {
        if (newUid == uid) return
        jobs.forEach { it.cancel() }
        jobs = emptyList()
        if (uid != null) scheduler.cancelAll()
        uid = newUid
        lastDeleted = null
        _ui.value = TaskUiState(loading = newUid != null)
        _categories.value = Categories.defaults
        if (newUid == null) return
        jobs = listOf(
            viewModelScope.launch {
                repo.observeTasks(newUid).collect { r ->
                    r.onSuccess { list ->
                        _ui.update { it.copy(loading = false, tasks = list, error = null) }
                        scheduler.syncAll(list)
                    }.onFailure { e ->
                        _ui.update { it.copy(loading = false, error = e.message ?: "Database error") }
                    }
                }
            },
            viewModelScope.launch { repo.observeConnection().collect { c -> _ui.update { it.copy(online = c) } } },
            viewModelScope.launch {
                repo.observeCategories(newUid).collect { c -> _categories.value = (Categories.defaults + c).distinct() }
            },
            viewModelScope.launch {
                // Offline with an empty cache never delivers data: stop the spinner eventually.
                delay(8000)
                _ui.update { if (it.loading) it.copy(loading = false) else it }
            }
        )
    }

    private fun emit(text: String, undoable: Boolean = false) {
        _events.tryEmit(UiEvent(text, undoable))
    }

    private fun fail(msg: String) = emit("Sync problem: $msg")

    private fun applySchedule(t: Task) {
        if (t.reminderEnabled && !t.completed) {
            val r = scheduler.schedule(t)
            when {
                !r.scheduled -> Unit
                !r.notificationsAllowed -> emit("Reminder saved, but notifications are blocked. Enable them in system settings.")
                !r.exact -> emit("Reminder saved. Exact alarms are not allowed, so it may arrive a little late.")
            }
        } else scheduler.cancel(t.id)
    }

    fun saveTask(task: Task, onDone: () -> Unit = {}) {
        val u = uid ?: return
        val now = System.currentTimeMillis()
        val isNew = task.id.isBlank()
        val id = if (isNew) repo.newId(u) else task.id
        val toSave = task.copy(
            id = id, ownerUid = u, title = task.title.trim(),
            createdAt = if (isNew) now else task.createdAt, updatedAt = now
        )
        if (isNew) repo.create(u, toSave, ::fail) else repo.update(u, id, toSave.editableMap(), ::fail)
        applySchedule(toSave)
        emit(if (isNew) "Task added" else "Task updated")
        onDone()
    }

    fun quickAdd(title: String, priority: com.example.c030_prashamdamani_assignment2.data.model.Priority) {
        if (title.isBlank()) return emit("Enter a title")
        saveTask(Task(title = title, priority = priority))
    }

    fun toggleComplete(t: Task) {
        val u = uid ?: return
        val now = System.currentTimeMillis()
        val done = !t.completed
        repo.update(
            u, t.id,
            mapOf<String, Any?>("completed" to done, "completedAt" to (if (done) now else null), "updatedAt" to now),
            ::fail
        )
        if (done) {
            scheduler.cancel(t.id)
            if (t.recurrence != Recurrence.NONE) {
                val next = TaskLogic.nextOccurrence(t)
                if (next != null) {
                    val nt = next.copy(id = repo.newId(u), ownerUid = u, createdAt = now, updatedAt = now)
                    repo.create(u, nt, ::fail)
                    applySchedule(nt)
                    emit("Completed. Next occurrence created.")
                    return
                }
            }
            emit("Task completed")
        } else {
            applySchedule(t.copy(completed = false))
            emit("Task reopened")
        }
    }

    fun deleteTask(t: Task) {
        val u = uid ?: return
        lastDeleted = t
        repo.delete(u, t.id, ::fail)
        scheduler.cancel(t.id)
        emit("Task deleted", undoable = true)
    }

    /** Restores the last deleted task from the copy kept in memory (only possible in this session). */
    fun undoDelete() {
        val u = uid ?: return
        val t = lastDeleted ?: return
        lastDeleted = null
        repo.create(u, t.copy(ownerUid = u), ::fail)
        applySchedule(t)
        emit("Task restored")
    }

    fun setReminder(t: Task, enabled: Boolean, at: Long?, offset: Int) {
        val u = uid ?: return
        val now = System.currentTimeMillis()
        val updated = t.copy(reminderEnabled = enabled, reminderAt = at, reminderOffsetMinutes = offset, updatedAt = now)
        repo.update(
            u, t.id,
            mapOf<String, Any?>(
                "reminderEnabled" to enabled, "reminderAt" to at,
                "reminderOffsetMinutes" to offset, "updatedAt" to now
            ),
            ::fail
        )
        applySchedule(updated)
        emit(if (enabled) "Reminder updated" else "Reminder turned off")
    }

    fun reschedule(t: Task, deltaMs: Long) {
        val u = uid ?: return
        val shifted = TaskLogic.shift(t, deltaMs).copy(updatedAt = System.currentTimeMillis())
        repo.update(
            u, t.id,
            mapOf<String, Any?>(
                "startAt" to shifted.startAt, "dueAt" to shifted.dueAt,
                "reminderAt" to shifted.reminderAt, "updatedAt" to shifted.updatedAt
            ),
            ::fail
        )
        applySchedule(shifted)
        val clash = TaskLogic.conflicts(_ui.value.tasks, shifted.startAt, shifted.durationMinutes, shifted.allDay, shifted.id)
        emit(if (clash.isEmpty()) "Task rescheduled" else "Rescheduled, but it overlaps with \"${clash.first().title}\"")
    }

    fun addCategory(name: String) {
        val u = uid ?: return
        val n = name.trim()
        if (n.isEmpty() || n.length > 40) return emit("Category name must be 1 to 40 characters")
        if (_categories.value.any { it.equals(n, ignoreCase = true) }) return emit("Category already exists")
        repo.addCategory(u, n, ::fail)
    }

    fun notify(text: String) = emit(text)
}
