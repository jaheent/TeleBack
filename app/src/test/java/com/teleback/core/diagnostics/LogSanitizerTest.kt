package com.teleback.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogSanitizerTest {

    @Test
    fun testSanitizeBotToken() {
        val raw = "Connecting to https://api.telegram.org/bot1234567890:AAFDnzdYbWt0DIQ44PgNpwQ5H_Wxp45eEgk/getMe"
        val sanitized = LogSanitizer.sanitize(raw)
        assertFalse(sanitized.contains("1234567890:AAFDnzdYbWt0DIQ44PgNpwQ5H_Wxp45eEgk"))
        assertTrue(sanitized.contains("[REDACTED_BOT_TOKEN]"))
    }

    @Test
    fun testSanitizeChatIdAndMasterKey() {
        val raw = "Posting to -1003900951055 with key uWz2k9L8M7N6P5Q4R3S2T1U0V9W8X7Y6Z5A4B3C2D1E="
        val sanitized = LogSanitizer.sanitize(raw)
        assertFalse(sanitized.contains("-1003900951055"))
        assertFalse(sanitized.contains("uWz2k9L8M7N6P5Q4R3S2T1U0V9W8X7Y6Z5A4B3C2D1E="))
        assertTrue(sanitized.contains("[REDACTED_CHAT_ID]"))
        assertTrue(sanitized.contains("[REDACTED_MASTER_KEY]"))
    }

    @Test
    fun testSanitizeStoragePath() {
        val raw = "Reading file /storage/emulated/0/Documents/SecretVault/personal.txt"
        val sanitized = LogSanitizer.sanitize(raw)
        assertFalse(sanitized.contains("/storage/emulated/0/Documents/SecretVault/personal.txt"))
        assertTrue(sanitized.contains("[STORAGE_PATH]"))
    }

    @Test
    fun testExplicitCredentialsRedaction() {
        val raw = "Custom target folder: PrivateDocuments"
        val sanitized = LogSanitizer.sanitize(raw, knownFolderName = "PrivateDocuments")
        assertEquals("Custom target folder: [FOLDER_NAME]", sanitized)
    }
}
