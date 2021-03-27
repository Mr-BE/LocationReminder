package com.udacity.project4.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresApi
import timber.log.Timber
import kotlin.properties.Delegates

/**
* Class to validate the network connection
*/ @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class NetworkMonitor(private val context: Context) {


    fun startNetworkCallback() {
        val cm: ConnectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val builder: NetworkRequest.Builder = NetworkRequest.Builder()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cm.registerDefaultNetworkCallback(networkCallback)
        } else {
            cm.registerNetworkCallback(
                builder.build(), networkCallback
            )
        }
    }


    fun stopNetworkCallback() {
        val cm: ConnectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.unregisterNetworkCallback(ConnectivityManager.NetworkCallback())
    }

    /**
     * The callback will listen to and notify every time there’s a change with the network
     */
    private val networkCallback = object:
        ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            Variables.isNetworkConnected = true
        }

        override fun onLost(network: Network) {
            Variables.isNetworkConnected = false
        }
    }


}

/**
 * Global variable where the isNetworkConnected property is created once and used anywhere.
 * Created by using an Observable delegation, in order to get notified using callbacks
 * whenever the property changes.
 */
object Variables {
    var isNetworkConnected: Boolean by Delegates.observable(false) {
            property, oldValue, newValue ->
        Timber.i("Network connectivity $newValue")
    }
}