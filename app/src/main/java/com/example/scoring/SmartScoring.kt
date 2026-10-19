package com.example.scoring

import com.example.model.Song
import java.util.Calendar
import kotlin.math.*
import kotlin.random.Random

/**
 * SmartScoring - Pure Kotlin Algorithmic Intelligence Layer (A-O)
 * Zero heavy ML models, zero battery drain, microsecond deterministic evaluation.
 * All constants are configurable and support 1-tap reset to factory defaults.
 */
object SmartScoring {

    // Configurable constants with defaults
    var tauDays: Double = 7.0
    var familiarityK: Double = 5.0
    var overlapLambda: Double = 0.6
    var artistGapK: Int = 3
    var defaultEpsilon: Double = 0.1
    var discoveryEpsilon: Double = 0.3
    var targetMusicLufs: Float = -14.0f
    var targetPodcastLufs: Float = -16.0f
    var drcThresholdDb: Float = -18.0f
    var drcRatio: Float = 3.0f
    var hearingGuardCapDb: Float = -6.0f

    fun resetToDefaults() {
        tauDays = 7.0
        familiarityK = 5.0
        overlapLambda = 0.6
        artistGapK = 3
        defaultEpsilon = 0.1
        discoveryEpsilon = 0.3
        targetMusicLufs = -14.0f
        targetPodcastLufs = -16.0f
        drcThresholdDb = -18.0f
        drcRatio = 3.0f
        hearingGuardCapDb = -6.0f
    }

    // A. DSP FEATURE EXTRACTION (Mock/Analytical simulation over audio metadata)
    data class DspFeatures(
        val rmsEnergy: Float,
        val spectralCentroid: Float,
        val spectralRolloff85: Float,
        val zeroCrossingRate: Float,
        val tempoAutocorrelation: Float,
        val onsetDensity: Float,
        val dynamicRangeDb: Float
    )

    fun extractDspFeatures(song: Song): DspFeatures {
        // Analytical derivation from cached attributes
        val energyNorm = song.energy.coerceIn(0f, 1f)
        val zcr = 0.05f + 0.35f * energyNorm
        val sc = 800f + 3200f * energyNorm
        val rolloff = sc * 1.3f
        val dynamicRange = 6f + 24f * (1f - energyNorm)
        return DspFeatures(
            rmsEnergy = energyNorm,
            spectralCentroid = sc,
            spectralRolloff85 = rolloff,
            zeroCrossingRate = zcr,
            tempoAutocorrelation = (song.bpm.toFloat() / 200f).coerceIn(0.1f, 1f),
            onsetDensity = (song.bpm.toFloat() / 60f) * (0.5f + 0.5f * energyNorm),
            dynamicRangeDb = dynamicRange
        )
    }

    // B. FUZZY CLASSIFIERS
    data class MoodVector(val calm: Float, val happy: Float, val sad: Float, val intense: Float)

    fun calculateFuzzyMood(bpm: Double, energy: Float, valence: Float): MoodVector {
        val tempoNorm = (bpm.toFloat() / 180f).coerceIn(0f, 1f)
        val calm = ((1f - energy) * (1f - tempoNorm) * (0.5f + 0.5f * valence)).coerceIn(0f, 1f)
        val happy = (valence * (0.6f * energy + 0.4f * tempoNorm)).coerceIn(0f, 1f)
        val sad = ((1f - valence) * (1f - energy)).coerceIn(0f, 1f)
        val intense = (energy * tempoNorm * (1f - 0.3f * valence)).coerceIn(0f, 1f)
        return MoodVector(calm, happy, sad, intense)
    }

    fun calculateDanceability(beatStrength: Float, tempoNorm: Float, onsetDensity: Float, energy: Float): Float {
        return (0.4f * beatStrength + 0.3f * tempoNorm + 0.2f * onsetDensity + 0.1f * energy).coerceIn(0f, 1f)
    }

    fun calculateChillness(energy: Float, tempoNorm: Float): Float {
        return (1.0f - 0.7f * energy - 0.3f * tempoNorm).coerceIn(0f, 1f)
    }

    // C. RECENCY, FAMILIARITY, DIVERSITY & MIN-GAP SHUFFLE
    fun calculateRecency(lastPlayedMs: Long, currentTimeMs: Long = System.currentTimeMillis()): Double {
        if (lastPlayedMs <= 0L) return 0.1
        val deltaDays = max(0.0, (currentTimeMs - lastPlayedMs) / (1000.0 * 60 * 60 * 24))
        return exp(-deltaDays / tauDays)
    }

    fun calculateFamiliarity(playsLast30Days: Int): Double {
        return min(1.0, playsLast30Days / familiarityK)
    }

    fun minGapShuffle(tracks: List<Song>, gap: Int = artistGapK): List<Song> {
        if (tracks.size <= 2) return tracks.shuffled()
        val mutable = tracks.shuffled().toMutableList()
        val result = mutableListOf<Song>()
        val recentArtists = ArrayDeque<String>()

        while (mutable.isNotEmpty()) {
            val candidateIndex = mutable.indexOfFirst { !recentArtists.contains(it.artist) }
            val chosen = if (candidateIndex >= 0) {
                mutable.removeAt(candidateIndex)
            } else {
                mutable.removeAt(0)
            }
            result.add(chosen)
            recentArtists.addLast(chosen.artist)
            if (recentArtists.size > gap) recentArtists.removeFirst()
        }
        return result
    }

    // D. HARMONIC MIXING (Camelot Wheel: 1A..12B)
    data class CamelotKey(val number: Int, val mode: Char) // e.g. 8, 'B'

    fun parseCamelot(keyStr: String): CamelotKey {
        val clean = keyStr.trim().uppercase()
        val mode = if (clean.endsWith("A") || clean.endsWith("B")) clean.last() else 'B'
        val numStr = clean.dropLast(1).filter { it.isDigit() }
        val num = numStr.toIntOrNull()?.coerceIn(1, 12) ?: 8
        return CamelotKey(num, mode)
    }

    fun camelotDistance(key1: String, key2: String): Int {
        val k1 = parseCamelot(key1)
        val k2 = parseCamelot(key2)
        if (k1 == k2) return 0
        val numDiff = abs(k1.number - k2.number)
        val circleDist = min(numDiff, 12 - numDiff)

        return when {
            circleDist == 0 && k1.mode != k2.mode -> 1 // Relative major/minor
            circleDist == 1 && k1.mode == k2.mode -> 1 // Adjacent key
            circleDist == 1 && k1.mode != k2.mode -> 2 // Diagonal jump
            circleDist == 2 && k1.mode == k2.mode -> 2 // Two steps
            else -> 3 + circleDist // Potential clash
        }
    }

    fun calculateHarmonicCrossfadeSec(bpm1: Double, bpm2: Double, key1: String, key2: String): Float {
        val dist = camelotDistance(key1, key2)
        val bpmDelta = abs(bpm1 - bpm2) / max(1.0, bpm1)
        var crossfade = 3.0f
        if (bpmDelta < 0.05) crossfade += 2.0f
        if (dist <= 1) crossfade += 2.0f
        if (dist >= 3) crossfade -= 1.0f
        return crossfade.coerceIn(1.0f, 8.0f)
    }

    // E. ENERGY CURVE PLANNING
    enum class EnergyIntent { WARMUP, WORKOUT, FOCUS, SLEEP, PARTY }

    fun planEnergyCurve(intent: EnergyIntent, count: Int): List<Float> {
        if (count <= 0) return emptyList()
        return (0 until count).map { i ->
            val progress = i.toFloat() / max(1, count - 1)
            when (intent) {
                EnergyIntent.WARMUP -> 0.3f + 0.4f * progress
                EnergyIntent.WORKOUT -> {
                    // low -> peak -> cooldown
                    if (progress < 0.7f) 0.5f + 0.5f * (progress / 0.7f)
                    else 1.0f - 0.5f * ((progress - 0.7f) / 0.3f)
                }
                EnergyIntent.FOCUS -> 0.45f + 0.1f * sin(progress * Math.PI.toFloat())
                EnergyIntent.SLEEP -> (0.5f - 0.35f * progress).coerceAtLeast(0.1f)
                EnergyIntent.PARTY -> 0.8f + 0.2f * sin(progress * 2 * Math.PI.toFloat())
            }
        }
    }

    // F. TIME-OF-DAY SCORING
    fun timeOfDayPrior(calendar: Calendar = Calendar.getInstance()): Pair<Float, Float> {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> Pair(0.4f, 0.7f) // Morning: calm & happy
            in 11..14 -> Pair(0.6f, 0.5f) // Midday: focus
            in 15..18 -> Pair(0.8f, 0.6f) // Afternoon: energetic
            in 19..22 -> Pair(0.6f, 0.8f) // Evening: social & upbeat
            else -> Pair(0.2f, 0.3f) // Night: calm & low energy
        }
    }

    fun calculateTimeOfDayScore(song: Song): Double {
        val (targetEnergy, targetValence) = timeOfDayPrior()
        val energyDiff = abs(song.energy - targetEnergy)
        val valenceDiff = abs(song.valence - targetValence)
        return (1.0 - (energyDiff * 0.5 + valenceDiff * 0.5)).coerceIn(0.0, 1.0)
    }

    // G. EXPLORATION / EXPLOITATION (Thompson Beta / Epsilon-Greedy)
    fun sampleThompsonBeta(plays: Int, skips: Int): Double {
        val a = plays + 1.0
        val b = skips + 1.0
        // Analytical Beta mean approximation with variance jitter
        val mean = a / (a + b)
        val variance = (a * b) / ((a + b) * (a + b) * (a + b + 1.0))
        val stdDev = sqrt(variance)
        val jitter = (Random.nextDouble() * 2.0 - 1.0) * stdDev
        return (mean + jitter).coerceIn(0.0, 1.0)
    }

    // H. SKIP FATIGUE DETECTION
    fun evaluateSkipFatigue(recentSkips: List<Long>, recentCompletions: List<Long>): Pair<Float, Int> {
        val rapidSkips = recentSkips.count { it < 10_000L }
        val completionCount = recentCompletions.size
        return when {
            rapidSkips >= 3 -> Pair(0.5f, 5) // Halve energy weight, widen artist gap to 5
            completionCount >= 5 -> Pair(1.2f, 1) // Tighten gap, reinforce current vibe
            else -> Pair(1.0f, artistGapK)
        }
    }

    // I. SMART PREFETCH
    fun calculatePrefetchWindow(skipRateLast10: Float): Int {
        return (1 + ceil(skipRateLast10 * 2.0)).toInt().coerceIn(1, 4)
    }

    // J. LEXICON SENTIMENT (AFINN micro-lexicon)
    private val sentimentLexicon = mapOf(
        "love" to 3, "happy" to 3, "joy" to 3, "light" to 2, "sun" to 2, "peace" to 2,
        "sweet" to 2, "dance" to 2, "dream" to 1, "smile" to 2, "hope" to 2, "alive" to 2,
        "sad" to -2, "pain" to -3, "dark" to -2, "cry" to -2, "rain" to -1, "lost" to -2,
        "cold" to -1, "hurt" to -2, "alone" to -2, "tears" to -2, "die" to -3, "fear" to -2
    )

    fun scoreLyricSentiment(lyrics: String): Float {
        val words = lyrics.lowercase().split(Regex("[^a-zA-Z]+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return 0.0f
        var total = 0
        var matches = 0
        for (w in words) {
            val score = sentimentLexicon[w]
            if (score != null) {
                total += score
                matches++
            }
        }
        if (matches == 0) return 0.0f
        return (total.toFloat() / sqrt(matches.toFloat())).coerceIn(-3f, 3f) / 3f
    }

    // K. AUTO-EQ BY GENRE
    data class EqProfile(val lowBandGainDb: Float, val midBandGainDb: Float, val highBandGainDb: Float)

    fun getGenreEqProfile(genre: String): EqProfile {
        return when (genre.lowercase()) {
            "classical", "acoustic" -> EqProfile(lowBandGainDb = 0f, midBandGainDb = 0f, highBandGainDb = 2.0f)
            "hip-hop", "hip hop", "rap", "r&b" -> EqProfile(lowBandGainDb = 3.5f, midBandGainDb = -1.0f, highBandGainDb = 1.0f)
            "rock", "metal", "punk" -> EqProfile(lowBandGainDb = 1.0f, midBandGainDb = 2.5f, highBandGainDb = 1.5f)
            "electronic", "edm", "dance", "house" -> EqProfile(lowBandGainDb = 4.0f, midBandGainDb = -2.0f, highBandGainDb = 3.0f)
            "podcast", "vocal", "speech" -> EqProfile(lowBandGainDb = -2.0f, midBandGainDb = 3.0f, highBandGainDb = 0f)
            else -> EqProfile(0f, 0f, 0f)
        }
    }

    // M. LOUDNESS NORMALIZATION (EBU R128)
    fun calculateLoudnessGainDb(measuredLufs: Float, isPodcast: Boolean): Float {
        val target = if (isPodcast) targetPodcastLufs else targetMusicLufs
        val gain = target - measuredLufs
        return gain.coerceIn(-12.0f, 12.0f)
    }

    // N. DYNAMIC RANGE COMPRESSION (DRC)
    fun applyDrcEnvelope(inputLevelDb: Float, ratio: Float = drcRatio, threshold: Float = drcThresholdDb): Float {
        if (inputLevelDb <= threshold) return inputLevelDb
        val overshoot = inputLevelDb - threshold
        return threshold + (overshoot / ratio)
    }

    // O. MONO DOWNMIX + HEARING GUARD
    fun checkHearingGuard(peakLevelDb: Float): Boolean {
        // Returns true if safe, false if warning triggered (> -3dBFS)
        return peakLevelDb <= -3.0f
    }

    // UNIFIED CONTINUOUS RANKING ALGORITHM
    // score(track) = sigmoid(sum(w_i * x_i)) + lambda * embedding_similarity
    fun calculateContinuousRank(song: Song, referenceKey: String? = null): Pair<Double, String> {
        val totalPlays = (song.playCount + song.skipCount).coerceAtLeast(1)
        val listenRatio = song.playCount.toDouble() / totalPlays
        val completionRate = song.completionCount.toDouble() / max(1, song.playCount)
        val recency = calculateRecency(song.lastPlayedTimestamp)
        val favoriteBonus = if (song.isFavorite) 0.25 else 0.0
        val timeScore = calculateTimeOfDayScore(song)
        val harmonicBonus = if (referenceKey != null) {
            val dist = camelotDistance(referenceKey, song.key)
            when (dist) {
                0 -> 0.20
                1 -> 0.12
                2 -> 0.05
                else -> -0.10
            }
        } else 0.0

        // Sigmoid linear combination
        val weightedSum = (0.25 * listenRatio) +
                (0.20 * completionRate) +
                (0.20 * recency) +
                (0.15 * timeScore) +
                favoriteBonus +
                harmonicBonus

        // Standard sigmoid continuous mapping 0..1
        val score = 1.0 / (1.0 + exp(-weightedSum * 3.0))

        val why = buildString {
            append("Score ${(score * 100).roundToInt()}%: ")
            if (song.isFavorite) append("Favorite • ")
            if (recency > 0.6) append("Recently loved • ")
            if (completionRate > 0.8) append("High completion • ")
            if (harmonicBonus > 0) append("Camelot ${song.key} match • ")
            append("Circadian match")
        }

        return Pair(score.coerceIn(0.0, 1.0), why)
    }
}
