@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.c030_prashamdamani_assignment2.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.c030_prashamdamani_assignment2.viewmodel.AuthUiState

@Composable
fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Checklist, contentDescription = null, tint = Color.White, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(12.dp))
            Text("To Do app", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("C030 - Prasham Damani", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
private fun Brand(subtitle: String) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Checklist, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
        Text("To Do app", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("C030 - Prasham Damani | Assignment 2", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text(subtitle, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password"
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun Messages(state: AuthUiState) {
    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp)) }
    state.info?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp)) }
}

@Composable
fun LoginScreen(
    state: AuthUiState,
    onLogin: (String, String) -> Unit,
    onReset: (String) -> Unit,
    onGoRegister: () -> Unit,
    onClearMessages: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showReset by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Brand("Sign in to Prasham's task manager")
        OutlinedTextField(
            value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        PasswordField(password, { password = it }, "Password")
        Messages(state)
        Button(onClick = { onLogin(email, password) }, enabled = !state.loading, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text("Sign in")
        }
        TextButton(onClick = { onClearMessages(); showReset = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Forgot password?") }
        TextButton(onClick = { onClearMessages(); onGoRegister() }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("New here? Create an account") }
    }
    if (showReset) {
        var resetEmail by rememberSaveable { mutableStateOf(email) }
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Reset password") },
            text = {
                Column {
                    Text("We will email you a link to reset your password.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = resetEmail, onValueChange = { resetEmail = it }, label = { Text("Email") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                }
            },
            confirmButton = { TextButton(onClick = { onReset(resetEmail); showReset = false }) { Text("Send link") } },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun RegisterScreen(
    state: AuthUiState,
    onRegister: (name: String, roll: String, email: String, password: String, confirm: String) -> Unit,
    onGoLogin: () -> Unit,
    onClearMessages: () -> Unit
) {
    // Prefilled with the assignment demo profile. Other users can simply edit these fields.
    var name by rememberSaveable { mutableStateOf("Prasham Damani") }
    var roll by rememberSaveable { mutableStateOf("C030") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Brand("Create your account")
        OutlinedTextField(
            value = name, onValueChange = { name = it }, label = { Text("Full name") }, singleLine = true,
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth()
        )
        PasswordField(password, { password = it }, "Password (min 6 characters)")
        PasswordField(confirm, { confirm = it }, "Confirm password")
        Messages(state)
        Button(
            onClick = { onRegister(name, roll, email, password, confirm) },
            enabled = !state.loading, modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text("Create account")
        }
        TextButton(onClick = { onClearMessages(); onGoLogin() }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Already registered? Sign in") }
        Text("Your password is handled only by Firebase Authentication and is never stored in the database.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())
    }
}
