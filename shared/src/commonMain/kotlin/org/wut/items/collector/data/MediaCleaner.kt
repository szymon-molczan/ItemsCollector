package org.wut.items.collector.data

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
















class MediaCleaner(
    private val fileSystem: FileSystem,
    private val referencedPaths: () -> List<String>
) {
    


    constructor(
        fileSystem: FileSystem,
        collRepo: CollectionRepository,
        itemRepo: ItemRepository,
        itemImageRepo: ItemImageRepository
    ) : this(fileSystem, {
        collRepo.allPendingBannerPaths() +
                itemRepo.allPendingImagePaths() +
                itemImageRepo.allPendingImagePaths()
    })

    





    fun cleanOrphans(mediaDir: String): Int {
        val dir: Path = mediaDir.toPath()
        if (!fileSystem.exists(dir)) return 0

        val referenced = referencedPaths().toSet()
        var deleted = 0
        runCatching {
            fileSystem.list(dir).forEach { p ->
                val abs = p.toString()
                
                
                
                
                
                
                
                val name = p.name
                val isEditorFile = name.startsWith("edit_") ||
                    name.startsWith("cam_") ||
                    name.startsWith("img_") ||
                    name.startsWith("import_")
                if (!isEditorFile) return@forEach
                if (abs in referenced) return@forEach
                runCatching { fileSystem.delete(p) }
                    .onSuccess { deleted++ }
            }
        }
        return deleted
    }
}
