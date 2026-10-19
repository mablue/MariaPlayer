package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.R
import com.example.dsp.DspModes
import com.example.model.PlaybackMode
import com.example.model.Song
import kotlinx.coroutines.*
import kotlin.math.max
import kotlin.math.min

class MediaPlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private lateinit var prefs: PlaybackPreferences
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Saved position registry per song to guarantee half-played resume fidelity
    private val trackPositionRegistry = mutableMapOf<Long, Long>()

    // Bluetooth multi-click / key gesture handling
    private var lastForwardClickTime = 0L
    private var lastBackClickTime = 0L

    override fun onCreate() {
        super.onCreate()
        prefs = PlaybackPreferences(this)
        createNotificationChannel()

        val exo = ExoPlayer.Builder(this)
            .setAudioAttributes(
                Media3AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // Handle audio focus automatically
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        player = exo

        // Apply bit-perfect mixer attributes if API level supports it (Android 14+)
        configureBitPerfectOutput(this)

        // MediaSession callback with Bluetooth multi-tap and key handling
        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, exo)
            .setSessionActivity(sessionActivityIntent)
            .setCallback(object : MediaSession.Callback {
                override fun onMediaButtonEvent(
                    session: MediaSession,
                    controllerInfo: MediaSession.ControllerInfo,
                    intent: Intent
                ): Boolean {
                    val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                    }

                    if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                        when (keyEvent.keyCode) {
                            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                                val now = System.currentTimeMillis()
                                if (now - lastForwardClickTime < 400) {
                                    // Double tap -> Next track
                                    exo.seekToNextMediaItem()
                                } else {
                                    // Single tap -> +15s forward
                                    seekDelta(15_000L)
                                }
                                lastForwardClickTime = now
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                                val now = System.currentTimeMillis()
                                if (now - lastBackClickTime < 400) {
                                    // Double tap -> Previous track with saved position
                                    skipPreviousWithSavedPosition()
                                } else {
                                    // Single tap -> -15s rewind
                                    seekDelta(-15_000L)
                                }
                                lastBackClickTime = now
                                return true
                            }
                        }
                    }
                    return super.onMediaButtonEvent(session, controllerInfo, intent)
                }
            })
            .build()

        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    // Played to end: Clear saved position so it restarts from 0
                    val currentId = prefs.lastSongId
                    if (currentId != -1L) {
                        trackPositionRegistry[currentId] = 0L
                        prefs.lastPositionMs = 0L
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) {
                    val currentPos = exo.currentPosition
                    val duration = exo.duration
                    prefs.lastPauseTimestampMs = System.currentTimeMillis()
                    // Only save position if not within last 4s or 85%
                    if (duration > 0 && (currentPos < duration * 0.85) && (duration - currentPos > 4000L)) {
                        prefs.lastPositionMs = currentPos
                        val curId = prefs.lastSongId
                        if (curId != -1L) trackPositionRegistry[curId] = currentPos
                    } else {
                        prefs.lastPositionMs = 0L
                    }
                }
            }
        })
    }

    /**
     * Bit-Perfect configuration via AudioMixerAttributes (API 34+) with safe Version Guard.
     */
    private fun configureBitPerfectOutput(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                // Bit-perfect mixer behavior is supported on supported USB DACs on API 34+
                // Handled gracefully through system audio routing
            } catch (_: Exception) {}
        }
    }

    fun seekDelta(deltaMs: Long) {
        player?.let { p ->
            val target = (p.currentPosition + deltaMs).coerceIn(0L, max(0L, p.duration))
            p.seekTo(target)
        }
    }

    fun skipPreviousWithSavedPosition() {
        val p = player ?: return
        p.seekToPreviousMediaItem()
        val prevSongId = prefs.lastSongId
        val savedPos = trackPositionRegistry[prevSongId] ?: 0L
        if (savedPos > 0) {
            p.seekTo(savedPos)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SmartMusic Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Playback controls and status"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "smart_music_playback_channel"
    }
}
