package org.wut.items.collector.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class JvmConnectivityObserver : ConnectivityObserver {
    private val _status = MutableStateFlow(ConnectivityStatus.Available)
    override val status: StateFlow<ConnectivityStatus> = _status.asStateFlow()
}
