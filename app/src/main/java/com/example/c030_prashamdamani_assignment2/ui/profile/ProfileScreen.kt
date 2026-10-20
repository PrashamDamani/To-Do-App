@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.c030_prashamdamani_assignment2.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.c030_prashamdamani_assignment2.data.model.UserProfile
import com.example.c030_prashamdamani_assignment2.ui.components.ConfirmDialog
import com.example.c030_prashamdamani_assignment2.util.DateFmt
import com.example.c030_prashamdamani_assignment2.util.SortMode
import com.example.c030_prashamdamani_assignment2.util.openExactAlarmSettings
import com.example.c030_prashamdamani_assignment2.util.openNotificationSettings

@Composable
fun ProfileScreen(
    profile: UserProfile?,
    reduceMotion: Boolean,
    defaultSort: SortMode,
    onReduceMotion: (Boolean) -> Unit,
    onDefaultSort: (SortMode) -> Unit,
    onUpdateProfile: (String, String) -> Unit,
    onOpenInsights: () -> Unit,
    onOpenTimer: () -> Unit,
    onLogout: () -> Unit
) {
    val ctx = LocalContext.current
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Profile & Settings", fontWeight = FontWeight.Bold) },
            actions = { IconButton(onClick = { editing = true }) { Icon(Icons.Default.Edit, contentDescription = "Edit profile") } }
        )
        if (profile == null) return@Column
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                    Text(profile.fullName.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineSmall)
                }
                Column(Modifier.padding(start = 16.dp)) {
                    Text(profile.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(profile.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Info("Display name", profile.fullName)
                    Info("Email", profile.email)
                    if (profile.rollNumber.isNotBlank()) {
                        Info("Roll number", profile.rollNumber)
                        Info("Assignment", "Assignment 2 - ${profile.identity}")
                    }
                    Info("Account created", if (profile.createdAt > 0) DateFmt.date(profile.createdAt) else "Not available")
                }
            }
            Text("Shortcuts", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenInsights, Modifier.weight(1f)) { Icon(Icons.Default.Insights, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Insights") }
                OutlinedButton(onClick = onOpenTimer, Modifier.weight(1f)) { Icon(Icons.Default.Timer, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Timer") }
            }
            HorizontalDivider()
            Text("Settings", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Reduce animations", style = MaterialTheme.typography.bodyLarge)
                    Text("Also follows the system animation setting", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = reduceMotion, onCheckedChange = onReduceMotion)
            }
            Text("Default task sort", style = MaterialTheme.typography.bodyLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortMode.values().forEach { m -> FilterChip(defaultSort == m, { onDefaultSort(m) }, { Text(m.label) }) }
            }
            OutlinedButton(onClick = { openNotificationSettings(ctx) }, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Notifications, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Notification settings")
            }
            OutlinedButton(onClick = { openExactAlarmSettings(ctx) }, Modifier.fillMaxWidth()) { Text("Exact alarm permission") }
            HorizontalDivider()
            Button(
                onClick = { confirmLogout = true }, modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Icon(Icons.AutoMirrored.Filled.Logout, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Log out") }
        }
    }
    if (confirmLogout) ConfirmDialog("Log out?", "You will need to sign in again. Pending reminders on this device will be cancelled.", "Log out",
        onConfirm = { confirmLogout = false; onLogout() }, onDismiss = { confirmLogout = false })
    if (editing && profile != null) {
        var name by rememberSaveable { mutableStateOf(profile.fullName) }
        var roll by rememberSaveable { mutableStateOf(profile.rollNumber) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Edit profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it.take(100) }, label = { Text("Full name") }, singleLine = true)
                    OutlinedTextField(value = roll, onValueChange = { roll = it.take(30) }, label = { Text("Roll number") }, singleLine = true)
                }
            },
            confirmButton = { TextButton(onClick = { onUpdateProfile(name, roll); editing = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun Info(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
