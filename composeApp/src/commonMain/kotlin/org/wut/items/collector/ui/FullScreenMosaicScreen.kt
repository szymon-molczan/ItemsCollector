package org.wut.items.collector.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.wut.items.collector.model.ItemDto













@Composable
fun FullScreenMosaicScreen(
    items: List<ItemDto>,
    primaryImages: Map<String, org.wut.items.collector.data.ItemImageRepository.PrimaryImage>,
    gridColumns: Int,
    serverBaseUrl: String?,
    onGridColumnsChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    
    data class MosaicEntry(val itemId: String, val model: Any)

    val entries = remember(items, primaryImages) {
        items.mapNotNull { item ->
            val primary = primaryImages[item.id]
            val model: Any? = primary?.pendingImagePath?.let { toCoilModel(it, serverBaseUrl) }
                ?: primary?.imageUrl?.takeIf { it.isNotBlank() }?.let { toCoilModel(it, serverBaseUrl) }
                ?: item.pendingImagePath?.let { toCoilModel(it, serverBaseUrl) }
                ?: item.imageUrl?.takeIf { it.isNotBlank() }?.let { toCoilModel(it, serverBaseUrl) }
            if (model != null) MosaicEntry(item.id, model) else null
        }
    }

    
    var carouselIndex by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Brak zdjęć w kolekcji",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(bottom = 64.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(entries, key = { it.itemId }) { entry ->
                    val index = entries.indexOf(entry)
                    AsyncImage(
                        model = entry.model,
                        contentDescription = null,
                        placeholder = ColorPainter(Color.DarkGray),
                        error = ColorPainter(MaterialTheme.colorScheme.errorContainer),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clickable { carouselIndex = index }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }

        
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Wróć",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.size(12.dp))
            Text(
                "Siatka:",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(end = 8.dp)
            )
            (1..5).forEach { n ->
                val selected = gridColumns == n
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(32.dp)
                        .background(
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(999.dp)
                        )
                        .clickable { onGridColumnsChange(n) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$n",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        
        val cIdx = carouselIndex
        if (cIdx != null && entries.isNotEmpty()) {
            val pagerState = rememberPagerState(
                initialPage = cIdx.coerceIn(0, entries.size - 1),
                pageCount = { entries.size }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    ZoomableImage(
                        model = entries[page].model,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                IconButton(
                    onClick = { carouselIndex = null },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(16.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Zamknij",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (entries.size > 1) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${entries.size}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}
