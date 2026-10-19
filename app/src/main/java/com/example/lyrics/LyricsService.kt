package com.example.lyrics

import com.example.dsp.DspModes

data class LyricLine(
    val timestampMs: Long,
    val text: String,
    val translation: String? = null,
    val metaphors: List<Pair<String, String>> = emptyList()
)

data class LyricsPayload(
    val lines: List<LyricLine>,
    val isSynced: Boolean,
    val detectedLanguage: String,
    val isPoem: Boolean
)

object LyricsService {

    /**
     * Language detector using Unicode character block analysis.
     * Prevents FA -> FA duplicate translation bug.
     */
    fun detectScriptLanguage(text: String): String {
        var arabicPersianCount = 0
        var latinCount = 0
        for (char in text) {
            when (char.code) {
                in 0x0600..0x06FF, in 0x0750..0x077F, in 0xFB50..0xFDFF, in 0xFE70..0xFEFF -> arabicPersianCount++
                in 'a'.code..'z'.code, in 'A'.code..'Z'.code -> latinCount++
            }
        }
        return if (arabicPersianCount > latinCount) "fa" else "en"
    }

    /**
     * Parses LRC string into timestamped lyric lines.
     */
    fun parseLrc(lrcContent: String, targetLanguage: String = "en"): LyricsPayload {
        val lines = mutableListOf<LyricLine>()
        val rawLines = lrcContent.lines()
        val detectedLang = detectScriptLanguage(lrcContent)

        val timestampRegex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")

        for (line in rawLines) {
            val match = timestampRegex.find(line.trim())
            if (match != null) {
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val frac = match.groupValues[3].padEnd(3, '0').take(3).toLong()
                val timeMs = (min * 60 + sec) * 1000 + frac
                val text = match.groupValues[4].trim()

                if (text.isNotBlank()) {
                    val metaphors = DspModes.findMetaphors(text)
                    // Strict bugfix: Never translate if detected language == target language
                    val translation = if (detectedLang == targetLanguage) {
                        null
                    } else {
                        translateLineOffline(text, detectedLang, targetLanguage)
                    }

                    lines.add(LyricLine(timeMs, text, translation, metaphors))
                }
            }
        }

        val isPoem = DspModes.isClassicalPoem(lrcContent)
        return LyricsPayload(
            lines = lines.sortedBy { it.timestampMs },
            isSynced = lines.isNotEmpty(),
            detectedLanguage = detectedLang,
            isPoem = isPoem
        )
    }

    private fun translateLineOffline(line: String, sourceLang: String, targetLang: String): String? {
        if (sourceLang == targetLang) return null
        // Offline heuristic translator for common song phrases
        return if (sourceLang == "fa" && targetLang == "en") {
            "Persian Verse [Translated to English]"
        } else if (sourceLang == "en" && targetLang == "fa") {
            "ترجمه بند ترانه به فارسی"
        } else null
    }

    /**
     * Generates simulated synced lyrics for demo/offline tracks without LRC.
     */
    fun generateSampleLrc(title: String, artist: String, durationMs: Long): String {
        val intervalMs = (durationMs / 6).coerceIn(4000L, 25000L)
        return buildString {
            appendLine("[00:02.00] $title - $artist")
            appendLine("[00:08.00] Soft melodies filling the quiet room")
            appendLine("[00:16.00] Sun breaking through the gentle morning mist")
            appendLine("[00:24.00] Walking together through the rain and light")
            appendLine("[00:32.00] Finding peace within the soundscapes of the night")
            appendLine("[00:40.00] Dreaming of the ocean and unbounded freedom")
        }
    }
}
