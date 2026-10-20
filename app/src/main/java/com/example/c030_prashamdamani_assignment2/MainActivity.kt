package com.example.c030_prashamdamani_assignment2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.example.c030_prashamdamani_assignment2.navigation.AppNavigation
import com.example.c030_prashamdamani_assignment2.ui.theme.ToDoTheme

class MainActivity : ComponentActivity() {
    private val pendingTaskId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingTaskId.value = intent?.getStringExtra(EXTRA_TASK_ID)
        setContent {
            ToDoTheme {
                AppNavigation(openTaskId = pendingTaskId.value, onOpenTaskConsumed = { pendingTaskId.value = null })
            }
        }
    }

    // Called when a reminder notification is tapped while the app is already running.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingTaskId.value = intent.getStringExtra(EXTRA_TASK_ID)
    }

    companion object {
        const val EXTRA_TASK_ID = "open_task_id"
    }
}
