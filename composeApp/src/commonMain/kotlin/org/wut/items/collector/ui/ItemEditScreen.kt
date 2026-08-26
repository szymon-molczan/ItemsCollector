package org.wut.items.collector.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.wut.items.collector.media.MediaPicker
import org.wut.items.collector.model.AttributeDef
import org.wut.items.collector.model.AttributeType
import org.wut.items.collector.model.AttributeValue
import org.wut.items.collector.model.ItemDto
import org.wut.items.collector.model.ItemImageDto
import org.wut.items.collector.util.newUuid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditScreen(
    vm: CollectionDetailViewModel,
    itemId: String?,
    mediaPicker: MediaPicker,
    onClose: () -> Unit
) {
    val collection by vm.collection.collectAsState()
    val items by vm.items.collectAsState()
    val scope = rememberCoroutineScope()

    
    
    val actualItemId = remember { itemId ?: newUuid() }
    val isNew = itemId == null

    val existing: ItemDto? = remember(items, actualItemId) {
        items.firstOrNull { it.id == actualItemId }
    }

    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var description by remember(existing?.id) { mutableStateOf(existing?.description ?: "") }

    val attrValues = remember(existing?.id) { mutableStateMapOf<String, String>() }
    var error by remember { mutableStateOf<String?>(null) }

    
    var pickerState by remember { mutableStateOf<PickerState>(PickerState.Idle) }

    
    val galleryImages: List<ItemImageDto> = vm.observeImages(actualItemId)
        .collectAsState(initial = emptyList()).value

    
    var galleryActionFor by remember { mutableStateOf<ItemImageDto?>(null) }

    
    var carouselStartIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(existing?.id, collection) {
        attrValues.clear()
        existing?.attributes?.forEach { attrValues[it.key] = it.value }
        collection?.schema?.forEach { def ->
            if (!attrValues.containsKey(def.key)) {
                attrValues[def.key] = when (def.type) {
                    AttributeType.BOOLEAN -> "false"
                    else -> ""
                }
            }
        }
    }

    
    val currentPickerState = pickerState
    if (currentPickerState is PickerState.Editing) {
        ImageEditScreen(
            sourcePath = currentPickerState.path,
            onResult = { resultPath ->
                
                vm.addImageToGallery(actualItemId, resultPath)
                pickerState = PickerState.Idle
            },
            onCancel = {
                pickerState = PickerState.Idle
            }
        )
        return
    }

    
    val carouselIdx = carouselStartIndex
    if (carouselIdx != null && galleryImages.isNotEmpty()) {
        FullScreenImageCarousel(
            images = galleryImages,
            startIndex = carouselIdx,
            serverBaseUrl = vm.serverBaseUrl,
            onDismiss = { carouselStartIndex = null }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    
                    Text(
                        if (existing == null) "Nowy przedmiot" else "Edycja: ${existing.name}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nazwa") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Opis") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Galeria zdjęć (${galleryImages.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedButton(
                    onClick = { pickerState = PickerState.Menu }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.size(4.dp))
                    Text("Dodaj zdjęcie")
                }
            }

            if (galleryImages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Brak zdjęć. Dodaj je przyciskiem powyżej.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val pagerState = rememberPagerState(pageCount = { galleryImages.size })
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    ) { page ->
                        val img = galleryImages[page]
                        val model: Any? = img.pendingImagePath?.let { toCoilModel(it, vm.serverBaseUrl) }
                            ?: img.imageUrl?.let { toCoilModel(it, vm.serverBaseUrl) }
                        Box(
                            modifier = Modifier.fillMaxSize().pointerInput(img.id) {
                                detectTapGestures(
                                    onTap = { carouselStartIndex = page },
                                    onLongPress = { galleryActionFor = img }
                                )
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            if (model != null) {
                                AsyncImage(
                                    model = model,
                                    contentDescription = null,
                                    placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                                    error = ColorPainter(MaterialTheme.colorScheme.errorContainer),
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text("(brak podglądu)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (img.isPrimary) {
                                
                                Row(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .align(Alignment.TopStart)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.size(2.dp))
                                    Text(
                                        "Glowne",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }

                    
                    if (galleryImages.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(galleryImages.size) { idx ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .size(if (idx == pagerState.currentPage) 8.dp else 6.dp)
                                        .background(
                                            color = if (idx == pagerState.currentPage)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                )
                            }
                        }
                    }

                    
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(galleryImages, key = { it.id }) { img ->
                            val model: Any? = img.pendingImagePath?.let { toCoilModel(it, vm.serverBaseUrl) }
                                ?: img.imageUrl?.let { toCoilModel(it, vm.serverBaseUrl) }
                            val isCurrent = galleryImages.indexOf(img) == pagerState.currentPage
                            val borderColor = when {
                                img.isPrimary -> MaterialTheme.colorScheme.primary
                                isCurrent -> MaterialTheme.colorScheme.secondary
                                else -> Color.Transparent
                            }
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                    .border(2.dp, borderColor)
                                    .pointerInput(img.id) {
                                        detectTapGestures(
                                            onTap = {
                                                scope.launch { pagerState.animateScrollToPage(galleryImages.indexOf(img)) }
                                            },
                                            onLongPress = { galleryActionFor = img }
                                        )
                                    }
                            ) {
                                if (model != null) {
                                    AsyncImage(
                                        model = model,
                                        contentDescription = null,
                                        placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                                        error = ColorPainter(MaterialTheme.colorScheme.errorContainer),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        "Przytrzymaj zdjęcie, aby ustawić je jako główne lub je usunąć.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val schema = collection?.schema.orEmpty()
            if (schema.isNotEmpty()) {
                HorizontalDivider()
                Text("Atrybuty kolekcji", style = MaterialTheme.typography.titleMedium)
                schema.forEach { def ->
                    val current = attrValues[def.key] ?: ""
                    AttributeField(
                        def = def,
                        value = current,
                        onValueChange = { attrValues[def.key] = it }
                    )
                }
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(8.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val missing = schema.firstOrNull { def ->
                        def.required && (attrValues[def.key].isNullOrBlank())
                    }
                    when {
                        name.isBlank() -> error = "Nazwa nie może być pusta"
                        missing != null -> error = "Pole „${missing.label}” jest wymagane"
                        else -> {
                            error = null
                            val attributes = schema.map { def ->
                                AttributeValue(def.key, attrValues[def.key] ?: "")
                            }
                            
                            vm.saveItem(
                                id = actualItemId,
                                name = name.trim(),
                                description = description.trim(),
                                attributes = attributes,
                                isNew = isNew
                            )
                            onClose()
                        }
                    }
                }
            ) { Text(if (isNew) "Dodaj" else "Zapisz") }
            Spacer(Modifier.height(40.dp))
        }
    }

    
    
    val targetForAction = galleryActionFor
    if (targetForAction != null) {
        AlertDialog(
            onDismissRequest = { galleryActionFor = null },
            title = { Text("Akcje zdjęcia") },
            text = {
                Text(if (targetForAction.isPrimary) "To zdjęcie jest główne." else "Wybierz akcję:")
            },
            confirmButton = {
                if (!targetForAction.isPrimary) {
                    TextButton(onClick = {
                        vm.setPrimaryImage(targetForAction.id)
                        galleryActionFor = null
                    }) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(4.dp))
                        Text("Ustaw jako główne")
                    }
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        vm.deleteImage(targetForAction.id)
                        galleryActionFor = null
                    }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(4.dp))
                        Text("Usuń", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { galleryActionFor = null }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(4.dp))
                        Text("Anuluj")
                    }
                }
            }
        )
    }

    
    if (currentPickerState is PickerState.Menu) {
        AlertDialog(
            onDismissRequest = { pickerState = PickerState.Idle },
            title = { Text("Dodaj zdjęcie") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            pickerState = PickerState.Idle
                            scope.launch {
                                val result = mediaPicker.pickFromGallery()
                                if (result != null) pickerState = PickerState.Editing(result.localPath)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("Wybierz z galerii")
                    }
                    if (mediaPicker.canTakePhoto) {
                        OutlinedButton(
                            onClick = {
                                pickerState = PickerState.Idle
                                scope.launch {
                                    val result = mediaPicker.takePhoto()
                                    if (result != null) pickerState = PickerState.Editing(result.localPath)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Filled.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text("Zrób zdjęcie")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pickerState = PickerState.Idle }) {
                    Text("Anuluj")
                }
            }
        )
    }
}


private sealed class PickerState {
    object Idle : PickerState()
    object Menu : PickerState()
    
    object Preparing : PickerState()
    data class Editing(val path: String) : PickerState()
}





@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    labelText: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    val initialMillis: Long? = remember(value) { parseIsoDateToUtcMillis(value) }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(labelText) },
        placeholder = { Text("YYYY-MM-DD") },
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            TextButton(onClick = { showPicker = true }) {
                Text("Kalendarz")
            }
        }
    )

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        onValueChange(formatUtcMillisToIsoDate(millis))
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Anuluj") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun parseIsoDateToUtcMillis(iso: String): Long? {
    if (iso.length != 10) return null
    val parts = iso.split("-")
    if (parts.size != 3) return null
    val y = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val d = parts[2].toIntOrNull() ?: return null
    if (m !in 1..12 || d !in 1..31) return null
    return daysFromCivil(y, m, d) * 86_400_000L
}

private fun formatUtcMillisToIsoDate(millis: Long): String {
    val days = millis.floorDiv(86_400_000L)
    val (y, m, d) = civilFromDays(days)
    val mm = if (m < 10) "0$m" else "$m"
    val dd = if (d < 10) "0$d" else "$d"
    return "$y-$mm-$dd"
}

private fun daysFromCivil(y: Int, m: Int, d: Int): Long {
    val yAdj = if (m <= 2) y - 1 else y
    val era = if (yAdj >= 0) yAdj / 400 else (yAdj - 399) / 400
    val yoe = (yAdj - era * 400).toLong()
    val mp = if (m > 2) m - 3 else m + 9
    val doy = (153 * mp + 2) / 5 + d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era.toLong() * 146097L + doe - 719468L
}

private fun civilFromDays(z: Long): Triple<Int, Int, Int> {
    val zAdj = z + 719468L
    val era = if (zAdj >= 0) zAdj / 146097 else (zAdj - 146096) / 146097
    val doe = zAdj - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
    val m = (if (mp < 10) mp + 3 else mp - 9).toInt()
    val yFinal = (if (m <= 2) y + 1 else y).toInt()
    return Triple(yFinal, m, d)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttributeField(
    def: AttributeDef,
    value: String,
    onValueChange: (String) -> Unit
) {
    val labelText = if (def.required) "${def.label} *" else def.label
    when (def.type) {
        AttributeType.TEXT -> {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(labelText) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        AttributeType.NUMBER -> {
            OutlinedTextField(
                value = value,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("-?\\d*(\\.\\d*)?"))) {
                        onValueChange(input)
                    }
                },
                label = { Text(labelText) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        AttributeType.DATE -> {
            DatePickerField(
                labelText = labelText,
                value = value,
                onValueChange = onValueChange
            )
        }
        AttributeType.BOOLEAN -> {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(labelText, modifier = Modifier
                    .weight(1f), style = MaterialTheme.typography.bodyLarge)
                val checked = value == "true"
                Switch(
                    checked = checked,
                    onCheckedChange = { onValueChange(if (it) "true" else "false") }
                )
            }
        }
        AttributeType.SELECT -> {
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = value,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(labelText) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        androidx.compose.material3.TextButton(onClick = { expanded = true }) {
                            Text("Wybierz")
                        }
                    }
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    if (def.options.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("(brak opcji w schemacie)") },
                            onClick = { expanded = false }
                        )
                    } else {
                        def.options.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    onValueChange(opt)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
