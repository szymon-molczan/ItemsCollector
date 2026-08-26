package org.wut.items.collector.backup

actual class ImportPaths {
    actual fun newImageFile(): String =
        error("Import backup'u nie jest zaimplementowany na iOS")
}
