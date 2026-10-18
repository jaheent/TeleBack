package com.teleback.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.InputStream
import java.io.OutputStream
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

    suspend fun getMe(): Result<TelegramUser> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/getMe")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val parsed = json.decodeFromString<TelegramResponse<TelegramUser>>(body)
                if (parsed.ok && parsed.result != null) {
                    parsed.result
                } else {
                    throw RuntimeException(parsed.description ?: "Unknown Telegram error (${response.code})")
                }
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
                val parsed = json.decodeFromString<TelegramResponse<List<TelegramUpdate>>>(body)
                if (parsed.ok && parsed.result != null) {
                    parsed.result
                } else {
                    throw RuntimeException(parsed.description ?: "Error fetching updates")
                }
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
                val parsed = json.decodeFromString<TelegramResponse<TelegramMessage>>(body)
                if (parsed.ok && parsed.result != null) {
                    parsed.result
                } else {
                    throw RuntimeException(parsed.description ?: "Failed to send message")
                }
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
                val parsed = json.decodeFromString<TelegramResponse<TelegramMessage>>(body)
                if (parsed.ok && parsed.result != null) {
                    parsed.result
                } else {
                    throw RuntimeException(parsed.description ?: "Failed to upload document (${response.code})")
                }
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
                val parsed = json.decodeFromString<TelegramResponse<TelegramFile>>(body)
                if (parsed.ok && parsed.result != null) {
                    parsed.result
                } else {
                    throw RuntimeException(parsed.description ?: "Failed to get file info")
                }
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
                    throw RuntimeException("Download failed with HTTP ${response.code}")
                }
                val body = response.body ?: throw RuntimeException("Empty response body")
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
}
