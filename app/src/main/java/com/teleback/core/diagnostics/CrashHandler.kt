package com.teleback.core.diagnostics

import android.content.Context
import android.os.Build
import com.teleback.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashHandler {
    private const val CRASH_FILE_NAME = "crash_dump.txt"
    private const val MAX_LOG_HISTORY = 50
    private val logHistory = mutableListOf<String>()
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun init(context: Context) {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleCrash(context.applicationContext, thread, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    @Synchronized
    fun recordLog(log: String) {
        if (logHistory.size >= MAX_LOG_HISTORY) {
            logHistory.removeAt(0)
        }
        logHistory.add(log)
    }

    @Synchronized
    fun getRecentLogs(): List<String> {
        return logHistory.toList()
    }

    private fun handleCrash(context: Context, thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stacktrace = sw.toString()

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).format(Date())
            val logsTail = synchronized(this) { logHistory.joinToString("\n") }

            val report = buildString {
                appendLine("🚨 #CRASH_DUMP • TeleBack v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                appendLine("📅 Timestamp: $timestamp")
                appendLine("📱 Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.BRAND})")
                appendLine("🤖 Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("🧵 Thread: ${thread.name}")
                appendLine()
                appendLine("📋 STACKTRACE:")
                appendLine(stacktrace.trim())
                appendLine()
                appendLine("📜 RECENT CONSOLE LOGS:")
                if (logsTail.isNotBlank()) {
                    appendLine(logsTail)
                } else {
                    appendLine("(No console logs recorded)")
                }
            }

            val file = File(context.filesDir, CRASH_FILE_NAME)
            file.writeText(report)
        } catch (_: Exception) {
            // Fail-safe: Never crash inside the crash handler
        }
    }

    fun hasCrashDump(context: Context): Boolean {
        return File(context.filesDir, CRASH_FILE_NAME).exists()
    }

    fun getCrashDump(context: Context): String? {
        val file = File(context.filesDir, CRASH_FILE_NAME)
        return if (file.exists()) file.readText() else null
    }

    fun clearCrashDump(context: Context) {
        try {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }
}
