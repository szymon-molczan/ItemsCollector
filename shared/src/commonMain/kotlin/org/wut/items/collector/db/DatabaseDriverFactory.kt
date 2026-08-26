package org.wut.items.collector.db

import app.cash.sqldelight.db.SqlDriver





expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(factory: DatabaseDriverFactory): AppDatabase =
    AppDatabase(factory.createDriver())
