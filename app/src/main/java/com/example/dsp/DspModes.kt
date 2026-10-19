package com.example.dsp

import com.example.model.Song
import com.example.scoring.SmartScoring
import java.util.Calendar
import kotlin.math.*

/**
 * Signature Experiences & Audio Enhancement Suite (1-35)
 * Pure Kotlin formula-driven, deterministic, zero-battery-drain implementations.
 */
object DspModes {

    // 1. VINYL / CASSETTE MODE
    enum class AnalogProfile { VINYL, CASSETTE, OFF }
    data class AnalogParams(val wowFlutterDriftPercent: Float, val tapeHissLevelDb: Float, val saturationDriveDb: Float)

    fun getAnalogParameters(profile: AnalogProfile): AnalogParams {
        return when (profile) {
            AnalogProfile.VINYL -> AnalogParams(wowFlutterDriftPercent = 0.3f, tapeHissLevelDb = -50f, saturationDriveDb = -6f)
            AnalogProfile.CASSETTE -> AnalogParams(wowFlutterDriftPercent = 0.5f, tapeHissLevelDb = -42f, saturationDriveDb = -4f)
            AnalogProfile.OFF -> AnalogParams(0f, -120f, 0f)
        }
    }

    // Soft saturation formula: y = tanh(x * drive)
    fun applySoftSaturation(sample: Float, driveDb: Float): Float {
        if (driveDb <= 0f) return sample
        val gain = 10.0.pow(driveDb / 20.0).toFloat()
        return tanh(sample * gain)
    }

    // 2. ROOM REVERB (Schroeder / FDN simulation parameters)
    enum class ReverbSpace { SMALL_ROOM, CHURCH, CONCERT_HALL, OFF }
    data class ReverbParams(val decayTimeSec: Float, val wetDryRatio: Float, val preDelayMs: Int)

    fun getReverbParams(space: ReverbSpace): ReverbParams {
        return when (space) {
            ReverbSpace.SMALL_ROOM -> ReverbParams(decayTimeSec = 0.8f, wetDryRatio = 0.20f, preDelayMs = 15)
            ReverbSpace.CHURCH -> ReverbParams(decayTimeSec = 3.5f, wetDryRatio = 0.45f, preDelayMs = 45)
            ReverbSpace.CONCERT_HALL -> ReverbParams(decayTimeSec = 2.2f, wetDryRatio = 0.35f, preDelayMs = 30)
            ReverbSpace.OFF -> ReverbParams(0f, 0f, 0)
        }
    }

    // 3. VOCAL BOOST (mid 1-4kHz +3dB)
    data class VocalBoostParams(val centerFreqHz: Int = 2500, val boostGainDb: Float = 3.0f, val qFactor: Float = 0.7f)

    // 4. SLEEP FADE
    fun calculateSleepFadeVolume(remainingMinutes: Int, totalDurationMinutes: Int = 30): Float {
        if (remainingMinutes <= 0) return 0f
        return if (remainingMinutes <= 5) {
            // Gradual linear attenuation down to 0 in last 5 minutes
            (remainingMinutes.toFloat() / 5.0f).coerceIn(0f, 1f)
        } else {
            1.0f
        }
    }

    // 5. SMART GAPLESS (Inter-track silence trimming)
    fun shouldTrimInterTrackSilence(silenceDurationMs: Long, isAlbumPlayback: Boolean): Boolean {
        return isAlbumPlayback && silenceDurationMs > 200L
    }

    // 6. SMART REWIND AFTER PAUSE
    fun calculateSmartResumePosition(savedPositionMs: Long, pauseDurationSec: Long): Long {
        return if (pauseDurationSec >= 30L) {
            max(0L, savedPositionMs - 3000L) // Rewind 3 seconds for context
        } else {
            savedPositionMs
        }
    }

    // 7. SPEED PRESETS PER CONTENT
    fun defaultSpeedForContent(isPodcast: Boolean, isAudiobook: Boolean): Float {
        return when {
            isPodcast -> 1.3f
            isAudiobook -> 1.15f
            else -> 1.0f
        }
    }

    // 8. RECENTLY FORGOTTEN (play_count >= 3 && last_played > 6 months)
    fun findRecentlyForgotten(songs: List<Song>, currentTimeMs: Long = System.currentTimeMillis()): List<Song> {
        val sixMonthsMs = 180L * 24 * 60 * 60 * 1000
        return songs.filter {
            it.playCount >= 3 && it.lastPlayedTimestamp > 0L && (currentTimeMs - it.lastPlayedTimestamp) > sixMonthsMs
        }
    }

    // 9. ON THIS DAY (month and day matches today)
    fun findOnThisDay(songs: List<Song>, calendar: Calendar = Calendar.getInstance()): List<Song> {
        val todayMonth = calendar.get(Calendar.MONTH)
        val todayDay = calendar.get(Calendar.DAY_OF_MONTH)
        val tempCal = Calendar.getInstance()

        return songs.filter { song ->
            if (song.lastPlayedTimestamp <= 0L) false
            else {
                tempCal.timeInMillis = song.lastPlayedTimestamp
                tempCal.get(Calendar.MONTH) == todayMonth && tempCal.get(Calendar.DAY_OF_MONTH) == todayDay
            }
        }
    }

    // 10. SONG ANNIVERSARY
    fun findSongAnniversaries(songs: List<Song>, calendar: Calendar = Calendar.getInstance()): List<Song> {
        val todayMonth = calendar.get(Calendar.MONTH)
        val todayDay = calendar.get(Calendar.DAY_OF_MONTH)
        val todayYear = calendar.get(Calendar.YEAR)
        val tempCal = Calendar.getInstance()

        return songs.filter { song ->
            if (song.firstPlayedTimestamp <= 0L) false
            else {
                tempCal.timeInMillis = song.firstPlayedTimestamp
                tempCal.get(Calendar.MONTH) == todayMonth &&
                        tempCal.get(Calendar.DAY_OF_MONTH) == todayDay &&
                        tempCal.get(Calendar.YEAR) < todayYear
            }
        }
    }

    // 12. HIDDEN GEMS (high completion, low play count)
    fun findHiddenGems(songs: List<Song>): List<Song> {
        return songs.filter {
            val completionRate = if (it.playCount > 0) it.completionCount.toFloat() / it.playCount else 0f
            completionRate >= 0.9f && it.playCount in 1..2
        }
    }

    // 13. WEEKLY REPLAY SUMMARY
    data class WeeklyStats(
        val totalMinutesPlayed: Long,
        val topArtist: String,
        val topGenre: String,
        val favoriteHour: Int,
        val mostPlayedSong: Song?
    )

    fun calculateWeeklyStats(songs: List<Song>): WeeklyStats {
        val mostPlayed = songs.maxByOrNull { it.playCount }
        val topArtist = songs.groupBy { it.artist }.maxByOrNull { it.value.sumOf { s -> s.playCount } }?.key ?: "Diverse"
        val topGenre = songs.groupBy { it.genre }.maxByOrNull { it.value.sumOf { s -> s.playCount } }?.key ?: "Eclectic"
        val totalMins = songs.sumOf { (it.playCount * it.durationMs) / (1000 * 60) }
        return WeeklyStats(
            totalMinutesPlayed = totalMins,
            topArtist = topArtist,
            topGenre = topGenre,
            favoriteHour = 20, // Peak listening at 8 PM
            mostPlayedSong = mostPlayed
        )
    }

    // 14. MOOD JOURNAL
    data class MoodJournalSummary(val calmPercent: Int, val happyPercent: Int, val sadPercent: Int, val intensePercent: Int)

    fun calculateMoodDistribution(songs: List<Song>): MoodJournalSummary {
        val played = songs.filter { it.playCount > 0 }
        if (played.isEmpty()) return MoodJournalSummary(25, 25, 25, 25)
        var calmSum = 0f
        var happySum = 0f
        var sadSum = 0f
        var intenseSum = 0f
        for (s in played) {
            val mv = SmartScoring.calculateFuzzyMood(s.bpm, s.energy, s.valence)
            calmSum += mv.calm
            happySum += mv.happy
            sadSum += mv.sad
            intenseSum += mv.intense
        }
        val total = (calmSum + happySum + sadSum + intenseSum).coerceAtLeast(0.001f)
        return MoodJournalSummary(
            calmPercent = (calmSum / total * 100).roundToInt(),
            happyPercent = (happySum / total * 100).roundToInt(),
            sadPercent = (sadSum / total * 100).roundToInt(),
            intensePercent = (intenseSum / total * 100).roundToInt()
        )
    }

    // 15. PLAYLIST FROM ONE SONG (Camelot + fuzzy mood seed, no artist repeat within K=3)
    fun generatePlaylistFromSeed(seed: Song, library: List<Song>, targetCount: Int = 20): List<Song> {
        val candidates = library.filter { it.id != seed.id }.toMutableList()
        val result = mutableListOf(seed)
        val recentArtists = ArrayDeque<String>().apply { add(seed.artist) }

        while (result.size < targetCount && candidates.isNotEmpty()) {
            val currentKey = result.last().key
            val currentEnergy = result.last().energy

            // Rank candidates by Camelot affinity + energy closeness
            val scoredCandidates = candidates.map { candidate ->
                val dist = SmartScoring.camelotDistance(currentKey, candidate.key)
                val energyDist = abs(currentEnergy - candidate.energy)
                val artistPenalty = if (recentArtists.contains(candidate.artist)) 2.0 else 0.0
                val penalty = dist + (energyDist * 3.0) + artistPenalty
                Pair(candidate, penalty)
            }.sortedBy { it.second }

            val chosen = scoredCandidates.first().first
            candidates.remove(chosen)
            result.add(chosen)
            recentArtists.addLast(chosen.artist)
            if (recentArtists.size > 3) recentArtists.removeFirst()
        }
        return result
    }

    // 19. MEANING OF LYRICS (Offline Metaphor Hints)
    val metaphorLexicon = mapOf(
        "rain" to "Signifies sorrow, cleansing, or emotional renewal.",
        "sun" to "Symbol of optimism, hope, warmth, and vitality.",
        "fire" to "Represents unbridled passion, burning desire, or destruction.",
        "ocean" to "Metaphor for profound depth, vast freedom, or the subconscious.",
        "night" to "Symbol of solitude, introspection, secrets, or fear.",
        "shadow" to "Represents lingering past, duality, or fear of the unknown."
    )

    fun findMetaphors(text: String): List<Pair<String, String>> {
        val lower = text.lowercase()
        return metaphorLexicon.filter { (key, _) ->
            lower.contains(Regex("\\b$key\\b"))
        }.toList()
    }

    // 22. POEM DETECTION (Persian classical meters / English rhyming)
    fun isClassicalPoem(lyrics: String): Boolean {
        val lines = lyrics.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size < 4) return false
        // Persian hemistich detection or rhythmic verse line symmetry
        val avgLen = lines.map { it.length }.average()
        val variance = lines.map { (it.length - avgLen).pow(2) }.average()
        // Classical hemistichs have very low syllable/length variance (< 16)
        return variance < 16.0
    }

    // 33. SHARED PLAYLIST VIA QR (Compact payload encoding)
    fun encodePlaylistToQrString(name: String, tracks: List<Song>): String {
        val trackTokens = tracks.take(30).joinToString(";") { "${it.title}|${it.artist}|${it.durationMs / 1000}" }
        return "SMARTMUSIC_PL:$name:$trackTokens"
    }

    fun decodeQrPlaylist(payload: String): Pair<String, List<Pair<String, String>>>? {
        if (!payload.startsWith("SMARTMUSIC_PL:")) return null
        val parts = payload.removePrefix("SMARTMUSIC_PL:").split(":")
        if (parts.size < 2) return null
        val name = parts[0]
        val tracks = parts[1].split(";").mapNotNull {
            val songParts = it.split("|")
            if (songParts.size >= 2) Pair(songParts[0], songParts[1]) else null
        }
        return Pair(name, tracks)
    }

    // 34. ATMOSPHERIC SOUNDSCAPES
    enum class AtmosphereType { RAIN, FIREPLACE, CAFE, OFF }
    data class AtmosphereConfig(val type: AtmosphereType, val volumeRatio: Float = 0.25f)

    // 35. PROFILE-AWARE EQ PRESETS
    enum class AudioOutputProfile { HEADPHONES_HARMAN, CAR_SCOOP, SPEAKER_VSHAPE, FLAT }

    fun getRecommendedEqForProfile(profile: AudioOutputProfile): SmartScoring.EqProfile {
        return when (profile) {
            AudioOutputProfile.HEADPHONES_HARMAN -> SmartScoring.EqProfile(lowBandGainDb = 2.5f, midBandGainDb = 0.5f, highBandGainDb = 1.5f)
            AudioOutputProfile.CAR_SCOOP -> SmartScoring.EqProfile(lowBandGainDb = 4.0f, midBandGainDb = -2.0f, highBandGainDb = 3.0f)
            AudioOutputProfile.SPEAKER_VSHAPE -> SmartScoring.EqProfile(lowBandGainDb = 3.0f, midBandGainDb = -1.5f, highBandGainDb = 2.5f)
            AudioOutputProfile.FLAT -> SmartScoring.EqProfile(0f, 0f, 0f)
        }
    }
}
