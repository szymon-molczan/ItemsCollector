package org.wut.items.collector.pdf

import org.wut.items.collector.model.CollectionDto
import org.wut.items.collector.model.ItemDto


actual class PdfExporter {
    actual suspend fun export(
        collection: CollectionDto,
        items: List<ItemDto>,
        primaryImageProvider: (itemId: String) -> PrimaryImageRef?
    ): String {
        error("Eksport PDF nie jest zaimplementowany na iOS")
    }
}
