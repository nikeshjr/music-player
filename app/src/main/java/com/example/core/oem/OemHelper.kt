package com.example.core.oem

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import com.example.core.logger.AuraLog
import java.util.Locale

object OemHelper {
    private const val TAG = "OemHelper"

    val manufacturer: String = Build.MANUFACTURER.lowercase(Locale.ROOT)
    val brand: String = Build.BRAND.lowercase(Locale.ROOT)

    val isVivoOrIqoo: Boolean
        get() = manufacturer.contains("vivo") || manufacturer.contains("iqoo") ||
                brand.contains("vivo") || brand.contains("iqoo")


    fun isXiaomi(): Boolean {
        return manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco")
    }

    fun isOppoOrRealme(): Boolean {
        return manufacturer.contains("oppo") || manufacturer.contains("realme")
    }

    fun isSamsung(): Boolean {
        return manufacturer.contains("samsung")
    }

    fun isOnePlus(): Boolean {
        return manufacturer.contains("oneplus")
    }

    fun isHuaweiOrHonor(): Boolean {
        return manufacturer.contains("huawei") || manufacturer.contains("honor")
    }

    fun canResolve(context: Context, intent: Intent): Boolean {
        return try {
            intent.resolveActivity(context.packageManager) != null
        } catch (e: Exception) {
            AuraLog.e(TAG, "Error checking intent resolution: ${e.message}")
            false
        }
    }

    fun safeStartActivity(context: Context, intent: Intent): Boolean {
        return try {
            if (canResolve(context, intent)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                AuraLog.w(TAG, "Intent not resolvable on this device: $intent")
                false
            }
        } catch (e: Exception) {
            AuraLog.e(TAG, "Exception launching intent: ${e.message}")
            false
        }
    }

    fun openAppDetailsSettings(context: Context) {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        safeStartActivity(context, intent)
    }

    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (!safeStartActivity(context, intent)) {
            val fallback = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            safeStartActivity(context, fallback)
        }
    }

    fun openVivoBackgroundPopups(context: Context) {
        val intent = Intent().apply {
            component = ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.PurviewTabActivity")
            putExtra("packagename", context.packageName)
            putExtra("tabId", "1")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (!safeStartActivity(context, intent)) {
            val intent2 = Intent("com.iqoo.secure").apply {
                component = ComponentName("com.iqoo.secure", "com.iqoo.secure.safeguard.PurviewTabActivity")
                putExtra("packagename", context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (!safeStartActivity(context, intent2)) {
                openAppDetailsSettings(context)
            }
        }
    }

    fun openAutostartSettings(context: Context) {
        val autostartIntents = listOf(
            Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
            Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.oplus.battery", "com.oplus.battery.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
            Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"))
        )
        for (intent in autostartIntents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (safeStartActivity(context, intent)) {
                return
            }
        }
        openAppDetailsSettings(context)
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true) {
            AuraLog.i(TAG, "Battery optimizations already ignored.")
            return
        }
        val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (!safeStartActivity(context, intent)) {
            val fallback = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            safeStartActivity(context, fallback)
        }
    }
}
