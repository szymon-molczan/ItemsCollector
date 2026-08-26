package org.wut.items.collector

import okio.FileSystem
import org.wut.items.collector.data.CollectionRepository
import org.wut.items.collector.data.ItemImageRepository
import org.wut.items.collector.data.ItemRepository
import org.wut.items.collector.data.MediaCleaner
import org.wut.items.collector.data.PresetRepository
import org.wut.items.collector.data.SessionStore
import org.wut.items.collector.data.SyncEngine
import org.wut.items.collector.db.AppDatabase
import org.wut.items.collector.db.DatabaseDriverFactory
import org.wut.items.collector.db.createDatabase
import org.wut.items.collector.media.MediaPicker
import org.wut.items.collector.pdf.PdfExporter
import org.wut.items.collector.share.FileSharer
import org.wut.items.collector.theme.ThemePreferences
import org.wut.items.collector.backup.BackupExporter
import org.wut.items.collector.backup.BackupImporter
import org.wut.items.collector.backup.BackupPaths
import org.wut.items.collector.backup.FilePicker
import org.wut.items.collector.backup.ImportPaths
import org.wut.items.collector.backup.ZipBuilder
import org.wut.items.collector.backup.ZipReader
import org.wut.items.collector.network.ApiClient
import org.wut.items.collector.network.ConnectivityObserver
import org.wut.items.collector.network.httpClient









class AppContainer(
    driverFactory: DatabaseDriverFactory,
    val fileSystem: FileSystem,
    val mediaPicker: MediaPicker,
    val pdfExporter: PdfExporter,
    val fileSharer: FileSharer,
    val zipBuilder: ZipBuilder,
    val backupPaths: BackupPaths,
    val filePicker: FilePicker,
    val zipReader: ZipReader,
    val importPaths: ImportPaths,
    val themePreferences: ThemePreferences,
    val connectivityObserver: ConnectivityObserver
) {
    val database: AppDatabase = createDatabase(driverFactory)
    val sessionStore: SessionStore = SessionStore(database)
    val presetRepo: PresetRepository = PresetRepository(database)
    val collectionRepo: CollectionRepository = CollectionRepository(database)
    val itemRepo: ItemRepository = ItemRepository(database)

    





    val itemImageRepo: ItemImageRepository = ItemImageRepository(database).also { repo ->
        runCatching {
            val migrated = repo.runOneShotMigration()
            if (migrated > 0) println("ItemImageRepository: migrated $migrated items to item_images")
        }.onFailure { t ->
            println("ItemImageRepository.runOneShotMigration failed: ${t.message}")
        }
    }

    val apiClient: ApiClient = ApiClient(
        httpClientFactory = { config -> httpClient(config) },
        baseUrlProvider = { sessionStore.serverUrl() },
        tokenProvider = { sessionStore.token() }
    )

    val syncEngine: SyncEngine = SyncEngine(
        apiClient,
        collectionRepo,
        itemRepo,
        itemImageRepo,
        fileSystem,
        connectivityObserver,
        sessionStore
    )

    




    val mediaCleaner: MediaCleaner = MediaCleaner(
        fileSystem,
        collRepo = collectionRepo,
        itemRepo = itemRepo,
        itemImageRepo = itemImageRepo
    )

    




    val backupExporter: BackupExporter = BackupExporter(
        collectionRepo = collectionRepo,
        itemRepo = itemRepo,
        itemImageRepo = itemImageRepo,
        sessionStore = sessionStore,
        apiClient = apiClient,
        fileSystem = fileSystem,
        zipBuilder = zipBuilder,
        backupPaths = backupPaths
    )

    




    val backupImporter: BackupImporter = BackupImporter(
        collectionRepo = collectionRepo,
        itemRepo = itemRepo,
        itemImageRepo = itemImageRepo,
        fileSystem = fileSystem,
        zipReader = zipReader,
        importPaths = importPaths
    )
}
