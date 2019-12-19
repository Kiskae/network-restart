package net.serverpeon.networkrestart.router.db

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import net.serverpeon.networkrestart.router.AbstractRouter
import net.serverpeon.networkrestart.router.NotAuthenticated
import net.serverpeon.networkrestart.router.ignoreAuthentication
import net.serverpeon.networkrestart.router.whileAuthenticated
import okhttp3.*
import org.jsoup.nodes.Document
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.net.InetAddress
import okhttp3.Response as RawResponse

internal class UpcConnectBox private constructor(
        private val contract: Contract,
        private val routerPassword: String
) : AbstractRouter(), NotAuthenticated.Helper {
    fun status(): Flow<String> = flow {
        try {
            whileAuthenticated {
                emit(contract.getter(GetterDefinition.WirelessResetting))

                // Router interface uses 5 second polling
                delay(5000)
            }
        } finally {
            withContext(NonCancellable) {
                logout()
            }
        }
    }

    suspend fun reboot(): Unit = whileAuthenticated {
        return contract.setter(SetterDefinition.Reboot)
    }

    private suspend fun logout() = ignoreAuthentication {
        contract.setter(SetterDefinition.Logout)
    }

    override suspend fun authenticate() = when (val result = contract.setter(SetterDefinition.Login, mapOf(
            "Username" to "NULL",
            "Password" to routerPassword
    ))) {
        "successful" -> Unit
        "idloginincorrect" -> TODO("Login Incorrect, abort!")
        else -> TODO("Unhandled response: $result")
    }

    companion object : AbstractRouter.Contract<UpcConnectBox>() {
        private const val LOGIN_PATH = "/common_page/login.html"
        private const val ACCESS_DENIED_PATH = "/common_page/Access-denied.html"

        override fun match(response: RawResponse, body: Document): Boolean {
            //TODO: does not account for if the user is already logged in...
            return response.headers["Server"] == "NET-DK/1.0" &&
                    response.request.url.encodedPath in listOf(LOGIN_PATH, ACCESS_DENIED_PATH) &&
                    Cookie.parseAll(response.request.url, response.headers).any {
                        it.name == "sessionToken"
                    }
        }

        override fun provide(
                routerAddress: InetAddress,
                client: OkHttpClient,
                parameters: Map<String, String>
        ): UpcConnectBox = UpcConnectBox(provideContract(
                HttpUrl.Builder()
                        .scheme("http")
                        .host(routerAddress.hostAddress)
                        .build(),
                client
        ), checkNotNull(parameters["Password"]))

        private fun provideContract(
                routerUrl: HttpUrl,
                client: OkHttpClient
        ): Contract {
            val tokenHolder = TokenHolder()
            val retrofit = Retrofit.Builder()
                    .baseUrl(routerUrl)
                    .validateEagerly(true)
                    .client(client.newBuilder()
                            .cookieJar(tokenHolder)
                            .followRedirects(false)
                            .build())
                    .build()

            return Contract(retrofit.create(), tokenHolder)
        }
    }

    private interface RawContract {
        @POST("xml/getter.xml")
        @FormUrlEncoded
        suspend fun getter(
                @Field("token") sessionToken: String,
                @Field("fun") function: Int
        ): Response<ResponseBody>

        @POST("xml/setter.xml")
        @FormUrlEncoded
        suspend fun setter(
                @Field("token") sessionToken: String,
                @Field("fun") function: Int,
                @FieldMap parameters: Map<String, String>
        ): Response<ResponseBody>
    }

    private class Contract(
            private val raw: RawContract,
            private val tokenHolder: TokenHolder
    ) {
        suspend fun <R> getter(getter: GetterDefinition<R>): R = raw.getter(
                tokenHolder.token,
                getter.funId
        ).handleAuth().body()!!.use(getter.converter)

        suspend fun <R> setter(
                setter: SetterDefinition<R>,
                parameters: Map<String, String> = emptyMap()
        ): R = raw.setter(
                tokenHolder.token,
                setter.funId,
                parameters.also { it.keys.containsAll(setter.requiredParameters) }
        ).handleAuth().body()!!.use(setter.converter)

        private fun <R> Response<R>.handleAuth(): Response<R> = apply {
            raw().takeIf { it.isRedirect }?.apply {
                when (headers["Location"]) {
                    "..$LOGIN_PATH" -> throw NotAuthenticated
                    "..$ACCESS_DENIED_PATH" -> TODO("Access Denied, maybe because session lockout")
                }
            }
        }
    }

    private sealed class GetterDefinition<R>(
            val funId: Int,
            val converter: ResponseBody.() -> R
    ) {
        abstract class Placeholder(funId: Int) : GetterDefinition<String>(funId, ResponseBody::string)

        // Unauthenticated_Access
        object GlobalSettings : Placeholder(1)

        object SystemInfo : Placeholder(2)

        // Unauthenticated_Access
        object MultiLang : Placeholder(3)

        object LangSetList : Placeholder(21)

        object WanSettings : Placeholder(107)

        object LanUserTable : Placeholder(123)

        object WirelessBasic : Placeholder(315)

        object WirelessWps : Placeholder(323)

        object WifiState : Placeholder(326)

        object WirelessResetting : Placeholder(328)

        object Status : Placeholder(500)
    }

    private sealed class SetterDefinition<R>(
            val funId: Int,
            val requiredParameters: Set<String>,
            val converter: ResponseBody.() -> R
    ) {
        object SetLang : SetterDefinition<Unit>(4, setOf("lang"), {})

        object Reboot : SetterDefinition<Unit>(8, emptySet(), {})

        object Login : SetterDefinition<String>(15, setOf("Username", "Password"), {
            string().split(';').first()
        })

        object Logout : SetterDefinition<Unit>(16, emptySet(), {})
    }

    private class TokenHolder : CookieJar {
        private var sessionCookie: Cookie? = null

        val token: String
            get() = sessionCookie?.value ?: "-1"

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return sessionCookie?.let { listOf(it) } ?: emptyList()
        }

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookies.firstOrNull { it.name == "sessionToken" }?.let {
                sessionCookie = it
            }
        }
    }
}
