package org.wut.items.collector.backup

import java.io.File
import java.util.zip.ZipFile








actual class ZipReader actual constructor() {

    private var zip: ZipFile? = null

    actual fun open(absolutePath: String) {
        val file = File(absolutePath)
        require(file.exists() && file.isFile) { "Plik nie istnieje: $absolutePath" }
        zip = try {
            ZipFile(file)
        } catch (t: Throwable) {
            throw IllegalArgumentException("Nieprawidłowy plik ZIP: ${t.message}", t)
        }
    }

    actual fun readEntry(entryName: String): ByteArray? {
        val z = zip ?: error("ZipReader.open() musi byc wywolane przed readEntry()")
        val entry = z.getEntry(entryName) ?: return null
        return z.getInputStream(entry).use { it.readBytes() }
    }

    actual fun close() {
        zip?.close()
        zip = null
    }
}
