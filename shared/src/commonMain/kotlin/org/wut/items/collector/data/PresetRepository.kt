package org.wut.items.collector.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.wut.items.collector.db.AppDatabase
import org.wut.items.collector.model.AttributeDef
import org.wut.items.collector.model.CollectionPreset
import org.wut.items.collector.model.CollectionPresets
import org.wut.items.collector.util.newUuid

class PresetRepository(private val db: AppDatabase) {

    private val attrSerializer = ListSerializer(AttributeDef.serializer())
    private val json = Json { ignoreUnknownKeys = true }

    init {
        initBuiltInPresets()
    }

    private fun initBuiltInPresets() {
        CollectionPresets.ALL.forEach { preset ->
            db.presetsQueries.upsert(
                id = preset.id,
                name = preset.name,
                description = preset.description,
                schemaJson = json.encodeToString(attrSerializer, preset.schema),
                isBuiltIn = 1L
            )
        }
    }

    fun observeAll(): Flow<List<CollectionPreset>> =
        db.presetsQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toModel() } }

    fun saveAsPreset(name: String, description: String, schema: List<AttributeDef>) {
        val id = newUuid()
        db.presetsQueries.upsert(
            id = id,
            name = name,
            description = description,
            schemaJson = json.encodeToString(attrSerializer, schema),
            isBuiltIn = 0L
        )
    }

    fun deletePreset(id: String) {
        db.presetsQueries.delete(id)
    }

    private fun org.wut.items.collector.db.Collection_presets.toModel(): CollectionPreset {
        val schema = if (schemaJson.isBlank()) emptyList()
        else json.decodeFromString(attrSerializer, schemaJson)
        return CollectionPreset(id, name, description, schema, isBuiltIn == 1L)
    }
}
