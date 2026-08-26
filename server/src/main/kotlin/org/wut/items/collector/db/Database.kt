package org.wut.items.collector.db

import java.io.File
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction












object Db {

    
    fun fileDbPath(dir: File = File(System.getenv("DB_DIR") ?: "server-data/db")): File {
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "items_collector")
    }

    fun init() {
        val mode = System.getenv("DB_MODE") ?: "file"
        val url = when (mode) {
            "mem" -> "jdbc:h2:mem:items_collector;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
            else -> {
                val path = fileDbPath().absolutePath
                
                
                "jdbc:h2:file:$path;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;AUTO_SERVER=TRUE"
            }
        }
        Database.connect(
            url = url,
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
        transaction {
            SchemaUtils.createMissingTablesAndColumns(Users, Collections, Items, ItemImages)
        }
    }
}
