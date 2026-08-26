package org.wut.items.collector.theme

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


actual class ThemePreferences {
    private val state = MutableStateFlow(ThemeMode.SYSTEM)
    actual val mode: Flow<ThemeMode> = state.asStateFlow()
    actual fun setMode(mode: ThemeMode) { state.value = mode }
}
