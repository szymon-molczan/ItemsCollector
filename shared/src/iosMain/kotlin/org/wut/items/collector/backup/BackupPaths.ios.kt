package org.wut.items.collector.backup

actual class BackupPaths {
    actual fun newBackupFile(): String = error("Backup nie jest zaimplementowany na iOS")
}
