package org.wut.items.collector.backup


actual class ZipBuilder {
    actual fun open(absolutePath: String) { error("Backup nie jest zaimplementowany na iOS") }
    actual fun addEntry(entryName: String, bytes: ByteArray) { error("Backup nie jest zaimplementowany na iOS") }
    actual fun close() {  }
}
