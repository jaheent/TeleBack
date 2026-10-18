package com.teleback.core.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object IconSwitcher {
    private const val ALIAS_A = "com.teleback.MainActivity"
    private const val ALIAS_B = "com.teleback.MainActivityAliasB"
    private const val ALIAS_C = "com.teleback.MainActivityAliasC"

    fun switchIcon(context: Context, iconId: String) {
        val targetAlias = when (iconId) {
            "icon_b" -> ALIAS_B
            "icon_c" -> ALIAS_C
            else -> ALIAS_A
        }

        val pm = context.packageManager
        val aliases = listOf(ALIAS_A, ALIAS_B, ALIAS_C)

        for (alias in aliases) {
            val component = ComponentName(context, alias)
            val newState = if (alias == targetAlias) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            try {
                pm.setComponentEnabledSetting(
                    component,
                    newState,
                    PackageManager.DONT_KILL_APP
                )
            } catch (_: Exception) {
                // Graceful fallback on restricted or test environments
            }
        }
    }
}
