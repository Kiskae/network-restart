package net.serverpeon.networkrestart.router.db

import net.serverpeon.networkrestart.router.AbstractRouter
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.GET
import java.net.InetAddress

enum class KnownRouter(private val contract: AbstractRouter.Contract<*>) {
    UPC_CONNECTBOX(UpcConnectBox);

    companion object {
        private val cached_known = enumValues<KnownRouter>().asSequence()

        internal suspend fun detectRouterInternal(
                addr: InetAddress,
                client: OkHttpClient
        ): Sequence<KnownRouter> {
            val helper = Retrofit.Builder()
                    .baseUrl(HttpUrl.Builder()
                            .scheme("http")
                            .host(addr.hostAddress)
                            .build())
                    .client(client)
                    .build()
                    .create<CallHelper>()

            val result = helper.call()
            val document = result.body()?.use {
                Jsoup.parse(
                        it.byteStream(),
                        it.contentType()?.charset()?.name(),
                        result.raw().request.url.toString()
                )
            } ?: return emptySequence()

            return cached_known.filter { it.contract.match(result.raw(), document) }
        }

        suspend fun detectRouter(addr: InetAddress, client: OkHttpClient): KnownRouter? =
                detectRouterInternal(addr, client).singleOrNull()
    }

    private interface CallHelper {
        @GET("")
        suspend fun call(): Response<ResponseBody>
    }
}