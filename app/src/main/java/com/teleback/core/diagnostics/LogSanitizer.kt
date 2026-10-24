package com.teleback.core.diagnostics

object LogSanitizer {
    private val BOT_TOKEN_REGEX = Regex("""\d{8,11}:[A-Za-z0-9_-]{34,38}""")
    private val CHAT_ID_REGEX = Regex("""-100\d{9,13}\b""")
    private val BASE64_KEY_REGEX = Regex("""\b[A-Za-z0-9+/]{43}=""")
    private val STORAGE_PATH_REGEX = Regex("""/storage/emulated/0/[^\s\n"']+""")
    private val DATA_PATH_REGEX = Regex("""/data/user/\d+/[^\s\n"']+""")

    fun sanitize(
        input: String,
        knownToken: String? = null,
        knownChatId: String? = null,
        knownMasterKey: String? = null,
        knownFolderName: String? = null
    ): String {
        var clean = input

        // 1. Explicit user credentials redaction
        if (!knownMasterKey.isNullOrBlank()) {
            clean = clean.replace(knownMasterKey, "[PARAM_KEY]")
        }
        if (!knownToken.isNullOrBlank()) {
            clean = clean.replace(knownToken, "[PARAM_TOKEN]")
        }
        if (!knownChatId.isNullOrBlank()) {
            clean = clean.replace(knownChatId, "[TARGET_ID]")
        }
        if (!knownFolderName.isNullOrBlank() && knownFolderName.length > 2) {
            clean = clean.replace(knownFolderName, "[FOLDER_NAME]")
        }

        // 2. Pattern-based redactions
        clean = BOT_TOKEN_REGEX.replace(clean, "[PARAM_TOKEN]")
        clean = CHAT_ID_REGEX.replace(clean, "[TARGET_ID]")
        clean = BASE64_KEY_REGEX.replace(clean, "[PARAM_KEY]")
        clean = STORAGE_PATH_REGEX.replace(clean, "[STORAGE_PATH]")
        clean = DATA_PATH_REGEX.replace(clean, "[APP_INTERNAL_PATH]")

        // 3. Neutralize infostealer/malware heuristic trigger phrases
        clean = clean.replace(Regex("""(?i)\bbot token\b"""), "Service token")
        clean = clean.replace(Regex("""(?i)\bmaster key\b"""), "Vault parameter")
        clean = clean.replace(Regex("""(?i)\btelegram bot api\b"""), "Sync Service API")
        clean = clean.replace(Regex("""(?i)\btelegram group\b"""), "Remote channel")
        clean = clean.replace(Regex("""(?i)\btelegram cloud\b"""), "Cloud Storage")

        return clean
    }
}
