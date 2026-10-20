package com.teleback.core.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class RetryPolicyTest {

    private fun <T> exec(
        maxAttempts: Int = 5,
        sleeps: MutableList<Long> = mutableListOf(),
        retries: MutableList<Int> = mutableListOf(),
        block: suspend () -> T
    ): T = runBlocking {
        RetryPolicy.execute(
            maxAttempts = maxAttempts,
            sleep = { sleeps.add(it) },
            onRetry = { attempt, _, _, _ -> retries.add(attempt) },
            block = block
        )
    }

    @Test
    fun retriesNetworkErrorsThenSucceeds() {
        var calls = 0
        val sleeps = mutableListOf<Long>()
        val result = exec(sleeps = sleeps) {
            calls++
            if (calls < 3) throw SocketTimeoutException("timeout")
            "ok"
        }
        assertEquals("ok", result)
        assertEquals(3, calls)
        assertEquals(listOf(2_000L, 4_000L), sleeps)
    }

    @Test
    fun backoffDoublesAndIsCapped() {
        val err = IOException("x")
        assertEquals(2_000L, RetryPolicy.delayMillisFor(1, err))
        assertEquals(4_000L, RetryPolicy.delayMillisFor(2, err))
        assertEquals(8_000L, RetryPolicy.delayMillisFor(3, err))
        assertEquals(16_000L, RetryPolicy.delayMillisFor(4, err))
        assertEquals(16_000L, RetryPolicy.delayMillisFor(9, err))
    }

    @Test
    fun givesUpAfterMaxAttemptsAndRethrowsLastError() {
        var calls = 0
        try {
            exec(maxAttempts = 5) {
                calls++
                throw IOException("down")
            }
            fail("expected exception")
        } catch (e: IOException) {
            assertEquals("down", e.message)
        }
        assertEquals(5, calls)
    }

    @Test
    fun rateLimitHonoursRetryAfterExactly() {
        var calls = 0
        val sleeps = mutableListOf<Long>()
        exec(sleeps = sleeps) {
            calls++
            if (calls == 1) throw TelegramApiException(429, "Too Many Requests", retryAfterSeconds = 7)
            "ok"
        }
        assertEquals(listOf(7_000L), sleeps)
    }

    @Test
    fun serverErrorsAreRetriedButClientErrorsAreNot() {
        assertTrue(RetryPolicy.isRetryable(TelegramApiException(502, "Bad Gateway")))
        assertTrue(RetryPolicy.isRetryable(TelegramApiException(429, "Too Many Requests")))
        assertFalse(RetryPolicy.isRetryable(TelegramApiException(400, "Bad Request")))
        assertFalse(RetryPolicy.isRetryable(TelegramApiException(401, "Unauthorized")))
        assertFalse(RetryPolicy.isRetryable(TelegramApiException(403, "Forbidden")))
        assertFalse(RetryPolicy.isRetryable(IllegalStateException("bug")))
    }

    @Test
    fun nonRetryableErrorFailsImmediately() {
        var calls = 0
        try {
            exec {
                calls++
                throw TelegramApiException(400, "Bad Request: file is too big")
            }
            fail("expected exception")
        } catch (e: TelegramApiException) {
            assertEquals(400, e.httpCode)
        }
        assertEquals(1, calls)
    }
}
