package org.wut.items.collector.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.wut.items.collector.backup.BackupImporter












@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupImportScreen(
    vm: BackupImportViewModel,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import kopii zapasowej") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Wstecz"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (val s = state) {
                is BackupImportViewModel.State.Idle -> IdleContent(onPick = vm::pickAndImport)
                is BackupImportViewModel.State.Picking -> ProgressContent(label = "Trwa wybieranie pliku...")
                is BackupImportViewModel.State.Importing -> ProgressContent(
                    label = "Trwa import. Kopiowanie zdjęć może chwilę potrwać."
                )
                is BackupImportViewModel.State.Done -> SuccessContent(result = s.result, onBack = onBack)
                is BackupImportViewModel.State.Error -> ErrorContent(message = s.message, onRetry = {
                    vm.reset()
                    vm.pickAndImport()
                })
            }
        }
    }
}

@Composable
private fun IdleContent(onPick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Jak to działa", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Wybierz plik ZIP utworzony funkcją „Eksportuj dane”. Wszystkie kolekcje " +
                    "i pozycje z pliku zostaną dodane jako nowe wpisy, bez nadpisywania istniejących. " +
                    "Zdjęcia zostaną skopiowane do pamięci urządzenia i wysłane na serwer po " +
                    "ręcznym uruchomieniu synchronizacji.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
                Text("Wybierz plik ZIP")
            }
        }
    }
}

@Composable
private fun ProgressContent(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(label)
        }
    }
}

@Composable
private fun SuccessContent(
    result: BackupImporter.Result.Success,
    onBack: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Import zakończony", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Dodanych kolekcji: ${result.collectionsImported}")
            Text("Dodanych pozycji: ${result.itemsImported}")
            Text("Przywróconych zdjęć: ${result.imagesRestored}")
            if (result.imagesMissing > 0) {
                Text(
                    "Brakujące zdjęcia (nie było ich w pliku ZIP): ${result.imagesMissing}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Zdjęcia zostaną wysłane na serwer po ręcznym uruchomieniu synchronizacji.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Wróć do ustawień")
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Import nie powiódł się",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text("Spróbuj ponownie")
            }
        }
    }
}
