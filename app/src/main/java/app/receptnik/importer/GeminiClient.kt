package app.receptnik.importer

import android.util.Base64
import app.receptnik.core.IngredientParser
import app.receptnik.core.Nutrition
import app.receptnik.core.RecipeDraft
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder

/**
 * Optional smart parsing via Google Gemini (Google offers a free tier;
 * keys come from aistudio.google.com). Without a key the app uses the built-in parser.
 */
/** [targetLanguage] is the language to translate into (e.g. "Russian"), or null to keep the original. */
class GeminiClient(private val apiKey: String, private val model: String, private val targetLanguage: String?) {

    @Serializable
    private data class GNutrition(val kcal: Double = 0.0, val protein: Double = 0.0, val fat: Double = 0.0, val carbs: Double = 0.0)

    @Serializable
    private data class GRecipe(
        val isRecipe: Boolean = true,
        val title: String = "",
        val description: String = "",
        val servings: Int? = null,
        val totalTimeMin: Int? = null,
        val ingredients: List<String> = emptyList(),
        val steps: List<String> = emptyList(),
        val nutritionPerServing: GNutrition? = null,
        val cuisine: String? = null,
        val mealType: String? = null,
        val tags: List<String> = emptyList(),
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private fun prompt(): String = buildString {
        append("You are the assistant of a cooking app. Extract the recipe from the provided material ")
        append("(a social media caption, a video description, page text or a photo of a recipe). ")
        if (targetLanguage != null) append("Write everything in $targetLanguage (translate if the recipe is in another language). ")
        else append("Keep the original language. ")
        append("Return ONLY JSON of this shape: ")
        append("{\"isRecipe\":true,\"title\":\"\",\"description\":\"1-2 sentences\",\"servings\":2,\"totalTimeMin\":30,")
        append("\"ingredients\":[\"200 g flour\",\"2 eggs\",\"salt to taste\"],")
        append("\"steps\":[\"Step 1...\",\"Step 2...\"],")
        append("\"nutritionPerServing\":{\"kcal\":0,\"protein\":0,\"fat\":0,\"carbs\":0},")
        append("\"cuisine\":\"\",\"mealType\":\"breakfast|lunch|dinner|snack|dessert|drink (in the output language)\",\"tags\":[]}. ")
        append("Each ingredient is one string: quantity, unit, product — metric units where possible. ")
        append("If quantities are missing, estimate reasonable ones. Steps must be short and clear. ")
        append("Estimate calories and macros per serving from the ingredients. ")
        append("If this is not a recipe, return {\"isRecipe\":false}.")
    }

    /** Parses recipe text and/or a photo. Returns null when the material contains no recipe. */
    suspend fun extract(text: String?, imageBytes: ByteArray? = null, imageMime: String = "image/jpeg"): RecipeDraft? {
        val parts = buildJsonArray {
            add(buildJsonObject { put("text", prompt()) })
            if (!text.isNullOrBlank()) add(buildJsonObject { put("text", text.take(15000)) })
            if (imageBytes != null) add(buildJsonObject {
                putJsonObject("inline_data") {
                    put("mime_type", imageMime)
                    put("data", Base64.encodeToString(imageBytes, Base64.NO_WRAP))
                }
            })
        }
        return generate(parts)
    }

    /**
     * Watches a recipe video and extracts the recipe from speech, on-screen text and visuals.
     * Pass either a public [youtubeUrl] (read by Gemini directly) or a downloaded [videoFile],
     * which is uploaded through the Gemini Files API and deleted afterwards.
     */
    suspend fun extractFromVideo(videoFile: File?, youtubeUrl: String?, caption: String?): RecipeDraft? {
        require(videoFile != null || youtubeUrl != null)
        var uploadedName: String? = null
        try {
            val fileUri: String
            val mime: String?
            if (youtubeUrl != null) {
                fileUri = youtubeUrl; mime = null
            } else {
                val uploaded = upload(videoFile!!, "video/mp4")
                uploadedName = uploaded.first
                fileUri = uploaded.second; mime = "video/mp4"
            }
            val parts = buildJsonArray {
                add(buildJsonObject {
                    putJsonObject("file_data") {
                        if (mime != null) put("mime_type", mime)
                        put("file_uri", fileUri)
                    }
                })
                add(buildJsonObject {
                    put("text", prompt() + " The recipe is in the attached video: use what is said, shown on screen and " +
                        "written in captions. Infer quantities from what you see if they are not stated.")
                })
                if (!caption.isNullOrBlank()) add(buildJsonObject { put("text", "Post caption:\n" + caption.take(5000)) })
            }
            return generate(parts)
        } finally {
            uploadedName?.let { name -> runCatching { delete(name) } }
        }
    }

    // ───────────────────────── HTTP ─────────────────────────

    private val base = "https://generativelanguage.googleapis.com"
    private fun keyParam() = "key=" + URLEncoder.encode(apiKey, "UTF-8")
    private val longClient by lazy {
        Http.client.newBuilder().callTimeout(5, TimeUnit.MINUTES).readTimeout(3, TimeUnit.MINUTES).writeTimeout(3, TimeUnit.MINUTES).build()
    }

    private suspend fun generate(parts: JsonArray): RecipeDraft? {
        val body = buildJsonObject {
            putJsonArray("contents") { add(buildJsonObject { put("parts", parts) }) }
            putJsonObject("generationConfig") {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            }
        }
        val url = "$base/v1beta/models/" + URLEncoder.encode(model, "UTF-8") + ":generateContent?" + keyParam()
        val resp = call(Request.Builder().url(url).post(body.toString().toRequestBody(JSON)).build())
        val root = json.parseToJsonElement(resp).jsonObject
        val textOut = root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject?.get("parts")?.jsonArray
            ?.joinToString("") { (it as? JsonObject)?.get("text")?.jsonPrimitive?.content ?: "" }
            ?: return null
        val cleaned = textOut.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val g = runCatching { json.decodeFromString(GRecipe.serializer(), cleaned) }.getOrNull() ?: return null
        if (!g.isRecipe || g.ingredients.isEmpty()) return null
        val servings = g.servings?.takeIf { it in 1..100 } ?: 2
        return RecipeDraft(
            title = g.title,
            description = g.description,
            servings = servings,
            totalTimeMin = g.totalTimeMin?.takeIf { it in 1..3000 },
            ingredients = g.ingredients.map { IngredientParser.parse(it) },
            steps = g.steps.filter { it.isNotBlank() },
            nutrition = g.nutritionPerServing?.takeIf { it.kcal > 0 }?.let {
                Nutrition(it.kcal, it.protein, it.fat, it.carbs) * servings.toDouble()
            },
            nutritionNote = NutritionNote.AI,
            tags = g.tags.take(8),
            cuisine = g.cuisine,
            mealType = g.mealType?.substringBefore('|'),
        )
    }

    /** Resumable upload to the Files API; waits until the file is ACTIVE. Returns (name, uri). */
    private suspend fun upload(file: File, mime: String): Pair<String, String> {
        val start = Request.Builder().url("$base/upload/v1beta/files?" + keyParam())
            .header("X-Goog-Upload-Protocol", "resumable")
            .header("X-Goog-Upload-Command", "start")
            .header("X-Goog-Upload-Header-Content-Length", file.length().toString())
            .header("X-Goog-Upload-Header-Content-Type", mime)
            .post("""{"file":{"display_name":"recipe-video"}}""".toRequestBody(JSON))
            .build()
        val uploadUrl = withRetry {
            longClient.newCall(start).execute().use { r ->
                if (!r.isSuccessful) throw httpError(r.code, r.body?.string().orEmpty())
                r.header("X-Goog-Upload-URL") ?: r.header("x-goog-upload-url") ?: throw AiException(AiException.Reason.OTHER, "No upload URL")
            }
        }
        val put = Request.Builder().url(uploadUrl)
            .header("X-Goog-Upload-Offset", "0")
            .header("X-Goog-Upload-Command", "upload, finalize")
            .post(file.asRequestBody(mime.toMediaType()))
            .build()
        val info = json.parseToJsonElement(call(put)).jsonObject["file"]?.jsonObject
            ?: throw AiException(AiException.Reason.OTHER, "Upload failed")
        val name = info["name"]?.jsonPrimitive?.content ?: throw AiException(AiException.Reason.OTHER, "Upload failed")
        val uri = info["uri"]?.jsonPrimitive?.content ?: throw AiException(AiException.Reason.OTHER, "Upload failed")
        var state = info["state"]?.jsonPrimitive?.content
        var waited = 0
        while (state == "PROCESSING" && waited < 180) {
            delay(3000); waited += 3
            val f = json.parseToJsonElement(call(Request.Builder().url("$base/v1beta/$name?" + keyParam()).get().build())).jsonObject
            state = f["state"]?.jsonPrimitive?.content
        }
        if (state != null && state != "ACTIVE") throw AiException(AiException.Reason.VIDEO_FAILED, "File state $state")
        return name to uri
    }

    private suspend fun delete(name: String) {
        withContext(Dispatchers.IO) {
            Http.client.newCall(Request.Builder().url("$base/v1beta/$name?" + keyParam()).delete().build()).execute().close()
        }
    }

    /** Executes a request with one retry on network failure; maps errors to [AiException]. */
    private suspend fun call(req: Request): String = withRetry {
        longClient.newCall(req).execute().use { r ->
            val text = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw httpError(r.code, text)
            text
        }
    }

    private suspend fun <T> withRetry(block: () -> T): T = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (e: java.net.UnknownHostException) {
            delay(1500)
            try { block() } catch (e2: java.net.UnknownHostException) {
                throw AiException(AiException.Reason.NETWORK, e2.message)
            } catch (e2: java.io.IOException) {
                if (e2 is AiException) throw e2
                throw AiException(AiException.Reason.NETWORK, e2.message)
            }
        } catch (e: AiException) {
            throw e
        } catch (e: java.io.IOException) {
            throw AiException(AiException.Reason.NETWORK, e.message)
        }
    }

    private fun httpError(code: Int, body: String): AiException {
        val msg = runCatching {
            json.parseToJsonElement(body).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
        }.getOrNull() ?: "HTTP $code"
        val reason = when {
            code == 400 && msg.contains("API key", ignoreCase = true) -> AiException.Reason.KEY_INVALID
            code == 401 || code == 403 -> AiException.Reason.KEY_INVALID
            code == 404 -> AiException.Reason.MODEL_NOT_FOUND
            code == 429 -> AiException.Reason.QUOTA
            else -> AiException.Reason.OTHER
        }
        return AiException(reason, msg)
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

/** A typed Gemini failure; the UI maps [reason] to a localized message. */
class AiException(val reason: Reason, detail: String? = null) : java.io.IOException(detail ?: reason.name) {
    enum class Reason { NETWORK, KEY_INVALID, MODEL_NOT_FOUND, QUOTA, VIDEO_FAILED, OTHER }
}
