package com.example.rentease

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast

/**
 * Utility class for network connectivity checks.
 */
object NetworkUtils {

    /**
     * Check if the device has an active internet connection.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Show a toast if no internet connection is available.
     * Returns true if connected, false otherwise.
     */
    fun checkAndNotify(context: Context): Boolean {
        if (!isNetworkAvailable(context)) {
            Toast.makeText(context, "Tidak ada koneksi internet. Periksa koneksi Anda.", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }
}
