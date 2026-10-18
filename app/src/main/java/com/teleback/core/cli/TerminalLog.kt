package com.teleback.core.cli

import androidx.compose.ui.graphics.Color
import com.teleback.presentation.theme.TerminalAmber
import com.teleback.presentation.theme.TerminalCyan
import com.teleback.presentation.theme.TerminalMint
import com.teleback.presentation.theme.TerminalPurple
import com.teleback.presentation.theme.TerminalRed
import com.teleback.presentation.theme.TerminalTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class LogLevel(val tag: String, val color: Color) {
    CMD("[CMD] ", TerminalMint),
    INFO("[INFO]", TerminalCyan),
    SYNC("[SYNC]", TerminalCyan),
    OK("[ OK ]", TerminalMint),
    WARN("[WARN]", TerminalAmber),
    ERR("[FAIL]", TerminalRed),
    SNAP("[SNAP]", TerminalPurple),
    PLAIN("      ", TerminalTextSecondary)
}

data class TerminalLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val message: String,
    val progress: Float? = null // 0.0f..1.0f for progress bar rendering
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}
