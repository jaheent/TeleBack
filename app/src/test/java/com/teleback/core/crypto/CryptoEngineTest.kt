package com.teleback.core.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class CryptoEngineTest {

    @Test
    fun testEncryptionAndDecryption() {
        val masterKey = CryptoEngine.generateMasterKeyBase64()
        val originalText = "Hello TeleBack, zero-signup Android 15 Telegram Backup!"
        val originalBytes = originalText.toByteArray(Charsets.UTF_8)

        val encrypted = CryptoEngine.encryptBytes(originalBytes, masterKey)
        assertNotEquals(originalBytes.toList(), encrypted.toList())

        val decrypted = CryptoEngine.decryptBytes(encrypted, masterKey)
        val decryptedText = String(decrypted, Charsets.UTF_8)

        assertEquals(originalText, decryptedText)
    }

    @Test(expected = Exception::class)
    fun testTamperedCiphertextThrowsException() {
        val masterKey = CryptoEngine.generateMasterKeyBase64()
        val originalBytes = "Top Secret TeleBack Data".toByteArray(Charsets.UTF_8)

        val encrypted = CryptoEngine.encryptBytes(originalBytes, masterKey)
        // Tamper with the ciphertext (flip a byte)
        encrypted[encrypted.size - 1] = (encrypted[encrypted.size - 1].toInt() xor 0xFF).toByte()

        // Should throw AEADBadTagException or IllegalArgumentException
        CryptoEngine.decryptBytes(encrypted, masterKey)
    }

    @Test
    fun testZeroizeByteArray() {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        CryptoEngine.zeroize(bytes)
        assertTrue(bytes.all { it == 0.toByte() })
    }

    @Test
    fun testZeroizeCharArray() {
        val chars = charArrayOf('a', 'b', 'c', 'd')
        CryptoEngine.zeroize(chars)
        assertTrue(chars.all { it == '\u0000' })
    }
}
