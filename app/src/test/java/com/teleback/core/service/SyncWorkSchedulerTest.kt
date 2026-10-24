package com.teleback.core.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SyncWorkSchedulerTest {

    @Test
    fun testBroadcastReceiverActionName() {
        assertEquals("com.teleback.action.TRIGGER_SYNC", SyncBroadcastReceiver.ACTION_TRIGGER_SYNC)
    }

    @Test
    fun testWorkConstants() {
        assertEquals("teleback_periodic_sync", TeleBackSyncWorker.WORK_NAME_PERIODIC)
        assertEquals("teleback_one_time_sync", TeleBackSyncWorker.WORK_NAME_ONE_TIME)
    }
}
