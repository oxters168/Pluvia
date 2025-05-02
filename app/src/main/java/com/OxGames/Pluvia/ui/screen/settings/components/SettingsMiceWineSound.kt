package com.OxGames.Pluvia.ui.screen.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.OxGames.Pluvia.MiceWineUtils
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.ui.component.topbar.BackButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsMiceWineSound(
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.sound_settings_title)) },
                navigationIcon = { BackButton(onClick = onBack) },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsItemList(
                title = stringResource(R.string.select_audio_sink),
                description = null,
                spinnerOptions = arrayOf("SLES", "AAudio"),
                type = SettingsItemType.SPINNER,
                defaultValue = MiceWineUtils.PA_SINK_DEFAULT_VALUE,
                key = MiceWineUtils.PA_SINK,
            )
        }
    }
}
