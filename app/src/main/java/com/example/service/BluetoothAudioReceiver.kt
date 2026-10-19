package com.example.service

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log

class BluetoothAudioReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val prefs = PlaybackPreferences(context)

        when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                Log.d("BluetoothAudioReceiver", "Bluetooth connected")
                if (prefs.isBluetoothAutoplayEnabled) {
                    val resumeIntent = Intent(ACTION_RESUME_PLAYBACK).apply {
                        setPackage(context.packageName)
                    }
                    context.sendBroadcast(resumeIntent)
                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED, AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                Log.d("BluetoothAudioReceiver", "Audio device disconnected or becoming noisy -> pause")
                val pauseIntent = Intent(ACTION_PAUSE_PLAYBACK).apply {
                    setPackage(context.packageName)
                }
                context.sendBroadcast(pauseIntent)
            }
        }
    }

    companion object {
        const val ACTION_RESUME_PLAYBACK = "com.example.smartmusic.ACTION_RESUME_PLAYBACK"
        const val ACTION_PAUSE_PLAYBACK = "com.example.smartmusic.ACTION_PAUSE_PLAYBACK"
    }
}
