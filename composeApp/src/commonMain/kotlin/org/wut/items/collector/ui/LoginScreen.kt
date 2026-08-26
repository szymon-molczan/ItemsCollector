package org.wut.items.collector.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun LoginScreen(vm: AuthViewModel) {
    val ui by vm.ui.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Items Collector", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        TabRow(selectedTabIndex = if (ui.mode == AuthViewModel.Mode.LOGIN) 0 else 1) {
            Tab(
                selected = ui.mode == AuthViewModel.Mode.LOGIN,
                onClick = { vm.setMode(AuthViewModel.Mode.LOGIN) },
                text = { Text("Logowanie") }
            )
            Tab(
                selected = ui.mode == AuthViewModel.Mode.REGISTER,
                onClick = { vm.setMode(AuthViewModel.Mode.REGISTER) },
                text = { Text("Rejestracja") }
            )
        }
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = ui.serverUrl,
            onValueChange = vm::setServerUrl,
            label = { Text("Adres serwera") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = ui.email,
            onValueChange = vm::setEmail,
            label = { Text("Adres e-mail") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = ui.password,
            onValueChange = vm::setPassword,
            label = { Text("Hasło") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Ukryj hasło" else "Pokaż hasło"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        if (ui.mode == AuthViewModel.Mode.REGISTER) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = ui.displayName,
                onValueChange = vm::setDisplayName,
                label = { Text("Nazwa wyświetlana (opcjonalnie)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        ui.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))
        if (ui.isBusy) {
            CircularProgressIndicator()
        } else {
            Button(onClick = vm::submit, modifier = Modifier.fillMaxWidth()) {
                Text(if (ui.mode == AuthViewModel.Mode.LOGIN) "Zaloguj" else "Zarejestruj")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = vm::continueOffline, modifier = Modifier.fillMaxWidth()) {
                Text("Kontynuuj offline")
            }
        }
    }
}
