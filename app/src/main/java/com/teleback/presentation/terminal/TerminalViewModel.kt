package com.teleback.presentation.terminal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teleback.core.cli.CliCommand
import com.teleback.core.cli.LogLevel
import com.teleback.core.cli.TerminalLog
import com.teleback.core.service.SyncForegroundService
import com.teleback.data.local.AppDatabase
import com.teleback.data.local.AppPreferences
import com.teleback.data.local.SnapshotEntity
import com.teleback.domain.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TerminalViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AppPreferences(application)
    private val syncManager = SyncManager(application)
    private val db = AppDatabase.getDatabase(application)

    val botToken = prefs.botToken.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val chatTitle = prefs.chatTitle.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val folderName = prefs.folderName.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val isEncryptionEnabled = prefs.isEncryptionEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val masterKey = prefs.masterKey.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val snapshots = db.snapshotDao().getAllSnapshots().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _logs = MutableStateFlow<List<TerminalLog>>(emptyList())
    val logs: StateFlow<List<TerminalLog>> = _logs.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _showOnboarding = MutableStateFlow(false)
    val showOnboarding: StateFlow<Boolean> = _showOnboarding.asStateFlow()

    private val _showSnapshotPicker = MutableStateFlow(false)
    val showSnapshotPicker: StateFlow<Boolean> = _showSnapshotPicker.asStateFlow()

    init {
        printBanner()
    }

    private fun printBanner() {
        val banner = listOf(
            TerminalLog(level = LogLevel.PLAIN, message = "=================================================="),
            TerminalLog(level = LogLevel.OK,    message = " TeleBack v1.0 — Decentralized Telegram Cloud Storage"),
            TerminalLog(level = LogLevel.INFO,  message = " Target: Android 15+ | Zero-Signup | AES-256-GCM"),
            TerminalLog(level = LogLevel.PLAIN, message = "=================================================="),
            TerminalLog(level = LogLevel.INFO,  message = "Type a number [1-9] or tap a quick chip to run."),
            TerminalLog(level = LogLevel.PLAIN, message = "Type [7] or 'init' to configure bot & folder.")
        )
        _logs.value = banner
    }

    fun addLog(log: TerminalLog) {
        _logs.value = _logs.value + log
    }

    fun executeInput(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        addLog(TerminalLog(level = LogLevel.CMD, message = "> $trimmed"))

        val command = CliCommand.resolve(trimmed)
        if (command == null) {
            addLog(TerminalLog(level = LogLevel.ERR, message = "Unknown command: '$trimmed'. Type '0' or 'help' for command list."))
            return
        }

        executeCommand(command, trimmed)
    }

    fun executeCommand(command: CliCommand, rawInput: String = "") {
        when (command) {
            CliCommand.SYNC -> runSync(isFull = false)
            CliCommand.BACKUP -> runSync(isFull = true)
            CliCommand.RESTORE -> {
                // If user passed a snapshot id: e.g. "3 snap_20261018"
                val tokens = rawInput.split("\\s+".toRegex())
                if (tokens.size > 1 && tokens[1].startsWith("snap_")) {
                    runRestore(tokens[1])
                } else {
                    _showSnapshotPicker.value = true
                }
            }
            CliCommand.SNAPSHOT -> {
                val tag = rawInput.substringAfter("-m", "").trim().removeSurrounding("\"").ifEmpty { "Manual" }
                runSync(isFull = false, tag = tag)
            }
            CliCommand.STATUS -> runStatusCheck()
            CliCommand.TEST -> runConnectionTest()
            CliCommand.INIT -> _showOnboarding.value = true
            CliCommand.KEYS -> showKeys()
            CliCommand.CLEAR -> _logs.value = emptyList()
            CliCommand.HELP -> showHelp()
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
            try {
                val result = syncManager.runBackupOrSync(
                    isForceFullBackup = isFull,
                    tag = tag,
                    dryRun = false,
                    onLog = { addLog(it) }
                )
                if (result.isFailure) {
                    addLog(TerminalLog(level = LogLevel.ERR, message = "Operation failed: ${result.exceptionOrNull()?.message}"))
                }
            } catch (e: Exception) {
                addLog(TerminalLog(level = LogLevel.ERR, message = "Error: ${e.message}"))
            } finally {
                _isBusy.value = false
                SyncForegroundService.stop(getApplication())
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
            try {
                val result = syncManager.restoreSnapshot(snapshotId, onLog = { addLog(it) })
                if (result.isFailure) {
                    addLog(TerminalLog(level = LogLevel.ERR, message = "Restore failed: ${result.exceptionOrNull()?.message}"))
                }
            } catch (e: Exception) {
                addLog(TerminalLog(level = LogLevel.ERR, message = "Restore error: ${e.message}"))
            } finally {
                _isBusy.value = false
                SyncForegroundService.stop(getApplication())
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
    fun dismissSnapshotPicker() { _showSnapshotPicker.value = false }

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
}
