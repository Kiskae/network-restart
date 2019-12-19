package net.serverpeon.networkrestart.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.key
import androidx.compose.unaryPlus
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.ui.core.disposeActivityComposition
import androidx.ui.core.setContent
import androidx.ui.material.MaterialTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference

class TestActivity : AppCompatActivity(),
        CoroutineScope by MainScope(),
        ActivityCompat.OnRequestPermissionsResultCallback {
    private val rootHolder = AtomicReference(null)
    private val hasPermission = BroadcastChannel<Boolean>(Channel.CONFLATED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = observeNetwork(this)
        val perm = hasPermission.asFlow()

        setContent {
            MaterialTheme {
                val spec = +key<ConnectionSpec?>(+observeFlow(perm) == true) {
                    +observeFlow(data)
                }
                SpecDisplay(spec)
            }
        }

        if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {
            hasPermission.offer(false)
            ActivityCompat.requestPermissions(this, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
            ), 1)
        } else {
            hasPermission.offer(true)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Timber.d("RPR: (%d, %s, %s)", requestCode, permissions.toList(), grantResults.toList())
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hasPermission.offer(true)
            }
        }
    }

    fun printBSSID() {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        Timber.d("SSID: %s", wm.connectionInfo.ssid)
        Timber.d("BSSID: %s", wm.connectionInfo.bssid)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposeActivityComposition(this)
        cancel()
    }
}