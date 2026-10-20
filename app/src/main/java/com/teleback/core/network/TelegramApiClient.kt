package com.teleback.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.util.concurrent.TimeUnit

class TelegramApiClient(
    private val botToken: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .build()
) {
    private val baseUrl = "https://api.telegram.org/bot$botToken"
    private val fileBaseUrl = "https://api.telegram.org/file/bot$botToken"
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    /**
     * Decodes a Telegram envelope and returns its result, or throws a [TelegramApiException]
     * carrying the HTTP/Telegram error code (and `retry_after` for rate limits) so callers
     * can decide whether the failure is worth retrying.
     */
    private inline fun <reified T> parseOrThrow(httpCode: Int, body: String, fallback: String): T {
        val parsed = try {
            json.decodeFromString<TelegramResponse<T>>(body)
        } catch (e: SerializationException) {
            throw TelegramApiException(httpCode, "$fallback (HTTP $httpCode, unreadable response)")
        } catch (e: IllegalArgumentException) {
            throw TelegramApiException(httpCode, "$fallback (HTTP $httpCode, unreadable response)")
        }
        val result = parsed.result
        if (parsed.ok && result != null) return result
        throw TelegramApiException(
            httpCode = parsed.errorCode ?: httpCode,
            message = parsed.description ?: fallback,
            retryAfterSeconds = parsed.parameters?.retryAfter
        )
    }

    suspend fun getMe(): Result<TelegramUser> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/getMe")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                parseOrThrow<TelegramUser>(response.code, body, "Unknown Telegram error")
            }
        }
    }

    suspend fun getUpdates(offset: Long? = null): Result<List<TelegramUpdate>> = withContext(Dispatchers.IO) {
        runCatching {
            val urlBuilder = StringBuilder("$baseUrl/getUpdates?limit=20")
            if (offset != null) {
                urlBuilder.append("&offset=").append(offset)
            }
            val request = Request.Builder()
                .url(urlBuilder.toString())
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                parseOrThrow<List<TelegramUpdate>>(response.code, body, "Error fetching updates")
            }
        }
    }

    suspend fun sendMessage(
        chatId: Long,
        text: String
    ): Result<TelegramMessage> = withContext(Dispatchers.IO) {
        runCatching {
            val formBody = okhttp3.FormBody.Builder()
                .add("chat_id", chatId.toString())
                .add("text", text)
                .add("parse_mode", "HTML")
                .build()

            val request = Request.Builder()
                .url("$baseUrl/sendMessage")
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                parseOrThrow<TelegramMessage>(response.code, body, "Failed to send message")
            }
        }
    }

    suspend fun sendDocument(
        chatId: Long,
        fileName: String,
        fileBytes: ByteArray,
        caption: String? = null,
        onProgress: ((uploaded: Long, total: Long) -> Unit)? = null
    ): Result<TelegramMessage> = withContext(Dispatchers.IO) {
        runCatching {
            val countingBody = object : RequestBody() {
                private val mediaType = "application/octet-stream".toMediaTypeOrNull()
                override fun contentType() = mediaType
                override fun contentLength() = fileBytes.size.toLong()
                override fun writeTo(sink: BufferedSink) {
                    val total = fileBytes.size.toLong()
                    val chunkSize = 64 * 1024
                    var uploaded = 0L
                    var offset = 0
                    while (offset < fileBytes.size) {
                        val count = (fileBytes.size - offset).coerceAtMost(chunkSize)
                        sink.write(fileBytes, offset, count)
                        offset += count
                        uploaded += count
                        onProgress?.invoke(uploaded, total)
                    }
                }
            }

            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId.toString())
                .addFormDataPart("document", fileName, countingBody)

            if (!caption.isNullOrBlank()) {
                multipartBuilder.addFormDataPart("caption", caption)
            }

            val request = Request.Builder()
                .url("$baseUrl/sendDocument")
                .post(multipartBuilder.build())
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                parseOrThrow<TelegramMessage>(response.code, body, "Failed to upload document")
            }
        }
    }

    suspend fun getFile(fileId: String): Result<TelegramFile> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/getFile?file_id=$fileId")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                parseOrThrow<TelegramFile>(response.code, body, "Failed to get file info")
            }
        }
    }

    suspend fun downloadFileBytes(
        filePath: String,
        onProgress: ((downloaded: Long, total: Long) -> Unit)? = null
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$fileBaseUrl/$filePath")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw TelegramApiException(response.code, "Download failed with HTTP ${response.code}")
                }
                val body = response.body ?: throw TelegramApiException(response.code, "Empty response body")
                val totalBytes = body.contentLength()
                val inputStream = body.byteStream()
                val outputStream = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                var downloaded = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    onProgress?.invoke(downloaded, totalBytes)
                }
                outputStream.toByteArray()
            }
        }
    }

    suspend fun pinChatMessage(
        chatId: Long,
        messageId: Long
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val formBody = okhttp3.FormBody.Builder()
                .add("chat_id", chatId.toString())
                .add("message_id", messageId.toString())
                .build()

            val request = Request.Builder()
                .url("$baseUrl/pinChatMessage")
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val parsed = json.decodeFromString<TelegramResponse<Boolean>>(body)
                parsed.ok
            }
        }
    }

    suspend fun unpinChatMessage(
        chatId: Long,
        messageId: Long
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val formBody = okhttp3.FormBody.Builder()
                .add("chat_id", chatId.toString())
                .add("message_id", messageId.toString())
                .build()

            val request = Request.Builder()
                .url("$baseUrl/unpinChatMessage")
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val parsed = json.decodeFromString<TelegramResponse<Boolean>>(body)
                parsed.ok
            }
        }
    }

    suspend fun deleteMessage(
        chatId: Long,
        messageId: Long
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val formBody = okhttp3.FormBody.Builder()
                .add("chat_id", chatId.toString())
                .add("message_id", messageId.toString())
                .build()

            val request = Request.Builder()
                .url("$baseUrl/deleteMessage")
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val parsed = json.decodeFromString<TelegramResponse<Boolean>>(body)
                parsed.ok
            }
        }
    }
}
