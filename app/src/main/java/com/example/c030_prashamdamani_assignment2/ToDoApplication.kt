package com.example.c030_prashamdamani_assignment2

import android.app.Application
import android.util.Log
import com.example.c030_prashamdamani_assignment2.reminders.NotificationHelper
import com.google.firebase.database.FirebaseDatabase

class ToDoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Must run before any other Realtime Database call so cached data works offline.
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) {
            Log.w("ToDoApplication", "Persistence already configured: ${e.message}")
        }
        NotificationHelper.createChannels(this)
    }
}
