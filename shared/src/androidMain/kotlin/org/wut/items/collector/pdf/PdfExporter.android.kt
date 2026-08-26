package org.wut.items.collector.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wut.items.collector.model.CollectionDto
import org.wut.items.collector.model.ItemDto
import java.io.File
import java.io.FileOutputStream












actual class PdfExporter(private val context: Context) {

    actual suspend fun export(
        collection: CollectionDto,
        items: List<ItemDto>,
        primaryImageProvider: (itemId: String) -> PrimaryImageRef?
    ): String = withContext(Dispatchers.Default) {
        val doc = PdfDocument()

        val pageW = 595
        val pageH = 842
        val margin = 36
        val contentTop = margin + 60       
        val contentTopOther = margin + 24  
        val itemHeight = 160
        val thumbSize = 120

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 22f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val subPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
            isAntiAlias = true
        }
        val itemNamePaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val itemDescPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
            isAntiAlias = true
        }
        val attrPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            isAntiAlias = true
        }
        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val pageNumPaint = Paint().apply {
            color = Color.GRAY
            textSize = 10f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        var pageNum = 1
        var page: PdfDocument.Page? = null
        var canvas: android.graphics.Canvas? = null
        var y = 0

        fun newPage(isFirst: Boolean) {
            page?.let { doc.finishPage(it) }
            val info = PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create()
            val p = doc.startPage(info)
            page = p
            canvas = p.canvas
            
            canvas?.drawText("Strona $pageNum", (pageW - margin).toFloat(), (margin - 8).toFloat(), pageNumPaint)
            if (isFirst) {
                canvas?.drawText(collection.name, margin.toFloat(), (margin + 18).toFloat(), titlePaint)
                if (collection.description.isNotBlank()) {
                    canvas?.drawText(collection.description, margin.toFloat(), (margin + 38).toFloat(), subPaint)
                }
                canvas?.drawText(
                    "Liczba pozycji: ${items.size}",
                    margin.toFloat(),
                    (margin + 54).toFloat(),
                    subPaint
                )
                y = contentTop
            } else {
                y = contentTopOther
            }
            pageNum++
        }

        newPage(isFirst = true)

        
        val schemaMap = collection.schema.associate { it.key to it.label }

        for (item in items) {
            
            if (y + itemHeight > pageH - margin) {
                newPage(isFirst = false)
            }
            val canvasNonNull = canvas ?: continue

            
            canvasNonNull.drawRect(
                margin.toFloat(),
                y.toFloat(),
                (pageW - margin).toFloat(),
                (y + itemHeight).toFloat(),
                borderPaint
            )

            
            val thumbX = margin + 8
            val thumbY = y + 8
            val primaryRef = primaryImageProvider(item.id)
            val thumbBmp = loadThumbnail(item, primaryRef, thumbSize)
            if (thumbBmp != null) {
                val dst = Rect(thumbX, thumbY, thumbX + thumbSize, thumbY + thumbSize)
                canvasNonNull.drawBitmap(thumbBmp, null, dst, null)
            } else {
                
                val placePaint = Paint().apply {
                    color = Color.LTGRAY
                    style = Paint.Style.FILL
                }
                canvasNonNull.drawRect(
                    thumbX.toFloat(),
                    thumbY.toFloat(),
                    (thumbX + thumbSize).toFloat(),
                    (thumbY + thumbSize).toFloat(),
                    placePaint
                )
                canvasNonNull.drawText("(brak)", (thumbX + 30).toFloat(), (thumbY + 65).toFloat(), subPaint)
            }

            
            val textX = thumbX + thumbSize + 16
            var textY = y + 24
            canvasNonNull.drawText(item.name.take(60), textX.toFloat(), textY.toFloat(), itemNamePaint)
            textY += 18
            if (item.description.isNotBlank()) {
                drawWrappedText(canvasNonNull, item.description, textX, textY, pageW - margin - textX - 8, itemDescPaint, maxLines = 2)
                textY += 30
            }
            
            for (attr in item.attributes.take(4)) {
                val label = schemaMap[attr.key] ?: attr.key
                canvasNonNull.drawText("$label: ${attr.value.take(40)}", textX.toFloat(), textY.toFloat(), attrPaint)
                textY += 14
            }

            y += itemHeight + 8
        }

        if (items.isEmpty()) {
            canvas?.drawText("(kolekcja nie zawiera pozycji)", margin.toFloat(), y.toFloat(), subPaint)
        }

        page?.let { doc.finishPage(it) }

        
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val safeName = collection.name.replace(Regex("[^A-Za-z0-9_-]"), "_").take(40)
        val out = File(dir, "collection_${safeName}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(out).use { fos -> doc.writeTo(fos) }
        doc.close()

        out.absolutePath
    }

    





    private fun loadThumbnail(item: ItemDto, primaryRef: PrimaryImageRef?, target: Int): Bitmap? {
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
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(c, opts)
            if (opts.outWidth <= 0) continue
            var sample = 1
            while (opts.outWidth / sample > target * 2 || opts.outHeight / sample > target * 2) sample *= 2
            val real = BitmapFactory.Options().apply { inSampleSize = sample }
            return BitmapFactory.decodeFile(c, real)
        }
        return null
    }

    private fun drawWrappedText(
        canvas: android.graphics.Canvas,
        text: String,
        x: Int,
        y: Int,
        maxWidth: Int,
        paint: Paint,
        maxLines: Int
    ) {
        val words = text.split(" ")
        val sb = StringBuilder()
        var line = 0
        var curY = y
        for (w in words) {
            val attempt = if (sb.isEmpty()) w else "$sb $w"
            if (paint.measureText(attempt) > maxWidth) {
                canvas.drawText(sb.toString(), x.toFloat(), curY.toFloat(), paint)
                sb.clear()
                sb.append(w)
                line++
                curY += 14
                if (line >= maxLines) return
            } else {
                sb.clear()
                sb.append(attempt)
            }
        }
        if (sb.isNotEmpty() && line < maxLines) {
            canvas.drawText(sb.toString(), x.toFloat(), curY.toFloat(), paint)
        }
    }
}
