package app.receptnik.importer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** A user-facing reason for a failed import; messages live in UI resources. */
class ImportException(val reason: Reason, detail: String? = null) : Exception(detail ?: reason.name) {
    enum class Reason { NO_LINK, RATE_LIMITED, NOT_FOUND, BLOCKED, SITE_ERROR, BAD_RESPONSE }
}

object Http {
    const val UA_MOBILE =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
    const val UA_DESKTOP =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
    /** Social networks serve link previews (og: tags) to their own crawlers. */
    const val UA_BOT = "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)"

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private const val MAX_BYTES = 4L * 1024 * 1024

    data class Page(val finalUrl: String, val body: String, val code: Int) {
        val ok: Boolean get() = code in 200..299
    }

    private val noRedirect: OkHttpClient by lazy { client.newBuilder().followRedirects(false).followSslRedirects(false).build() }

    /** Expands a short link (vt.tiktok.com, pin.it…) by following Location headers without downloading the page. */
    suspend fun resolve(url: String, ua: String = UA_DESKTOP, maxHops: Int = 6): String = withContext(Dispatchers.IO) {
        var current = url
        repeat(maxHops) {
            val req = Request.Builder().url(current).header("User-Agent", ua).build()
            val next = noRedirect.newCall(req).execute().use { resp ->
                if (resp.isRedirect) resp.header("Location")?.let { loc -> resp.request.url.resolve(loc)?.toString() } else null
            } ?: return@withContext current
            current = next
        }
        current
    }

    /** GET with retries on 429 / 5xx. */
    suspend fun getWithRetry(url: String, ua: String = UA_MOBILE, attempts: Int = 3): Page {
        var last: Page? = null
        for (i in 0 until attempts) {
            val page = get(url, ua)
            if (page.ok) return page
            last = page
            if (page.code != 429 && page.code < 500) break
            kotlinx.coroutines.delay(1500L * (i + 1))
        }
        val p = last!!
        throw ImportException(
            when (p.code) {
                429 -> ImportException.Reason.RATE_LIMITED
                404, 410 -> ImportException.Reason.NOT_FOUND
                401, 403 -> ImportException.Reason.BLOCKED
                else -> ImportException.Reason.SITE_ERROR
            },
            "HTTP ${p.code}",
        )
    }

    suspend fun get(url: String, ua: String = UA_MOBILE): Page = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url)
            .header("User-Agent", ua)
            .header("Accept", "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "ru,en;q=0.8,he;q=0.6")
            .build()
        client.newCall(req).execute().use { resp ->
            val src = resp.body ?: throw IOException("Empty response")
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
            resp.body?.bytes() ?: throw IOException("Empty response")
        }
    }

    suspend fun postJson(url: String, json: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url)
            .post(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        client.newCall(req).execute().use { resp -> resp.code to (resp.body?.string() ?: "") }
    }
}
