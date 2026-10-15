package app.receptnik.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import app.receptnik.importer.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Stores recipe images in app storage: Instagram/TikTok CDN links
 * expire over time, so images are copied on save.
 */
class ImageStore(private val context: Context) {
    private val dir: File get() = File(context.filesDir, "images").apply { mkdirs() }

    fun isLocal(path: String?) = path != null && path.startsWith("/")

    /** Returns the local copy path, or the original address if the download failed. */
    suspend fun persist(source: String?): String? = withContext(Dispatchers.IO) {
        if (source.isNullOrBlank() || isLocal(source)) return@withContext source
        runCatching {
            val bytes = when {
                source.startsWith("content://") || source.startsWith("file://") ->
                    context.contentResolver.openInputStream(Uri.parse(source))?.use { it.readBytes() }
                source.startsWith("http") -> Http.getBytes(source, Http.UA_DESKTOP)
                else -> null
            } ?: return@runCatching source
            val f = File(dir, UUID.randomUUID().toString() + ".jpg")
            f.writeBytes(bytes)
            f.absolutePath
        }.getOrDefault(source)
    }

    fun exists(path: String?) = isLocal(path) && File(path!!).exists()

    fun readBase64(path: String?): String? =
        if (exists(path)) runCatching { Base64.encodeToString(File(path!!).readBytes(), Base64.NO_WRAP) }.getOrNull() else null

    fun writeBase64(data: String): String? = runCatching {
        val f = File(dir, UUID.randomUUID().toString() + ".jpg")
        f.writeBytes(Base64.decode(data, Base64.DEFAULT))
        f.absolutePath
    }.getOrNull()

    fun delete(path: String?) {
        if (isLocal(path)) runCatching { File(path!!).delete() }
    }

    /** Coil model: a File or a URL. */
    fun model(path: String?): Any? = if (isLocal(path)) File(path!!) else path
}
