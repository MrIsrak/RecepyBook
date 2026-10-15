package app.receptnik.importer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

object Http {
    const val UA_MOBILE =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
    const val UA_DESKTOP =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
    /** Соцсети отдают превью (og:теги) роботам соцсетей. */
    const val UA_BOT = "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)"

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private const val MAX_BYTES = 4L * 1024 * 1024

    data class Page(val finalUrl: String, val body: String, val code: Int)

    suspend fun get(url: String, ua: String = UA_MOBILE): Page = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url)
            .header("User-Agent", ua)
            .header("Accept", "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "ru,en;q=0.8,he;q=0.6")
            .build()
        client.newCall(req).execute().use { resp ->
            val src = resp.body ?: throw IOException("Пустой ответ")
            val bytes = src.source().let { s ->
                s.request(MAX_BYTES)
                val n = minOf(s.buffer.size, MAX_BYTES)
                s.buffer.readByteArray(n)
            }
            val charset = src.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
            Page(resp.request.url.toString(), String(bytes, charset), resp.code)
        }
    }

    suspend fun getBytes(url: String, ua: String = UA_MOBILE): ByteArray = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).header("User-Agent", ua).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            resp.body?.bytes() ?: throw IOException("Пустой ответ")
        }
    }

    suspend fun postJson(url: String, json: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url)
            .post(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        client.newCall(req).execute().use { resp -> resp.code to (resp.body?.string() ?: "") }
    }
}
