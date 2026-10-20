package com.teleback.core.cli

import org.junit.Assert.assertEquals
import org.junit.Test

class CliCommandTest {

    @Test
    fun testNumericCommandResolution() {
        assertEquals(CliCommand.SYNC, CliCommand.resolve("1"))
        assertEquals(CliCommand.BACKUP, CliCommand.resolve("2"))
        assertEquals(CliCommand.RESTORE, CliCommand.resolve("3"))
        assertEquals(CliCommand.CLOUD, CliCommand.resolve("4"))
        assertEquals(CliCommand.STATUS, CliCommand.resolve("5"))
        assertEquals(CliCommand.TEST, CliCommand.resolve("6"))
        assertEquals(CliCommand.CUSTOM, CliCommand.resolve("7"))
        assertEquals(CliCommand.KEYS, CliCommand.resolve("8"))
        assertEquals(CliCommand.CLEAR, CliCommand.resolve("9"))
        assertEquals(CliCommand.SETUP, CliCommand.resolve("0"))
    }

    @Test
    fun testNamedAndAliasCommandResolution() {
        assertEquals(CliCommand.SYNC, CliCommand.resolve("sync"))
        assertEquals(CliCommand.BACKUP, CliCommand.resolve("backup"))
        assertEquals(CliCommand.BACKUP, CliCommand.resolve("push"))
        assertEquals(CliCommand.RESTORE, CliCommand.resolve("restore"))
        assertEquals(CliCommand.RESTORE, CliCommand.resolve("pull"))
        assertEquals(CliCommand.CLOUD, CliCommand.resolve("cloud"))
        assertEquals(CliCommand.CLOUD, CliCommand.resolve("snaps"))
        assertEquals(CliCommand.STATUS, CliCommand.resolve("diff"))
        assertEquals(CliCommand.TEST, CliCommand.resolve("ping"))
        assertEquals(CliCommand.CUSTOM, CliCommand.resolve("custom"))
        assertEquals(CliCommand.CUSTOM, CliCommand.resolve("theme"))
        assertEquals(CliCommand.KEYS, CliCommand.resolve("keys"))
        assertEquals(CliCommand.CLEAR, CliCommand.resolve("clear"))
        assertEquals(CliCommand.SETUP, CliCommand.resolve("setup"))
        assertEquals(CliCommand.SETUP, CliCommand.resolve("init"))
    }

    @Test
    fun testNumericWithArguments() {
        assertEquals(CliCommand.SYNC, CliCommand.resolve("1 --dry-run"))
        assertEquals(CliCommand.CLOUD, CliCommand.resolve("4"))
    }
}
