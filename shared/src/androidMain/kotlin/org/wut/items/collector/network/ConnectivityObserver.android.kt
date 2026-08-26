package org.wut.items.collector.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AndroidConnectivityObserver(context: Context) : ConnectivityObserver {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _status = MutableStateFlow(getCurrentStatus())
    override val status: StateFlow<ConnectivityStatus> = _status.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _status.value = ConnectivityStatus.Available
        }

        override fun onLost(network: Network) {
            _status.value = getCurrentStatus()
        }
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
    }

    private fun getCurrentStatus(): ConnectivityStatus {
        val activeNetwork = connectivityManager.activeNetwork ?: return ConnectivityStatus.Unavailable
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return ConnectivityStatus.Unavailable
        return if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            ConnectivityStatus.Available
        } else {
            ConnectivityStatus.Unavailable
        }
    }
}
