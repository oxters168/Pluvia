package com.OxGames.Pluvia.ui.screen.downloads.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.OxGames.Pluvia.data.LibraryItem
import com.OxGames.Pluvia.ui.data.DownloadsState
import com.OxGames.Pluvia.ui.theme.PluviaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsList(
    modifier: Modifier = Modifier,
    state: DownloadsState,
    onRefresh: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    PullToRefreshBox(
        modifier = modifier,
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(Modifier.fillMaxSize()) {
            items(state.items, key = { it.index }) {
                ListItem(
                    headlineContent = { Text(text = it.name) },
                    supportingContent = { Text(text = "Beans") },
                    leadingContent = {
                        Icon(imageVector = Icons.Default.Games, contentDescription = null)
                    },
                    trailingContent = {
                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Re-download Game") },
                                    onClick = {
                                        expanded = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Wipe container & settings") },
                                    onClick = {
                                        expanded = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Remove Game") },
                                    onClick = {
                                        expanded = false
                                    },
                                )
                            }
                        }
                    },
                )
            }
        }

        if (state.items.isEmpty()) {
            Surface(
                modifier = Modifier.padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 8.dp,
            ) {
                Text(
                    modifier = Modifier.padding(24.dp),
                    text = "No games listed",
                )
            }
        }
    }
}

@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES or android.content.res.Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
private fun Preview_DownloadsList() {
    PluviaTheme {
        DownloadsList(
            state = DownloadsState(
                isRefreshing = true,
                items = List(20) { idx ->
                    LibraryItem(
                        index = idx,
                        appId = idx,
                        name = "Game Name $idx",
                    )
                },
            ),
            onRefresh = { },
        )
    }
}
