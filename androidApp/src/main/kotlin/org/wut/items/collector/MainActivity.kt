package org.wut.items.collector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.FileSystem
import org.wut.items.collector.backup.BackupPaths
import org.wut.items.collector.backup.FilePicker
import org.wut.items.collector.backup.ImportPaths
import org.wut.items.collector.backup.ZipBuilder
import org.wut.items.collector.backup.ZipReader
import org.wut.items.collector.db.DatabaseDriverFactory
import org.wut.items.collector.media.MediaPicker
import org.wut.items.collector.pdf.PdfExporter
import org.wut.items.collector.share.FileSharer
import org.wut.items.collector.theme.ThemePreferences
import org.wut.items.collector.network.AndroidConnectivityObserver
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var container: AppContainer
    private lateinit var mediaPicker: MediaPicker
    private lateinit var filePicker: FilePicker

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        
        mediaPicker = MediaPicker(this).also { it.register() }
        filePicker = FilePicker(this).also { it.register() }

        container = AppContainer(
            driverFactory = DatabaseDriverFactory(applicationContext),
            fileSystem = FileSystem.SYSTEM,
            mediaPicker = mediaPicker,
            pdfExporter = PdfExporter(applicationContext),
            fileSharer = FileSharer(applicationContext),
            zipBuilder = ZipBuilder(),
            backupPaths = BackupPaths(applicationContext),
            filePicker = filePicker,
            zipReader = ZipReader(),
            importPaths = ImportPaths(applicationContext),
            themePreferences = ThemePreferences(applicationContext),
            connectivityObserver = AndroidConnectivityObserver(applicationContext)
        )

        
        
        CoroutineScope(Dispatchers.IO).launch {
            val mediaDir = File(cacheDir, "media").absolutePath
            val deleted = container.mediaCleaner.cleanOrphans(mediaDir)
            if (deleted > 0) {
                println("MediaCleaner: usunieto $deleted osieroconych plikow z cache")
            }
        }

        setContent {
            App(container)
        }
    }
}
