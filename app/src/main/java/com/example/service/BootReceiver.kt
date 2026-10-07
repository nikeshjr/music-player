package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.core.di.AuraServiceLocator
import com.example.core.logger.AuraLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            AuraLog.i(TAG, "Device boot completed received.")
            if (Settings.canDrawOverlays(context)) {
                CoroutineScope(Dispatchers.IO).launch {
                    val prefs = AuraServiceLocator.providePreferences(context)
                    val isEnabled = prefs.isIslandEnabled.first()
                    if (isEnabled) {
                        AuraLog.i(TAG, "Restoring Dynamic Island overlay after boot.")
                        DynamicIslandService.start(context)
                    }
                }
            } else {
                AuraLog.w(TAG, "Overlay permission not granted; skipping boot restart.")
            }
        }
    }
}
