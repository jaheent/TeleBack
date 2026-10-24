package com.teleback.presentation.terminal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teleback.core.cli.CliCommand
import com.teleback.core.cli.LogLevel
import com.teleback.core.cli.TerminalLog
import com.teleback.core.network.ConnectivityObserver
import com.teleback.core.service.SyncForegroundService
import com.teleback.core.util.NotificationHelper
import com.teleback.core.util.SizeFormat
import com.teleback.data.local.AppDatabase
import com.teleback.data.local.AppPreferences
import com.teleback.data.local.SnapshotEntity
import com.teleback.domain.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TerminalViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AppPreferences(application)
    private val syncManager = SyncManager(application)
    private val db = AppDatabase.getDatabase(application)
    private val connectivityObserver = ConnectivityObserver(application)

    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val botToken = prefs.botToken.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val chatTitle = prefs.chatTitle.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val folderName = prefs.folderName.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val isEncryptionEnabled = prefs.isEncryptionEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val masterKey = prefs.masterKey.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val snapshots = db.snapshotDao().getAllSnapshots().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedThemeId = prefs.selectedThemeId.stateIn(viewModelScope, SharingStarted.Eagerly, "dark_green")

    val autoSyncEnabled = prefs.autoSyncEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val syncIntervalHours = prefs.syncIntervalHours.stateIn(viewModelScope, SharingStarted.Eagerly, 6)
    val syncRequiresUnmetered = prefs.syncRequiresUnmetered.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val syncRequiresCharging = prefs.syncRequiresCharging.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val syncRequiresBatteryNotLow = prefs.syncRequiresBatteryNotLow.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val isBiometricEnabled = prefs.isBiometricEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _logs = MutableStateFlow<List<TerminalLog>>(emptyList())
    val logs: StateFlow<List<TerminalLog>> = _logs.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _showWelcome = MutableStateFlow(false)
    val showWelcome: StateFlow<Boolean> = _showWelcome.asStateFlow()

    private val _showOnboarding = MutableStateFlow(false)
    val showOnboarding: StateFlow<Boolean> = _showOnboarding.asStateFlow()

    private val _showSnapshotPicker = MutableStateFlow(false)
    val showSnapshotPicker: StateFlow<Boolean> = _showSnapshotPicker.asStateFlow()

    private val _showCloudManager = MutableStateFlow(false)
    val showCloudManager: StateFlow<Boolean> = _showCloudManager.asStateFlow()

    private val _showCustomization = MutableStateFlow(false)
    val showCustomization: StateFlow<Boolean> = _showCustomization.asStateFlow()

    private val _showScheduleDialog = MutableStateFlow(false)
    val showScheduleDialog: StateFlow<Boolean> = _showScheduleDialog.asStateFlow()

    private val _showKeysDialog = MutableStateFlow(false)
    val showKeysDialog: StateFlow<Boolean> = _showKeysDialog.asStateFlow()

    private val _showWalkthrough = MutableStateFlow(false)
    val showWalkthrough: StateFlow<Boolean> = _showWalkthrough.asStateFlow()

    private val _isSettingsView = MutableStateFlow(false)
    val isSettingsView: StateFlow<Boolean> = _isSettingsView.asStateFlow()

    private val _promptSyncIsFull = MutableStateFlow<Boolean?>(null)
    val promptSyncIsFull: StateFlow<Boolean?> = _promptSyncIsFull.asStateFlow()

    private val _showCrashDialog = MutableStateFlow(false)
    val showCrashDialog: StateFlow<Boolean> = _showCrashDialog.asStateFlow()

    private val _crashDumpContent = MutableStateFlow<String?>(null)
    val crashDumpContent: StateFlow<String?> = _crashDumpContent.asStateFlow()

    private val _showFeedbackDialog = MutableStateFlow(false)
    val showFeedbackDialog: StateFlow<Boolean> = _showFeedbackDialog.asStateFlow()

    fun openSettingsView() {
        _isSettingsView.value = true
        addLog(TerminalLog(level = LogLevel.CMD, message = "[SETTINGS] Switched to Settings menu."))
    }

    fun closeSettingsView() {
        _isSettingsView.value = false
        addLog(TerminalLog(level = LogLevel.CMD, message = "[TERMINAL] Returned to main console."))
    }

    fun openThemesDialog() {
        _showCustomization.value = true
    }

    fun openKeysDialog() {
        _showKeysDialog.value = true
    }

    fun openFeedbackDialog() {
        _showFeedbackDialog.value = true
    }

    fun dismissFeedbackDialog() {
        _showFeedbackDialog.value = false
    }

    fun dismissCrashDialog() {
        _showCrashDialog.value = false
        _crashDumpContent.value = null
        com.teleback.core.diagnostics.CrashHandler.clearCrashDump(getApplication())
    }

    fun requestSyncPrompt(isFull: Boolean) {
        _promptSyncIsFull.value = isFull
    }

    fun dismissSyncPrompt() {
        _promptSyncIsFull.value = null
    }

    fun confirmSyncWithTag(tag: String?) {
        val isFull = _promptSyncIsFull.value ?: false
        _promptSyncIsFull.value = null
        runSync(isFull = isFull, tag = tag)
    }

    init {
        printBanner()
        checkFirstRunWelcome()
        checkBiometricLock()
        checkCrashDump()
    }

    private fun checkCrashDump() {
        val app = getApplication<Application>()
        if (com.teleback.core.diagnostics.CrashHandler.hasCrashDump(app)) {
            val dump = com.teleback.core.diagnostics.CrashHandler.getCrashDump(app)
            if (!dump.isNullOrBlank()) {
                _crashDumpContent.value = dump
                _showCrashDialog.value = true
            }
        }
    }

    private fun checkBiometricLock() {
        viewModelScope.launch {
            if (prefs.isBiometricEnabled.first()) {
                _isAppLocked.value = true
            }
        }
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun lockApp() {
        _isAppLocked.value = true
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setBiometricEnabled(enabled)
            addLog(TerminalLog(
                level = LogLevel.OK,
                message = if (enabled) "[SECURITY] Biometric Sentinel enabled (Biometrics/PIN guarded)." else "[SECURITY] Biometric Sentinel disabled."
            ))
        }
    }

    private fun checkFirstRunWelcome() {
        viewModelScope.launch {
            val seen = prefs.hasSeenWelcome.first()
            if (!seen) {
                _showWelcome.value = true
            }
        }
    }

    fun dismissWelcome() {
        viewModelScope.launch {
            prefs.setHasSeenWelcome(true)
            _showWelcome.value = false
        }
    }

    fun launchSetupFromWelcome() {
        viewModelScope.launch {
            prefs.setHasSeenWelcome(true)
            _showWelcome.value = false
            _showWalkthrough.value = true
        }
    }

    fun openScheduleDialog() {
        _showScheduleDialog.value = true
    }

    fun dismissScheduleDialog() {
        _showScheduleDialog.value = false
    }

    fun saveSchedule(
        enabled: Boolean,
        intervalHours: Int,
        unmetered: Boolean,
        charging: Boolean,
        batteryNotLow: Boolean
    ) {
        viewModelScope.launch {
            prefs.saveAutoSyncSettings(enabled, intervalHours, unmetered, charging, batteryNotLow)
            val context = getApplication<Application>()
            if (enabled) {
                com.teleback.core.service.SyncWorkScheduler.schedulePeriodicSync(
                    context = context,
                    intervalHours = intervalHours,
                    requiresUnmetered = unmetered,
                    requiresCharging = charging,
                    requiresBatteryNotLow = batteryNotLow
                )
                addLog(TerminalLog(level = LogLevel.OK, message = "[SCHED] Periodic auto-sync enabled (Every ${intervalHours}h)."))
            } else {
                com.teleback.core.service.SyncWorkScheduler.cancelPeriodicSync(context)
                addLog(TerminalLog(level = LogLevel.INFO, message = "[SCHED] Periodic auto-sync disabled."))
            }
        }
    }

    fun triggerInstantBackgroundSync() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            com.teleback.core.service.SyncWorkScheduler.enqueueOneTimeSync(context)
            addLog(TerminalLog(level = LogLevel.INFO, message = "[SCHED] Instant background sync enqueued with WorkManager."))
        }
    }

    private fun printBanner() {
        val banner = listOf(
            TerminalLog(level = LogLevel.PLAIN, message = "=================================================="),
            TerminalLog(level = LogLevel.OK,    message = " TeleBack v${com.teleback.BuildConfig.VERSION_NAME} — Decentralized Telegram Cloud Storage"),
            TerminalLog(level = LogLevel.INFO,  message = " Target: Android 15+ | Zero-Signup | AES-256-GCM"),
            TerminalLog(level = LogLevel.PLAIN, message = "=================================================="),
            TerminalLog(level = LogLevel.INFO,  message = "Type a number [1-8, 0] or tap a 2-column chip to run."),
            TerminalLog(level = LogLevel.PLAIN, message = "Tap [8] for Settings, [0] or 'setup' for config wizard.")
        )
        _logs.value = banner
    }

    fun addLog(log: TerminalLog) {
        _logs.value = _logs.value + log
        com.teleback.core.diagnostics.CrashHandler.recordLog("[${log.level.tag}] ${log.message}")
    }

    fun executeInput(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        addLog(TerminalLog(level = LogLevel.CMD, message = "> $trimmed"))

        if (trimmed.equals("help", ignoreCase = true) || trimmed == "?") {
            showHelp()
            return
        }

        // Settings sub-view context handling
        if (_isSettingsView.value) {
            when (trimmed.lowercase()) {
                "1", "theme", "themes", "custom" -> {
                    _showCustomization.value = true
                    return
                }
                "2", "keys", "key", "crypto" -> {
                    _showKeysDialog.value = true
                    return
                }
                "3", "auto", "schedule", "sched", "cron" -> {
                    _showScheduleDialog.value = true
                    return
                }
                "4", "report", "feedback", "bug" -> {
                    _showFeedbackDialog.value = true
                    return
                }
                "<", "back", "exit", "q" -> {
                    closeSettingsView()
                    return
                }
            }
        }

        if (trimmed.equals("back", ignoreCase = true) || trimmed.equals("exit", ignoreCase = true) || trimmed == "<") {
            if (_isSettingsView.value) {
                closeSettingsView()
                return
            }
        }

        if (trimmed.equals("report", ignoreCase = true) || trimmed.equals("feedback", ignoreCase = true) || trimmed.equals("bug", ignoreCase = true)) {
            _showFeedbackDialog.value = true
            return
        }

        if (trimmed.equals("schedule", ignoreCase = true) || trimmed.equals("cron", ignoreCase = true) || trimmed.equals("auto", ignoreCase = true)) {
            _showScheduleDialog.value = true
            return
        }

        if (trimmed.equals("theme", ignoreCase = true) || trimmed.equals("themes", ignoreCase = true) || trimmed.equals("custom", ignoreCase = true)) {
            _showCustomization.value = true
            return
        }

        if (trimmed.equals("keys", ignoreCase = true) || trimmed.equals("key", ignoreCase = true) || trimmed.equals("crypto", ignoreCase = true)) {
            _showKeysDialog.value = true
            return
        }

        val command = CliCommand.resolve(trimmed)
        if (command == null) {
            addLog(TerminalLog(level = LogLevel.ERR, message = "Unknown command: '$trimmed'. Type 'help' for command list."))
            return
        }

        executeCommand(command, trimmed)
    }

    fun executeCommand(command: CliCommand, rawInput: String = "") {
        when (command) {
            CliCommand.SYNC -> requestSyncPrompt(isFull = false)
            CliCommand.BACKUP -> requestSyncPrompt(isFull = true)
            CliCommand.RESTORE -> {
                // If user passed a snapshot id: e.g. "3 snap_20261018"
                val tokens = rawInput.split("\\s+".toRegex())
                if (tokens.size > 1 && tokens[1].startsWith("snap_")) {
                    runRestore(tokens[1])
                } else {
                    _showSnapshotPicker.value = true
                }
            }
            CliCommand.CLOUD -> {
                _showCloudManager.value = true
            }
            CliCommand.STATUS -> runStatusCheck()
            CliCommand.TEST -> runConnectionTest()
            CliCommand.CLEAR -> _logs.value = emptyList()
            CliCommand.SETTINGS -> openSettingsView()
            CliCommand.SETUP -> _showWalkthrough.value = true
        }
    }

    private fun runSync(isFull: Boolean, tag: String? = null) {
        if (_isBusy.value) {
            addLog(TerminalLog(level = LogLevel.WARN, message = "Another operation is already running."))
            return
        }
        _isBusy.value = true
        SyncForegroundService.start(getApplication())

        viewModelScope.launch {
            var isSuccess = false
            var failureMessage: String? = null
            try {
                val result = syncManager.runBackupOrSync(
                    isForceFullBackup = isFull,
                    tag = tag,
                    dryRun = false,
                    onLog = { addLog(it) }
                )
                if (result.isFailure) {
                    val msg = result.exceptionOrNull()?.message ?: "Unknown error"
                    addLog(TerminalLog(level = LogLevel.ERR, message = "Operation failed: $msg"))
                    failureMessage = msg
                } else {
                    isSuccess = true
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Unexpected error"
                addLog(TerminalLog(level = LogLevel.ERR, message = "Error: $msg"))
                failureMessage = msg
            } finally {
                _isBusy.value = false
                SyncForegroundService.stop(getApplication())
                val actionName = if (isFull) "Backup" else "Sync"
                if (isSuccess) {
                    NotificationHelper.notify(getApplication(), "TeleBack $actionName", "$actionName Completed")
                } else {
                    NotificationHelper.notify(getApplication(), "TeleBack $actionName", "$actionName Failed: ${failureMessage ?: "Error"}")
                }
            }
        }
    }

    fun runRestore(snapshotId: String? = null) {
        if (_isBusy.value) {
            addLog(TerminalLog(level = LogLevel.WARN, message = "Another operation is already running."))
            return
        }
        _isBusy.value = true
        SyncForegroundService.start(getApplication())

        viewModelScope.launch {
            var isSuccess = false
            var failureMessage: String? = null
            try {
                val result = syncManager.restoreSnapshot(snapshotId, onLog = { addLog(it) })
                if (result.isFailure) {
                    val msg = result.exceptionOrNull()?.message ?: "Unknown error"
                    addLog(TerminalLog(level = LogLevel.ERR, message = "Restore failed: $msg"))
                    failureMessage = msg
                } else {
                    isSuccess = true
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Unexpected error"
                addLog(TerminalLog(level = LogLevel.ERR, message = "Restore error: $msg"))
                failureMessage = msg
            } finally {
                _isBusy.value = false
                SyncForegroundService.stop(getApplication())
                if (isSuccess) {
                    NotificationHelper.notify(getApplication(), "TeleBack Restore", "Restore Completed")
                } else {
                    NotificationHelper.notify(getApplication(), "TeleBack Restore", "Restore Failed: ${failureMessage ?: "Error"}")
                }
            }
        }
    }

    private fun runStatusCheck() {
        viewModelScope.launch {
            try {
                val diff = syncManager.calculateDiff(onLog = { addLog(it) })
                addLog(TerminalLog(level = LogLevel.OK, message = "Folder state: ${diff.totalLocalFiles} total files"))
                addLog(TerminalLog(level = LogLevel.PLAIN, message = "  + ${diff.addedCount} new files"))
                addLog(TerminalLog(level = LogLevel.PLAIN, message = "  * ${diff.modifiedCount} modified files"))
                addLog(TerminalLog(level = LogLevel.PLAIN, message = "  = ${diff.unchangedCount} in-sync files"))
                addLog(TerminalLog(level = LogLevel.PLAIN, message = "  - ${diff.deletedCount} deleted files"))
            } catch (e: Exception) {
                addLog(TerminalLog(level = LogLevel.ERR, message = "Status check failed: ${e.message}"))
            }
            printCloudUsage()
        }
    }

    /** Prints how much of Telegram the backups occupy. Computed locally, so it also works offline. */
    private suspend fun printCloudUsage() {
        try {
            val usage = syncManager.cloudUsage()
            addLog(TerminalLog(level = LogLevel.SNAP, message = "CLOUD USAGE"))
            if (usage.snapshotCount == 0) {
                addLog(TerminalLog(level = LogLevel.PLAIN, message = "  No snapshots yet. Run [1] sync to create the first one."))
            } else {
                val encrypted = if (usage.isEncrypted) {
                    "yes (+${"%.1f".format(Locale.US, usage.overheadPercent)}% overhead)"
                } else {
                    "no"
                }
                val lastSync = usage.lastSyncMillis?.let {
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it))
                } ?: "never"
                addLog(TerminalLog(level = LogLevel.PLAIN, message = "  Snapshots    : ${usage.snapshotCount} / ${usage.snapshotLimit}"))
                addLog(TerminalLog(level = LogLevel.PLAIN, message = "  Unique files : ${"%,d".format(Locale.US, usage.uniqueFiles)}  (${SizeFormat.format(usage.storedBytes)} stored)"))
                addLog(TerminalLog(level = LogLevel.PLAIN, message = "  Encrypted    : $encrypted"))
                addLog(TerminalLog(level = LogLevel.PLAIN, message = "  Last sync    : $lastSync"))
            }
            val pending = syncManager.pendingUploadParts()
            if (pending > 0) {
                addLog(TerminalLog(level = LogLevel.WARN, message = "Interrupted sync: $pending uploaded part(s) pending. Run [1] sync to resume."))
            }
        } catch (e: Exception) {
            addLog(TerminalLog(level = LogLevel.ERR, message = "Could not read cloud usage: ${e.message}"))
        }
    }

    private fun runConnectionTest() {
        viewModelScope.launch {
            addLog(TerminalLog(level = LogLevel.INFO, message = "Pinging Telegram Bot API..."))
            val result = syncManager.testConnection()
            if (result.isSuccess) {
                addLog(TerminalLog(level = LogLevel.OK, message = result.getOrThrow()))
            } else {
                addLog(TerminalLog(level = LogLevel.ERR, message = "Connection failed: ${result.exceptionOrNull()?.message}"))
            }
        }
    }

    private fun showKeys() {
        viewModelScope.launch {
            val key = prefs.getOrGenerateMasterKey()
            addLog(TerminalLog(level = LogLevel.SNAP, message = "AES-256-GCM Master Key (Base64):"))
            addLog(TerminalLog(level = LogLevel.PLAIN, message = key))
            addLog(TerminalLog(level = LogLevel.WARN, message = "Keep this key secure. If lost, encrypted cloud backups cannot be recovered."))
        }
    }

    private fun showHelp() {
        addLog(TerminalLog(level = LogLevel.INFO, message = "NUMBERED COMMAND REFERENCE:"))
        CliCommand.entries.forEach { cmd ->
            addLog(TerminalLog(level = LogLevel.PLAIN, message = " [${cmd.number}] ${cmd.commandName.padEnd(8)} - ${cmd.description}"))
        }
    }

    fun dismissOnboarding() { _showOnboarding.value = false }
    fun dismissWalkthrough() { _showWalkthrough.value = false }

    /** Walkthrough finished (or skipped): continue straight into the setup wizard. */
    fun continueWalkthroughToSetup() {
        _showWalkthrough.value = false
        _showOnboarding.value = true
    }

    /** "Need help?" from inside the setup wizard: swap back to the visual guide. */
    fun openWalkthroughFromSetup() {
        _showOnboarding.value = false
        _showWalkthrough.value = true
    }
    fun dismissSnapshotPicker() { _showSnapshotPicker.value = false }
    fun dismissCustomization() { _showCustomization.value = false }
    fun dismissKeysDialog() { _showKeysDialog.value = false }

    fun finishOnboarding(derivedKey: String?, isExistingVault: Boolean) {
        _showOnboarding.value = false
        viewModelScope.launch {
            if (!derivedKey.isNullOrBlank()) {
                prefs.saveMasterKey(derivedKey)
                addLog(TerminalLog(level = LogLevel.OK, message = "Master Key configured successfully."))
            }
            // Always trigger default cloud scan right after setup is done
            addLog(TerminalLog(level = LogLevel.INFO, message = "Running initial cloud scan to check for existing backups..."))
            try {
                val result = syncManager.scanCloudSnapshots { log -> addLog(log) }
                if (result.isSuccess) {
                    val count = result.getOrNull()?.size ?: 0
                    addLog(TerminalLog(level = LogLevel.OK, message = "Cloud scan complete. $count snapshot(s) available in vault."))
                } else {
                    addLog(TerminalLog(level = LogLevel.WARN, message = "Cloud scan: ${result.exceptionOrNull()?.message ?: "No existing backups found."}"))
                }
            } catch (e: Exception) {
                addLog(TerminalLog(level = LogLevel.WARN, message = "Cloud scan notice: ${e.message}"))
            }
        }
    }

    fun setTheme(themeId: String) {
        viewModelScope.launch {
            prefs.saveThemeId(themeId)
            addLog(TerminalLog(level = LogLevel.OK, message = "Theme switched to '$themeId'."))
        }
    }

    fun updateMasterKey(newKey: String, triggerScan: Boolean = true) {
        viewModelScope.launch {
            prefs.saveMasterKey(newKey)
            addLog(TerminalLog(level = LogLevel.OK, message = "Master Key updated successfully."))
            if (triggerScan) {
                addLog(TerminalLog(level = LogLevel.INFO, message = "Scanning cloud with new key..."))
                val result = syncManager.scanCloudSnapshots { log -> addLog(log) }
                if (result.isSuccess) {
                    addLog(TerminalLog(level = LogLevel.OK, message = "Cloud catalog updated (${result.getOrNull()?.size ?: 0} snapshots total)."))
                }
            }
        }
    }

    fun saveBotToken(token: String) {
        viewModelScope.launch {
            prefs.saveBotToken(token)
            addLog(TerminalLog(level = LogLevel.OK, message = "Bot token saved."))
            runConnectionTest()
        }
    }

    fun detectChat() {
        viewModelScope.launch {
            addLog(TerminalLog(level = LogLevel.INFO, message = "Checking for recent messages in Telegram group..."))
            val result = syncManager.detectChatId()
            if (result.isSuccess) {
                val (id, title) = result.getOrThrow()
                addLog(TerminalLog(level = LogLevel.OK, message = "Linked to Group: '$title' (ID: $id)"))
            } else {
                addLog(TerminalLog(level = LogLevel.ERR, message = result.exceptionOrNull()?.message ?: "Failed"))
            }
        }
    }

    fun saveFolder(uriString: String, name: String) {
        viewModelScope.launch {
            prefs.saveFolder(uriString, name)
            addLog(TerminalLog(level = LogLevel.OK, message = "Target folder linked: $name"))
        }
    }

    fun dismissCloudManager() {
        _showCloudManager.value = false
    }

    fun openCloudManager() {
        _showCloudManager.value = true
    }

    fun scanCloudSnapshots() {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            try {
                val result = syncManager.scanCloudSnapshots { log -> addLog(log) }
                if (result.isSuccess) {
                    addLog(TerminalLog(level = LogLevel.OK, message = "Cloud catalog synchronized (${result.getOrNull()?.size ?: 0} snapshots total)."))
                } else {
                    addLog(TerminalLog(level = LogLevel.ERR, message = "Cloud scan failed: ${result.exceptionOrNull()?.message}"))
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun deleteCloudSnapshots(snapshotIds: List<String>) {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            try {
                val result = syncManager.deleteCloudSnapshots(snapshotIds) { log -> addLog(log) }
                if (result.isFailure) {
                    addLog(TerminalLog(level = LogLevel.ERR, message = "Delete failed: ${result.exceptionOrNull()?.message}"))
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun wipeCloudVault() {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            try {
                val result = syncManager.wipeAllCloudVault { log -> addLog(log) }
                if (result.isFailure) {
                    addLog(TerminalLog(level = LogLevel.ERR, message = "Vault wipe failed: ${result.exceptionOrNull()?.message}"))
                }
            } finally {
                _isBusy.value = false
            }
        }
    }
}
