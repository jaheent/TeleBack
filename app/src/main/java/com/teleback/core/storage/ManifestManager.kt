package com.teleback.core.storage

import com.teleback.core.crypto.CryptoEngine
import com.teleback.data.model.SnapshotManifest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object ManifestManager {
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    const val MANIFEST_TAG = "#TELEBACK_MANIFEST"

    fun serializeManifest(
        manifest: SnapshotManifest,
        masterKeyBase64: String? = null
    ): ByteArray {
        val jsonString = json.encodeToString(manifest)
        val rawBytes = jsonString.toByteArray(Charsets.UTF_8)

        return if (manifest.isEncrypted && masterKeyBase64 != null) {
            CryptoEngine.encryptBytes(rawBytes, masterKeyBase64)
        } else {
            rawBytes
        }
    }

    fun deserializeManifest(
        bytes: ByteArray,
        masterKeyBase64: String? = null
    ): SnapshotManifest {
        val decryptedBytes = if (CryptoEngine.isEncrypted(bytes)) {
            if (masterKeyBase64.isNullOrBlank()) {
                throw IllegalStateException("Manifest is encrypted but master key is not configured.")
            }
            try {
                CryptoEngine.decryptBytes(bytes, masterKeyBase64)
            } catch (e: Exception) {
                throw SecurityException("Failed to decrypt manifest: Master Passphrase does not match encryption key.", e)
            }
        } else {
            bytes
        }

        val jsonString = String(decryptedBytes, Charsets.UTF_8)
        return json.decodeFromString<SnapshotManifest>(jsonString)
    }
}
