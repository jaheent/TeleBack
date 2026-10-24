package com.teleback.presentation.feedback

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.teleback.BuildConfig
import com.teleback.core.diagnostics.CrashHandler
import com.teleback.core.diagnostics.LogSanitizer
import com.teleback.presentation.theme.MonospaceFontFamily
import com.teleback.presentation.theme.TerminalAmber
import com.teleback.presentation.theme.TerminalRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FeedbackDialog(
    isCrashReport: Boolean,
    crashDumpContent: String? = null,
    knownBotToken: String? = null,
    knownChatId: String? = null,
    knownMasterKey: String? = null,
    knownFolderName: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var userNote by remember { mutableStateOf("") }
    var includeLogs by remember { mutableStateOf(true) }

    // Generate sanitized diagnostic report
    val timestamp = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).format(Date()) }
    val sanitizedReport = remember(userNote, includeLogs, crashDumpContent) {
        val rawText = if (isCrashReport && !crashDumpContent.isNullOrBlank()) {
            buildString {
                appendLine(crashDumpContent.trim())
                if (userNote.isNotBlank()) {
                    appendLine()
                    appendLine("👤 USER COMMENT:")
                    appendLine(userNote.trim())
                }
            }
        } else {
            buildString {
                appendLine("💬 #FEEDBACK • TeleBack v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                appendLine("📅 Timestamp: $timestamp")
                appendLine("📦 Platform: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine()
                appendLine("👤 USER NOTE:")
                appendLine(if (userNote.isNotBlank()) userNote.trim() else "(No additional comment)")
                appendLine()
                if (includeLogs) {
                    appendLine("📋 OPERATIONAL TRACE:")
                    val logs = CrashHandler.getRecentLogs()
                    if (logs.isNotEmpty()) {
                        appendLine(logs.joinToString("\n"))
                    } else {
                        appendLine("(No trace available)")
                    }
                }
            }
        }

        LogSanitizer.sanitize(
            input = rawText,
            knownToken = knownBotToken,
            knownChatId = knownChatId,
            knownMasterKey = knownMasterKey,
            knownFolderName = knownFolderName
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = 1.dp,
                    color = if (isCrashReport) TerminalRed else colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isCrashReport) "🚨 CRASH DIAGNOSTIC REPORT" else "⚡ FEEDBACK & BUG REPORT",
                        color = if (isCrashReport) TerminalRed else colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MonospaceFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isCrashReport) {
                        "TeleBack encountered an unexpected error. You can transmit this sanitized report to @telebackfblogs_bot to help fix it."
                    } else {
                        "Share feedback, suggest improvements, or report issues directly to the TeleBack development team."
                    },
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    fontFamily = MonospaceFontFamily
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Optional User Comments
                OutlinedTextField(
                    value = userNote,
                    onValueChange = { userNote = it },
                    label = {
                        Text(
                            text = if (isCrashReport) "What happened? (Optional)" else "Your feedback or description...",
                            fontSize = 10.sp,
                            fontFamily = MonospaceFontFamily
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontFamily = MonospaceFontFamily
                    ),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline
                    )
                )

                if (!isCrashReport) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = includeLogs,
                            onCheckedChange = { includeLogs = it },
                            colors = CheckboxDefaults.colors(checkedColor = colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Include sanitized console logs",
                            color = colorScheme.onSurface,
                            fontSize = 10.sp,
                            fontFamily = MonospaceFontFamily
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sanitized Payload Preview Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ZERO-KNOWLEDGE PREVIEW",
                        color = TerminalAmber,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MonospaceFontFamily
                    )
                    Text(
                        text = "🔒 Tokens & Keys Redacted",
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 8.sp,
                        fontFamily = MonospaceFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Scrollable Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(colorScheme.surfaceVariant)
                        .border(1.dp, colorScheme.outline, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = sanitizedReport,
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        fontFamily = MonospaceFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Button(
                    onClick = {
                        sendViaTelegram(context, sanitizedReport)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "✈️ Send via @telebackfblogs_bot",
                        fontSize = 11.sp,
                        fontFamily = MonospaceFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            shareReport(context, sanitizedReport)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.onSurface)
                    ) {
                        Text(
                            text = "📤 Share / Copy",
                            fontSize = 10.sp,
                            fontFamily = MonospaceFontFamily
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.onSurfaceVariant)
                    ) {
                        Text(
                            text = if (isCrashReport) "Dismiss" else "Cancel",
                            fontSize = 10.sp,
                            fontFamily = MonospaceFontFamily
                        )
                    }
                }
            }
        }
    }
}

private fun sendViaTelegram(context: Context, reportText: String) {
    // 1. Copy sanitized ticket to clipboard
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("TeleBack Diagnostic Report", reportText)
    clipboard.setPrimaryClip(clip)

    Toast.makeText(
        context,
        "Report copied to clipboard! Paste it into @telebackfblogs_bot",
        Toast.LENGTH_LONG
    ).show()

    // 2. Open Telegram directly at @telebackfblogs_bot
    val tgIntent = Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=telebackfblogs_bot")).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(tgIntent)
    } catch (_: Exception) {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/telebackfblogs_bot")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(webIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "Could not open Telegram. Report is copied to your clipboard.", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun shareReport(context: Context, reportText: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, reportText)
        type = "text/plain"
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    val chooser = Intent.createChooser(sendIntent, "Share TeleBack Diagnostic Report").apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(chooser)
}
