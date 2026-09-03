package org.wut.items.collector.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val appDirectory = File(System.getProperty("user.home"), ".itemscollector")
        appDirectory.mkdirs()

        val databaseFile = File(appDirectory, "items_collector.db")
        val isNewDatabase = !databaseFile.exists()
        val jdbcUrl = "jdbc:sqlite:${databaseFile.absolutePath.replace('\\', '/')}"
        val driver = JdbcSqliteDriver(jdbcUrl)

        if (isNewDatabase) {
            AppDatabase.Schema.create(driver)
        }

        return driver
    }
}
