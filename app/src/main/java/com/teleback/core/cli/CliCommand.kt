package com.teleback.core.cli

enum class CliCommand(
    val number: Int,
    val commandName: String,
    val aliases: List<String>,
    val description: String,
    val usage: String
) {
    SYNC(
        number = 1,
        commandName = "sync",
        aliases = listOf("s", "diff-sync"),
        description = "Incremental diff sync to cloud",
        usage = "1  or  sync [--dry-run]"
    ),
    BACKUP(
        number = 2,
        commandName = "backup",
        aliases = listOf("b", "push"),
        description = "Force complete folder backup",
        usage = "2  or  backup"
    ),
    RESTORE(
        number = 3,
        commandName = "restore",
        aliases = listOf("r", "pull", "download"),
        description = "Browse & download snapshot",
        usage = "3  or  restore [snapshot_id]"
    ),
    SNAPSHOT(
        number = 4,
        commandName = "snapshot",
        aliases = listOf("snap", "tag"),
        description = "Create tagged version point",
        usage = "4  or  snapshot -m \"Label\""
    ),
    STATUS(
        number = 5,
        commandName = "status",
        aliases = listOf("st", "diff"),
        description = "Show target folder diff & state",
        usage = "5  or  status"
    ),
    TEST(
        number = 6,
        commandName = "test",
        aliases = listOf("ping", "health"),
        description = "Test bot & Telegram group link",
        usage = "6  or  test"
    ),
    INIT(
        number = 7,
        commandName = "init",
        aliases = listOf("setup", "config"),
        description = "Interactive setup & folder picker",
        usage = "7  or  init"
    ),
    KEYS(
        number = 8,
        commandName = "keys",
        aliases = listOf("key", "crypto"),
        description = "View / export AES-256 master key",
        usage = "8  or  keys"
    ),
    CLEAR(
        number = 9,
        commandName = "clear",
        aliases = listOf("cls", "c"),
        description = "Clear terminal console output",
        usage = "9  or  clear"
    ),
    HELP(
        number = 0,
        commandName = "help",
        aliases = listOf("h", "?"),
        description = "Show command syntax & shortcuts",
        usage = "0  or  help"
    );

    companion object {
        fun resolve(input: String): CliCommand? {
            val clean = input.trim().lowercase()
            if (clean.isEmpty()) return null

            // Check if input is a digit: e.g. "1" or "1 --dry-run"
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
