package org.wut.items.collector.network

import kotlinx.coroutines.flow.StateFlow




enum class ConnectivityStatus {
    Available, Unavailable
}




interface ConnectivityObserver {
    val status: StateFlow<ConnectivityStatus>
}
