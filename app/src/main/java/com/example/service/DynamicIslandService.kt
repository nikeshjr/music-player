package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.core.di.AuraServiceLocator
import com.example.core.logger.AuraLog
import com.example.core.theme.AuraThemeProvider
import com.example.core.theme.AuraThemeState
import com.example.service.island.CutoutAlignment
import com.example.service.island.DynamicIslandContent
import com.example.service.island.IslandLayoutConfig
import com.example.service.island.IslandState

/**
 * DynamicIslandService: Floating window overlay service.
 * Renders the Universal Dynamic Island above all applications using TYPE_APPLICATION_OVERLAY.
 * Runs as a foreground service with specialUse type for uninterrupted media visualization.
 */
class DynamicIslandService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        private const val TAG = "DynamicIslandService"
        private const val NOTIFICATION_ID = 2002
        private const val CHANNEL_ID = "aura_dynamic_island_channel"

        fun start(context: Context) {
            val intent = Intent(context, DynamicIslandService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, DynamicIslandService::class.java)
            context.stopService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val appViewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = appViewModelStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var islandState by mutableStateOf(IslandState.IDLE_PILL)
    private var layoutConfig by mutableStateOf(IslandLayoutConfig())

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        if (!Settings.canDrawOverlays(this)) {
            AuraLog.w(TAG, "Cannot start Dynamic Island: SYSTEM_ALERT_WINDOW permission missing.")
            stopSelf()
            return
        }

        initOverlayWindow()
        AuraLog.i(TAG, "DynamicIslandService running.")
    }

    private fun initOverlayWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val gravity = when (layoutConfig.cutoutAlignment) {
            CutoutAlignment.CENTER -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
            CutoutAlignment.LEFT -> Gravity.TOP or Gravity.START
            CutoutAlignment.RIGHT -> Gravity.TOP or Gravity.END
        }

        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
            this.y = (layoutConfig.offsetY * resources.displayMetrics.density).toInt()
            this.x = (layoutConfig.offsetX * resources.displayMetrics.density).toInt()
        }
        windowLayoutParams = params

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DynamicIslandService)
            setViewTreeViewModelStoreOwner(this@DynamicIslandService)
            setViewTreeSavedStateRegistryOwner(this@DynamicIslandService)

            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    if (islandState == IslandState.LARGE_CARD) {
                        islandState = IslandState.COMPACT
                        updateWindowFlags(isExpanded = false)
                    }
                    true
                } else false
            }

            setContent {
                val preferences = AuraServiceLocator.providePreferences(this@DynamicIslandService)
                val themeState by preferences.themeState.collectAsState(initial = AuraThemeState())
                val playerController = AuraPlayerController.getInstance(this@DynamicIslandService)
                val uiState by playerController.uiState.collectAsState()

                AuraThemeProvider(themeState = themeState) {
                    DynamicIslandContent(
                        uiState = uiState,
                        layoutConfig = layoutConfig,
                        currentState = islandState,
                        onStateChange = { newState ->
                            islandState = newState
                            updateWindowFlags(isExpanded = newState == IslandState.LARGE_CARD)
                        },
                        onPlayPause = { playerController.togglePlayPause() },
                        onNext = { playerController.next() },
                        onPrevious = { playerController.previous() },
                        onSeek = { playerController.seekToProgress(it) },
                        onToggleFavorite = { playerController.toggleFavorite() },
                        onDismissPill = {
                            overlayView?.visibility = View.GONE
                        },
                        onOpenApp = {
                            val intent = Intent(this@DynamicIslandService, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            startActivity(intent)
                            islandState = IslandState.COMPACT
                            updateWindowFlags(isExpanded = false)
                        }
                    )
                }
            }
        }

        overlayView = composeView
        windowManager?.addView(composeView, params)
    }

    private fun updateWindowFlags(isExpanded: Boolean) {
        val params = windowLayoutParams ?: return
        if (isExpanded) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        } else {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv()
        }
        windowManager?.updateViewLayout(overlayView, params)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Aura Dynamic Island",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps the Universal Dynamic Island floating overlay responsive."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Dynamic Island Active")
            .setContentText("Aura floating controls running")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    override fun onDestroy() {
        AuraLog.i(TAG, "Stopping DynamicIslandService...")
        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        appViewModelStore.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
