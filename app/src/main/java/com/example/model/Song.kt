package com.example.model

/**
 * Core Song representation.
 * Boundaries always use Long ID; logic uses Song object.
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: String,
    val albumArtUri: String? = null,
    val bpm: Double = 120.0,
    val key: String = "8B", // Camelot key e.g., 8B (C Major), 8A (A Minor)
    val energy: Float = 0.5f,
    val valence: Float = 0.5f,
    val danceability: Float = 0.5f,
    val acousticness: Float = 0.5f,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val skipCount: Int = 0,
    val completionCount: Int = 0,
    val lastPlayedTimestamp: Long = 0L,
    val firstPlayedTimestamp: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val bitrateKbps: Int = 320,
    val genre: String = "Unknown",
    val folderPath: String = "",
    val isPrivate: Boolean = false,
    val scheduledCapsuleDate: Long? = null,
    val scheduledCapsuleNote: String? = null
)

/**
 * MANDATORY Helper to guarantee type safety between ID and Song object.
 */
fun List<Song>.findByIdOrNull(id: Long): Song? = find { it.id == id }

/**
 * Dedicated Podcast Episode Model.
 * Strict separation: Podcasts must never be mixed into Music collections.
 */
data class PodcastEpisode(
    val id: Long,
    val title: String,
    val podcastName: String,
    val durationMs: Long,
    val contentUri: String = "",
    val playbackPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.2f,
    val isCompleted: Boolean = false,
    val releaseDate: Long = System.currentTimeMillis(),
    val description: String = "",
    val voiceClarityEnabled: Boolean = true
)

fun List<PodcastEpisode>.findByIdOrNull(id: Long): PodcastEpisode? = find { it.id == id }

/**
 * Dedicated Audiobook Model.
 */
data class AudiobookTrack(
    val id: Long,
    val title: String,
    val bookTitle: String,
    val author: String,
    val durationMs: Long,
    val contentUri: String,
    val playbackPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val chapterIndex: Int = 1
)

fun List<AudiobookTrack>.findByIdOrNull(id: Long): AudiobookTrack? = find { it.id == id }

enum class PlaybackMode {
    OFF,
    REPEAT_ALL,
    REPEAT_ONE,
    SHUFFLE
}

enum class SortOption {
    TITLE,
    ARTIST,
    DURATION,
    DATE_ADDED,
    FILE_SIZE,
    SMART_RANKING,
    QUALITY
}

/**
 * Tab Navigation Enum.
 * ALL when expressions across the app MUST explicitly handle every branch.
 */
enum class MainTab {
    MUSIC,
    HARMONIC,
    PODCASTS,
    AUDIOBOOKS,
    FOLDERS
}
