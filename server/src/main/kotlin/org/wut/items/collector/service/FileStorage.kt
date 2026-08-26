package org.wut.items.collector.service

import java.io.File
import java.util.UUID






class FileStorage(private val rootDir: File = File("server-data/uploads")) {

    init {
        if (!rootDir.exists()) rootDir.mkdirs()
    }

    
    fun save(bytes: ByteArray, originalName: String?): String {
        val ext = originalName?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() && it.length <= 6 }
            ?: "bin"
        val name = "${UUID.randomUUID()}.${ext.lowercase()}"
        File(rootDir, name).writeBytes(bytes)
        return "/uploads/$name"
    }

    fun rootDir(): File = rootDir
}
