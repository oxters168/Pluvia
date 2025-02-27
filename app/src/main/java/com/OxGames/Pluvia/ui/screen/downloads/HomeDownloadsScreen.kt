package com.OxGames.Pluvia.ui.screen.downloads

import android.content.res.Configuration
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.OxGames.Pluvia.ui.component.topbar.AccountButton
import com.OxGames.Pluvia.ui.component.topbar.BackButton
import com.OxGames.Pluvia.ui.model.DownloadsViewModel
import com.OxGames.Pluvia.ui.screen.downloads.components.DownloadsList
import com.OxGames.Pluvia.ui.theme.PluviaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDownloadsScreen(
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    viewModel: DownloadsViewModel = viewModel(),
) {
    val snackbarHost = remember { SnackbarHostState() }
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Downloads") },
                actions = {
                    AccountButton(
                        onSettings = onSettings,
                        onLogout = onLogout,
                    )
                },
                navigationIcon = { BackButton(onClick = { onBackPressedDispatcher?.onBackPressed() }) },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = {
                DownloadsList(
                    state = state,
                    onRefresh = {
                    },
                )
            },
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(
    device = "spec:width=1920px,height=1080px,dpi=440",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
) // Odin2 Mini
@Composable
private fun Preview_DownloadsScreenContent() {
    PluviaTheme {
        Surface {
            HomeDownloadsScreen(
                onSettings = {},
                onLogout = {},
            )
        }
    }
}
