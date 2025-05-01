package com.OxGames.Pluvia.ui.screen.settings

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.OxGames.Pluvia.MiceWineUtils
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.ui.theme.PluviaTheme
import com.OxGames.Pluvia.ui.theme.settingsTileColors
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink

@Composable
fun SettingsGroupEmulation() {
    var showGeneralSettings by rememberSaveable { mutableStateOf(false) }
    var showControllerMapper by rememberSaveable { mutableStateOf(false) }
    var showVirtualControllerMapper by rememberSaveable { mutableStateOf(false) }
    var showBox64PresetManager by rememberSaveable { mutableStateOf(false) }
    var showRatPackageManager by rememberSaveable { mutableStateOf(false) }
    var showRatPackageDownloader by rememberSaveable { mutableStateOf(false) }

    SettingsGroup(title = { Text(text = stringResource(R.string.settings_group_emulation)) }) {
        SettingsMenuLink(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.general_settings)) },
            subtitle = { Text(text = stringResource(R.string.settings_description)) },
            onClick = { showGeneralSettings = true },
        )
        SettingsMenuLink(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.controller_mapper_title)) },
            subtitle = { Text(text = stringResource(R.string.controller_mapper_description)) },
            onClick = { showControllerMapper = true },
        )
        SettingsMenuLink(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.virtual_controller_mapper_title)) },
            subtitle = { Text(text = stringResource(R.string.controller_virtual_mapper_description)) },
            onClick = { showVirtualControllerMapper = true },
        )
        if (!MiceWineUtils.isX86) {
            SettingsMenuLink(
                colors = settingsTileColors(),
                title = { Text(text = stringResource(R.string.box64_preset_manager_title)) },
                subtitle = { Text(text = stringResource(R.string.box64_preset_manager_description)) },
                onClick = { showBox64PresetManager = true },
            )
        }
        SettingsMenuLink(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.rat_manager_title)) },
            subtitle = { Text(text = stringResource(R.string.rat_manager_description)) },
            onClick = { showRatPackageManager = true },
        )
        SettingsMenuLink(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.rat_downloader_title)) },
            subtitle = { Text(text = stringResource(R.string.rat_downloader_description)) },
            onClick = { showRatPackageDownloader = true },
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
private fun Preview_SettingsGroupEmulation() {
    PluviaTheme {
        Surface {
            SettingsGroupEmulation()
        }
    }
}
