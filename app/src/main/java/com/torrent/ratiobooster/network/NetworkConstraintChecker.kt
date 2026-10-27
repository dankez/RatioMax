package com.torrent.ratiobooster.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

interface NetworkConstraintChecker {
    fun isWifiConnected(): Boolean
    fun canTransferData(wifiOnlyRequired: Boolean): Boolean
}

class AndroidNetworkConstraintChecker(
    private val context: Context
) : NetworkConstraintChecker {

    override fun isWifiConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    override fun canTransferData(wifiOnlyRequired: Boolean): Boolean {
        if (!wifiOnlyRequired) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        return isWifiConnected()
    }
}
