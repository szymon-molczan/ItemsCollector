package org.wut.items.collector.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
















@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupExportScreen(
    vm: BackupExportViewModel,
    onBack: () -> Unit
) {
    val collections by vm.collections.collectAsState()
    val selectedIds by vm.selectedIds.collectAsState()
    val exportState by vm.exportState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Eksport kopii zapasowej") },
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
                .padding(16.dp)
        ) {
            when (val state = exportState) {
                is BackupExportViewModel.ExportState.Done -> {
                    SuccessCard(
                        result = state.result,
                        onShare = { vm.share(state.result.path) },
                        onBack = onBack
                    )
                }
                is BackupExportViewModel.ExportState.InProgress -> {
                    ProgressCard()
                }
                else -> {
                    SelectionContent(
                        collections = collections,
                        selectedIds = selectedIds,
                        errorMessage = (state as? BackupExportViewModel.ExportState.Error)?.message,
                        onToggle = vm::toggle,
                        onSelectAll = vm::selectAll,
                        onClear = vm::clearSelection,
                        onExport = {
                            vm.resetState()
                            vm.runExport()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionContent(
    collections: List<org.wut.items.collector.model.CollectionDto>,
    selectedIds: Set<String>,
    errorMessage: String?,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onExport: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Wybierz kolekcje, które chcesz zachować w pliku ZIP. Eksport obejmuje " +
                "schemat, pozycje oraz zdjęcia.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onSelectAll) { Text("Zaznacz wszystkie") }
            TextButton(onClick = onClear) { Text("Wyczyść") }
        }
        Spacer(Modifier.height(8.dp))

        if (collections.isEmpty()) {
            Text(
                "Brak kolekcji do wyeksportowania.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(collections, key = { it.id }) { coll ->
                    val checked = coll.id in selectedIds
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(coll.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { onToggle(coll.id) }
                            )
                            Spacer(Modifier.height(0.dp))
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    coll.name,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (coll.description.isNotBlank()) {
                                    Text(
                                        coll.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onExport,
            enabled = selectedIds.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Eksportuj zaznaczone (${selectedIds.size})")
        }
    }
}

@Composable
private fun ProgressCard() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Trwa eksport. Pobieranie zdjęć może chwilę potrwać.")
        }
    }
}

@Composable
private fun SuccessCard(
    result: org.wut.items.collector.backup.BackupExporter.Result,
    onShare: () -> Unit,
    onBack: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Kopia zapasowa gotowa",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text("Kolekcji: ${result.collectionsExported}")
            Text("Pozycji: ${result.itemsExported}")
            Text("Dodanych zdjęć: ${result.imagesIncluded}")
            if (result.imagesFailed > 0) {
                Text(
                    "Pominiętych zdjęć (błąd pobierania): ${result.imagesFailed}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Plik: ${result.path}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
                Text("Udostępnij plik")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Wróć do ustawień")
            }
        }
    }
}
