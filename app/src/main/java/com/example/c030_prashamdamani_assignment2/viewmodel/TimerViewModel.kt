package com.example.c030_prashamdamani_assignment2.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.c030_prashamdamani_assignment2.reminders.ReminderScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TimerState(
    val totalMs: Long = 25 * 60_000L,
    val remainingMs: Long = 25 * 60_000L,
    val running: Boolean = false,
    val finished: Boolean = false,
    val taskId: String? = null,
    val taskTitle: String? = null
)

/**
 * Remaining time is derived from SystemClock.elapsedRealtime() (monotonic), not by decrementing
 * a counter, so it stays accurate even if ticks are late. A wall-clock alarm posts the
 * completion notification if the app is in the background.
 */
class TimerViewModel(app: Application) : AndroidViewModel(app) {
    private val scheduler = ReminderScheduler(app)
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private var endElapsed = 0L
    private var ticker: Job? = null

    fun configure(durationMs: Long, taskId: String? = _state.value.taskId, taskTitle: String? = _state.value.taskTitle) {
        if (_state.value.running || durationMs <= 0) return
        _state.value = TimerState(durationMs, durationMs, false, false, taskId, taskTitle)
    }

    fun link(taskId: String?, title: String?) {
        _state.update { it.copy(taskId = taskId, taskTitle = title) }
    }

    fun start() {
        val s = _state.value
        if (s.running || s.remainingMs <= 0) return
        endElapsed = SystemClock.elapsedRealtime() + s.remainingMs
        scheduler.scheduleTimer(System.currentTimeMillis() + s.remainingMs, s.taskTitle)
        _state.update { it.copy(running = true, finished = false) }
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                val rem = (endElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                _state.update { it.copy(remainingMs = rem) }
                if (rem == 0L) {
                    _state.update { it.copy(running = false, finished = true) }
                    break
                }
                delay(200)
            }
        }
    }

    fun pause() {
        if (!_state.value.running) return
        ticker?.cancel()
        scheduler.cancelTimer()
        val rem = (endElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        _state.update { it.copy(running = false, remainingMs = rem) }
    }

    fun reset() {
        ticker?.cancel()
        scheduler.cancelTimer()
        _state.update { it.copy(running = false, finished = false, remainingMs = it.totalMs) }
    }
}
