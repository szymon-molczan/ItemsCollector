package org.wut.items.collector.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wut.items.collector.db.AppDatabase
import org.wut.items.collector.model.AuthResponse




class SessionStore(private val db: AppDatabase) {

    data class Session(
        val token: String,
        val userId: String,
        val email: String,
        val displayName: String,
        val serverUrl: String
    )

    private val _state = MutableStateFlow(loadFromDb())
    val state: StateFlow<Session?> = _state.asStateFlow()

    
    private var pendingServerUrl: String = "http://10.0.2.2:8080"

    private fun loadFromDb(): Session? {
        val row = db.sessionQueries.current().executeAsOneOrNull() ?: return null
        return Session(row.token, row.userId, row.email, row.displayName, row.serverUrl)
    }

    fun save(auth: AuthResponse, serverUrl: String) {
        db.sessionQueries.save(auth.token, auth.userId, auth.email, auth.displayName, serverUrl)
        _state.value = Session(auth.token, auth.userId, auth.email, auth.displayName, serverUrl)
    }

    fun saveOffline() {
        val offlineSession = Session(
            token = "offline",
            userId = "offline_user",
            email = "offline@example.com",
            displayName = "Użytkownik Offline",
            serverUrl = pendingServerUrl
        )
        db.sessionQueries.save(
            offlineSession.token,
            offlineSession.userId,
            offlineSession.email,
            offlineSession.displayName,
            offlineSession.serverUrl
        )
        _state.value = offlineSession
    }

    fun logout() {
        db.sessionQueries.clear()
        _state.value = null
    }

    fun token(): String? = _state.value?.token
    fun serverUrl(): String = _state.value?.serverUrl ?: pendingServerUrl
    fun setPendingServerUrl(url: String) { pendingServerUrl = url }
    fun isOffline(): Boolean = _state.value?.token == "offline"
}
