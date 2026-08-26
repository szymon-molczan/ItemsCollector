package org.wut.items.collector.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.wut.items.collector.theme.ThemeMode
import org.wut.items.collector.theme.ThemePreferences











@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authVm: AuthViewModel,
    themePreferences: ThemePreferences,
    onBack: () -> Unit,
    onManagePresets: () -> Unit = {},
    onExport: () -> Unit = {},
    onImport: () -> Unit = {}
) {
    val session by authVm.session.collectAsState()
    val themeMode by themePreferences.mode.collectAsState(initial = ThemeMode.SYSTEM)
    val changePasswordState by authVm.changePasswordState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ustawienia") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Konto", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    if (session?.token == "offline") {
                        Text(
                            "Pracujesz w trybie offline (lokalnym). Dane nie są synchronizowane z serwerem.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        Text(
                            text = session?.email ?: "(brak sesji)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (session?.displayName?.isNotBlank() == true) {
                            Text(
                                session!!.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Serwer: ${session?.serverUrl ?: "-"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { authVm.logout() }) {
                        Text(if (session?.token == "offline") "Zakończ tryb offline" else "Wyloguj")
                    }
                }
            }

            
            if (session?.token != "offline" && session != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Zmień hasło", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))

                        var currentPassword by remember { mutableStateOf("") }
                        var newPassword by remember { mutableStateOf("") }
                        var confirmPassword by remember { mutableStateOf("") }
                        var passwordVisible by remember { mutableStateOf(false) }

                        
                        LaunchedEffect(changePasswordState.success) {
                            if (changePasswordState.success) {
                                currentPassword = ""
                                newPassword = ""
                                confirmPassword = ""
                            }
                        }

                        OutlinedTextField(
                            value = currentPassword,
                            onValueChange = { currentPassword = it },
                            label = { Text("Aktualne hasło") },
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
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("Nowe hasło") },
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
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Potwierdź nowe hasło") },
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

                        if (changePasswordState.error != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                changePasswordState.error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (changePasswordState.success) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Hasło zostało zmienione pomyślnie",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        if (changePasswordState.isBusy) {
                            CircularProgressIndicator()
                        } else {
                            Button(
                                onClick = {
                                    authVm.resetChangePasswordState()
                                    authVm.changePassword(currentPassword, newPassword)
                                },
                                enabled = currentPassword.isNotBlank() && newPassword.isNotBlank() && newPassword == confirmPassword,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Zmień hasło")
                            }
                            if (newPassword.isNotBlank() && confirmPassword.isNotBlank() && newPassword != confirmPassword) {
                                Text(
                                    "Hasła nie są zgodne",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Szablony", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Zarządzaj własnymi szablonami kolekcji. Możesz je usuwać, jeśli nie są już potrzebne.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onManagePresets) {
                        Text("Zarządzaj szablonami")
                    }
                }
            }

            
            
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Motyw", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Wybierz tryb wyglądu aplikacji. Tryb „System” śledzi ustawienia urządzenia " +
                            "i automatycznie przełącza motyw. Kolory aplikacji są stałe, dzięki czemu " +
                            "wygląd pozostaje spójny na różnych urządzeniach.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = themeMode == ThemeMode.SYSTEM,
                            onClick = { themePreferences.setMode(ThemeMode.SYSTEM) },
                            label = { Text("System") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        FilterChip(
                            selected = themeMode == ThemeMode.LIGHT,
                            onClick = { themePreferences.setMode(ThemeMode.LIGHT) },
                            label = { Text("Jasny") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        FilterChip(
                            selected = themeMode == ThemeMode.DARK,
                            onClick = { themePreferences.setMode(ThemeMode.DARK) },
                            label = { Text("Ciemny") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Kopia zapasowa", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Eksportuj wybrane kolekcje do pliku ZIP wraz z danymi i zdjęciami albo " +
                            "zaimportuj je z wcześniej zapisanego pliku. Import zawsze tworzy nowe " +
                            "kolekcje i nie nadpisuje istniejących.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onExport) {
                        Text("Eksportuj dane")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onImport) {
                        Text("Importuj z pliku")
                    }
                }
            }
        }
    }
}
