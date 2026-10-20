package com.teleback.core.network

import kotlinx.coroutines.delay
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Error returned by the Telegram Bot API (or by the HTTP layer in front of it).
 * [retryAfterSeconds] is populated from Telegram's `parameters.retry_after` on HTTP 429.
 */
class TelegramApiException(
    val httpCode: Int,
    message: String,
    val retryAfterSeconds: Int? = null
) : RuntimeException(message)

/**
 * Retry with exponential backoff for flaky networks and Telegram rate limits.
 *
 * - Network failures ([IOException]) and HTTP 5xx are retried.
 * - HTTP 429 is retried after exactly the delay Telegram asks for (`retry_after`).
 * - Everything else (400/401/403/404, decrypt errors, ...) fails immediately.
 * - Coroutine cancellation is never retried.
 */
object RetryPolicy {
    const val DEFAULT_MAX_ATTEMPTS = 5
    private const val BASE_DELAY_MS = 2_000L
    private const val MAX_BACKOFF_MS = 16_000L
    private const val MAX_RETRY_AFTER_MS = 5 * 60_000L

    fun isRetryable(error: Throwable): Boolean = when (error) {
        is CancellationException -> false
        is TelegramApiException -> error.httpCode == 429 || error.httpCode >= 500
        is IOException -> true
        else -> false
    }

    /** [attempt] is the 1-based number of the attempt that just failed. */
    fun delayMillisFor(attempt: Int, error: Throwable): Long {
        if (error is TelegramApiException && error.retryAfterSeconds != null) {
            return (error.retryAfterSeconds * 1_000L).coerceIn(1_000L, MAX_RETRY_AFTER_MS)
        }
        val shift = (attempt - 1).coerceIn(0, 10)
        return (BASE_DELAY_MS shl shift).coerceAtMost(MAX_BACKOFF_MS)
    }

    suspend fun <T> execute(
        maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
        sleep: suspend (Long) -> Unit = { delay(it) },
        onRetry: suspend (attempt: Int, maxAttempts: Int, delayMs: Long, error: Throwable) -> Unit =
            { _, _, _, _ -> },
        block: suspend () -> T
    ): T {
        var attempt = 1
        while (true) {
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (!isRetryable(e) || attempt >= maxAttempts) throw e
                val waitMs = delayMillisFor(attempt, e)
                onRetry(attempt, maxAttempts, waitMs, e)
                sleep(waitMs)
                attempt++
            }
        }
    }
}
