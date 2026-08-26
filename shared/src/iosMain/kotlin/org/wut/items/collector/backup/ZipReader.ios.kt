package org.wut.items.collector.backup

actual class ZipReader actual constructor() {
    actual fun open(absolutePath: String) {
        error("Import backup'u nie jest zaimplementowany na iOS")
    }
    actual fun readEntry(entryName: String): ByteArray? = null
    actual fun close() {}
}
