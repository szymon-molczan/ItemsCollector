package org.wut.items.collector

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import okio.FileSystem
import org.wut.items.collector.backup.BackupPaths
import org.wut.items.collector.backup.FilePicker
import org.wut.items.collector.backup.ImportPaths
import org.wut.items.collector.backup.ZipBuilder
import org.wut.items.collector.backup.ZipReader
import org.wut.items.collector.db.DatabaseDriverFactory
import org.wut.items.collector.media.MediaPicker
import org.wut.items.collector.network.JvmConnectivityObserver
import org.wut.items.collector.pdf.PdfExporter
import org.wut.items.collector.share.FileSharer
import org.wut.items.collector.theme.ThemePreferences

fun main() = application {
    val container = AppContainer(
        driverFactory = DatabaseDriverFactory(),
        fileSystem = FileSystem.SYSTEM,
        mediaPicker = MediaPicker(),
        pdfExporter = PdfExporter(),
        fileSharer = FileSharer(),
        zipBuilder = ZipBuilder(),
        backupPaths = BackupPaths(),
        filePicker = FilePicker(),
        zipReader = ZipReader(),
        importPaths = ImportPaths(),
        themePreferences = ThemePreferences(),
        connectivityObserver = JvmConnectivityObserver()
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "ItemsCollector"
    ) {
        App(container)
    }
}
