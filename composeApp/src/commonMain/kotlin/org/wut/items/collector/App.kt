package org.wut.items.collector

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import org.wut.items.collector.theme.ThemeMode
import org.wut.items.collector.ui.theme.AppTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.ui.tooling.preview.Preview
import org.wut.items.collector.ui.AuthViewModel
import org.wut.items.collector.ui.BackupExportScreen
import org.wut.items.collector.ui.BackupExportViewModel
import org.wut.items.collector.ui.BackupImportScreen
import org.wut.items.collector.ui.BackupImportViewModel
import org.wut.items.collector.ui.CollectionDetailViewModel
import org.wut.items.collector.ui.CollectionEditScreen
import org.wut.items.collector.ui.CollectionsListScreen
import org.wut.items.collector.ui.CollectionsViewModel
import org.wut.items.collector.ui.CollectionDetailScreen
import org.wut.items.collector.ui.ItemEditScreen
import org.wut.items.collector.ui.LoginScreen
import org.wut.items.collector.ui.ManagePresetsScreen
import org.wut.items.collector.ui.PlatformBackHandler
import org.wut.items.collector.ui.Screen
import org.wut.items.collector.ui.SettingsScreen

@Composable
fun App(container: AppContainer) {
    
    
    
    val themeMode by container.themePreferences.mode.collectAsState(initial = ThemeMode.SYSTEM)
    AppTheme(mode = themeMode) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            
            var screen: Screen by remember { mutableStateOf<Screen>(Screen.Login) }
            val authVm = remember { AuthViewModel(container) }
            val session by authVm.session.collectAsState()

            
            
            
            
            
            val detailVms = remember { mutableMapOf<String, CollectionDetailViewModel>() }
            fun detailVm(collectionId: String): CollectionDetailViewModel =
                detailVms.getOrPut(collectionId) {
                    CollectionDetailViewModel(container, collectionId)
                }

            
            val sessionExists = session != null
            if (sessionExists && screen is Screen.Login) screen = Screen.CollectionsList
            if (!sessionExists && screen !is Screen.Login) screen = Screen.Login

            
            PlatformBackHandler(enabled = screen !is Screen.Login && screen !is Screen.CollectionsList) {
                screen = when (val s = screen) {
                    is Screen.CollectionDetail -> Screen.CollectionsList
                    is Screen.CollectionEdit -> if (s.collectionId == null) Screen.CollectionsList else Screen.CollectionDetail(s.collectionId)
                    is Screen.ItemEdit -> Screen.CollectionDetail(s.collectionId)
                    is Screen.Settings -> Screen.CollectionsList
                    is Screen.ManagePresets -> Screen.Settings
                    is Screen.BackupExport -> Screen.Settings
                    is Screen.BackupImport -> Screen.Settings
                    else -> Screen.CollectionsList
                }
            }

            Crossfade(targetState = screen, animationSpec = tween(300)) { s ->
                when (s) {
                    Screen.Login -> LoginScreen(authVm)
                    Screen.CollectionsList -> {
                        val vm = remember { CollectionsViewModel(container) }
                        CollectionsListScreen(
                            vm = vm,
                            onAdd = { screen = Screen.CollectionEdit() },
                            onOpen = { id -> screen = Screen.CollectionDetail(id) },
                            onSettings = { screen = Screen.Settings }
                        )
                    }
                    Screen.Settings -> {
                        SettingsScreen(
                            authVm = authVm,
                            themePreferences = container.themePreferences,
                            onBack = { screen = Screen.CollectionsList },
                            onManagePresets = { screen = Screen.ManagePresets },
                            onExport = { screen = Screen.BackupExport },
                            onImport = { screen = Screen.BackupImport }
                        )
                    }
                    Screen.ManagePresets -> {
                        val vm = remember { CollectionsViewModel(container) }
                        ManagePresetsScreen(
                            vm = vm,
                            onBack = { screen = Screen.Settings }
                        )
                    }
                    Screen.BackupExport -> {
                        val vm = remember { BackupExportViewModel(container) }
                        BackupExportScreen(
                            vm = vm,
                            onBack = { screen = Screen.Settings }
                        )
                    }
                    Screen.BackupImport -> {
                        val vm = remember { BackupImportViewModel(container) }
                        BackupImportScreen(
                            vm = vm,
                            onBack = { screen = Screen.Settings }
                        )
                    }
                    is Screen.CollectionEdit -> {
                        val vm = remember { CollectionsViewModel(container) }
                        CollectionEditScreen(
                            vm = vm,
                            collectionId = s.collectionId,
                            mediaPicker = container.mediaPicker,
                            onClose = { screen = if (s.collectionId == null) Screen.CollectionsList else Screen.CollectionDetail(s.collectionId) }
                        )
                    }
                    is Screen.CollectionDetail -> {
                        CollectionDetailScreen(
                            vm = detailVm(s.collectionId),
                            onBack = { screen = Screen.CollectionsList },
                            onEditCollection = { id -> screen = Screen.CollectionEdit(id) },
                            onAddItem = { screen = Screen.ItemEdit(s.collectionId) },
                            onEditItem = { itemId -> screen = Screen.ItemEdit(s.collectionId, itemId) }
                        )
                    }
                    is Screen.ItemEdit -> {
                        ItemEditScreen(
                            vm = detailVm(s.collectionId),
                            itemId = s.itemId,
                            mediaPicker = container.mediaPicker,
                            onClose = { screen = Screen.CollectionDetail(s.collectionId) }
                        )
                    }
                }
            }
        }
    }
}
