package net.serverpeon.networkrestart.router

import okhttp3.OkHttpClient
import okhttp3.Response
import org.jsoup.nodes.Document
import java.net.InetAddress

abstract class AbstractRouter {
    //TODO: figure out stuff
    // Status flow, polling rate depends on router
    //   notifies about availability / special state

    // reboot action

    // Give estimate on reboot time perhaps?
    //   or add a special case if a reboot requires re-establishing connection with ISP

    abstract class Contract<T : AbstractRouter> {
        abstract fun match(response: Response, body: Document): Boolean

        abstract fun provide(
                routerAddress: InetAddress,
                client: OkHttpClient,
                parameters: Map<String, String>
        ): T
    }
}