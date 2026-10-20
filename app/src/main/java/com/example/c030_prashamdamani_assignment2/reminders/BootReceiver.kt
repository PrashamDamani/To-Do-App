package com.example.c030_prashamdamani_assignment2.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Restores future reminder alarms after reboot / app update from locally stored metadata. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            ReminderScheduler(context).restoreAll()
        }
    }
}
