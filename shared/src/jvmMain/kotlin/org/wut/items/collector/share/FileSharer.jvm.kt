package org.wut.items.collector.share

import java.awt.Desktop
import java.io.File





actual class FileSharer {
    actual fun share(path: String, mime: String, title: String) {
        try {
            val file = File(path)
            if (file.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file)
            }
        } catch (e: Throwable) {
            println("FileSharer: nie udalo sie otworzyc pliku: ${e.message}")
        }
    }
}
