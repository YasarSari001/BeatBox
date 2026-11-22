package com.blackstack.beatbox.playback
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

fun Throwable.causeChain(): List<Throwable> = generateSequence(this) { it.cause }.toList()

fun Context.hasUsableNetwork(): Boolean {
    val connectivityManager = getSystemService(ConnectivityManager::class.java) ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
