package com.OxGames.Pluvia.ui.screen.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.OxGames.Pluvia.PrefManager
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.ui.component.dialog.Box64PresetsDialog
import com.OxGames.Pluvia.ui.component.dialog.ContainerConfigDialog
import com.OxGames.Pluvia.ui.component.dialog.OrientationDialog
import com.OxGames.Pluvia.ui.theme.settingsTileColors
import com.OxGames.Pluvia.utils.ContainerUtils
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink

@Composable
fun SettingsGroupEmulation() {
    SettingsGroup(title = { Text(text = stringResource(R.string.settings_group_emulation)) }) {
        var showConfigDialog by rememberSaveable { mutableStateOf(false) }
        var showOrientationDialog by rememberSaveable { mutableStateOf(false) }
        var showBox64PresetsDialog by rememberSaveable { mutableStateOf(false) }

        OrientationDialog(
            openDialog = showOrientationDialog,
            onDismiss = { showOrientationDialog = false },
        )

        ContainerConfigDialog(
            visible = showConfigDialog,
            title = stringResource(R.string.dialog_title_default_container_config),
            initialConfig = ContainerUtils.getDefaultContainerData(),
            onDismiss = { showConfigDialog = false },
            onSave = {
                showConfigDialog = false
                ContainerUtils.setDefaultContainerData(it)
            },
            onReset = {
                PrefManager.resetDefaultContainer()
                showConfigDialog = false
            },
        )

        Box64PresetsDialog(
            visible = showBox64PresetsDialog,
            onDismissRequest = { showBox64PresetsDialog = false },
        )

        SettingsMenuLink(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.settings_allowed_orientations_title)) },
            subtitle = { Text(text = stringResource(R.string.settings_allowed_orientations_subtitle)) },
            onClick = { showOrientationDialog = true },
        )
        SettingsMenuLink(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.settings_modify_default_config_title)) },
            subtitle = { Text(text = stringResource(R.string.settings_modify_default_config_subtitle)) },
            onClick = { showConfigDialog = true },
        )
        SettingsMenuLink(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.settings_box64_presets_title)) },
            subtitle = { Text(text = stringResource(R.string.settings_box64_presets_subtitle)) },
            onClick = { showBox64PresetsDialog = true },
        )
    }
}
