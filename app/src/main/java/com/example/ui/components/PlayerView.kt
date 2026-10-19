package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.dsp.DspModes
import com.example.model.PlaybackMode
import com.example.model.Song
import com.example.ui.MusicUiState
import com.example.ui.theme.NeonTurquoise
import com.example.ui.theme.VividPurple
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerView(
    uiState: MusicUiState,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlaybackMode: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onToggleVocalBoost: () -> Unit,
    onToggleDrc: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenCarMode: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAtmosphere: () -> Unit,
    onClosePlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song = uiState.currentSong ?: return
    var showWhyTooltip by remember { mutableStateOf(false) }

    // Liquid Glass background brush
    val backdropBrush = Brush.verticalGradient(
        colors = listOf(
            VividPurple.copy(alpha = 0.45f),
            Color(0xFF0D0A1A).copy(alpha = 0.95f),
            Color(0xFF07050E)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backdropBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .testTag("player_fullscreen_view")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Slot: Header & collapse button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClosePlayer,
                    modifier = Modifier.testTag("close_player_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse Player",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonTurquoise
                        )
                    )
                    Text(
                        text = song.genre,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = onOpenCarMode, modifier = Modifier.testTag("player_car_mode_btn")) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = stringResource(R.string.car_mode),
                            tint = if (uiState.isCarMode) NeonTurquoise else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("player_settings_btn")) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = stringResource(R.string.settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Center Slot: Album Art with Camelot key badge & BPM
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(VividPurple, NeonTurquoise.copy(alpha = 0.5f))
                            )
                        )
                        .padding(3.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFF161228)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_music_note),
                        contentDescription = "Album Art",
                        modifier = Modifier.size(96.dp),
                        tint = NeonTurquoise.copy(alpha = 0.85f)
                    )

                    // Camelot Key Pill Badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Key ${song.key}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NeonTurquoise
                            )
                        }
                    }

                    // BPM Pill Badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "${song.bpm.toInt()} BPM",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title & Artist with Favorite button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { onToggleFavorite(song.id) },
                        modifier = Modifier.testTag("favorite_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (song.isFavorite) Color(0xFFFF5470) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Smart Ranking Score with Tooltip (?) Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .clickable { showWhyTooltip = !showWhyTooltip }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Algorithmic Rank",
                        modifier = Modifier.size(16.dp),
                        tint = NeonTurquoise
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Smart Affinity: ${uiState.rankingScorePercent}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = NeonTurquoise
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Why ranked here",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(visible = showWhyTooltip) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E1838),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Text(
                            text = uiState.whyRankedTooltip.ifEmpty { stringResource(R.string.why_ranking_tooltip) },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFEDEBF7),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            // Bottom Slot: Progress Slider, Transport Controls, DSP Pills
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Progress Slider
                val currentSec = uiState.currentPositionMs / 1000
                val totalSec = song.durationMs / 1000
                val sliderValue = if (song.durationMs > 0) {
                    (uiState.currentPositionMs.toFloat() / song.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Slider(
                    value = sliderValue,
                    onValueChange = { frac ->
                        onSeek((frac * song.durationMs).toLong())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_progress_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = NeonTurquoise,
                        activeTrackColor = NeonTurquoise,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", currentSec / 60, currentSec % 60),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Transport Controls: [Shuffle, Replay15, Play/Pause, Forward15, Repeat]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onTogglePlaybackMode,
                        modifier = Modifier.testTag("playback_mode_button")
                    ) {
                        val icon = when (uiState.playbackMode) {
                            PlaybackMode.REPEAT_ONE -> Icons.Default.RepeatOne
                            PlaybackMode.REPEAT_ALL -> Icons.Default.Repeat
                            PlaybackMode.SHUFFLE -> Icons.Default.Shuffle
                            PlaybackMode.OFF -> Icons.Default.Repeat
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = "Playback Mode",
                            tint = if (uiState.playbackMode != PlaybackMode.OFF) NeonTurquoise else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier.testTag("skip_prev_button")
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_skip_previous),
                            contentDescription = stringResource(R.string.skip_previous),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Main Play/Pause Button
                    FilledIconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(68.dp)
                            .testTag("main_play_pause_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = NeonTurquoise,
                            contentColor = Color(0xFF0D0A1A)
                        )
                    ) {
                        Icon(
                            painter = painterResource(if (uiState.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                            contentDescription = if (uiState.isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.testTag("skip_next_button")
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_skip_next),
                            contentDescription = stringResource(R.string.skip_next),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onOpenLyrics,
                        modifier = Modifier.testTag("lyrics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notes,
                            contentDescription = stringResource(R.string.lyrics),
                            tint = NeonTurquoise
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // DSP Signature Pills: [Voice Clarity, Dynamic Compression (DRC), Atmosphere]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterChip(
                        selected = uiState.isVocalBoost,
                        onClick = onToggleVocalBoost,
                        label = { Text("Voice Boost") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VividPurple.copy(alpha = 0.5f),
                            selectedLabelColor = NeonTurquoise
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    FilterChip(
                        selected = uiState.isDrcEnabled,
                        onClick = onToggleDrc,
                        label = { Text("DRC Night") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VividPurple.copy(alpha = 0.5f),
                            selectedLabelColor = NeonTurquoise
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    FilterChip(
                        selected = uiState.isAtmospherePlaying,
                        onClick = onOpenAtmosphere,
                        label = { Text("Atmosphere") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VividPurple.copy(alpha = 0.5f),
                            selectedLabelColor = NeonTurquoise
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}
