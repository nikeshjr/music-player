package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.core.di.AuraServiceLocator
import com.example.core.logger.AuraLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BootReceiver: Restores ONLY the Dynamic Island overlay on device boot, if enabled
 * and permissions are granted. Strictly avoids launching media playback on boot.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AuraLog.i("BootReceiver", "Device boot completed received.")

            // Check if overlay permission is granted
            if (Settings.canDrawOverlays(context)) {
                CoroutineScope(Dispatchers.IO).launch {
                    val prefs = AuraServiceLocator.providePreferences(context)
                    val isEnabled = prefs.isIslandEnabled.first()
                    if (isEnabled) {
                        AuraLog.i("BootReceiver", "Restoring Dynamic Island overlay after boot.")
                        DynamicIslandService.start(context)
                    }
                }
            } else {
                AuraLog.w("BootReceiver", "Overlay permission not granted; skipping boot restart.")
            }
        }
    }
}
