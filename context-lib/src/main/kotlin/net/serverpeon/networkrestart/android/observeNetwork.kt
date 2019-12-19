package net.serverpeon.networkrestart.android

import android.Manifest
import android.content.Context
import android.net.*
import android.net.wifi.WifiManager
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.net.Inet4Address
import java.net.InetAddress
import kotlin.experimental.ExperimentalTypeInference

@UseExperimental(ExperimentalCoroutinesApi::class)
@RequiresPermission(allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_WIFI_STATE])
@RequiresApi(21)
fun CoroutineScope.observeNetwork(context: Context): Flow<ConnectionSpec?> {
    val cs = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return cs.observeGateway(
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    )
}

@UseExperimental(ExperimentalCoroutinesApi::class)
fun <R> CoroutineScope.listenTo(source: Flow<R>, handler: (R) -> Unit): Job {
    return source.onStart {
        Timber.d("Starting flow")
    }.onCompletion {
        Timber.d("Disposing flow")
    }.onEach { handler(it) }.launchIn(this)
}

data class ConnectionSpec(
        val gateway: InetAddress?,
        val prefix: IpPrefix?,
        val wifiNetworkId: Int?,
        val ssid: String?,
        val bssid: String?
)

@UseExperimental(ExperimentalCoroutinesApi::class)
@RequiresPermission(allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_WIFI_STATE])
private fun ConnectivityManager.observeGateway(wifi: WifiManager?): Flow<ConnectionSpec?> = listenForRequest(NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .build()) {
    object : ConnectivityManager.NetworkCallback() {
        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            val gw = linkProperties.findGateway()
            val connInfo = wifi.takeIf {
                getNetworkCapabilities(network)
                        ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }?.connectionInfo

            //TODO: decouple from wifi, validate that associated ipAddress is within prefix
            //connInfo?.ipAddress
            offer(ConnectionSpec(
                    gw,
                    gw?.let(linkProperties::findPrefix),
                    connInfo?.networkId,
                    connInfo?.ssid,
                    connInfo?.bssid
            ))
        }

        override fun onLost(network: Network) {
            offer(null)
        }
    }
}

fun LinkProperties.findPrefix(address: InetAddress): IpPrefix? = routes.find {
    it.matches(address)
}?.destination

private fun LinkProperties.findGateway(): InetAddress? = routes.asSequence()
        .filter { it.isDefaultRoute }
        .map { it.gateway }
        .sortedByDescending {
            // Prefer IPv4
            it is Inet4Address
        }
        .firstOrNull()

@UseExperimental(ExperimentalTypeInference::class)
@ExperimentalCoroutinesApi
@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
@RequiresApi(21)
private fun <R> ConnectivityManager.listenForRequest(
        request: NetworkRequest,
        @BuilderInference listenerFactory: ProducerScope<R>.() -> ConnectivityManager.NetworkCallback
): Flow<R> = callbackFlow {
    val listener = listenerFactory()

    registerNetworkCallback(request, listener)
    awaitClose {
        unregisterNetworkCallback(listener)
    }
}