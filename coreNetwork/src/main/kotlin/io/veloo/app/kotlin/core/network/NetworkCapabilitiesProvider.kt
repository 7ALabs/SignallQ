package io.veloo.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

interface NetworkCapabilitiesProvider {
    fun isMeteredNetwork(): Boolean
}

class NetworkCapabilitiesProviderImpl(
    private val context: Context,
) : NetworkCapabilitiesProvider {
    override fun isMeteredNetwork(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
}
