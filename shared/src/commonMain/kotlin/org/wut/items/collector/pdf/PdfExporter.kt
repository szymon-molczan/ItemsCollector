package org.wut.items.collector.pdf

import org.wut.items.collector.model.AttributeDef
import org.wut.items.collector.model.CollectionDto
import org.wut.items.collector.model.ItemDto








data class PrimaryImageRef(val pendingImagePath: String?, val imageUrl: String?)










expect class PdfExporter {

    









    suspend fun export(
        collection: CollectionDto,
        items: List<ItemDto>,
        primaryImageProvider: (itemId: String) -> PrimaryImageRef?
    ): String
}
