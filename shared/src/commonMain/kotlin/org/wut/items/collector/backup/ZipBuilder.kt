package org.wut.items.collector.backup


















expect class ZipBuilder() {
    
    fun open(absolutePath: String)

    
    fun addEntry(entryName: String, bytes: ByteArray)

    
    fun close()
}
