package com.example

import com.example.dsp.DspModes
import com.example.lyrics.LyricsService
import com.example.model.MainTab
import com.example.model.Song
import com.example.model.findByIdOrNull
import com.example.scoring.SmartScoring
import org.junit.Assert.*
import org.junit.Test

class SmartScoringUnitTest {

    private val testSongA = Song(
        id = 1L,
        title = "Track A",
        artist = "Artist A",
        album = "Album A",
        durationMs = 180_000L,
        contentUri = "",
        key = "8B",
        bpm = 124.0,
        energy = 0.8f,
        valence = 0.7f,
        isFavorite = true,
        playCount = 10,
        completionCount = 9
    )

    private val testSongB = Song(
        id = 2L,
        title = "Track B",
        artist = "Artist B",
        album = "Album B",
        durationMs = 210_000L,
        contentUri = "",
        key = "8A", // Relative minor to 8B
        bpm = 126.0,
        energy = 0.75f,
        valence = 0.65f,
        isFavorite = false,
        playCount = 4,
        completionCount = 3
    )

    // Formula A: DSP Extraction Test
    @Test
    fun testDspFeatureExtraction() {
        val dsp = SmartScoring.extractDspFeatures(testSongA)
        assertTrue("Spectral centroid must be positive", dsp.spectralCentroid > 0)
        assertTrue("Onset density must be positive", dsp.onsetDensity > 0)
    }

    // Formula B: Fuzzy Mood Classifier Test
    @Test
    fun testFuzzyMoodClassifier() {
        val mood = SmartScoring.calculateFuzzyMood(testSongA.bpm, testSongA.energy, testSongA.valence)
        assertTrue("Intense mood should be high for 0.8 energy", mood.intense > 0.4f)
        val chillness = SmartScoring.calculateChillness(testSongA.energy, 0.6f)
        assertTrue("Chillness should be within 0..1", chillness in 0.0f..1.0f)
    }

    // Formula C: Recency & Min-gap shuffle
    @Test
    fun testMinGapShuffle() {
        val tracks = listOf(
            testSongA.copy(id = 1L, artist = "Artist 1"),
            testSongA.copy(id = 2L, artist = "Artist 1"),
            testSongA.copy(id = 3L, artist = "Artist 2"),
            testSongA.copy(id = 4L, artist = "Artist 3")
        )
        val shuffled = SmartScoring.minGapShuffle(tracks, gap = 2)
        assertEquals("Shuffled list must preserve size", tracks.size, shuffled.size)
    }

    // Formula D: Camelot Wheel Harmonic Distance & Crossfade
    @Test
    fun testCamelotDistanceAndCrossfade() {
        // 8B and 8A are relative major/minor (distance 1)
        val dist = SmartScoring.camelotDistance("8B", "8A")
        assertEquals("Relative major/minor distance should be 1", 1, dist)

        val crossfade = SmartScoring.calculateHarmonicCrossfadeSec(124.0, 126.0, "8B", "8A")
        assertTrue("Harmonic crossfade should be >= 4 seconds", crossfade >= 4.0f)
    }

    // Formula E: Energy Curve Planning
    @Test
    fun testEnergyCurvePlanning() {
        val curve = SmartScoring.planEnergyCurve(SmartScoring.EnergyIntent.WORKOUT, 10)
        assertEquals(10, curve.size)
        assertTrue("Workout peak energy should be high", curve.maxOrNull()!! >= 0.8f)
    }

    // Formula G: Thompson Beta Sampling
    @Test
    fun testThompsonBetaSampling() {
        val score = SmartScoring.sampleThompsonBeta(plays = 15, skips = 2)
        assertTrue("Score should be biased high for high play ratio", score > 0.5)
    }

    // Formula J: Lyric Sentiment Scoring
    @Test
    fun testLyricSentiment() {
        val happyLyrics = "I feel so happy, full of joy and love and sunshine"
        val sentiment = SmartScoring.scoreLyricSentiment(happyLyrics)
        assertTrue("Positive sentiment score expected", sentiment > 0.3f)
    }

    // Formula M: Loudness Normalization
    @Test
    fun testLoudnessNormalization() {
        val gain = SmartScoring.calculateLoudnessGainDb(-20.0f, isPodcast = false)
        assertEquals(6.0f, gain, 0.01f)
    }

    // Formula N: DRC Soft-Knee
    @Test
    fun testDrcEnvelope() {
        val compressed = SmartScoring.applyDrcEnvelope(-12.0f, ratio = 3.0f, threshold = -18.0f)
        // input -12dB is 6dB above threshold -18dB. With ratio 3:1, overshoot compressed to 2dB -> -16dB
        assertEquals(-16.0f, compressed, 0.01f)
    }

    // Critical Bug 2 Fix: Language Detection and No-op on identical source and target
    @Test
    fun testLanguageDetectionAndNoOp() {
        val persianText = "سلام این یک متن فارسی برای آهنگ است"
        val detected = LyricsService.detectScriptLanguage(persianText)
        assertEquals("fa", detected)

        // When detected is FA and target is FA, parser must yield null translation (strict NO-OP)
        val parsed = LyricsService.parseLrc("[00:05.00] $persianText", targetLanguage = "fa")
        assertNull("Translation must be null when source and target language match", parsed.lines.first().translation)
    }

    // Known Issue 7: ID vs Object Boundary Test
    @Test
    fun testFindByIdOrNullHelper() {
        val list = listOf(testSongA, testSongB)
        val found = list.findByIdOrNull(2L)
        assertNotNull("Should find song with id 2", found)
        assertEquals("Track B", found?.title)

        val notFound = list.findByIdOrNull(999L)
        assertNull("Should return null for nonexistent id", notFound)
    }

    // Known Issue 6: Exhaustive Tab When Check
    @Test
    fun testMainTabExhaustiveness() {
        for (tab in MainTab.entries) {
            val name = when (tab) {
                MainTab.MUSIC -> "Music"
                MainTab.HARMONIC -> "Harmonic"
                MainTab.PODCASTS -> "Podcasts"
                MainTab.AUDIOBOOKS -> "Audiobooks"
                MainTab.FOLDERS -> "Folders"
            }
            assertTrue(name.isNotEmpty())
        }
    }
}
