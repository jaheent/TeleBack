package com.teleback.core.cli

enum class CliCommand(
    val number: Int,
    val commandName: String,
    val aliases: List<String>,
    val shortLabel: String,
    val description: String,
    val usage: String
) {
    SYNC(
        number = 1,
        commandName = "sync",
        aliases = listOf("s", "diff-sync"),
        shortLabel = "Diff update",
        description = "Incremental diff sync to cloud",
        usage = "1  or  sync [--dry-run]"
    ),
    BACKUP(
        number = 2,
        commandName = "backup",
        aliases = listOf("b", "push"),
        shortLabel = "Full backup",
        description = "Force complete folder backup",
        usage = "2  or  backup"
    ),
    RESTORE(
        number = 3,
        commandName = "restore",
        aliases = listOf("r", "pull", "download"),
        shortLabel = "Pick snapshot",
        description = "Browse & download snapshot",
        usage = "3  or  restore [snapshot_id]"
    ),
    SNAPSHOT(
        number = 4,
        commandName = "snapshot",
        aliases = listOf("snap", "tag"),
        shortLabel = "Tagged version",
        description = "Create tagged version point",
        usage = "4  or  snapshot -m \"Label\""
    ),
    STATUS(
        number = 5,
        commandName = "status",
        aliases = listOf("st", "diff"),
        shortLabel = "Diff & folder",
        description = "Show target folder diff & state",
        usage = "5  or  status"
    ),
    TEST(
        number = 6,
        commandName = "test",
        aliases = listOf("ping", "health"),
        shortLabel = "Bot & group",
        description = "Test bot & Telegram group link",
        usage = "6  or  test"
    ),
    CUSTOM(
        number = 7,
        commandName = "custom",
        aliases = listOf("theme", "themes", "icon", "icons", "style"),
        shortLabel = "Themes & icons",
        description = "Select themes and visual icons",
        usage = "7  or  custom"
    ),
    KEYS(
        number = 8,
        commandName = "keys",
        aliases = listOf("key", "crypto"),
        shortLabel = "Export / import",
        description = "View / export AES-256 master key",
        usage = "8  or  keys"
    ),
    CLEAR(
        number = 9,
        commandName = "clear",
        aliases = listOf("cls", "c"),
        shortLabel = "Clear console",
        description = "Clear terminal console output",
        usage = "9  or  clear"
    ),
    SETUP(
        number = 0,
        commandName = "setup",
        aliases = listOf("init", "config", "wizard"),
        shortLabel = "Setup & config",
        description = "Interactive setup & folder picker",
        usage = "0  or  setup"
    );

    companion object {
        fun resolve(input: String): CliCommand? {
            val clean = input.trim().lowercase()
            if (clean.isEmpty()) return null

            // Check if input is a digit: e.g. "1" or "0"
            val firstToken = clean.split("\\s+".toRegex())[0]
            firstToken.toIntOrNull()?.let { num ->
                entries.firstOrNull { it.number == num }?.let { return it }
            }

            // Check command name or aliases
            return entries.firstOrNull { cmd ->
                cmd.commandName == firstToken || cmd.aliases.contains(firstToken)
            }
        }
    }
}
