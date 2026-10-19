package com.example.ui.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChangeCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Song
import com.example.scoring.SmartScoring
import com.example.ui.MusicUiState
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.NeonTurquoise
import com.example.ui.theme.VividPurple

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HarmonicMixingScreen(
    uiState: MusicUiState,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentKey = uiState.currentSong?.key ?: "8B"
    var selectedKey by remember(currentKey) { mutableStateOf(currentKey) }

    val camelotA = listOf("1A", "2A", "3A", "4A", "5A", "6A", "7A", "8A", "9A", "10A", "11A", "12A")
    val camelotB = listOf("1B", "2B", "3B", "4B", "5B", "6B", "7B", "8B", "9B", "10B", "11B", "12B")

    // Filter tracks matching compatible harmonic keys
    val compatibleTracks = remember(selectedKey, uiState.songs) {
        uiState.songs.map { song ->
            val dist = SmartScoring.camelotDistance(selectedKey, song.key)
            Pair(song, dist)
        }.filter { it.second <= 2 } // Keep distance 0, 1, or 2 (Harmonic matches)
            .sortedBy { it.second }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("harmonic_mixing_screen")
    ) {
        // Header Banner: Camelot Harmonic Assistant
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1B1438),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = NeonTurquoise,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Harmonic Camelot Wheel",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Current track key pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NeonTurquoise.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Current: $currentKey",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NeonTurquoise,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Mixing tracks with Camelot distance ≤ 1 guarantees melodic resonance with zero acoustic clashing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB5A4FF)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive Camelot Key Selector Grid
        Text(
            text = "Select Root Harmonic Key (Major B / Minor A):",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Major Keys (Outer Circle - B)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            camelotB.forEach { key ->
                val isSelected = key == selectedKey
                val dist = SmartScoring.camelotDistance(currentKey, key)
                val isHarmonic = dist <= 1
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isSelected -> NeonTurquoise
                                isHarmonic -> VividPurple.copy(alpha = 0.6f)
                                else -> Color(0xFF1E1838)
                            }
                        )
                        .clickable { selectedKey = key }
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) DeepPurpleDark else Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Minor Keys (Inner Circle - A)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            camelotA.forEach { key ->
                val isSelected = key == selectedKey
                val dist = SmartScoring.camelotDistance(currentKey, key)
                val isHarmonic = dist <= 1
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isSelected -> NeonTurquoise
                                isHarmonic -> VividPurple.copy(alpha = 0.6f)
                                else -> Color(0xFF161228)
                            }
                        )
                        .clickable { selectedKey = key }
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) DeepPurpleDark else Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Harmonic Compatibility Summary Card
        val crossfadeSec = SmartScoring.calculateHarmonicCrossfadeSec(
            uiState.currentSong?.bpm ?: 120.0,
            124.0,
            currentKey,
            selectedKey
        )

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF161226),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Mixing from $currentKey ➔ $selectedKey",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    val relation = when (SmartScoring.camelotDistance(currentKey, selectedKey)) {
                        0 -> "Exact Key Match (Perfect resonance)"
                        1 -> "Harmonic Neighbor (Energy shift / Smooth flow)"
                        2 -> "Diagonal / Two-step Shift"
                        else -> "Harmonic Clash (Avoid)"
                    }
                    Text(text = relation, style = MaterialTheme.typography.bodySmall, color = NeonTurquoise)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = VividPurple.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = "Crossfade ${crossfadeSec.toInt()}s",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Compatible Songs in Library
        Text(
            text = "Compatible Songs in Library (${compatibleTracks.size}):",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (compatibleTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No compatible harmonic tracks found for key $selectedKey",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(compatibleTracks, key = { it.first.id }) { (song, dist) ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161226)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSongClick(song) }
                            .testTag("harmonic_song_${song.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Key Pill
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (dist == 0) NeonTurquoise else VividPurple.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = song.key,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (dist == 0) DeepPurpleDark else Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.White
                                )
                                Text(
                                    text = "${song.artist} • ${song.bpm.toInt()} BPM",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFA6A0C2)
                                )
                            }

                            FilledIconButton(
                                onClick = { onSongClick(song) },
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = NeonTurquoise,
                                    contentColor = DeepPurpleDark
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play Mix", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
