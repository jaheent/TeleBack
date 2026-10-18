package com.teleback.core.crypto

import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_SIZE = 256
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private val HEADER_MAGIC = "TBK1".toByteArray(Charsets.US_ASCII)
    private const val BUFFER_SIZE = 64 * 1024

    fun generateMasterKeyBase64(): String {
        val keyGen = KeyGenerator.getInstance(ALGORITHM)
        keyGen.init(KEY_SIZE, SecureRandom())
        val secretKey = keyGen.generateKey()
        return Base64.getEncoder().encodeToString(secretKey.encoded)
    }

    private fun getSecretKey(keyBase64: String): SecretKey {
        val decoded = Base64.getDecoder().decode(keyBase64)
        return SecretKeySpec(decoded, ALGORITHM)
    }

    fun encryptBytes(data: ByteArray, keyBase64: String): ByteArray {
        val secretKey = getSecretKey(keyBase64)
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

        val ciphertext = cipher.doFinal(data)
        // Format: [TBK1 (4 bytes)][IV (12 bytes)][Ciphertext]
        val result = ByteArray(HEADER_MAGIC.size + GCM_IV_LENGTH + ciphertext.size)
        System.arraycopy(HEADER_MAGIC, 0, result, 0, HEADER_MAGIC.size)
        System.arraycopy(iv, 0, result, HEADER_MAGIC.size, GCM_IV_LENGTH)
        System.arraycopy(ciphertext, 0, result, HEADER_MAGIC.size + GCM_IV_LENGTH, ciphertext.size)
        return result
    }

    fun decryptBytes(data: ByteArray, keyBase64: String): ByteArray {
        if (data.size < HEADER_MAGIC.size + GCM_IV_LENGTH) {
            throw IllegalArgumentException("Data is too short to be an encrypted TeleBack payload")
        }

        // Verify magic header
        for (i in HEADER_MAGIC.indices) {
            if (data[i] != HEADER_MAGIC[i]) {
                throw IllegalArgumentException("Invalid TeleBack header: payload is not encrypted with TeleBack v1")
            }
        }

        val secretKey = getSecretKey(keyBase64)
        val iv = ByteArray(GCM_IV_LENGTH)
        System.arraycopy(data, HEADER_MAGIC.size, iv, 0, GCM_IV_LENGTH)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)

        val ciphertextOffset = HEADER_MAGIC.size + GCM_IV_LENGTH
        val ciphertextSize = data.size - ciphertextOffset
        return cipher.doFinal(data, ciphertextOffset, ciphertextSize)
    }

    fun encryptStream(input: InputStream, output: OutputStream, keyBase64: String) {
        val allBytes = input.readBytes()
        val encrypted = encryptBytes(allBytes, keyBase64)
        output.write(encrypted)
        output.flush()
    }

    fun decryptStream(input: InputStream, output: OutputStream, keyBase64: String) {
        val allBytes = input.readBytes()
        val decrypted = decryptBytes(allBytes, keyBase64)
        output.write(decrypted)
        output.flush()
    }
}
