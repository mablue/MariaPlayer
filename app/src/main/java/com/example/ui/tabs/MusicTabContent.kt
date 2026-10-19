package com.example.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.dsp.DspModes
import com.example.model.Song
import com.example.model.SortOption
import com.example.scoring.SmartScoring
import com.example.ui.MusicUiState
import com.example.ui.theme.NeonTurquoise
import com.example.ui.theme.VividPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicTabContent(
    uiState: MusicUiState,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onSearchChange: (String) -> Unit,
    onSortChange: (SortOption) -> Unit,
    onToggleMultiSelect: () -> Unit,
    onSongSelectionToggle: (Long) -> Unit,
    onCreatePlaylist: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onMoreLikeThis: (Song) -> Unit,
    onShareQr: (Song) -> Unit,
    onEditTags: (Song) -> Unit,
    onOpenWeeklyReplay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Search & Action Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonTurquoise) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonTurquoise,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedContainerColor = Color(0xFF161226),
                    unfocusedContainerColor = Color(0xFF130F22)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_text_field")
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onToggleMultiSelect,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (uiState.isMultiSelectActive) NeonTurquoise else Color(0xFF1E1838))
                    .testTag("toggle_multiselect_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Checklist,
                    contentDescription = stringResource(R.string.multi_select),
                    tint = if (uiState.isMultiSelectActive) Color(0xFF0D0A1A) else Color.White
                )
            }
        }

        // Multi-Select Action Strip
        if (uiState.isMultiSelectActive) {
            Surface(
                color = VividPurple.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${uiState.selectedSongIds.size} selected",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = NeonTurquoise
                    )
                    Row {
                        Button(
                            onClick = onCreatePlaylist,
                            enabled = uiState.selectedSongIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonTurquoise, contentColor = Color(0xFF0D0A1A)),
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .testTag("btn_create_playlist_from_selection")
                        ) {
                            Text("New Playlist", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = onAddToPlaylist,
                            enabled = uiState.selectedSongIds.isNotEmpty(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Add", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Sort Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SortChipItem("Smart Rank", SortOption.SMART_RANKING, uiState.sortOption) { onSortChange(it) }
            SortChipItem("Title", SortOption.TITLE, uiState.sortOption) { onSortChange(it) }
            SortChipItem("Artist", SortOption.ARTIST, uiState.sortOption) { onSortChange(it) }
            SortChipItem("Duration", SortOption.DURATION, uiState.sortOption) { onSortChange(it) }
            SortChipItem("Date Added", SortOption.DATE_ADDED, uiState.sortOption) { onSortChange(it) }
            SortChipItem("File Size", SortOption.FILE_SIZE, uiState.sortOption) { onSortChange(it) }
            SortChipItem("Quality", SortOption.QUALITY, uiState.sortOption) { onSortChange(it) }
        }

        // Library Memory Quick Pills Carousel (Compact, elegant, not large!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Memory Card 1: Weekly Replay
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF221A3D),
                modifier = Modifier
                    .clickable { onOpenWeeklyReplay() }
                    .testTag("weekly_replay_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Insights, contentDescription = null, tint = NeonTurquoise, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Weekly Replay", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }
            }

            // Memory Card 2: Recently Forgotten
            val forgottenCount = DspModes.findRecentlyForgotten(uiState.songs).size
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1736),
                modifier = Modifier.clickable { }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFFB5A4FF), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Rediscover ($forgottenCount)", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }

            // Memory Card 3: Hidden Gems
            val gemsCount = DspModes.findHiddenGems(uiState.songs).size
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1736),
                modifier = Modifier.clickable { }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Diamond, contentDescription = null, tint = Color(0xFFFF5470), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hidden Gems ($gemsCount)", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
        }

        // Song List
        val displayList = uiState.filteredSongs.ifEmpty { uiState.songs }
        if (displayList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_tracks_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag("songs_lazy_column"),
                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayList, key = { it.id }) { song ->
                    val isSelected = uiState.selectedSongIds.contains(song.id)
                    val isPlayingThis = uiState.currentSong?.id == song.id
                    val (score, _) = SmartScoring.calculateContinuousRank(song, uiState.currentSong?.key)

                    SongItemCard(
                        song = song,
                        rankScorePercent = (score * 100).toInt(),
                        isPlayingThis = isPlayingThis,
                        isMultiSelectActive = uiState.isMultiSelectActive,
                        isSelected = isSelected,
                        onClick = {
                            if (uiState.isMultiSelectActive) {
                                onSongSelectionToggle(song.id)
                            } else {
                                onSongClick(song)
                            }
                        },
                        onLongClick = {
                            onSongLongClick(song)
                        },
                        onToggleFavorite = { onToggleFavorite(song.id) },
                        onMoreLikeThis = { onMoreLikeThis(song) },
                        onShareQr = { onShareQr(song) },
                        onEditTags = { onEditTags(song) }
                    )
                }
            }
        }
    }
}

@Composable
fun SortChipItem(
    label: String,
    option: SortOption,
    current: SortOption,
    onSelect: (SortOption) -> Unit
) {
    FilterChip(
        selected = option == current,
        onClick = { onSelect(option) },
        label = { Text(label, fontSize = 12.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = NeonTurquoise,
            selectedLabelColor = Color(0xFF0D0A1A),
            containerColor = Color(0xFF161226),
            labelColor = Color(0xFFEDEBF7)
        )
    )
}

@Composable
fun SongItemCard(
    song: Song,
    rankScorePercent: Int,
    isPlayingThis: Boolean,
    isMultiSelectActive: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoreLikeThis: () -> Unit,
    onShareQr: () -> Unit,
    onEditTags: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlayingThis) Color(0xFF261D45) else Color(0xFF161226)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("song_item_${song.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelectActive) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(checkedColor = NeonTurquoise)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Thumbnail with Camelot key badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            if (isPlayingThis) listOf(NeonTurquoise, VividPurple)
                            else listOf(VividPurple.copy(alpha = 0.5f), Color(0xFF1E1838))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(if (isPlayingThis) R.drawable.ic_pause else R.drawable.ic_music_note),
                    contentDescription = null,
                    tint = if (isPlayingThis) Color(0xFF0D0A1A) else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isPlayingThis) NeonTurquoise else MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Camelot Key Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = song.key,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NeonTurquoise,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${rankScorePercent}% match",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Favorite Icon
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (song.isFavorite) Color(0xFFFF5470) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Overflow Context Menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Song options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("More like this (Camelot Mix)") },
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonTurquoise) },
                        onClick = {
                            menuExpanded = false
                            onMoreLikeThis()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share via QR") },
                        leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onShareQr()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit tags") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEditTags()
                        }
                    )
                }
            }
        }
    }
}
