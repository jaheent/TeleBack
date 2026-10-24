package com.teleback.core.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SyncBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_SYNC = "com.teleback.action.TRIGGER_SYNC"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_TRIGGER_SYNC) {
            SyncWorkScheduler.enqueueOneTimeSync(context)
        }
    }
}
