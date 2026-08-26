package org.wut.items.collector.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wut.items.collector.AppContainer
import org.wut.items.collector.data.SessionStore
import org.wut.items.collector.model.LoginRequest
import org.wut.items.collector.model.RegisterRequest
import org.wut.items.collector.model.ChangePasswordRequest
import org.wut.items.collector.network.ApiException

class AuthViewModel(private val app: AppContainer) : ViewModel() {

    data class UiState(
        val email: String = "",
        val password: String = "",
        val displayName: String = "",
        val serverUrl: String = "http://10.0.2.2:8080",
        val mode: Mode = Mode.LOGIN,
        val isBusy: Boolean = false,
        val error: String? = null
    )
    enum class Mode { LOGIN, REGISTER }

    val session: StateFlow<SessionStore.Session?> = app.sessionStore.state

    private val _ui = MutableStateFlow(UiState(serverUrl = app.sessionStore.serverUrl()))
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    fun setEmail(v: String) { _ui.value = _ui.value.copy(email = v, error = null) }
    fun setPassword(v: String) { _ui.value = _ui.value.copy(password = v, error = null) }
    fun setDisplayName(v: String) { _ui.value = _ui.value.copy(displayName = v) }
    fun setServerUrl(v: String) { _ui.value = _ui.value.copy(serverUrl = v) }
    fun setMode(m: Mode) { _ui.value = _ui.value.copy(mode = m, error = null) }

    fun submit() {
        val s = _ui.value
        if (s.isBusy) return
        _ui.value = s.copy(isBusy = true, error = null)
        viewModelScope.launch {
            try {
                
                app.sessionStore.setPendingServerUrl(s.serverUrl)
                val auth = if (s.mode == Mode.LOGIN) {
                    app.apiClient.login(LoginRequest(s.email.trim(), s.password))
                } else {
                    app.apiClient.register(RegisterRequest(s.email.trim(), s.password, s.displayName))
                }
                app.sessionStore.save(auth, s.serverUrl)
                _ui.value = _ui.value.copy(isBusy = false)
            } catch (e: ApiException) {
                _ui.value = _ui.value.copy(isBusy = false, error = e.message)
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(isBusy = false, error = "Brak połączenia z serwerem: ${t.message}")
            }
        }
    }

    fun logout() = app.sessionStore.logout()

    fun continueOffline() {
        app.sessionStore.saveOffline()
    }

    

    data class ChangePasswordState(
        val isBusy: Boolean = false,
        val success: Boolean = false,
        val error: String? = null
    )

    private val _changePasswordState = MutableStateFlow(ChangePasswordState())
    val changePasswordState: StateFlow<ChangePasswordState> = _changePasswordState.asStateFlow()

    fun changePassword(currentPassword: String, newPassword: String) {
        if (_changePasswordState.value.isBusy) return
        if (newPassword.length < 4) {
            _changePasswordState.value = ChangePasswordState(error = "Nowe hasło musi mieć co najmniej 4 znaki")
            return
        }
        _changePasswordState.value = ChangePasswordState(isBusy = true)
        viewModelScope.launch {
            try {
                app.apiClient.changePassword(ChangePasswordRequest(currentPassword, newPassword))
                _changePasswordState.value = ChangePasswordState(success = true)
            } catch (e: ApiException) {
                _changePasswordState.value = ChangePasswordState(error = e.message)
            } catch (t: Throwable) {
                _changePasswordState.value = ChangePasswordState(error = "Brak połączenia: ${t.message}")
            }
        }
    }

    fun resetChangePasswordState() {
        _changePasswordState.value = ChangePasswordState()
    }
}
