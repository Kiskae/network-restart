package net.serverpeon.networkrestart.android

import android.net.IpPrefix
import androidx.compose.*
import androidx.ui.core.Text
import androidx.ui.core.TextField
import androidx.ui.core.dp
import androidx.ui.input.KeyboardType
import androidx.ui.layout.Column
import androidx.ui.layout.Spacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import java.net.InetAddress

@Composable
fun SpecDisplay(spec: ConnectionSpec?) {
    Column(
            modifier = Spacing(16.dp)
    ) {
        Text("Gateway: ${spec?.gateway}")
        Text("Range: ${spec?.prefix}")
        spec?.prefix?.let {
            INetInput(it, +stateFor(it) {
                spec.gateway ?: it.address
            })
        }
        Text("N-ID: ${spec?.wifiNetworkId}")
        Text("SSID: ${spec?.ssid}")
        Text("BSSID: ${spec?.bssid}")
    }
}

@Composable
fun INetInput(prefix: IpPrefix, output: State<InetAddress>) {
    val raw = +memo(prefix) { prefix.rawAddress }
    if (raw.size <= 4) {
        TextField(
                value = raw[0].toString(),
                keyboardType = KeyboardType.Number
        )
    } else {

    }
}

fun <R> CoroutineScope.observeFlow(flow: Flow<R>): Effect<R?> = effectOf {
    var state by +state<R?> { null }

    +onCommit(flow) {
        val job = listenTo(flow) {
            state = it
        }

        onDispose {
            job.cancel()
        }
    }

    state
}