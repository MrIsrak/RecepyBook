package app.receptnik.importer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import app.receptnik.core.CaptionParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Рецепт с фото (скриншот, страница кулинарной книги).
 * С ключом Gemini — понимает любой язык, включая русский рукописный текст.
 * Без ключа — встроенное распознавание текста ML Kit (латиница).
 */
class PhotoImporter(private val context: Context, private val gemini: () -> GeminiClient?, private val links: LinkImporter) {

    suspend fun import(uri: Uri): ImportResult {
        val jpeg = withContext(Dispatchers.IO) { loadScaledJpeg(uri) }
        val ai = gemini()
        val warnings = mutableListOf<ImportWarning>()
        var aiError: String? = null
        if (ai != null) {
            val d = runCatching { ai.extract(null, jpeg, "image/jpeg") }
                .onFailure { aiError = it.message ?: "Gemini error" }
                .getOrNull()
            if (d != null) return ImportResult(links.withNutrition(d.copy(imageUrl = uri.toString())))
        }
        val text = recognize(uri)
        val draft = CaptionParser.parse(text)
        if (draft.ingredients.isEmpty()) {
            warnings += if (ai == null) ImportWarning.PHOTO_UNREADABLE else ImportWarning.PHOTO_NO_RECIPE
        }
        return ImportResult(links.withNutrition(draft.copy(imageUrl = uri.toString())), warnings, aiError)
    }

    private suspend fun recognize(uri: Uri): String = suspendCancellableCoroutine { cont ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromFilePath(context, uri)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                recognizer.close()
                cont.resume(result.textBlocks.joinToString("\n") { it.text })
            }
            .addOnFailureListener { e ->
                recognizer.close()
                cont.resumeWithException(e)
            }
    }

    private fun loadScaledJpeg(uri: Uri): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2000) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp: Bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw IllegalArgumentException("Cannot open image")
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
        bmp.recycle()
        return out.toByteArray()
    }
}
