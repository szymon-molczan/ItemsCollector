package org.wut.items.collector.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import org.wut.items.collector.model.AttributeType
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState

@Composable
private fun FabMenuAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.55f)
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
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
fun CollectionDetailScreen(
    vm: CollectionDetailViewModel,
    onBack: () -> Unit,
    onEditCollection: (String) -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (String) -> Unit
) {
    val collection by vm.collection.collectAsState()
    val items by vm.items.collectAsState()
    
    
    val primaryImages by vm.primaryImagesByItem.collectAsState(initial = emptyMap())
    val sortBy by vm.sortBy.collectAsState()
    val sortAttrKey by vm.sortAttributeKey.collectAsState()
    val sortAsc by vm.sortAsc.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val searchAttrKey by vm.searchAttributeKey.collectAsState()
    val viewMode by vm.viewMode.collectAsState()
    val gridColumns by vm.gridColumns.collectAsState()
    val onlyFavorites by vm.onlyFavorites.collectAsState()
    val exportState by vm.exportState.collectAsState()
    val bulkImportState by vm.bulkImportState.collectAsState()

    var searchMenuOpen by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var searchVisible by remember { mutableStateOf(false) }
    
    var confirmDeleteItemId by remember { mutableStateOf<String?>(null) }
    
    var showMosaic by remember { mutableStateOf(false) }
    var fabMenuOpen by remember { mutableStateOf(false) }
    var bulkUseFileNames by remember { mutableStateOf(true) }
    var bulkOptimizeImages by remember { mutableStateOf(true) }

    
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    
    
    
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(vm) {
        vm.uiMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    
    if (showMosaic) {
        val displayed = remember(items, sortBy, sortAttrKey, sortAsc, searchQuery, searchAttrKey, onlyFavorites) { vm.displayItems() }
        FullScreenMosaicScreen(
            items = displayed,
            primaryImages = primaryImages,
            gridColumns = gridColumns,
            serverBaseUrl = vm.serverBaseUrl,
            onGridColumnsChange = { vm.setGridColumns(it) },
            onDismiss = { showMosaic = false }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box {
                val bannerUrl = collection?.pendingBannerPath ?: collection?.bannerImageUrl
                if (bannerUrl != null) {
                    AsyncImage(
                        model = toCoilModel(bannerUrl, vm.serverBaseUrl),
                        contentDescription = null,
                        placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                        error = ColorPainter(MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().height(160.dp).alpha(0.3f),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)))
                }

                TopAppBar(
                    title = { },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Wstecz",
                                tint = if (bannerUrl != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {}
                )

                
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    collection?.let { c ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = c.name,
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                enabled = exportState !is CollectionDetailViewModel.ExportState.InProgress,
                                onClick = { vm.exportToPdf() }
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = "Eksport do PDF")
                            }
                        }
                        if (c.description.isNotBlank()) {
                            Text(
                                c.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            val bulkImportInProgress = bulkImportState != CollectionDetailViewModel.BulkImportState.Idle
            Surface(
                tonalElevation = 0.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(visible = fabMenuOpen) {
                        Column(
                            modifier = Modifier.padding(bottom = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FabMenuAction(
                                icon = Icons.Filled.Add,
                                label = "Dodaj jeden element",
                                onClick = {
                                    fabMenuOpen = false
                                    onAddItem()
                                }
                            )
                            FabMenuAction(
                                icon = Icons.Filled.PhotoLibrary,
                                label = if (bulkImportInProgress) "Importowanie..." else "Dodaj wiele zdjęć",
                                enabled = !bulkImportInProgress,
                                onClick = {
                                    fabMenuOpen = false
                                    vm.importImagesAsItems()
                                }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomBarAction(
                            icon = Icons.Filled.Edit,
                            label = "Edycja",
                            enabled = collection != null,
                            onClick = {
                                collection?.let { onEditCollection(it.id) }
                                fabMenuOpen = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                        BottomBarAction(
                            icon = Icons.Filled.Search,
                            label = "Szukaj",
                            selected = searchVisible || searchQuery.isNotBlank() || onlyFavorites,
                            onClick = {
                                searchVisible = !searchVisible
                                fabMenuOpen = false
                                sortMenuOpen = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            SmallFloatingActionButton(
                                onClick = {
                                    if (!bulkImportInProgress) {
                                        fabMenuOpen = !fabMenuOpen
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                if (bulkImportInProgress) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (fabMenuOpen) Icons.Filled.Close else Icons.Filled.Add,
                                        contentDescription = if (fabMenuOpen) "Zamknij menu dodawania" else "Dodaj"
                                    )
                                }
                            }
                        }
                        BottomBarAction(
                            icon = Icons.Filled.Image,
                            label = "Mozaika",
                            onClick = {
                                showMosaic = true
                                fabMenuOpen = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                        BottomBarAction(
                            icon = if (viewMode == CollectionDetailViewModel.ViewMode.LIST) Icons.Filled.GridView else Icons.Filled.ViewList,
                            label = "Widok",
                            onClick = {
                                vm.toggleViewMode()
                                fabMenuOpen = false
                                sortMenuOpen = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            
            AnimatedVisibility(visible = searchVisible || searchQuery.isNotBlank() || onlyFavorites) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { vm.setSearchQuery(it) },
                        label = {
                            val label = if (searchAttrKey == null) "nazwie"
                                        else collection?.schema?.firstOrNull { it.key == searchAttrKey }?.label ?: "polu"
                            Text("Szukaj po $label")
                        },
                        singleLine = true,
                        leadingIcon = {
                            Box {
                                IconButton(onClick = { searchMenuOpen = true }) {
                                    Icon(Icons.Filled.Search, contentDescription = "Wybierz pole wyszukiwania")
                                }
                                DropdownMenu(expanded = searchMenuOpen, onDismissRequest = { searchMenuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Nazwa") },
                                        onClick = { vm.setSearchAttribute(null); searchMenuOpen = false }
                                    )
                                    collection?.schema?.filter { it.type == AttributeType.TEXT || it.type == AttributeType.SELECT }?.forEach { def ->
                                        DropdownMenuItem(
                                            text = { Text(def.label) },
                                            onClick = { vm.setSearchAttribute(def.key); searchMenuOpen = false }
                                        )
                                    }
                                }
                            }
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { vm.toggleOnlyFavorites() }) {
                                    Icon(
                                        if (onlyFavorites) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = "Tylko ulubione",
                                        tint = if (onlyFavorites) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { vm.setSearchQuery("") }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Wyczyść")
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Sortuj",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            val sortLabel = when (sortBy) {
                                CollectionDetailViewModel.SortBy.UPDATED -> "Data modyfikacji"
                                CollectionDetailViewModel.SortBy.NAME -> "Nazwa"
                                CollectionDetailViewModel.SortBy.FAVORITE -> "Ulubione"
                                CollectionDetailViewModel.SortBy.ATTRIBUTE ->
                                    collection?.schema?.firstOrNull { it.key == sortAttrKey }?.label ?: "Atrybut"
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
                                        vm.setSortBy(CollectionDetailViewModel.SortBy.UPDATED)
                                        sortMenuOpen = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Nazwa") },
                                    onClick = {
                                        vm.setSortBy(CollectionDetailViewModel.SortBy.NAME)
                                        sortMenuOpen = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Ulubione") },
                                    onClick = {
                                        vm.setSortBy(CollectionDetailViewModel.SortBy.FAVORITE)
                                        sortMenuOpen = false
                                    }
                                )
                                collection?.schema?.forEach { def ->
                                    DropdownMenuItem(
                                        text = { Text(def.label) },
                                        onClick = {
                                            vm.setSortBy(CollectionDetailViewModel.SortBy.ATTRIBUTE, def.key)
                                            sortMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                        TextButton(onClick = { vm.toggleSortDirection() }) {
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

            
            val displayed = remember(items, sortBy, sortAttrKey, sortAsc, searchQuery, searchAttrKey, onlyFavorites) { vm.displayItems() }

            if (displayed.isEmpty()) {
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
                            "Brak wyników dla „$searchQuery”"
                        else
                            "Brak przedmiotów. Dodaj pierwszy przyciskiem +."
                        Text(msg, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                if (viewMode == CollectionDetailViewModel.ViewMode.LIST) {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        items(displayed, key = { it.id }) { item ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        confirmDeleteItemId = item.id
                                    }
                                    false 
                                }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(vertical = 6.dp)
                                            .background(
                                                MaterialTheme.colorScheme.errorContainer,
                                                RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Usuń",
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(end = 20.dp)
                                        )
                                    }
                                },
                                enableDismissFromStartToEnd = false
                            ) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable {
                                    onEditItem(item.id)
                                }
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    val primary = primaryImages[item.id]
                                    val thumb: Any? = primary?.pendingImagePath?.let { toCoilModel(it, vm.serverBaseUrl) }
                                        ?: primary?.imageUrl?.takeIf { it.isNotBlank() }?.let { toCoilModel(it, vm.serverBaseUrl) }
                                        ?: item.pendingImagePath?.let { toCoilModel(it, vm.serverBaseUrl) }
                                        ?: item.imageUrl?.takeIf { it.isNotBlank() }?.let { toCoilModel(it, vm.serverBaseUrl) }
                                    if (thumb != null) {
                                        AsyncImage(
                                            model = thumb,
                                            contentDescription = null,
                                            placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                                            error = ColorPainter(MaterialTheme.colorScheme.errorContainer),
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                                        )
                                        Spacer(Modifier.size(12.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            item.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (item.description.isNotBlank()) {
                                            Text(
                                                item.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        item.attributes.take(3).forEach { v ->
                                            val label = collection?.schema?.firstOrNull { it.key == v.key }?.label ?: v.key
                                            Text(
                                                "$label: ${v.value}",
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    IconButton(onClick = { vm.toggleFavorite(item.id) }) {
                                        Icon(
                                            if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                            contentDescription = "Ulubione",
                                            tint = if (item.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { confirmDeleteItemId = item.id }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Usuń przedmiot",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            } 
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                } else {
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Siatka:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 8.dp))
                        (1..5).forEach { n ->
                            val selected = gridColumns == n
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(36.dp)
                                    .background(
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp)
                                    )
                                    .clickable { vm.setGridColumns(n) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$n",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        state = gridState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayed, key = { it.id }) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onEditItem(item.id) }
                            ) {
                                Column {
                                    val primary = primaryImages[item.id]
                                    val thumb: Any? = primary?.pendingImagePath?.let { toCoilModel(it, vm.serverBaseUrl) }
                                        ?: primary?.imageUrl?.takeIf { it.isNotBlank() }?.let { toCoilModel(it, vm.serverBaseUrl) }
                                        ?: item.pendingImagePath?.let { toCoilModel(it, vm.serverBaseUrl) }
                                        ?: item.imageUrl?.takeIf { it.isNotBlank() }?.let { toCoilModel(it, vm.serverBaseUrl) }
                                    
                                    val imageHeight = when {
                                        gridColumns <= 1 -> 220.dp
                                        gridColumns == 2 -> 150.dp
                                        gridColumns == 3 -> 110.dp
                                        else -> 80.dp
                                    }
                                    Box(modifier = Modifier.fillMaxWidth().height(imageHeight).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                        if (thumb != null) {
                                            AsyncImage(
                                                model = thumb,
                                                contentDescription = null,
                                                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                                                error = ColorPainter(MaterialTheme.colorScheme.errorContainer),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                Icons.Filled.Image, 
                                                contentDescription = null,
                                                modifier = Modifier.align(Alignment.Center).size(48.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                        
                                        IconButton(
                                            onClick = { vm.toggleFavorite(item.id) },
                                            modifier = Modifier.align(Alignment.TopEnd)
                                        ) {
                                            Icon(
                                                if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                                contentDescription = "Ulubione",
                                                tint = if (item.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        item.name,
                                        modifier = Modifier.padding(if (gridColumns <= 2) 8.dp else 4.dp),
                                        style = if (gridColumns <= 2) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelSmall,
                                        maxLines = if (gridColumns <= 2) 2 else 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    
    confirmDeleteItemId?.let { id ->
        val itemName = items.firstOrNull { it.id == id }?.name ?: "ten przedmiot"
        ConfirmDeleteDialog(
            title = "Usunąć przedmiot?",
            message = "Czy na pewno chcesz usunąć \"$itemName\"? Wszystkie zdjęcia tego przedmiotu zostaną ukryte.",
            onConfirm = {
                vm.deleteItem(id)
                confirmDeleteItemId = null
            },
            onDismiss = { confirmDeleteItemId = null }
        )
    }

    val bulkConfirmState = bulkImportState as? CollectionDetailViewModel.BulkImportState.Confirming
    if (bulkConfirmState != null) {
        AlertDialog(
            onDismissRequest = { vm.cancelBulkImageImport() },
            title = { Text("Import zdjęć") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Wybrano ${bulkConfirmState.count} zdjęć. Aplikacja utworzy z nich nowe pozycje w tej kolekcji.")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { bulkUseFileNames = !bulkUseFileNames }
                    ) {
                        Checkbox(
                            checked = bulkUseFileNames,
                            onCheckedChange = { bulkUseFileNames = it }
                        )
                        Text("Użyj nazw plików jako nazw pozycji")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { bulkOptimizeImages = !bulkOptimizeImages }
                    ) {
                        Checkbox(
                            checked = bulkOptimizeImages,
                            onCheckedChange = { bulkOptimizeImages = it }
                        )
                        Text("Optymalizuj zdjęcia przed zapisem")
                    }
                    Text(
                        "Opis i atrybuty pozostaną puste. Synchronizację należy uruchomić ręcznie.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { vm.confirmBulkImageImport(bulkUseFileNames, bulkOptimizeImages) }) {
                    Text("Importuj")
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.cancelBulkImageImport() }) {
                    Text("Anuluj")
                }
            }
        )
    }
}
