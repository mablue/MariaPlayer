package com.example.service

import android.content.Context
import android.content.SharedPreferences
import com.example.dsp.DspModes
import com.example.model.PlaybackMode

class PlaybackPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("smart_music_prefs", Context.MODE_PRIVATE)

    var lastSongId: Long
        get() = prefs.getLong(KEY_LAST_SONG_ID, -1L)
        set(value) = prefs.edit().putLong(KEY_LAST_SONG_ID, value).apply()

    var lastPositionMs: Long
        get() = prefs.getLong(KEY_LAST_POSITION_MS, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_POSITION_MS, value).apply()

    var lastPauseTimestampMs: Long
        get() = prefs.getLong(KEY_LAST_PAUSE_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_PAUSE_TIMESTAMP, value).apply()

    var playbackMode: PlaybackMode
        get() = PlaybackMode.valueOf(prefs.getString(KEY_PLAYBACK_MODE, PlaybackMode.OFF.name) ?: PlaybackMode.OFF.name)
        set(value) = prefs.edit().putString(KEY_PLAYBACK_MODE, value.name).apply()

    var playbackSpeed: Float
        get() = prefs.getFloat(KEY_PLAYBACK_SPEED, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_PLAYBACK_SPEED, value).apply()

    var isBluetoothAutoplayEnabled: Boolean
        get() = prefs.getBoolean(KEY_BT_AUTOPLAY, false)
        set(value) = prefs.edit().putBoolean(KEY_BT_AUTOPLAY, value).apply()

    var isCarModeActive: Boolean
        get() = prefs.getBoolean(KEY_CAR_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_CAR_MODE, value).apply()

    var isPrivateModeActive: Boolean
        get() = prefs.getBoolean(KEY_PRIVATE_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_PRIVATE_MODE, value).apply()

    var isLiquidGlassEnabled: Boolean
        get() = prefs.getBoolean(KEY_LIQUID_GLASS, true)
        set(value) = prefs.edit().putBoolean(KEY_LIQUID_GLASS, value).apply()

    var isSmartRewindEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMART_REWIND, true)
        set(value) = prefs.edit().putBoolean(KEY_SMART_REWIND, value).apply()

    var isLoudnessNormEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOUDNESS_NORM, true)
        set(value) = prefs.edit().putBoolean(KEY_LOUDNESS_NORM, value).apply()

    var isDrcEnabled: Boolean
        get() = prefs.getBoolean(KEY_DRC, false)
        set(value) = prefs.edit().putBoolean(KEY_DRC, value).apply()

    var isHearingGuardEnabled: Boolean
        get() = prefs.getBoolean(KEY_HEARING_GUARD, true)
        set(value) = prefs.edit().putBoolean(KEY_HEARING_GUARD, value).apply()

    var analogProfile: DspModes.AnalogProfile
        get() = DspModes.AnalogProfile.valueOf(prefs.getString(KEY_ANALOG_PROFILE, DspModes.AnalogProfile.OFF.name) ?: DspModes.AnalogProfile.OFF.name)
        set(value) = prefs.edit().putString(KEY_ANALOG_PROFILE, value.name).apply()

    var reverbSpace: DspModes.ReverbSpace
        get() = DspModes.ReverbSpace.valueOf(prefs.getString(KEY_REVERB_SPACE, DspModes.ReverbSpace.OFF.name) ?: DspModes.ReverbSpace.OFF.name)
        set(value) = prefs.edit().putString(KEY_REVERB_SPACE, value.name).apply()

    var isVocalBoostEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOCAL_BOOST, false)
        set(value) = prefs.edit().putBoolean(KEY_VOCAL_BOOST, value).apply()

    fun resetAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_LAST_SONG_ID = "last_song_id"
        private const val KEY_LAST_POSITION_MS = "last_position_ms"
        private const val KEY_LAST_PAUSE_TIMESTAMP = "last_pause_timestamp"
        private const val KEY_PLAYBACK_MODE = "playback_mode"
        private const val KEY_PLAYBACK_SPEED = "playback_speed"
        private const val KEY_BT_AUTOPLAY = "bt_autoplay"
        private const val KEY_CAR_MODE = "car_mode"
        private const val KEY_PRIVATE_MODE = "private_mode"
        private const val KEY_LIQUID_GLASS = "liquid_glass"
        private const val KEY_SMART_REWIND = "smart_rewind"
        private const val KEY_LOUDNESS_NORM = "loudness_norm"
        private const val KEY_DRC = "drc"
        private const val KEY_HEARING_GUARD = "hearing_guard"
        private const val KEY_ANALOG_PROFILE = "analog_profile"
        private const val KEY_REVERB_SPACE = "reverb_space"
        private const val KEY_VOCAL_BOOST = "vocal_boost"
    }
}
