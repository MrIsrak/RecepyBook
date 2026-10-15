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
 * Хранит картинки рецептов в памяти приложения: ссылки на картинки из Instagram/TikTok
 * со временем перестают работать, поэтому копируем их сразу.
 */
class ImageStore(private val context: Context) {
    private val dir: File get() = File(context.filesDir, "images").apply { mkdirs() }

    fun isLocal(path: String?) = path != null && path.startsWith("/")

    /** Возвращает путь к локальной копии или исходный адрес, если скачать не удалось. */
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

    /** Модель для Coil: файл или URL. */
    fun model(path: String?): Any? = if (isLocal(path)) File(path!!) else path
}
