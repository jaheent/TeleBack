package com.teleback.presentation.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teleback.presentation.cloud.CloudManagerDialog
import com.teleback.presentation.customization.CustomizationDialog
import com.teleback.presentation.feedback.FeedbackDialog
import com.teleback.presentation.onboarding.OnboardingDialog
import com.teleback.presentation.onboarding.SetupWalkthroughDialog
import com.teleback.presentation.onboarding.WelcomeDialog
import com.teleback.presentation.restore.SnapshotPickerSheet
import com.teleback.presentation.settings.KeysDialog
import com.teleback.presentation.terminal.components.CommandGrid
import com.teleback.presentation.terminal.components.SnapshotPromptDialog
import com.teleback.presentation.terminal.components.TerminalHeader
import com.teleback.presentation.terminal.components.TerminalLogView
import com.teleback.presentation.theme.MonospaceFontFamily

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
    val showCloudManager by viewModel.showCloudManager.collectAsState()
    val showCustomization by viewModel.showCustomization.collectAsState()
    val showKeysDialog by viewModel.showKeysDialog.collectAsState()
    val showWalkthrough by viewModel.showWalkthrough.collectAsState()
    val selectedThemeId by viewModel.selectedThemeId.collectAsState()
    val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsState()
    val syncIntervalHours by viewModel.syncIntervalHours.collectAsState()
    val syncRequiresUnmetered by viewModel.syncRequiresUnmetered.collectAsState()
    val syncRequiresCharging by viewModel.syncRequiresCharging.collectAsState()
    val syncRequiresBatteryNotLow by viewModel.syncRequiresBatteryNotLow.collectAsState()
    val showScheduleDialog by viewModel.showScheduleDialog.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val isAppLocked by viewModel.isAppLocked.collectAsState()
    val isSettingsView by viewModel.isSettingsView.collectAsState()
    val promptSyncIsFull by viewModel.promptSyncIsFull.collectAsState()
    val showCrashDialog by viewModel.showCrashDialog.collectAsState()
    val crashDumpContent by viewModel.crashDumpContent.collectAsState()
    val showFeedbackDialog by viewModel.showFeedbackDialog.collectAsState()

    if (isAppLocked) {
        com.teleback.presentation.security.BiometricLockOverlay(
            onUnlockSuccess = { viewModel.unlockApp() }
        )
    } else {
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
                // Retro Header (Strictly Read-Only Indicator)
                TerminalHeader(
                    isOnline = isOnline,
                    botName = if (botToken != null) "Ready" else null,
                    chatTitle = chatTitle,
                    folderName = folderName,
                    isEncrypted = isEncrypted,
                    isBusy = isBusy,
                    autoSyncStatus = if (autoSyncEnabled) "${syncIntervalHours}h" else "OFF"
                )

                // Main Monospace Log Stream with Compact Scrollbar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    TerminalLogView(logs = logs)
                }

                // High-Density 2-Column Command Grid with 0.5x Glitch Transition into Settings
                CommandGrid(
                    isSettingsView = isSettingsView,
                    onCommandSelected = { cmd ->
                        viewModel.executeCommand(cmd)
                    },
                    onOpenSettings = { viewModel.openSettingsView() },
                    onBackFromSettings = { viewModel.closeSettingsView() },
                    onOpenThemes = { viewModel.openThemesDialog() },
                    onOpenKeys = { viewModel.openKeysDialog() },
                    onOpenSchedule = { viewModel.openScheduleDialog() },
                    onOpenFeedback = { viewModel.openFeedbackDialog() },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Bottom App Creator Signature
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✨ App Creator: Jaheen ✨",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontFamily = MonospaceFontFamily
                    )
                }

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
                    onCompleteSetup = { derivedKey, isExistingVault ->
                        viewModel.finishOnboarding(derivedKey, isExistingVault)
                    },
                    onOpenGuide = { viewModel.openWalkthroughFromSetup() },
                    onDismiss = { viewModel.dismissOnboarding() }
                )
            }

            // Visual Telegram Bot/Group Walkthrough (first run + setup [0])
            if (showWalkthrough) {
                SetupWalkthroughDialog(
                    onContinueToSetup = { viewModel.continueWalkthroughToSetup() },
                    onDismiss = { viewModel.dismissWalkthrough() }
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

            // Cloud Snapshot Manager Dialog [4]
            if (showCloudManager) {
                CloudManagerDialog(
                    snapshots = snapshots,
                    isScanning = isBusy,
                    isBiometricEnabled = isBiometricEnabled,
                    onScanCloud = { viewModel.scanCloudSnapshots() },
                    onDeleteSelected = { selectedIds ->
                        viewModel.deleteCloudSnapshots(selectedIds)
                    },
                    onWipeAll = { viewModel.wipeCloudVault() },
                    onDismiss = { viewModel.dismissCloudManager() }
                )
            }

            // Terminal Themes Dialog
            if (showCustomization) {
                CustomizationDialog(
                    currentThemeId = selectedThemeId,
                    onSelectTheme = { viewModel.setTheme(it) },
                    onDismiss = { viewModel.dismissCustomization() }
                )
            }

            // Auto-Sync & Background Scheduler Dialog
            if (showScheduleDialog) {
                com.teleback.presentation.schedule.ScheduleDialog(
                    initialEnabled = autoSyncEnabled,
                    initialIntervalHours = syncIntervalHours,
                    initialRequiresUnmetered = syncRequiresUnmetered,
                    initialRequiresCharging = syncRequiresCharging,
                    initialRequiresBatteryNotLow = syncRequiresBatteryNotLow,
                    onSaveSchedule = { enabled, interval, unmetered, charging, batteryNotLow ->
                        viewModel.saveSchedule(enabled, interval, unmetered, charging, batteryNotLow)
                    },
                    onTriggerInstantSync = {
                        viewModel.triggerInstantBackgroundSync()
                    },
                    onDismiss = { viewModel.dismissScheduleDialog() }
                )
            }

            // Master Encryption Keys Dialog
            if (showKeysDialog) {
                KeysDialog(
                    currentMasterKey = masterKey ?: "",
                    isBiometricEnabled = isBiometricEnabled,
                    onToggleBiometric = { viewModel.setBiometricEnabled(it) },
                    onSaveKey = { viewModel.updateMasterKey(it) },
                    onDismiss = { viewModel.dismissKeysDialog() }
                )
            }

            // Custom Snapshot Note/Tag Creation Prompt
            promptSyncIsFull?.let { isFull ->
                SnapshotPromptDialog(
                    isFullBackup = isFull,
                    onConfirm = { tag ->
                        viewModel.confirmSyncWithTag(tag)
                    },
                    onDismiss = {
                        viewModel.dismissSyncPrompt()
                    }
                )
            }

            // Startup Crash Recovery Dialog
            if (showCrashDialog) {
                FeedbackDialog(
                    isCrashReport = true,
                    crashDumpContent = crashDumpContent,
                    knownBotToken = botToken,
                    knownChatId = null,
                    knownMasterKey = masterKey,
                    knownFolderName = folderName,
                    onDismiss = { viewModel.dismissCrashDialog() }
                )
            }

            // Manual Feedback & Bug Report Dialog
            if (showFeedbackDialog) {
                FeedbackDialog(
                    isCrashReport = false,
                    knownBotToken = botToken,
                    knownChatId = null,
                    knownMasterKey = masterKey,
                    knownFolderName = folderName,
                    onDismiss = { viewModel.dismissFeedbackDialog() }
                )
            }
        }
    }
}
}
