package org.wut.items.collector.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import itemscollector.composeapp.generated.resources.Res
import itemscollector.composeapp.generated.resources.app_icon
import org.wut.items.collector.data.SyncState
import org.wut.items.collector.network.ConnectivityStatus
import org.jetbrains.compose.resources.painterResource









@Composable
private fun SyncStatusIcon(state: SyncState, connectivity: ConnectivityStatus, isOfflineMode: Boolean) {
    if (isOfflineMode) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = "Tryb offline (lokalny)",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        return
    }
    if (connectivity == ConnectivityStatus.Unavailable) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = "Brak połączenia sieciowego",
            tint = Color.Gray,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        return
    }

    val (icon: ImageVector, color: Color, tooltip: String) = when (state) {
        SyncState.Idle -> Triple(Icons.Filled.Sync, Color.Gray, "Oczekiwanie na synchronizację")
        SyncState.Syncing -> Triple(Icons.Filled.Sync, Color.Gray, "Synchronizacja w toku")
        is SyncState.Ok -> if (state.imageUploadFailures == 0) {
            Triple(Icons.Filled.CloudDone, Color(0xFF2E7D32), "Wszystko zsynchronizowane")
        } else {
            Triple(Icons.Filled.Warning, Color(0xFFE65100),
                "Zsynchronizowano dane, ale nie wysłano ${state.imageUploadFailures} zdjęć")
        }
        is SyncState.Error -> Triple(Icons.Filled.Error, Color(0xFFC62828), "Błąd synchronizacji: ${state.message}")
    }
    Icon(
        imageVector = icon,
        contentDescription = tooltip,
        tint = color,
        modifier = Modifier.padding(horizontal = 6.dp)
    )
}

@Composable
private fun BottomBarAction(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsListScreen(
    vm: CollectionsViewModel,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onSettings: () -> Unit
) {
    val collections by vm.collections.collectAsState()
    val sortBy by vm.sortBy.collectAsState()
    val sortAsc by vm.sortAsc.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val isSyncing by vm.isSyncing.collectAsState()
    val syncState by vm.syncState.collectAsState()
    val connectivity by vm.connectivityStatus.collectAsState()
    val itemCounts by vm.itemCounts.collectAsState()
    val isEditMode by vm.isEditMode.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(vm.uiEvent) {
        vm.uiEvent.collect { event ->
            if (event is CollectionsViewModel.UiEvent.ShowSnackbar) {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val filtered = remember(collections, searchQuery) {
        if (searchQuery.isBlank()) collections
        else collections.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    val sorted = remember(filtered, sortBy, sortAsc) {
        when (sortBy) {
            CollectionsViewModel.SortBy.UPDATED -> if (sortAsc) filtered.sortedBy { it.updatedAt } else filtered.sortedByDescending { it.updatedAt }
            CollectionsViewModel.SortBy.NAME -> if (sortAsc) filtered.sortedBy { it.name.lowercase() } else filtered.sortedByDescending { it.name.lowercase() }
        }
    }

    
    
    var confirmDeleteCollectionId by remember { mutableStateOf<String?>(null) }
    var searchVisible by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    val collListState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.app_icon),
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        )
                        Text(
                            "Moje kolekcje",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    
                    
                    Box(modifier = Modifier.padding(end = 12.dp)) {
                        SyncStatusIcon(state = syncState, connectivity = connectivity, isOfflineMode = vm.isOfflineMode)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 0.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomBarAction(
                        icon = if (isEditMode) Icons.Filled.EditOff else Icons.Filled.Edit,
                        label = "Edycja",
                        selected = isEditMode,
                        onClick = {
                            vm.toggleEditMode()
                            sortMenuOpen = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    BottomBarAction(
                        icon = Icons.Filled.Search,
                        label = "Szukaj",
                        selected = searchVisible || searchQuery.isNotBlank(),
                        onClick = {
                            searchVisible = !searchVisible
                            sortMenuOpen = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        SmallFloatingActionButton(
                            onClick = onAdd,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Dodaj kolekcję")
                        }
                    }
                    BottomBarAction(
                        icon = Icons.Filled.Refresh,
                        label = "Sync",
                        selected = isSyncing,
                        enabled = !vm.isOfflineMode && !isSyncing,
                        onClick = {
                            vm.sync()
                            sortMenuOpen = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    BottomBarAction(
                        icon = Icons.Filled.Settings,
                        label = "Ustawienia",
                        onClick = {
                            sortMenuOpen = false
                            onSettings()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            AnimatedVisibility(visible = searchVisible || searchQuery.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { vm.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth(),
                placeholder = { Text("Szukaj kolekcji...") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { vm.setSearchQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Wyczyść")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Sortuj:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        val sortLabel = when (sortBy) {
                            CollectionsViewModel.SortBy.UPDATED -> "Data modyfikacji"
                            CollectionsViewModel.SortBy.NAME -> "Nazwa"
                        }
                        TextButton(
                            onClick = { sortMenuOpen = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                sortLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Data modyfikacji") },
                                onClick = {
                                    vm.setSortBy(CollectionsViewModel.SortBy.UPDATED)
                                    sortMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Nazwa") },
                                onClick = {
                                    vm.setSortBy(CollectionsViewModel.SortBy.NAME)
                                    sortMenuOpen = false
                                }
                            )
                        }
                    }
                    TextButton(onClick = { vm.setSortBy(sortBy) }) {
                        Icon(
                            if (sortAsc) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(if (sortAsc) "Rosnąco" else "Malejąco")
                    }
                }
                }
            }

            if (isSyncing) {
                Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            if (sorted.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (searchQuery.isNotBlank()) Icons.Filled.Search else Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(12.dp))
                        val msg = if (searchQuery.isNotBlank())
                            "Brak kolekcji pasuj\u0105cych do \"$searchQuery\""
                        else
                            "Brak kolekcji - dodaj pierwsz\u0105 klikaj\u0105c +"
                        Text(msg, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(state = collListState, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(sorted, key = { it.id }) { c ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(140.dp).clickable { onOpen(c.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                
                                val bannerUrl = c.pendingBannerPath ?: c.bannerImageUrl
                                if (bannerUrl != null) {
                                    AsyncImage(
                                        model = toCoilModel(bannerUrl, vm.serverBaseUrl),
                                        contentDescription = null,
                                        placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                                        error = ColorPainter(MaterialTheme.colorScheme.errorContainer),
                                        modifier = Modifier.matchParentSize().alpha(0.2f),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        
                                        
                                        
                                        Text(
                                            c.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (c.description.isNotBlank()) {
                                            Text(
                                                c.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        
                                        
                                        
                                        val count = itemCounts[c.id] ?: 0
                                        Text(
                                            "Pozycji: $count  ·  Pól w schemacie: ${c.schema.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isEditMode) {
                                        IconButton(onClick = { confirmDeleteCollectionId = c.id }) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Usuń kolekcję",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    
    
    
    confirmDeleteCollectionId?.let { id ->
        val name = sorted.firstOrNull { it.id == id }?.name ?: "tę kolekcję"
        ConfirmDeleteDialog(
            title = "Usunąć kolekcję?",
            message = "Czy na pewno chcesz usunąć \"$name\"? Wszystkie przedmioty w kolekcji zostaną ukryte. Tej operacji nie można cofnąć z poziomu aplikacji.",
            onConfirm = {
                vm.deleteCollection(id)
                confirmDeleteCollectionId = null
            },
            onDismiss = { confirmDeleteCollectionId = null }
        )
    }
}
