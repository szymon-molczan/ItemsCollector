package org.wut.items.collector.pdf

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.wut.items.collector.model.CollectionDto
import org.wut.items.collector.model.ItemDto
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale








actual class PdfExporter {

    actual suspend fun export(
        collection: CollectionDto,
        items: List<ItemDto>,
        primaryImageProvider: (itemId: String) -> PrimaryImageRef?
    ): String = withContext(Dispatchers.IO) {
        val doc = PDDocument()

        val pageW = PDRectangle.A4.width   
        val pageH = PDRectangle.A4.height  
        val margin = 36f
        val contentTop = margin + 60f
        val contentTopOther = margin + 24f
        val itemHeight = 160f
        val thumbSize = 120f

        val fontBold = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
        val fontRegular = PDType1Font(Standard14Fonts.FontName.HELVETICA)

        var pageNum = 1
        var cs: PDPageContentStream? = null
        var y = 0f

        fun newPage(isFirst: Boolean) {
            cs?.close()
            val page = PDPage(PDRectangle.A4)
            doc.addPage(page)
            cs = PDPageContentStream(doc, page)

            
            cs!!.beginText()
            cs!!.setFont(fontRegular, 10f)
            cs!!.newLineAtOffset(pageW - margin - 60f, pageH - margin + 8f)
            cs!!.showText("Strona $pageNum")
            cs!!.endText()

            if (isFirst) {
                
                cs!!.beginText()
                cs!!.setFont(fontBold, 22f)
                cs!!.newLineAtOffset(margin, pageH - margin - 18f)
                cs!!.showText(collection.name.take(60))
                cs!!.endText()

                if (collection.description.isNotBlank()) {
                    cs!!.beginText()
                    cs!!.setFont(fontRegular, 12f)
                    cs!!.newLineAtOffset(margin, pageH - margin - 38f)
                    cs!!.showText(collection.description.take(80))
                    cs!!.endText()
                }

                cs!!.beginText()
                cs!!.setFont(fontRegular, 12f)
                cs!!.newLineAtOffset(margin, pageH - margin - 54f)
                cs!!.showText("Liczba pozycji: ${items.size}")
                cs!!.endText()

                y = pageH - contentTop
            } else {
                y = pageH - contentTopOther
            }
            pageNum++
        }

        newPage(isFirst = true)

        val schemaMap = collection.schema.associate { it.key to it.label }

        for (item in items) {
            if (y - itemHeight < margin) {
                newPage(isFirst = false)
            }
            val stream = cs ?: continue

            val itemTop = y
            val itemBottom = y - itemHeight

            
            stream.setStrokingColor(0.8f, 0.8f, 0.8f)
            stream.setLineWidth(1f)
            stream.addRect(margin, itemBottom, pageW - 2 * margin, itemHeight)
            stream.stroke()

            
            val thumbX = margin + 8f
            val thumbY = itemBottom + (itemHeight - thumbSize) / 2f
            val thumbImage = loadThumbnail(doc, item, primaryImageProvider(item.id))
            if (thumbImage != null) {
                stream.drawImage(thumbImage, thumbX, thumbY, thumbSize, thumbSize)
            } else {
                
                stream.setNonStrokingColor(0.9f, 0.9f, 0.9f)
                stream.addRect(thumbX, thumbY, thumbSize, thumbSize)
                stream.fill()
                stream.setNonStrokingColor(0.5f, 0.5f, 0.5f)
                stream.beginText()
                stream.setFont(fontRegular, 12f)
                stream.newLineAtOffset(thumbX + 30f, thumbY + thumbSize / 2f)
                stream.showText("(brak)")
                stream.endText()
            }

            
            val textX = thumbX + thumbSize + 16f
            var textY = itemTop - 24f

            stream.setNonStrokingColor(0f, 0f, 0f)
            stream.beginText()
            stream.setFont(fontBold, 16f)
            stream.newLineAtOffset(textX, textY)
            stream.showText(sanitize(item.name.take(60)))
            stream.endText()

            textY -= 18f
            if (item.description.isNotBlank()) {
                stream.setNonStrokingColor(0.3f, 0.3f, 0.3f)
                stream.beginText()
                stream.setFont(fontRegular, 12f)
                stream.newLineAtOffset(textX, textY)
                stream.showText(sanitize(item.description.take(80)))
                stream.endText()
                textY -= 18f
            }

            
            stream.setNonStrokingColor(0f, 0f, 0f)
            for (attr in item.attributes.take(4)) {
                val label = schemaMap[attr.key] ?: attr.key
                stream.beginText()
                stream.setFont(fontRegular, 11f)
                stream.newLineAtOffset(textX, textY)
                stream.showText(sanitize("$label: ${attr.value.take(40)}"))
                stream.endText()
                textY -= 14f
            }

            
            stream.setNonStrokingColor(0f, 0f, 0f)

            y = itemBottom - 8f
        }

        if (items.isEmpty()) {
            cs?.beginText()
            cs?.setFont(fontRegular, 12f)
            cs?.newLineAtOffset(margin, y)
            cs?.showText("(kolekcja nie zawiera pozycji)")
            cs?.endText()
        }

        cs?.close()

        
        val dir = File(System.getProperty("user.home"), ".itemscollector/exports").apply { mkdirs() }
        val safeName = collection.name.replace(Regex("[^A-Za-z0-9_-]"), "_").take(40)
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
        val outFile = File(dir, "collection_${safeName}_$ts.pdf")
        doc.save(outFile)
        doc.close()

        outFile.absolutePath
    }

    private fun loadThumbnail(
        doc: PDDocument,
        item: ItemDto,
        primaryRef: PrimaryImageRef?
    ): PDImageXObject? {
        val candidates = listOfNotNull(
            primaryRef?.pendingImagePath,
            primaryRef?.imageUrl,
            item.pendingImagePath,
            item.imageUrl
        )
        for (c in candidates) {
            if (c.startsWith("http://") || c.startsWith("https://")) continue
            val f = File(c)
            if (!f.exists() || f.length() == 0L) continue
            return try {
                PDImageXObject.createFromFileByContent(f, doc)
            } catch (_: Throwable) {
                null
            }
        }
        return null
    }

    



    private fun sanitize(text: String): String {
        val map = mapOf(
            'ą' to 'a', 'ć' to 'c', 'ę' to 'e', 'ł' to 'l', 'ń' to 'n',
            'ó' to 'o', 'ś' to 's', 'ź' to 'z', 'ż' to 'z',
            'Ą' to 'A', 'Ć' to 'C', 'Ę' to 'E', 'Ł' to 'L', 'Ń' to 'N',
            'Ó' to 'O', 'Ś' to 'S', 'Ź' to 'Z', 'Ż' to 'Z'
        )
        return buildString {
            for (ch in text) {
                val replacement = map[ch]
                if (replacement != null) {
                    append(replacement)
                } else if (ch.code in 32..126 || ch.code in 160..255) {
                    append(ch)
                } else {
                    append('?')
                }
            }
        }
    }
}
