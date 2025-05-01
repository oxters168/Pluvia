package com.OxGames.Pluvia.ui.screen.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.OxGames.Pluvia.MiceWineUtils
import com.OxGames.Pluvia.R

@Composable
internal fun SettingsMiceWineSound() {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
