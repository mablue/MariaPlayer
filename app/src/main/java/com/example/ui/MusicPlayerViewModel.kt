package com.example.ui

import android.app.Application
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.SmartMusicApp
import com.example.dsp.DspModes
import com.example.lyrics.LyricsPayload
import com.example.lyrics.LyricsService
import com.example.model.*
import com.example.scoring.SmartScoring
import com.example.service.PlaybackPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class MusicUiState(
    val songs: List<Song> = emptyList(),
    val filteredSongs: List<Song> = emptyList(),
    val podcasts: List<PodcastEpisode> = emptyList(),
    val audiobooks: List<AudiobookTrack> = emptyList(),
    val folders: Map<String, List<Song>> = emptyMap(),
    val currentSong: Song? = null,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val playbackMode: PlaybackMode = PlaybackMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val selectedTab: MainTab = MainTab.MUSIC,
    val sortOption: SortOption = SortOption.SMART_RANKING,
    val searchQuery: String = "",
    val selectedSongIds: Set<Long> = emptySet(),
    val isMultiSelectActive: Boolean = false,
    val playlists: Map<String, List<Long>> = mapOf("Favorites" to emptyList(), "Relax & Focus" to emptyList()),
    val currentPlaylistName: String? = null,
    val isCarMode: Boolean = false,
    val isSleepTimerActive: Boolean = false,
    val sleepTimerMinutesLeft: Int = 0,
    val isAtmospherePlaying: Boolean = false,
    val atmosphereType: DspModes.AtmosphereType = DspModes.AtmosphereType.OFF,
    val atmosphereVolumeRatio: Float = 0.25f,
    val analogProfile: DspModes.AnalogProfile = DspModes.AnalogProfile.OFF,
    val reverbSpace: DspModes.ReverbSpace = DspModes.ReverbSpace.OFF,
    val isVocalBoost: Boolean = false,
    val isDrcEnabled: Boolean = false,
    val isLoudnessNorm: Boolean = true,
    val isMonoGuard: Boolean = true,
    val isLiquidGlass: Boolean = true,
    val activeLyrics: LyricsPayload? = null,
    val selectedMetaphors: List<Pair<String, String>> = emptyList(),
    val whyRankedTooltip: String = "",
    val rankingScorePercent: Int = 85,
    val showLyricsDialog: Boolean = false,
    val showCarModeDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showAtmosphereDialog: Boolean = false,
    val showQrShareDialog: Boolean = false,
    val showWeeklyReplayDialog: Boolean = false,
    val showCreatePlaylistDialog: Boolean = false,
    val editingSong: Song? = null,
    val qrSharePayload: String = ""
)

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: PlaybackPreferences = SmartMusicApp.instance.playbackPreferences
    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(application)
        .setAudioAttributes(
            Media3AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        loadInitialPreferences()
        setupPlayerListener()
        scanAudioLibrary()
        startProgressUpdater()
    }

    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    playNextTrack()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }
        })
    }

    private fun loadInitialPreferences() {
        _uiState.update {
            it.copy(
                playbackMode = prefs.playbackMode,
                playbackSpeed = prefs.playbackSpeed,
                isCarMode = prefs.isCarModeActive,
                analogProfile = prefs.analogProfile,
                reverbSpace = prefs.reverbSpace,
                isVocalBoost = prefs.isVocalBoostEnabled,
                isDrcEnabled = prefs.isDrcEnabled,
                isLoudnessNorm = prefs.isLoudnessNormEnabled,
                isMonoGuard = prefs.isHearingGuardEnabled,
                isLiquidGlass = prefs.isLiquidGlassEnabled
            )
        }
    }

    fun scanAudioLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            val loadedSongs = mutableListOf<Song>()
            val resolver = getApplication<Application>().contentResolver
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATA
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

            try {
                val cursor: Cursor? = resolver.query(uri, projection, selection, null, "${MediaStore.Audio.Media.TITLE} ASC")
                cursor?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                    val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                    while (c.moveToNext()) {
                        val id = c.getLong(idCol)
                        val title = c.getString(titleCol) ?: "Unknown Track"
                        val artist = c.getString(artistCol) ?: "Unknown Artist"
                        val album = c.getString(albumCol) ?: "Unknown Album"
                        val duration = c.getLong(durCol)
                        val size = c.getLong(sizeCol)
                        val path = c.getString(dataCol) ?: ""
                        val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()
                        val folder = path.substringBeforeLast('/', "Music")

                        // Deterministic camelot key & energy simulation based on title hash
                        val keyNum = (abs(title.hashCode()) % 12) + 1
                        val keyMode = if (abs(artist.hashCode()) % 2 == 0) 'B' else 'A'
                        val camelot = "$keyNum$keyMode"
                        val energy = 0.3f + (abs(id.hashCode()) % 70) / 100f

                        loadedSongs.add(
                            Song(
                                id = id,
                                title = title,
                                artist = artist,
                                album = album,
                                durationMs = max(10_000L, duration),
                                contentUri = contentUri,
                                key = camelot,
                                energy = energy,
                                fileSizeBytes = size,
                                folderPath = folder,
                                lastPlayedTimestamp = System.currentTimeMillis() - (abs(id.hashCode()) % 20) * 86_400_000L,
                                playCount = abs(id.hashCode()) % 10
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // MediaStore access failed or empty
            }

            // Provide high-quality built-in library if device storage is empty
            if (loadedSongs.isEmpty()) {
                loadedSongs.addAll(generateDefaultSongs())
            }

            val foldersMap = loadedSongs.groupBy { it.folderPath }
            val podcastsList = generateDefaultPodcasts()
            val audiobooksList = generateDefaultAudiobooks()

            withContext(Dispatchers.Main) {
                // Restore last played song by ID safely
                val savedSongId = prefs.lastSongId
                val restoredSong = loadedSongs.findByIdOrNull(savedSongId) ?: loadedSongs.firstOrNull()
                val savedPos = if (prefs.lastPositionMs > 0) prefs.lastPositionMs else 0L

                _uiState.update { current ->
                    current.copy(
                        songs = loadedSongs,
                        podcasts = podcastsList,
                        audiobooks = audiobooksList,
                        folders = foldersMap,
                        currentSong = restoredSong,
                        currentPositionMs = savedPos,
                        durationMs = restoredSong?.durationMs ?: 0L
                    )
                }
                applyFilterAndSort()
                restoredSong?.let { updateRankingExplanation(it) }
            }
        }
    }

    private fun generateDefaultSongs(): List<Song> {
        return listOf(
            Song(
                id = 101L,
                title = "Midnight Starlight",
                artist = "Aura Bloom",
                album = "Celestial Waves",
                durationMs = 215_000L,
                contentUri = "",
                bpm = 124.0,
                key = "8B",
                energy = 0.72f,
                valence = 0.65f,
                isFavorite = true,
                playCount = 12,
                completionCount = 10,
                genre = "Electronic",
                folderPath = "/storage/Music/Ambient",
                bitrateKbps = 320
            ),
            Song(
                id = 102L,
                title = "Rain Over Tehran",
                artist = "Sohrab & Delaram",
                album = "Persian Echoes",
                durationMs = 260_000L,
                contentUri = "",
                bpm = 95.0,
                key = "7A",
                energy = 0.38f,
                valence = 0.42f,
                isFavorite = true,
                playCount = 18,
                completionCount = 16,
                genre = "Acoustic",
                folderPath = "/storage/Music/Persian",
                bitrateKbps = 320
            ),
            Song(
                id = 103L,
                title = "Golden Horizon",
                artist = "Solar Drift",
                album = "Warm Tides",
                durationMs = 198_000L,
                contentUri = "",
                bpm = 118.0,
                key = "8A",
                energy = 0.60f,
                valence = 0.78f,
                playCount = 6,
                completionCount = 5,
                genre = "Indie Pop",
                folderPath = "/storage/Music/Indie",
                bitrateKbps = 256
            ),
            Song(
                id = 104L,
                title = "Deep Focus Study Flow",
                artist = "NeuroBeat",
                album = "Cognitive States",
                durationMs = 310_000L,
                contentUri = "",
                bpm = 85.0,
                key = "9B",
                energy = 0.28f,
                valence = 0.50f,
                playCount = 22,
                completionCount = 21,
                genre = "Focus / Ambient",
                folderPath = "/storage/Music/Focus",
                bitrateKbps = 320
            ),
            Song(
                id = 105L,
                title = "Analog Dreams & Cassettes",
                artist = "Vintage Soundscapes",
                album = "Tape Memories",
                durationMs = 245_000L,
                contentUri = "",
                bpm = 108.0,
                key = "8B",
                energy = 0.52f,
                valence = 0.58f,
                playCount = 4,
                completionCount = 4,
                genre = "Lo-Fi",
                folderPath = "/storage/Music/Lo-Fi",
                bitrateKbps = 320
            )
        )
    }

    private fun generateDefaultPodcasts(): List<PodcastEpisode> {
        return listOf(
            PodcastEpisode(
                id = 201L,
                title = "The Future of On-Device Audio AI",
                podcastName = "Tech SoundBytes",
                durationMs = 1_850_000L,
                playbackPositionMs = 240_000L,
                playbackSpeed = 1.3f,
                description = "Exploring neural DSP, Camelot harmonic flows, and mobile acoustic optimization."
            ),
            PodcastEpisode(
                id = 202L,
                title = "Persian Literature & Classical Meters",
                podcastName = "Radio Farhang",
                durationMs = 2_100_000L,
                playbackPositionMs = 50_000L,
                playbackSpeed = 1.2f,
                description = "Analysis of Hafez, Rumi, and Saadi poetic structures and lyrical meters."
            )
        )
    }

    private fun generateDefaultAudiobooks(): List<AudiobookTrack> {
        return listOf(
            AudiobookTrack(
                id = 301L,
                title = "Chapter 1: The Principle of Simple Formulas",
                bookTitle = "Architecting Clean Software",
                author = "Martin Vance",
                durationMs = 1_420_000L,
                contentUri = "",
                playbackPositionMs = 120_000L,
                playbackSpeed = 1.1f,
                chapterIndex = 1
            ),
            AudiobookTrack(
                id = 302L,
                title = "Chapter 2: Harmonic Flow and Acoustics",
                bookTitle = "Architecting Clean Software",
                author = "Martin Vance",
                durationMs = 1_680_000L,
                contentUri = "",
                playbackPositionMs = 0L,
                chapterIndex = 2
            )
        )
    }

    private fun startProgressUpdater() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                delay(300)
                if (exoPlayer.isPlaying) {
                    val pos = exoPlayer.currentPosition
                    val duration = if (exoPlayer.duration > 0) exoPlayer.duration else _uiState.value.durationMs
                    prefs.lastPositionMs = pos
                    _uiState.update { state ->
                        state.copy(currentPositionMs = pos, durationMs = duration, isPlaying = true)
                    }
                } else if (_uiState.value.isPlaying) {
                    _uiState.update { state ->
                        val nextPos = state.currentPositionMs + 300L
                        if (nextPos >= state.durationMs && state.durationMs > 0) {
                            playNextTrack()
                            state
                        } else {
                            prefs.lastPositionMs = nextPos
                            state.copy(currentPositionMs = nextPos)
                        }
                    }
                }
            }
        }
    }

    fun playSongById(songId: Long) {
        val song = _uiState.value.songs.findByIdOrNull(songId) ?: return
        playSong(song)
    }

    fun playSong(song: Song) {
        val lastPauseTime = prefs.lastPauseTimestampMs
        val pauseDurationSec = if (lastPauseTime > 0) (System.currentTimeMillis() - lastPauseTime) / 1000L else 0L

        // Smart Rewind after Pause feature: if paused > 30s, resume 3s earlier
        val startPos = if (song.id == prefs.lastSongId && prefs.isSmartRewindEnabled && pauseDurationSec >= 30L) {
            DspModes.calculateSmartResumePosition(prefs.lastPositionMs, pauseDurationSec)
        } else if (song.id == prefs.lastSongId) {
            prefs.lastPositionMs
        } else {
            0L
        }

        prefs.lastSongId = song.id
        prefs.lastPositionMs = startPos

        // Play via ExoPlayer if valid contentUri is present
        if (song.contentUri.isNotBlank()) {
            try {
                val mediaItem = MediaItem.fromUri(Uri.parse(song.contentUri))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.seekTo(startPos)
                exoPlayer.setPlaybackSpeed(_uiState.value.playbackSpeed)
                exoPlayer.play()
            } catch (e: Exception) {
                Log.e("MusicPlayerViewModel", "Error playing media item", e)
            }
        }

        // Load lyrics payload
        val rawLrc = LyricsService.generateSampleLrc(song.title, song.artist, song.durationMs)
        val parsedLyrics = LyricsService.parseLrc(rawLrc, targetLanguage = "en")

        _uiState.update {
            it.copy(
                currentSong = song,
                currentPositionMs = startPos,
                durationMs = song.durationMs,
                isPlaying = true,
                activeLyrics = parsedLyrics
            )
        }
        updateRankingExplanation(song)
    }

    fun togglePlayPause() {
        val currentlyPlaying = _uiState.value.isPlaying || exoPlayer.isPlaying
        if (currentlyPlaying) {
            exoPlayer.pause()
            prefs.lastPauseTimestampMs = System.currentTimeMillis()
            prefs.lastPositionMs = _uiState.value.currentPositionMs
            _uiState.update { it.copy(isPlaying = false) }
        } else {
            if (_uiState.value.currentSong?.contentUri?.isNotBlank() == true) {
                if (exoPlayer.playbackState == Player.STATE_IDLE || exoPlayer.mediaItemCount == 0) {
                    val mediaItem = MediaItem.fromUri(Uri.parse(_uiState.value.currentSong!!.contentUri))
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.seekTo(_uiState.value.currentPositionMs)
                }
                exoPlayer.play()
            }
            _uiState.update { it.copy(isPlaying = true) }
        }
    }

    fun seekTo(positionMs: Long) {
        val duration = _uiState.value.durationMs
        val safePos = positionMs.coerceIn(0L, max(0L, duration))
        prefs.lastPositionMs = safePos
        if (exoPlayer.mediaItemCount > 0) {
            exoPlayer.seekTo(safePos)
        }
        _uiState.update { it.copy(currentPositionMs = safePos) }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer.release()
    }

    fun playNextTrack() {
        val songs = _uiState.value.filteredSongs.ifEmpty { _uiState.value.songs }
        if (songs.isEmpty()) return
        val current = _uiState.value.currentSong
        val currentIndex = songs.indexOfFirst { it.id == current?.id }

        val nextSong = when (_uiState.value.playbackMode) {
            PlaybackMode.REPEAT_ONE -> current ?: songs.first()
            PlaybackMode.SHUFFLE -> {
                val shuffled = SmartScoring.minGapShuffle(songs)
                shuffled.firstOrNull { it.id != current?.id } ?: songs.first()
            }
            else -> {
                val nextIndex = (currentIndex + 1) % songs.size
                songs[nextIndex]
            }
        }
        playSong(nextSong)
    }

    fun playPreviousTrack() {
        val songs = _uiState.value.filteredSongs.ifEmpty { _uiState.value.songs }
        if (songs.isEmpty()) return
        val current = _uiState.value.currentSong
        val currentIndex = songs.indexOfFirst { it.id == current?.id }

        // If playing > 4s, restart song
        if (_uiState.value.currentPositionMs > 4000L) {
            seekTo(0L)
            return
        }

        val prevIndex = if (currentIndex - 1 < 0) songs.size - 1 else currentIndex - 1
        playSong(songs[prevIndex])
    }

    fun togglePlaybackMode() {
        val nextMode = when (_uiState.value.playbackMode) {
            PlaybackMode.OFF -> PlaybackMode.REPEAT_ALL
            PlaybackMode.REPEAT_ALL -> PlaybackMode.REPEAT_ONE
            PlaybackMode.REPEAT_ONE -> PlaybackMode.SHUFFLE
            PlaybackMode.SHUFFLE -> PlaybackMode.OFF
        }
        prefs.playbackMode = nextMode
        _uiState.update { it.copy(playbackMode = nextMode) }
    }

    fun toggleFavorite(songId: Long) {
        _uiState.update { state ->
            val updated = state.songs.map { s ->
                if (s.id == songId) s.copy(isFavorite = !s.isFavorite) else s
            }
            state.copy(songs = updated)
        }
        applyFilterAndSort()
    }

    fun selectTab(tab: MainTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
        applyFilterAndSort()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilterAndSort()
    }

    private fun applyFilterAndSort() {
        val query = _uiState.value.searchQuery.trim().lowercase()
        val baseList = _uiState.value.songs.filter {
            if (query.isBlank()) true
            else it.title.lowercase().contains(query) || it.artist.lowercase().contains(query) || it.album.lowercase().contains(query)
        }

        val sorted = when (_uiState.value.sortOption) {
            SortOption.TITLE -> baseList.sortedBy { it.title.lowercase() }
            SortOption.ARTIST -> baseList.sortedBy { it.artist.lowercase() }
            SortOption.DURATION -> baseList.sortedByDescending { it.durationMs }
            SortOption.DATE_ADDED -> baseList.sortedByDescending { it.lastPlayedTimestamp }
            SortOption.FILE_SIZE -> baseList.sortedByDescending { it.fileSizeBytes }
            SortOption.QUALITY -> baseList.sortedByDescending { it.bitrateKbps }
            SortOption.SMART_RANKING -> {
                val currentKey = _uiState.value.currentSong?.key
                baseList.sortedByDescending { SmartScoring.calculateContinuousRank(it, currentKey).first }
            }
        }

        _uiState.update { it.copy(filteredSongs = sorted) }
    }

    private fun updateRankingExplanation(song: Song) {
        val (score, why) = SmartScoring.calculateContinuousRank(song, _uiState.value.currentSong?.key)
        _uiState.update {
            it.copy(
                rankingScorePercent = (score * 100).toInt(),
                whyRankedTooltip = why
            )
        }
    }

    // MULTI-SELECT & PLAYLISTS
    fun toggleMultiSelect() {
        _uiState.update {
            it.copy(
                isMultiSelectActive = !it.isMultiSelectActive,
                selectedSongIds = emptySet()
            )
        }
    }

    fun toggleSongSelection(songId: Long) {
        _uiState.update { state ->
            val set = state.selectedSongIds.toMutableSet()
            if (set.contains(songId)) set.remove(songId) else set.add(songId)
            state.copy(selectedSongIds = set)
        }
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        _uiState.update { state ->
            val map = state.playlists.toMutableMap()
            map[name] = state.selectedSongIds.toList()
            state.copy(
                playlists = map,
                isMultiSelectActive = false,
                selectedSongIds = emptySet(),
                showCreatePlaylistDialog = false
            )
        }
    }

    fun addSelectedToPlaylist(name: String) {
        _uiState.update { state ->
            val map = state.playlists.toMutableMap()
            val existing = map[name]?.toMutableList() ?: mutableListOf()
            existing.addAll(state.selectedSongIds)
            map[name] = existing.distinct()
            state.copy(
                playlists = map,
                isMultiSelectActive = false,
                selectedSongIds = emptySet()
            )
        }
    }

    // DSP & SIGNATURE TOGGLES
    fun setAnalogProfile(profile: DspModes.AnalogProfile) {
        prefs.analogProfile = profile
        _uiState.update { it.copy(analogProfile = profile) }
    }

    fun setReverbSpace(space: DspModes.ReverbSpace) {
        prefs.reverbSpace = space
        _uiState.update { it.copy(reverbSpace = space) }
    }

    fun toggleVocalBoost() {
        val newBoost = !_uiState.value.isVocalBoost
        prefs.isVocalBoostEnabled = newBoost
        _uiState.update { it.copy(isVocalBoost = newBoost) }
    }

    fun toggleDrc() {
        val newDrc = !_uiState.value.isDrcEnabled
        prefs.isDrcEnabled = newDrc
        _uiState.update { it.copy(isDrcEnabled = newDrc) }
    }

    fun toggleCarMode() {
        val newCarMode = !_uiState.value.isCarMode
        prefs.isCarModeActive = newCarMode
        _uiState.update {
            it.copy(
                isCarMode = newCarMode,
                // In Car Mode, automatically engage DRC and Vocal Boost
                isDrcEnabled = if (newCarMode) true else it.isDrcEnabled,
                isVocalBoost = if (newCarMode) true else it.isVocalBoost
            )
        }
    }

    fun setAtmosphere(type: DspModes.AtmosphereType, volumeRatio: Float = 0.25f) {
        _uiState.update {
            it.copy(
                atmosphereType = type,
                atmosphereVolumeRatio = volumeRatio,
                isAtmospherePlaying = type != DspModes.AtmosphereType.OFF
            )
        }
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _uiState.update { it.copy(isSleepTimerActive = false, sleepTimerMinutesLeft = 0) }
            return
        }

        _uiState.update { it.copy(isSleepTimerActive = true, sleepTimerMinutesLeft = minutes) }
        sleepTimerJob = viewModelScope.launch {
            var left = minutes
            while (left > 0) {
                delay(60_000L)
                left--
                _uiState.update { it.copy(sleepTimerMinutesLeft = left) }
            }
            // Sleep timer expired -> pause playback
            _uiState.update { it.copy(isPlaying = false, isSleepTimerActive = false) }
        }
    }

    // DIALOG VISIBILITY CONTROLLERS
    fun setShowLyricsDialog(show: Boolean) = _uiState.update { it.copy(showLyricsDialog = show) }
    fun setShowCarModeDialog(show: Boolean) = _uiState.update { it.copy(showCarModeDialog = show) }
    fun setShowSettingsDialog(show: Boolean) = _uiState.update { it.copy(showSettingsDialog = show) }
    fun setShowAtmosphereDialog(show: Boolean) = _uiState.update { it.copy(showAtmosphereDialog = show) }
    fun setShowWeeklyReplayDialog(show: Boolean) = _uiState.update { it.copy(showWeeklyReplayDialog = show) }
    fun setShowCreatePlaylistDialog(show: Boolean) = _uiState.update { it.copy(showCreatePlaylistDialog = show) }
    fun setEditingSong(song: Song?) = _uiState.update { it.copy(editingSong = song) }

    fun sharePlaylistAsQr(playlistName: String) {
        val tracks = _uiState.value.songs.take(15)
        val qrPayload = DspModes.encodePlaylistToQrString(playlistName, tracks)
        _uiState.update { it.copy(qrSharePayload = qrPayload, showQrShareDialog = true) }
    }
}
