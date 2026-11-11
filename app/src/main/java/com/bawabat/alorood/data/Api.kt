package com.bawabat.alorood.data

import com.bawabat.alorood.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** عميل REST بسيط للخادم (لوحة التحكم). يعمل فقط إذا ضُبط API_BASE_URL. */
object Api {
    val enabled: Boolean get() = BuildConfig.API_BASE_URL.isNotBlank()
    private val base: String get() = BuildConfig.API_BASE_URL.trim().trimEnd('/')

    @Volatile var token: String? = null

    class ApiException(val status: Int, val code: String, message: String) : Exception(message)

    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .writeTimeout(40, TimeUnit.SECONDS)
        .build()
    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    private fun build(method: String, path: String, bodyText: String?, contentType: String? = null, bytes: ByteArray? = null): Request {
        val b = Request.Builder().url(base + path)
            .header("Accept", "application/json")
            .header("X-App-Version", BuildConfig.VERSION_CODE.toString())
        token?.let { b.header("Authorization", "Bearer $it") }
        val needsBody = method == "POST" || method == "PUT" || method == "PATCH"
        val body = when {
            bytes != null -> bytes.toRequestBody((contentType ?: "application/octet-stream").toMediaType())
            bodyText != null -> bodyText.toRequestBody(JSON_TYPE)
            needsBody -> "{}".toRequestBody(JSON_TYPE)
            method == "DELETE" -> null
            else -> null
        }
        return b.method(method, body).build()
    }

    private fun parseError(code: Int, text: String): ApiException {
        return try {
            val o = json.parseToJsonElement(text).jsonObject
            val err = o["error"]?.jsonPrimitive?.contentOrNull ?: "HTTP_$code"
            val msg = o["message"]?.jsonPrimitive?.contentOrNull ?: err
            ApiException(code, err, msg)
        } catch (e: Exception) {
            ApiException(code, "HTTP_$code", "HTTP $code")
        }
    }

    private suspend fun exec(req: Request): JsonElement = withContext(Dispatchers.IO) {
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw parseError(resp.code, text)
            if (text.isBlank()) JsonObject(emptyMap()) else json.parseToJsonElement(text)
        }
    }

    suspend fun call(method: String, path: String, body: JsonElement? = null): JsonElement =
        exec(build(method, path, body?.toString()))

    suspend fun upload(folder: String, bytes: ByteArray, contentType: String = "image/jpeg"): String {
        val el = exec(build("POST", "/api/v1/uploads?folder=$folder", null, contentType, bytes))
        return el.jsonObject["url"]?.jsonPrimitive?.contentOrNull ?: throw ApiException(500, "NO_URL", "upload failed")
    }

    /** تنزيل صورة من رابط (لعرض صور الإعلانات). */
    suspend fun download(url: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { r -> if (r.isSuccessful) r.body?.bytes() else null }
        } catch (e: Exception) { null }
    }
}
