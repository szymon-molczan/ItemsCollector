package org.wut.items.collector.backup





















expect class ZipReader() {
    
    fun open(absolutePath: String)

    



    fun readEntry(entryName: String): ByteArray?

    
    fun close()
}
