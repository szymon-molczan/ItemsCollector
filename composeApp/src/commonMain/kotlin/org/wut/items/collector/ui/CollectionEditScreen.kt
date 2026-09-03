package org.wut.items.collector.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.wut.items.collector.media.MediaPicker
import org.wut.items.collector.model.AttributeDef
import org.wut.items.collector.model.AttributeType
import org.wut.items.collector.util.newUuid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider

@Composable
private fun premiumTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    disabledBorderColor = MaterialTheme.colorScheme.outline,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionEditScreen(
    vm: CollectionsViewModel,
    collectionId: String? = null,
    mediaPicker: MediaPicker,
    onClose: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var bannerImageUrl by remember { mutableStateOf<String?>(null) }
    var pendingBannerPath by remember { mutableStateOf<String?>(null) }
    var bannerAlignment = 0.5f
    val schema = remember { mutableStateListOf<AttributeDef>() }
    var presetMenuOpen by remember { mutableStateOf(false) }
    val availablePresets by vm.availablePresets.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editingBannerPath by remember { mutableStateOf<String?>(null) }
    var bannerAspect by remember { mutableStateOf(1f) }
    var originalSchema by remember { mutableStateOf<List<AttributeDef>>(emptyList()) }
    var confirmSchemaSave by remember { mutableStateOf(false) }

    LaunchedEffect(vm.uiEvent) {
        vm.uiEvent.collect { event ->
            if (event is CollectionsViewModel.UiEvent.ShowSnackbar) {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(collectionId) {
        if (collectionId != null) {
            vm.getCollection(collectionId)?.let {
                name = it.name
                description = it.description
                bannerImageUrl = it.bannerImageUrl
                pendingBannerPath = it.pendingBannerPath
                schema.clear()
                schema.addAll(it.schema)
                originalSchema = it.schema
            }
        }
    }

    fun saveCollection() {
        if (collectionId == null) {
            vm.createCollection(name.trim(), description.trim(), schema.toList(), bannerImageUrl, bannerAlignment, pendingBannerPath)
        } else {
            vm.updateCollection(collectionId, name.trim(), description.trim(), schema.toList(), bannerImageUrl, bannerAlignment, pendingBannerPath)
        }
        onClose()
    }

    
    editingBannerPath?.let { path ->
        ImageEditScreen(
            sourcePath = path,
            onResult = { resultPath ->
                pendingBannerPath = resultPath
                editingBannerPath = null
            },
            onCancel = { editingBannerPath = null },
            
            cropAspectRatio = bannerAspect
        )
        return
    }

    val schemaKeys = schema.map { it.key }.toSet()
    val removedFields = originalSchema.filter { it.key !in schemaKeys }
    if (confirmSchemaSave) {
        AlertDialog(
            onDismissRequest = { confirmSchemaSave = false },
            title = { Text("Zapisać zmiany struktury?") },
            text = {
                Text(
                    "Usunięcie pól ${removedFields.joinToString { it.label.ifBlank { "bez nazwy" } }} trwale usunie ich wartości z aktywnych przedmiotów tej kolekcji."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmSchemaSave = false
                        saveCollection()
                    }
                ) {
                    Text("Zapisz zmiany")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmSchemaSave = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (collectionId == null) "Nowa kolekcja" else "Edytuj kolekcję") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Wstecz"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Nazwa kolekcji") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = premiumTextFieldColors()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Opis (opcjonalny)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = premiumTextFieldColors()
            )
            Spacer(Modifier.height(16.dp))

            Text("Tło banera", style = MaterialTheme.typography.titleMedium)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(140.dp).padding(vertical = 8.dp)) {
                val bw = constraints.maxWidth.toFloat()
                val bh = constraints.maxHeight.toFloat()
                LaunchedEffect(bw, bh) {
                    if (bh > 0) bannerAspect = bw / bh
                }

                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    onClick = {
                        scope.launch {
                            val result = mediaPicker.pickFromGallery()
                            if (result != null) {
                                editingBannerPath = result.localPath
                            }
                        }
                    }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val url = pendingBannerPath ?: bannerImageUrl
                        if (url != null) {
                            AsyncImage(
                                model = toCoilModel(url, vm.serverBaseUrl),
                                contentDescription = "Tło banera",
                                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                                error = ColorPainter(MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Row(
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                
                                IconButton(
                                    onClick = { 
                                        
                                        
                                        
                                        
                                        
                                        pendingBannerPath?.let { editingBannerPath = it }
                                    },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), MaterialTheme.shapes.small)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edytuj baner")
                                }
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = { 
                                        bannerImageUrl = null
                                        pendingBannerPath = null
                                    },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), MaterialTheme.shapes.small)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Usuń tło", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(48.dp))
                                Text("Wybierz zdjęcie tła", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { presetMenuOpen = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Wczytaj z szablonu")
                }
                DropdownMenu(expanded = presetMenuOpen, onDismissRequest = { presetMenuOpen = false }) {
                    availablePresets.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(preset.name) },
                            onClick = {
                                if (name.isBlank()) name = preset.name
                                if (description.isBlank()) description = preset.description
                                schema.clear()
                                schema.addAll(preset.schema)
                                presetMenuOpen = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { vm.saveAsPreset(name, description, schema.toList()) },
                enabled = name.isNotBlank() && schema.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Zapisz jako nowy szablon")
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("Pola dynamiczne", style = MaterialTheme.typography.titleMedium)
            Text(
                "Zdefiniuj strukturę przedmiotów w kolekcji. Każde pole będzie wyświetlane w formularzu dodawania przedmiotu.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            schema.forEachIndexed { index, def ->
                AttributeDefRow(
                    def = def,
                    onChange = { schema[index] = it },
                    onRemove = { schema.removeAt(index) }
                )
                Spacer(Modifier.height(6.dp))
            }
            OutlinedButton(
                onClick = {
                    schema.add(
                        AttributeDef(
                            key = "field_${newUuid()}",
                            label = "Nowe pole",
                            type = AttributeType.TEXT
                        )
                    )
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("+ Dodaj pole")
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        if (collectionId != null && removedFields.isNotEmpty()) {
                            confirmSchemaSave = true
                        } else {
                            saveCollection()
                        }
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (collectionId == null) "Zapisz kolekcję" else "Zapisz zmiany")
            }
        }
    }
}

@Composable
private fun AttributeDefRow(
    def: AttributeDef,
    onChange: (AttributeDef) -> Unit,
    onRemove: () -> Unit
) {
    var typeMenuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = def.label,
                    onValueChange = {
                        onChange(def.copy(label = it))
                    },
                    label = { Text("Nazwa pola") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = premiumTextFieldColors()
                )
                IconButton(onClick = onRemove) {
                    Text("X")
                }
            }
            Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { typeMenuOpen = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) { Text("Typ: ${def.type.displayName()}") }
                DropdownMenu(expanded = typeMenuOpen, onDismissRequest = { typeMenuOpen = false }) {
                    AttributeType.values().forEach { t ->
                        DropdownMenuItem(text = { Text(t.displayName()) }, onClick = {
                            onChange(def.copy(type = t))
                            typeMenuOpen = false
                        })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = def.required, onCheckedChange = { onChange(def.copy(required = it)) })
                    Text("Wymagane")
                }
            }
            if (def.type == AttributeType.SELECT) {
                Spacer(Modifier.height(6.dp))
                
                
                
                
                
                
                
                
                
                
                
                
                
                var rawText by remember { mutableStateOf(def.options.joinToString(",")) }
                
                
                LaunchedEffect(def.options) {
                    val parsedFromRaw = rawText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (parsedFromRaw != def.options) {
                        rawText = def.options.joinToString(",")
                    }
                }
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { newText ->
                        rawText = newText
                        val parsed = newText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        if (parsed != def.options) {
                            onChange(def.copy(options = parsed))
                        }
                    },
                    label = { Text("Opcje (oddzielone przecinkami)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = premiumTextFieldColors()
                )
            }
        }
    }
}

private fun AttributeType.displayName(): String = when (this) {
    AttributeType.TEXT -> "Tekst"
    AttributeType.NUMBER -> "Liczba"
    AttributeType.DATE -> "Data"
    AttributeType.BOOLEAN -> "Tak/Nie"
    AttributeType.SELECT -> "Lista wyboru"
}
