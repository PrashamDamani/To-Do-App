package com.example.c030_prashamdamani_assignment2.reminders

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.c030_prashamdamani_assignment2.MainActivity
import com.example.c030_prashamdamani_assignment2.R

object NotificationHelper {
    const val CHANNEL_REMINDERS = "task_reminders"
    const val CHANNEL_TIMER = "countdown_timer"

    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Task reminders", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Reminders for your tasks" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_TIMER, "Countdown timer", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Alerts when the countdown timer finishes" }
        )
    }

    /** False when the user denied the POST_NOTIFICATIONS permission or disabled notifications. */
    fun enabled(ctx: Context): Boolean = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    @SuppressLint("MissingPermission")
    fun showReminder(ctx: Context, taskId: String, title: String) {
        if (!enabled(ctx)) return
        val open = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_TASK_ID, taskId)
        }
        val id = ReminderScheduler.requestId(taskId)
        val pi = PendingIntent.getActivity(ctx, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Reminder: $title")
            .setContentText("Tap to open this task")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, n) // same id per task => no duplicates
    }

    @SuppressLint("MissingPermission")
    fun showTimerDone(ctx: Context, label: String?) {
        if (!enabled(ctx)) return
        val open = Intent(ctx, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val pi = PendingIntent.getActivity(ctx, 7002, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Timer finished")
            .setContentText(label?.let { "Time is up for: $it" } ?: "Your countdown has finished")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        NotificationManagerCompat.from(ctx).notify(7001, n)
    }
}
