package com.vlcsync.remote.data

import com.vlcsync.remote.security.SecureCookieStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.IOException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

interface VlcProtocolAdapter {
    fun buildStatusRequest(): Request
    fun buildPlayRequest(): Request
    fun buildPauseRequest(): Request
    fun buildSeekRequest(positionMs: Long): Request
    fun parseStatus(responseBody: String): PlaybackStatus
}

class StubProtocolAdapter(private val baseUrl: String) : VlcProtocolAdapter {
    override fun buildStatusRequest() =
        Request.Builder().url("$baseUrl/api/status").get().build()
    
    override fun buildPlayRequest() =
        Request.Builder().url("$baseUrl/api/play")
            .post(RequestBody.create(null, ByteArray(0))).build()
    
    override fun buildPauseRequest() =
        Request.Builder().url("$baseUrl/api/pause")
            .post(RequestBody.create(null, ByteArray(0))).build()
    
    override fun buildSeekRequest(positionMs: Long) =
        Request.Builder().url("$baseUrl/api/seek?pos=$positionMs")
            .post(RequestBody.create(null, ByteArray(0))).build()
    
    override fun parseStatus(responseBody: String): PlaybackStatus {
        // TODO: заменить после разведки API (Этап 0)
        return PlaybackStatus(PlaybackState.UNKNOWN, 0, 0, null)
    }
}

class VlcRemoteClient(
    private val device: VlcDevice,
    private val cookieStore: SecureCookieStore,
    private val adapter: VlcProtocolAdapter
) {
    private val client: OkHttpClient

    init {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        client = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { hostname, _ -> hostname == device.host }
            .cookieJar(VlcCookieJar(cookieStore, device.id))
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    suspend fun getStatus(): PlaybackStatus = withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(adapter.buildStatusRequest()).execute()
            if (response.isSuccessful) {
                adapter.parseStatus(response.body?.string() ?: "")
            } else {
                PlaybackStatus(PlaybackState.UNKNOWN, 0, 0, null)
            }
        } catch (e: IOException) {
            PlaybackStatus(PlaybackState.UNKNOWN, 0, 0, null)
        }
    }

    suspend fun play() = withContext(Dispatchers.IO) {
        try { client.newCall(adapter.buildPlayRequest()).execute() } catch (e: IOException) {}
    }

    suspend fun pause() = withContext(Dispatchers.IO) {
        try { client.newCall(adapter.buildPauseRequest()).execute() } catch (e: IOException) {}
    }

    suspend fun seek(positionMs: Long) = withContext(Dispatchers.IO) {
        try { client.newCall(adapter.buildSeekRequest(positionMs)).execute() } catch (e: IOException) {}
    }
}

class VlcCookieJar(
    private val store: SecureCookieStore,
    private val deviceId: String
) : CookieJar {
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val rawCookies = store.getCookies(deviceId) ?: return emptyList()
        return rawCookies.split(";").mapNotNull {
            val parts = it.trim().split("=", limit = 2)
            if (parts.size == 2) {
                Cookie.Builder()
                    .name(parts[0])
                    .value(parts[1])
                    .domain(url.host)
                    .build()
            } else null
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val raw = cookies.joinToString("; ") { "${it.name}=${it.value}" }
        store.saveCookies(deviceId, raw)
    }
}