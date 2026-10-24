package com.teleback

import android.app.Application
import com.teleback.core.diagnostics.CrashHandler

class TeleBackApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.init(this)
    }
}
