package com.example.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.PodcastEpisode
import com.example.ui.MusicUiState
import com.example.ui.theme.NeonTurquoise
import com.example.ui.theme.VividPurple
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastsTabContent(
    uiState: MusicUiState,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    onToggleSpeed: (PodcastEpisode) -> Unit,
    onToggleVoiceClarity: (PodcastEpisode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("podcasts_tab_content")
    ) {
        // Top banner: Voice clarity & podcast loudness norm indicator
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1736),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Podcasts,
                    contentDescription = null,
                    tint = NeonTurquoise,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Podcast Audio Engine",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Auto Voice Clarity + Dialogue -16 LUFS Normalization Active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(uiState.podcasts, key = { it.id }) { ep ->
                PodcastEpisodeCard(
                    episode = ep,
                    onClick = { onEpisodeClick(ep) },
                    onToggleSpeed = { onToggleSpeed(ep) },
                    onToggleVoiceClarity = { onToggleVoiceClarity(ep) }
                )
            }
        }
    }
}

@Composable
fun PodcastEpisodeCard(
    episode: PodcastEpisode,
    onClick: () -> Unit,
    onToggleSpeed: () -> Unit,
    onToggleVoiceClarity: () -> Unit
) {
    var speedState by remember { mutableFloatStateOf(episode.playbackSpeed) }
    var clarityState by remember { mutableStateOf(episode.voiceClarityEnabled) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161226)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("podcast_episode_${episode.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(VividPurple.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = NeonTurquoise
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = episode.podcastName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = episode.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color(0xFFA6A0C2)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            val totalMins = episode.durationMs / (1000 * 60)
            val currentMins = episode.playbackPositionMs / (1000 * 60)
            val progress = if (episode.durationMs > 0) {
                (episode.playbackPositionMs.toFloat() / episode.durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = NeonTurquoise,
                trackColor = Color.White.copy(alpha = 0.08f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Podcast Controls Strip: Resume position, Speed pill (1.3x), Voice Clarity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentMins}m / ${totalMins}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Voice Clarity Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (clarityState) VividPurple.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f),
                        modifier = Modifier
                            .clickable {
                                clarityState = !clarityState
                                onToggleVoiceClarity()
                            }
                            .padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hearing,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (clarityState) NeonTurquoise else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Voice Clarity",
                                fontSize = 11.sp,
                                color = if (clarityState) NeonTurquoise else Color.Gray
                            )
                        }
                    }

                    // Speed Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.clickable {
                            speedState = when (speedState) {
                                1.0f -> 1.2f
                                1.2f -> 1.3f
                                1.3f -> 1.5f
                                else -> 1.0f
                            }
                            onToggleSpeed()
                        }
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1fx", speedState),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonTurquoise,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
