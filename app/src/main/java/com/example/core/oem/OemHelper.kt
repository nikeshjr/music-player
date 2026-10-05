package com.example.core.oem

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.example.core.logger.AuraLog

/**
 * OemHelper: OEM device detection and crash-safe intent resolution.
 * Detects specialized Android distributions (vivo, iQOO, Xiaomi/MIUI, Oppo/ColorOS,
 * Samsung OneUI, OnePlus/OxygenOS, Huawei/EMUI) and provides deep links to background
 * and autostart permission screens to prevent aggressive background killing of the
 * MediaPlayback and DynamicIsland services.
 */
object OemHelper {

    private const val TAG = "OemHelper"

    val manufacturer: String = Build.MANUFACTURER.lowercase()
    val brand: String = Build.BRAND.lowercase()

    val isVivoOrIqoo: Boolean
        get() = manufacturer.contains("vivo") || manufacturer.contains("iqoo") ||
                brand.contains("vivo") || brand.contains("iqoo")

    val isXiaomi: Boolean
        get() = manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco")

    val isOppoOrRealme: Boolean
        get() = manufacturer.contains("oppo") || manufacturer.contains("realme")

    val isSamsung: Boolean
        get() = manufacturer.contains("samsung")

    val isOnePlus: Boolean
        get() = manufacturer.contains("oneplus")

    val isHuaweiOrHonor: Boolean
        get() = manufacturer.contains("huawei") || manufacturer.contains("honor")

    /**
     * Resolves intent safely with PackageManager before launching.
     * Prevents ActivityNotFoundException crashes across all OEM variants.
     */
    fun canResolve(context: Context, intent: Intent): Boolean {
        return try {
            intent.resolveActivity(context.packageManager) != null
        } catch (e: Exception) {
            AuraLog.e(TAG, "Error checking intent resolution: ${e.message}")
            false
        }
    }

    /**
     * Attempts to launch an intent safely; returns true if launched, false if fallback is needed.
     */
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

    /**
     * Standard App Details Settings fallback.
     */
    fun openAppDetailsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        safeStartActivity(context, intent)
    }

    /**
     * Step 1: Manage Overlay Permission (Settings.ACTION_MANAGE_OVERLAY_PERMISSION).
     */
    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        if (!safeStartActivity(context, intent)) {
            // Generic fallback
            val fallback = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            safeStartActivity(context, fallback)
        }
    }

    /**
     * Step 2 (vivo/iQOO): "Display pop-ups while running in the background".
     * vivo permission manager deep link with graceful fallback to app settings.
     */
    fun openVivoBackgroundPopups(context: Context) {
        val intent = Intent().apply {
            component = ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.PurviewTabActivity"
            )
            putExtra("packagename", context.packageName)
            putExtra("tabId", "1")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (!safeStartActivity(context, intent)) {
            // Secondary vivo permission manager path
            val intent2 = Intent("com.iqoo.secure").apply {
                component = ComponentName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.safeguard.PurviewTabActivity"
                )
                putExtra("packagename", context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (!safeStartActivity(context, intent2)) {
                openAppDetailsSettings(context)
            }
        }
    }

    /**
     * Step 3: Autostart / Background Startup.
     */
    fun openAutostartSettings(context: Context) {
        val autostartIntents = listOf(
            // vivo / iQOO
            Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
            Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
            // Xiaomi / MIUI / HyperOS
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            // Oppo / ColorOS
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.oplus.battery", "com.oplus.battery.startup.StartupAppListActivity")),
            // Huawei / Honor
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
            // Samsung
            Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"))
        )

        for (intent in autostartIntents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (safeStartActivity(context, intent)) {
                return
            }
        }

        // Fallback to app details
        openAppDetailsSettings(context)
    }

    /**
     * Step 4: Battery Optimization / Unrestricted background execution.
     */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            AuraLog.i(TAG, "Battery optimizations already ignored.")
            return
        }

        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (!safeStartActivity(context, intent)) {
            val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            safeStartActivity(context, fallback)
        }
    }
}
