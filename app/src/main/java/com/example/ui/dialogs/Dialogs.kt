package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.dsp.DspModes
import com.example.model.Song
import com.example.scoring.SmartScoring
import com.example.ui.MusicUiState
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.NeonTurquoise
import com.example.ui.theme.VividPurple

@Composable
fun LyricsDialog(
    uiState: MusicUiState,
    onDismiss: () -> Unit
) {
    val lyrics = uiState.activeLyrics
    val song = uiState.currentSong

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF161226),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 540.dp)
                .testTag("lyrics_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with Language & Poem Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = song?.title ?: "Lyrics",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Lang: ${lyrics?.detectedLanguage?.uppercase() ?: "AUTO"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonTurquoise
                            )
                            if (lyrics?.isPoem == true) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = VividPurple.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "Classical Poem",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (lyrics == null || lyrics.lines.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("No synchronized lyrics found.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(lyrics.lines) { line ->
                            val isCurrent = uiState.currentPositionMs >= line.timestampMs &&
                                    uiState.currentPositionMs < line.timestampMs + 8000L

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = line.text,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = if (isCurrent) 18.sp else 15.sp
                                    ),
                                    color = if (isCurrent) NeonTurquoise else Color(0xFFC7C2E0)
                                )

                                if (!line.translation.isNullOrBlank()) {
                                    Text(
                                        text = line.translation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF8E88AB)
                                    )
                                }

                                if (line.metaphors.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.padding(top = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = NeonTurquoise
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = line.metaphors.first().second,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonTurquoise.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTurquoise, contentColor = DeepPurpleDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun CarModeDialog(
    uiState: MusicUiState,
    onTogglePlayPause: () -> Unit,
    onSeekDelta: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF0D0A1A),
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .testTag("car_mode_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = NeonTurquoise, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CAR MODE", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), color = Color.White)
                    }

                    FilledTonalButton(onClick = onDismiss) {
                        Text("Exit")
                    }
                }

                // Track Info Large
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.currentSong?.title ?: "No Track",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.currentSong?.artist ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        color = NeonTurquoise,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = VividPurple.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "Auto DRC & Vocal Boost Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Jumbo Buttons for driving accessibility
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Jumbo Play / Pause
                    FilledIconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(110.dp)
                            .testTag("car_play_pause_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = NeonTurquoise,
                            contentColor = DeepPurpleDark
                        )
                    ) {
                        Icon(
                            painter = painterResource(if (uiState.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                            contentDescription = "Car Play/Pause",
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Secondary Jumbo Controls (-15s, Prev, Next, +15s)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onSeekDelta(-15_000L) },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_replay_10),
                                contentDescription = "Rewind 15s",
                                modifier = Modifier.size(36.dp),
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = onPrevious,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_skip_previous),
                                contentDescription = "Previous",
                                modifier = Modifier.size(40.dp),
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = onNext,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_skip_next),
                                contentDescription = "Next",
                                modifier = Modifier.size(40.dp),
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { onSeekDelta(15_000L) },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_forward_10),
                                contentDescription = "Forward 15s",
                                modifier = Modifier.size(36.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    uiState: MusicUiState,
    onSetAnalogProfile: (DspModes.AnalogProfile) -> Unit,
    onSetReverbSpace: (DspModes.ReverbSpace) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF161226),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .testTag("settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Algorithmic Intelligence (A-O) Tuning
                Text("Algorithmic Engine (A-O)", style = MaterialTheme.typography.titleSmall, color = NeonTurquoise)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Zero heavy models • Pure microsecond Kotlin formulas", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                Spacer(modifier = Modifier.height(12.dp))

                // Reset to Defaults Button
                OutlinedButton(
                    onClick = {
                        SmartScoring.resetToDefaults()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonTurquoise)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.reset_to_defaults))
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                // Analog Emulation (Vinyl / Cassette)
                Text(stringResource(R.string.feature_vinyl), style = MaterialTheme.typography.titleSmall, color = Color.White)
                Text(stringResource(R.string.feature_vinyl_desc), style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        DspModes.AnalogProfile.OFF to "Off",
                        DspModes.AnalogProfile.VINYL to "Vinyl",
                        DspModes.AnalogProfile.CASSETTE to "Cassette"
                    ).forEach { (profile, label) ->
                        FilterChip(
                            selected = uiState.analogProfile == profile,
                            onClick = { onSetAnalogProfile(profile) },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Room Reverb
                Text(stringResource(R.string.feature_room_reverb), style = MaterialTheme.typography.titleSmall, color = Color.White)
                Text(stringResource(R.string.feature_room_reverb_desc), style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        DspModes.ReverbSpace.OFF to "Off",
                        DspModes.ReverbSpace.SMALL_ROOM to "Room",
                        DspModes.ReverbSpace.CONCERT_HALL to "Hall"
                    ).forEach { (space, label) ->
                        FilterChip(
                            selected = uiState.reverbSpace == space,
                            onClick = { onSetReverbSpace(space) },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTurquoise, contentColor = DeepPurpleDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
fun AtmosphereDialog(
    uiState: MusicUiState,
    onSelectAtmosphere: (DspModes.AtmosphereType) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF161226),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("atmosphere_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.atmosphere_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Add gentle background soundscapes to quiet listening sessions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                listOf(
                    DspModes.AtmosphereType.OFF to "Off (Disabled)",
                    DspModes.AtmosphereType.RAIN to "Rain on Window",
                    DspModes.AtmosphereType.FIREPLACE to "Fireplace Hearth",
                    DspModes.AtmosphereType.CAFE to "Acoustic Café Ambiance"
                ).forEach { (type, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectAtmosphere(type) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.atmosphereType == type,
                            onClick = { onSelectAtmosphere(type) },
                            colors = RadioButtonDefaults.colors(selectedColor = NeonTurquoise)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTurquoise, contentColor = DeepPurpleDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply")
                }
            }
        }
    }
}

@Composable
fun WeeklyReplayDialog(
    uiState: MusicUiState,
    onDismiss: () -> Unit
) {
    val stats = DspModes.calculateWeeklyStats(uiState.songs)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1B1438),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("weekly_replay_dialog")
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "WEEKLY REPLAY",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                    color = NeonTurquoise
                )
                Text(
                    text = "Your Listening Journey This Week",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF241B4A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Top Artist: ${stats.topArtist}", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Favorite Genre: ${stats.topGenre}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFEDEBF7))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Peak Hour: 8:00 PM (Circadian Evening)", style = MaterialTheme.typography.bodyMedium, color = NeonTurquoise)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Total Listening: ${stats.totalMinutesPlayed} minutes", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFB5A4FF))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTurquoise, contentColor = DeepPurpleDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun QrShareDialog(
    payload: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF161226),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("qr_share_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.feature_qr_share),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.feature_qr_share_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Visual Representation of QR Code Matrix
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = "QR Code",
                        modifier = Modifier.fillMaxSize(),
                        tint = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = payload.take(60) + "…",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonTurquoise,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTurquoise, contentColor = DeepPurpleDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
fun CreatePlaylistDialog(
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF161226),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.create_playlist),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonTurquoise,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onCreate(name) },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonTurquoise, contentColor = DeepPurpleDark)
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
fun EditSongTagsDialog(
    song: Song,
    onSave: (Song) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album by remember { mutableStateOf(song.album) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF161226),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.edit_tags),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Album") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(song.copy(title = title, artist = artist, album = album))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonTurquoise, contentColor = DeepPurpleDark)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
