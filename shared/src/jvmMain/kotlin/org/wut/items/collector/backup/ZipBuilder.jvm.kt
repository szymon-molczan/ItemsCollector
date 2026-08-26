package org.wut.items.collector.backup

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream





actual class ZipBuilder {
    private var zos: ZipOutputStream? = null

    actual fun open(absolutePath: String) {
        check(zos == null) { "ZipBuilder.open() wywolane dwa razy bez close()" }
        val file = File(absolutePath)
        file.parentFile?.mkdirs()
        zos = ZipOutputStream(FileOutputStream(file))
    }

    actual fun addEntry(entryName: String, bytes: ByteArray) {
        val z = zos ?: error("ZipBuilder.addEntry() przed open()")
        z.putNextEntry(ZipEntry(entryName))
        z.write(bytes)
        z.closeEntry()
    }

    actual fun close() {
        zos?.close()
        zos = null
    }
}
