package org.wut.items.collector.ui


sealed class Screen {
    object Login : Screen()
    object CollectionsList : Screen()
    data class CollectionDetail(val collectionId: String) : Screen()
    data class CollectionEdit(val collectionId: String? = null) : Screen()
    data class ItemEdit(val collectionId: String, val itemId: String? = null) : Screen()
    




    object Settings : Screen()
    



    object BackupExport : Screen()
    



    object BackupImport : Screen()
    
    object ManagePresets : Screen()
}
