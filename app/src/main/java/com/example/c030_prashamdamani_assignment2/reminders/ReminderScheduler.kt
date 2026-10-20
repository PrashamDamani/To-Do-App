package com.example.c030_prashamdamani_assignment2.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.c030_prashamdamani_assignment2.data.model.Task

/** Persists scheduled reminder metadata so alarms can be restored after a reboot. */
class ReminderStore(context: Context) {
    private val prefs = context.getSharedPreferences("reminder_store", Context.MODE_PRIVATE)

    fun put(taskId: String, at: Long, title: String) {
        prefs.edit().putString(taskId, "$at|$title").apply()
    }

    fun remove(taskId: String) {
        prefs.edit().remove(taskId).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    /** taskId -> (trigger time, title) */
    fun all(): Map<String, Pair<Long, String>> {
        val out = mutableMapOf<String, Pair<Long, String>>()
        for ((k, v) in prefs.all) {
            val s = v as? String ?: continue
            val i = s.indexOf('|')
            if (i < 0) continue
            val at = s.substring(0, i).toLongOrNull() ?: continue
            out[k] = at to s.substring(i + 1)
        }
        return out
    }
}

class ReminderScheduler(context: Context) {
    private val ctx = context.applicationContext
    private val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val store = ReminderStore(ctx)

    data class Result(val scheduled: Boolean, val exact: Boolean, val notificationsAllowed: Boolean)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()

    /** Schedules (replacing any previous alarm for this task) or cancels when not valid. */
    fun schedule(task: Task): Result {
        val at = task.reminderAt
        val notif = NotificationHelper.enabled(ctx)
        if (!task.reminderEnabled || task.completed || at == null || at <= System.currentTimeMillis()) {
            cancel(task.id)
            return Result(false, false, notif)
        }
        cancel(task.id)
        val exact = setAlarm(at, pending(task.id, task.title, PendingIntent.FLAG_UPDATE_CURRENT)!!)
        store.put(task.id, at, task.title)
        return Result(true, exact, notif)
    }

    fun cancel(taskId: String) {
        pending(taskId, null, PendingIntent.FLAG_NO_CREATE)?.let {
            am.cancel(it)
            it.cancel()
        }
        store.remove(taskId)
    }

    fun cancelAll() {
        store.all().keys.forEach { cancel(it) }
        store.clear()
    }

    /** Re-aligns alarms with the latest task list (also recovers schedules after app updates). */
    fun syncAll(tasks: List<Task>) {
        val now = System.currentTimeMillis()
        val wanted = tasks.filter { t ->
            val at = t.reminderAt
            t.reminderEnabled && !t.completed && at != null && at > now
        }
        val ids = wanted.map { it.id }.toSet()
        val stored = store.all()
        stored.keys.filter { it !in ids }.forEach { cancel(it) }
        for (t in wanted) {
            val s = stored[t.id]
            if (s == null || s.first != t.reminderAt || s.second != t.title) schedule(t)
        }
    }

    /** Called after reboot from stored metadata (no network or login needed). */
    fun restoreAll() {
        val now = System.currentTimeMillis()
        for ((id, v) in store.all()) {
            if (v.first > now) setAlarm(v.first, pending(id, v.second, PendingIntent.FLAG_UPDATE_CURRENT)!!)
            else store.remove(id)
        }
    }

    fun scheduleTimer(atMillis: Long, label: String?) {
        cancelTimer()
        val pi = timerPending(label, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        setAlarm(atMillis, pi)
    }

    fun cancelTimer() {
        timerPending(null, PendingIntent.FLAG_NO_CREATE)?.let {
            am.cancel(it)
            it.cancel()
        }
    }

    private fun setAlarm(at: Long, pi: PendingIntent): Boolean = try {
        if (canScheduleExact()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            true
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi) // inexact: may be delayed
            false
        }
    } catch (e: SecurityException) {
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        false
    }

    private fun pending(taskId: String, title: String?, flag: Int): PendingIntent? {
        val i = Intent(ctx, TaskNotificationReceiver::class.java)
            .setAction(ACTION_REMINDER)
            .setData(Uri.parse("todo://reminder/$taskId"))
            .putExtra(EXTRA_TASK_ID, taskId)
        if (title != null) i.putExtra(EXTRA_TITLE, title)
        return PendingIntent.getBroadcast(ctx, requestId(taskId), i, flag or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun timerPending(label: String?, flag: Int): PendingIntent? {
        val i = Intent(ctx, TaskNotificationReceiver::class.java)
            .setAction(ACTION_TIMER)
            .setData(Uri.parse("todo://timer"))
        if (label != null) i.putExtra(EXTRA_TITLE, label)
        return PendingIntent.getBroadcast(ctx, TIMER_REQUEST, i, flag or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val ACTION_REMINDER = "com.example.c030_prashamdamani_assignment2.REMINDER"
        const val ACTION_TIMER = "com.example.c030_prashamdamani_assignment2.TIMER"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TITLE = "title"
        private const val TIMER_REQUEST = 7001

        /** Stable per-task request id so the same task always maps to the same alarm/notification. */
        fun requestId(taskId: String): Int = taskId.hashCode() and 0x3FFFFFFF
    }
}
