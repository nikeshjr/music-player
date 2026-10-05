package com.example.service

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.example.core.logger.AuraLog

/**
 * HeadsetReceiver: Listens for wired headphone connections and Bluetooth headset events.
 * Triggers audio pause when disconnected (preventing accidental speaker broadcasts)
 * and notifies AuraPlayerController of output device changes.
 */
class HeadsetReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "HeadsetReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val controller = AuraPlayerController.getInstance(context)

        when (intent.action) {
            AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                AuraLog.i(TAG, "Audio becoming noisy (headset unplugged). Pausing playback.")
                controller.pause()
            }
            Intent.ACTION_HEADSET_PLUG -> {
                val state = intent.getIntExtra("state", -1)
                val name = intent.getStringExtra("name") ?: "Wired Headset"
                when (state) {
                    0 -> AuraLog.i(TAG, "Headset unplugged: $name")
                    1 -> AuraLog.i(TAG, "Headset plugged in: $name")
                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                AuraLog.i(TAG, "Bluetooth audio device disconnected. Pausing playback.")
                controller.pause()
            }
        }
    }
}
