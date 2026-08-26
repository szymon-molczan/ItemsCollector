package org.wut.items.collector.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow






actual class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val state: MutableStateFlow<ThemeMode> = MutableStateFlow(loadFromPrefs())

    actual val mode: Flow<ThemeMode> = state.asStateFlow()

    actual fun setMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
        state.value = mode
    }

    private fun loadFromPrefs(): ThemeMode {
        val raw = prefs.getString(KEY_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
    }

    private companion object {
        const val PREF_NAME = "theme_prefs"
        const val KEY_MODE = "mode"
    }
}
