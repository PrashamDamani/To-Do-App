package com.example.c030_prashamdamani_assignment2.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TaskNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ReminderScheduler.ACTION_REMINDER -> {
                val id = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_ID) ?: return
                val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "Task reminder"
                NotificationHelper.showReminder(context, id, title)
                ReminderStore(context).remove(id)
            }
            ReminderScheduler.ACTION_TIMER ->
                NotificationHelper.showTimerDone(context, intent.getStringExtra(ReminderScheduler.EXTRA_TITLE))
        }
    }
}
