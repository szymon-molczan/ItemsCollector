package org.wut.items.collector.theme

import kotlinx.coroutines.flow.Flow








enum class ThemeMode { SYSTEM, LIGHT, DARK }












expect class ThemePreferences {
    
    val mode: Flow<ThemeMode>

    
    fun setMode(mode: ThemeMode)
}
