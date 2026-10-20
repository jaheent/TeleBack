package com.teleback.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.teleback.presentation.theme.MonospaceFontFamily
import com.teleback.presentation.theme.TerminalAmber
import com.teleback.presentation.theme.TerminalBlack
import com.teleback.presentation.theme.TerminalBorder
import com.teleback.presentation.theme.TerminalCyan
import com.teleback.presentation.theme.TerminalDark
import com.teleback.presentation.theme.TerminalMint
import com.teleback.presentation.theme.TerminalRed
import com.teleback.presentation.theme.TerminalSurface
import com.teleback.presentation.theme.TerminalSurfaceVariant
import com.teleback.presentation.theme.TerminalTextMuted
import com.teleback.presentation.theme.TerminalTextPrimary
import com.teleback.presentation.theme.TerminalTextSecondary

/** One line inside the little fake Telegram screen shown on each walkthrough page. */
private data class MockLine(
    val text: String,
    val fromUser: Boolean = false,
    val highlight: Boolean = false
)

private data class WalkStep(
    val title: String,
    val body: String,
    val mockTitle: String,
    val mock: List<MockLine>,
    val note: String? = null,
    val noteIsWarning: Boolean = false
)

private val walkSteps = listOf(
    WalkStep(
        title = "Open @BotFather",
        body = "BotFather is Telegram's official bot for creating bots. In Telegram, search for @BotFather (look for the blue tick) and tap START.",
        mockTitle = "Search",
        mock = listOf(
            MockLine("@BotFather  [verified]", highlight = true),
            MockLine("@BotFatherTeam  (not this one)"),
            MockLine("Tap START at the bottom", fromUser = true)
        )
    ),
    WalkStep(
        title = "Create your bot",
        body = "Send /newbot. Choose any display name, then a username that must end in \"bot\".",
        mockTitle = "BotFather",
        mock = listOf(
            MockLine("/newbot", fromUser = true),
            MockLine("Alright, a new bot. How are we going to call it?"),
            MockLine("TeleBack Vault", fromUser = true),
            MockLine("Now choose a username. It must end in 'bot'."),
            MockLine("my_teleback_vault_bot", fromUser = true, highlight = true)
        )
    ),
    WalkStep(
        title = "Copy the bot token",
        body = "BotFather replies with an HTTP API token. Long-press it to copy. You will paste it into TeleBack in the next step of setup.",
        mockTitle = "BotFather",
        mock = listOf(
            MockLine("Done! Congratulations on your new bot."),
            MockLine("Use this token to access the HTTP API:"),
            MockLine("123456789:AAH\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022", highlight = true)
        ),
        note = "Treat the token like a password. Anyone who has it controls your bot. Never share it or post it online.",
        noteIsWarning = true
    ),
    WalkStep(
        title = "Create a private group",
        body = "In Telegram tap New Group. Telegram asks for at least one member: add any contact, you can remove them afterwards. Name it anything, e.g. \"My TeleBack Vault\".",
        mockTitle = "New Group",
        mock = listOf(
            MockLine("Group name", highlight = false),
            MockLine("My TeleBack Vault", fromUser = true, highlight = true),
            MockLine("Type: Private  (keep it private!)")
        ),
        note = "This group is where your encrypted backups live. Do not invite anyone you do not trust."
    ),
    WalkStep(
        title = "Add the bot as admin",
        body = "Open the group, tap its name, then Add Member and search for your bot's username. After it joins, open Administrators and promote the bot.",
        mockTitle = "Group info > Administrators",
        mock = listOf(
            MockLine("Members: You, my_teleback_vault_bot"),
            MockLine("my_teleback_vault_bot  [admin]", highlight = true),
            MockLine("Allow: Delete messages, Pin messages", fromUser = true)
        ),
        note = "TeleBack needs to pin manifests and delete old snapshots, so these two admin rights are required."
    ),
    WalkStep(
        title = "Send a message, then detect",
        body = "Write any message in the group, for example \"hello\". Then come back to TeleBack's setup wizard and tap Auto-Detect Chat ID.",
        mockTitle = "My TeleBack Vault",
        mock = listOf(
            MockLine("hello", fromUser = true),
            MockLine("TeleBack > Auto-Detect Chat ID"),
            MockLine("Linked Group: My TeleBack Vault", highlight = true)
        )
    ),
    WalkStep(
        title = "Test the connection",
        body = "Back in the terminal, type 6 (or tap the [6] test chip). A green [ OK ] line means TeleBack can talk to your bot.",
        mockTitle = "teleback>",
        mock = listOf(
            MockLine("> 6", fromUser = true),
            MockLine("[INFO] Pinging Telegram Bot API..."),
            MockLine("[ OK ] Connected to @my_teleback_vault_bot", highlight = true)
        ),
        note = "Then pick a folder, run [1] sync, and your first backup is in the cloud."
    )
)

/**
 * Visual, step-by-step guide to creating the Telegram bot and group. Shown on first run and
 * whenever setup [0] is opened. The illustrations are drawn in-app, so they follow the terminal
 * look and never go out of date with Telegram's own screens.
 */
@Composable
fun SetupWalkthroughDialog(
    onContinueToSetup: () -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    val current = walkSteps[step]
    val isFirst = step == 0
    val isLast = step == walkSteps.lastIndex

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, TerminalCyan, RoundedCornerShape(8.dp))
                .pointerInput(step) {
                    var dragged = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragged = 0f },
                        onHorizontalDrag = { _, amount -> dragged += amount },
                        onDragEnd = {
                            if (dragged < -80f && step < walkSteps.lastIndex) step++
                            else if (dragged > 80f && step > 0) step--
                        }
                    )
                },
            colors = CardDefaults.cardColors(containerColor = TerminalDark)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VISUAL GUIDE // TELEGRAM SETUP",
                        color = TerminalCyan,
                        fontSize = 12.sp,
                        fontFamily = MonospaceFontFamily
                    )
                    Text(
                        text = "STEP ${step + 1} OF ${walkSteps.size}",
                        color = TerminalTextMuted,
                        fontSize = 10.sp,
                        fontFamily = MonospaceFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                ProgressDots(total = walkSteps.size, current = step)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "${step + 1}. ${current.title}",
                    color = TerminalMint,
                    fontSize = 14.sp,
                    fontFamily = MonospaceFontFamily
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = current.body,
                    color = TerminalTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontFamily = MonospaceFontFamily
                )

                Spacer(modifier = Modifier.height(10.dp))
                MockScreen(title = current.mockTitle, lines = current.mock)

                if (current.note != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val noteColor = if (current.noteIsWarning) TerminalRed else TerminalAmber
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(noteColor.copy(alpha = 0.12f))
                            .border(1.dp, noteColor, RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = current.note,
                            color = noteColor,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            fontFamily = MonospaceFontFamily
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (isFirst) onContinueToSetup() else step-- },
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalTextSecondary)
                    ) {
                        Text(if (isFirst) "Skip" else "<- Back", fontFamily = MonospaceFontFamily, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { if (isLast) onContinueToSetup() else step++ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = TerminalMint, contentColor = TerminalBlack)
                    ) {
                        Text(if (isLast) "Start Setup" else "Next ->", fontFamily = MonospaceFontFamily, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Swipe left/right to move between steps.",
                    color = TerminalTextMuted,
                    fontSize = 9.sp,
                    fontFamily = MonospaceFontFamily
                )
            }
        }
    }
}

@Composable
private fun ProgressDots(total: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until total) {
            Box(
                modifier = Modifier
                    .size(if (i == current) 9.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            i == current -> TerminalMint
                            i < current -> TerminalCyan
                            else -> TerminalBorder
                        }
                    )
            )
        }
    }
}

/** A tiny stylised chat window: header bar plus left (bot/system) and right (you) bubbles. */
@Composable
private fun MockScreen(title: String, lines: List<MockLine>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(TerminalBlack)
            .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalSurfaceVariant)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(TerminalRed))
            Spacer(Modifier.size(4.dp))
            Box(Modifier.size(7.dp).clip(CircleShape).background(TerminalAmber))
            Spacer(Modifier.size(4.dp))
            Box(Modifier.size(7.dp).clip(CircleShape).background(TerminalMint))
            Spacer(Modifier.size(10.dp))
            Text(
                text = title,
                color = TerminalTextPrimary,
                fontSize = 10.sp,
                fontFamily = MonospaceFontFamily
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            lines.forEach { line -> MockBubble(line) }
        }
    }
}

@Composable
private fun MockBubble(line: MockLine) {
    val bubbleColor: Color = if (line.fromUser) TerminalCyan.copy(alpha = 0.18f) else TerminalSurface
    val borderColor: Color = when {
        line.highlight -> TerminalMint
        line.fromUser -> TerminalCyan.copy(alpha = 0.5f)
        else -> TerminalBorder
    }
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (line.fromUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bubbleColor)
                .border(if (line.highlight) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Text(
                text = line.text,
                color = if (line.highlight) TerminalMint else TerminalTextPrimary,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                fontFamily = MonospaceFontFamily
            )
        }
    }
}
