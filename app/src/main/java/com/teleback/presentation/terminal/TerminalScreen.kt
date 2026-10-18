package com.teleback.presentation.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teleback.presentation.customization.CustomizationDialog
import com.teleback.presentation.onboarding.OnboardingDialog
import com.teleback.presentation.onboarding.WelcomeDialog
import com.teleback.presentation.restore.SnapshotPickerSheet
import com.teleback.presentation.settings.KeysDialog
import com.teleback.presentation.terminal.components.CommandGrid
import com.teleback.presentation.terminal.components.CommandInputBar
import com.teleback.presentation.terminal.components.TerminalHeader
import com.teleback.presentation.terminal.components.TerminalLogView

@Composable
fun TerminalScreen(
    onPickFolderClick: () -> Unit,
    viewModel: TerminalViewModel = viewModel()
) {
    val logs by viewModel.logs.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()
    val botToken by viewModel.botToken.collectAsState()
    val chatTitle by viewModel.chatTitle.collectAsState()
    val folderName by viewModel.folderName.collectAsState()
    val isEncrypted by viewModel.isEncryptionEnabled.collectAsState()
    val masterKey by viewModel.masterKey.collectAsState()
    val snapshots by viewModel.snapshots.collectAsState()
    val showWelcome by viewModel.showWelcome.collectAsState()
    val showOnboarding by viewModel.showOnboarding.collectAsState()
    val showSnapshotPicker by viewModel.showSnapshotPicker.collectAsState()
    val showCustomization by viewModel.showCustomization.collectAsState()
    val showKeysDialog by viewModel.showKeysDialog.collectAsState()
    val selectedThemeId by viewModel.selectedThemeId.collectAsState()
    val selectedIconId by viewModel.selectedIconId.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
                .imePadding()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Retro Header
                TerminalHeader(
                    isOnline = isOnline,
                    botName = if (botToken != null) "Ready" else null,
                    chatTitle = chatTitle,
                    folderName = folderName,
                    isEncrypted = isEncrypted,
                    isBusy = isBusy
                )

                // Main Monospace Log Stream
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    TerminalLogView(logs = logs)
                }

                // High-Density 2-Column Command Grid (No Scroll Needed)
                CommandGrid(
                    onCommandSelected = { cmd ->
                        viewModel.executeCommand(cmd)
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Bottom Numbered Input Prompt Bar
                CommandInputBar(
                    onExecuteInput = { input ->
                        viewModel.executeInput(input)
                    }
                )

                Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
            }

            // First-Run Welcome Dialog
            if (showWelcome) {
                WelcomeDialog(
                    onLaunchSetup = { viewModel.launchSetupFromWelcome() },
                    onDismiss = { viewModel.dismissWelcome() }
                )
            }

            // Interactive Setup Wizard Dialog
            if (showOnboarding) {
                OnboardingDialog(
                    initialBotToken = botToken,
                    initialChatTitle = chatTitle,
                    initialFolderName = folderName,
                    masterKeyBase64 = masterKey,
                    onSaveToken = { viewModel.saveBotToken(it) },
                    onDetectChat = { viewModel.detectChat() },
                    onPickFolderClick = onPickFolderClick,
                    onDismiss = { viewModel.dismissOnboarding() }
                )
            }

            // Restore Snapshot Picker Dialog
            if (showSnapshotPicker) {
                SnapshotPickerSheet(
                    snapshots = snapshots,
                    onSelectSnapshot = { snapId ->
                        viewModel.runRestore(snapId)
                    },
                    onDismiss = { viewModel.dismissSnapshotPicker() }
                )
            }

            // Customization Suite Dialog (Themes & App Icons)
            if (showCustomization) {
                CustomizationDialog(
                    currentThemeId = selectedThemeId,
                    currentIconId = selectedIconId,
                    onSelectTheme = { viewModel.setTheme(it) },
                    onSelectIcon = { viewModel.setIcon(it) },
                    onDismiss = { viewModel.dismissCustomization() }
                )
            }

            // Master Encryption Keys Dialog
            if (showKeysDialog) {
                KeysDialog(
                    currentMasterKey = masterKey ?: "",
                    onSaveKey = { viewModel.updateMasterKey(it) },
                    onDismiss = { viewModel.dismissKeysDialog() }
                )
            }
        }
    }
}
