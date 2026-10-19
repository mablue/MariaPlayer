package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.dsp.DspModes
import com.example.model.MainTab
import com.example.model.Song
import com.example.ui.components.EmojiExplosion
import com.example.ui.components.MiniController
import com.example.ui.components.PlayerView
import com.example.ui.dialogs.*
import com.example.ui.tabs.*
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.NeonTurquoise
import com.example.ui.theme.VividPurple
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isPlayerExpanded by remember { mutableStateOf(false) }

    // 16 November check for Maryam's birthday
    val isReal16November = remember {
        val cal = Calendar.getInstance()
        cal.get(Calendar.MONTH) == Calendar.NOVEMBER && cal.get(Calendar.DAY_OF_MONTH) == 16
    }
    var triggerMaryamBirthdayCelebration by remember { mutableStateOf(isReal16November) }
    var showEmojiExplosion by remember { mutableStateOf(isReal16November) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_scaffold"),
        containerColor = DeepPurpleDark,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // MariaPlayer Top Branding Header with Tuberose flower icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            // Tap logo to trigger/replay Maryam celebration anytime!
                            triggerMaryamBirthdayCelebration = true
                            showEmojiExplosion = true
                        }
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_maria_flower),
                            contentDescription = "موزیک پلیر گل مریم",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "موزیک پلیر گل مریم",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonTurquoise
                            )
                        }
                    }

                    // Birthday Celebration Trigger Button (16 November)
                    FilledTonalButton(
                        onClick = {
                            triggerMaryamBirthdayCelebration = true
                            showEmojiExplosion = true
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (triggerMaryamBirthdayCelebration) Color(0xFFFF5470) else VividPurple.copy(alpha = 0.4f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_hbd_maryam")
                    ) {
                        Text(text = "HBD Maryam 🎉", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Celebratory Maryam Birthday Banner on 16 November or when active
                AnimatedVisibility(visible = triggerMaryamBirthdayCelebration) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF32143B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🎂 16 November: Happy Birthday Maryam! 🌸",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFFF85A1)
                                )
                                Text(
                                    text = "تولدت مبارک مریم جان! سالی سرشار از نغمه‌های دلنشین و شادکامی",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFCEEF5)
                                )
                            }
                            IconButton(
                                onClick = { showEmojiExplosion = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Celebration, contentDescription = "Confetti", tint = NeonTurquoise)
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Floating MiniController pill above NavigationBar
                if (!isPlayerExpanded && uiState.currentSong != null) {
                    MiniController(
                        song = uiState.currentSong,
                        isPlaying = uiState.isPlaying,
                        currentPositionMs = uiState.currentPositionMs,
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.playNextTrack() },
                        onClick = { isPlayerExpanded = true }
                    )
                }

                // Material 3 Bottom Navigation Bar with Harmonic Mixing
                NavigationBar(
                    containerColor = Color(0xFF130F24),
                    tonalElevation = 6.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = uiState.selectedTab == MainTab.MUSIC,
                        onClick = { viewModel.selectTab(MainTab.MUSIC) },
                        icon = { Icon(Icons.Default.MusicNote, contentDescription = stringResource(R.string.tab_music)) },
                        label = { Text(stringResource(R.string.tab_music)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeepPurpleDark,
                            selectedTextColor = NeonTurquoise,
                            indicatorColor = NeonTurquoise,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_music")
                    )

                    NavigationBarItem(
                        selected = uiState.selectedTab == MainTab.HARMONIC,
                        onClick = { viewModel.selectTab(MainTab.HARMONIC) },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = stringResource(R.string.tab_harmonic_mixing)) },
                        label = { Text("Harmonic") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeepPurpleDark,
                            selectedTextColor = NeonTurquoise,
                            indicatorColor = NeonTurquoise,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_harmonic")
                    )

                    NavigationBarItem(
                        selected = uiState.selectedTab == MainTab.PODCASTS,
                        onClick = { viewModel.selectTab(MainTab.PODCASTS) },
                        icon = { Icon(Icons.Default.Podcasts, contentDescription = stringResource(R.string.tab_podcasts)) },
                        label = { Text(stringResource(R.string.tab_podcasts)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeepPurpleDark,
                            selectedTextColor = NeonTurquoise,
                            indicatorColor = NeonTurquoise,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_podcasts")
                    )

                    NavigationBarItem(
                        selected = uiState.selectedTab == MainTab.AUDIOBOOKS,
                        onClick = { viewModel.selectTab(MainTab.AUDIOBOOKS) },
                        icon = { Icon(Icons.Default.MenuBook, contentDescription = stringResource(R.string.tab_audiobooks)) },
                        label = { Text(stringResource(R.string.tab_audiobooks)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeepPurpleDark,
                            selectedTextColor = NeonTurquoise,
                            indicatorColor = NeonTurquoise,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_audiobooks")
                    )

                    NavigationBarItem(
                        selected = uiState.selectedTab == MainTab.FOLDERS,
                        onClick = { viewModel.selectTab(MainTab.FOLDERS) },
                        icon = { Icon(Icons.Default.Folder, contentDescription = stringResource(R.string.tab_folders)) },
                        label = { Text(stringResource(R.string.tab_folders)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeepPurpleDark,
                            selectedTextColor = NeonTurquoise,
                            indicatorColor = NeonTurquoise,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_folders")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // EXHAUSTIVE WHEN ON MainTab ENUM (Mandatory rule #6)
            when (uiState.selectedTab) {
                MainTab.MUSIC -> {
                    MusicTabContent(
                        uiState = uiState,
                        onSongClick = { song ->
                            viewModel.playSong(song)
                            isPlayerExpanded = true
                        },
                        onSongLongClick = { song ->
                            viewModel.toggleMultiSelect()
                            viewModel.toggleSongSelection(song.id)
                        },
                        onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                        onSearchChange = { q -> viewModel.setSearchQuery(q) },
                        onSortChange = { s -> viewModel.setSortOption(s) },
                        onToggleMultiSelect = { viewModel.toggleMultiSelect() },
                        onSongSelectionToggle = { id -> viewModel.toggleSongSelection(id) },
                        onCreatePlaylist = { viewModel.setShowCreatePlaylistDialog(true) },
                        onAddToPlaylist = { viewModel.addSelectedToPlaylist("Favorites") },
                        onMoreLikeThis = { seed ->
                            val playlist = DspModes.generatePlaylistFromSeed(seed, uiState.songs)
                            if (playlist.isNotEmpty()) viewModel.playSong(playlist.first())
                        },
                        onShareQr = { song ->
                            viewModel.sharePlaylistAsQr("Instant Mix")
                        },
                        onEditTags = { song ->
                            viewModel.setEditingSong(song)
                        },
                        onOpenWeeklyReplay = {
                            viewModel.setShowWeeklyReplayDialog(true)
                        }
                    )
                }
                MainTab.HARMONIC -> {
                    HarmonicMixingScreen(
                        uiState = uiState,
                        onSongClick = { song ->
                            viewModel.playSong(song)
                            isPlayerExpanded = true
                        }
                    )
                }
                MainTab.PODCASTS -> {
                    PodcastsTabContent(
                        uiState = uiState,
                        onEpisodeClick = { ep ->
                            // Play podcast episode safely
                        },
                        onToggleSpeed = { ep ->
                            // Cycle episode speed
                        },
                        onToggleVoiceClarity = { ep ->
                            viewModel.toggleVocalBoost()
                        }
                    )
                }
                MainTab.AUDIOBOOKS -> {
                    AudiobooksTabContent(
                        uiState = uiState,
                        onTrackClick = { track ->
                            // Play audiobook track safely
                        }
                    )
                }
                MainTab.FOLDERS -> {
                    FoldersTabContent(
                        uiState = uiState,
                        onSongClick = { song ->
                            viewModel.playSong(song)
                            isPlayerExpanded = true
                        }
                    )
                }
            }

            // Fullscreen PlayerView Modal with Animated Transition
            AnimatedVisibility(
                visible = isPlayerExpanded && uiState.currentSong != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                PlayerView(
                    uiState = uiState,
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onSeek = { pos -> viewModel.seekTo(pos) },
                    onNext = { viewModel.playNextTrack() },
                    onPrevious = { viewModel.playPreviousTrack() },
                    onTogglePlaybackMode = { viewModel.togglePlaybackMode() },
                    onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                    onToggleVocalBoost = { viewModel.toggleVocalBoost() },
                    onToggleDrc = { viewModel.toggleDrc() },
                    onOpenLyrics = { viewModel.setShowLyricsDialog(true) },
                    onOpenCarMode = { viewModel.setShowCarModeDialog(true) },
                    onOpenSettings = { viewModel.setShowSettingsDialog(true) },
                    onOpenAtmosphere = { viewModel.setShowAtmosphereDialog(true) },
                    onClosePlayer = { isPlayerExpanded = false }
                )
            }

            // Emoji Explosions on 16 November or when Maryam's day is celebrated
            EmojiExplosion(
                isActive = showEmojiExplosion,
                onFinished = { showEmojiExplosion = false }
            )
        }
    }

    // Interactive Dialogs
    if (uiState.showLyricsDialog) {
        LyricsDialog(
            uiState = uiState,
            onDismiss = { viewModel.setShowLyricsDialog(false) }
        )
    }

    if (uiState.showCarModeDialog) {
        CarModeDialog(
            uiState = uiState,
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onSeekDelta = { delta -> viewModel.seekTo(uiState.currentPositionMs + delta) },
            onNext = { viewModel.playNextTrack() },
            onPrevious = { viewModel.playPreviousTrack() },
            onDismiss = { viewModel.setShowCarModeDialog(false) }
        )
    }

    if (uiState.showSettingsDialog) {
        SettingsDialog(
            uiState = uiState,
            onSetAnalogProfile = { p -> viewModel.setAnalogProfile(p) },
            onSetReverbSpace = { r -> viewModel.setReverbSpace(r) },
            onDismiss = { viewModel.setShowSettingsDialog(false) }
        )
    }

    if (uiState.showAtmosphereDialog) {
        AtmosphereDialog(
            uiState = uiState,
            onSelectAtmosphere = { a -> viewModel.setAtmosphere(a) },
            onDismiss = { viewModel.setShowAtmosphereDialog(false) }
        )
    }

    if (uiState.showWeeklyReplayDialog) {
        WeeklyReplayDialog(
            uiState = uiState,
            onDismiss = { viewModel.setShowWeeklyReplayDialog(false) }
        )
    }

    if (uiState.showQrShareDialog) {
        QrShareDialog(
            payload = uiState.qrSharePayload,
            onDismiss = { viewModel.setShowLyricsDialog(false) }
        )
    }

    if (uiState.showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onCreate = { name -> viewModel.createPlaylist(name) },
            onDismiss = { viewModel.setShowCreatePlaylistDialog(false) }
        )
    }

    uiState.editingSong?.let { songToEdit ->
        EditSongTagsDialog(
            song = songToEdit,
            onSave = { updated ->
                viewModel.setEditingSong(null)
            },
            onDismiss = { viewModel.setEditingSong(null) }
        )
    }
}
