package com.example

import android.app.Application
import com.example.service.PlaybackPreferences

class SmartMusicApp : Application() {

    lateinit var playbackPreferences: PlaybackPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        playbackPreferences = PlaybackPreferences(this)
    }

    companion object {
        lateinit var instance: SmartMusicApp
            private set
    }
}
