package com.OxGames.Pluvia.ui.screen.settings.components

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PestControlRodent
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.OxGames.Pluvia.MiceWineUtils
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.ui.theme.PluviaTheme
import com.OxGames.Pluvia.ui.theme.settingsTileColors
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink

@Composable
fun SettingsGroupEmulation(
    onClick: (String) -> Unit,
) {
    val view = LocalView.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {

        SettingsGroup(
            title = { Text(text = "General") },
            content = {
                SettingsMenuLink(
                    colors = settingsTileColors(),
                    title = { Text(text = stringResource(R.string.debug_settings_title)) },
                    subtitle = { Text(text = stringResource(R.string.debug_settings_description)) },
                    icon = { Icon(imageVector = Icons.Default.BugReport, null) },
                    onClick = { onClick("mw_general_debug") },
                )
                SettingsMenuLink(
                    colors = settingsTileColors(),
                    title = { Text(text = stringResource(R.string.sound_settings_title)) },
                    subtitle = { Text(text = stringResource(R.string.sound_settings_description)) },
                    icon = { Icon(imageVector = Icons.AutoMirrored.Default.VolumeUp, contentDescription = null) },
                    onClick = { onClick("mw_general_sound") },
                )
                SettingsMenuLink(
                    colors = settingsTileColors(),
                    title = { Text(text = stringResource(R.string.driver_settings_title)) },
                    subtitle = { Text(text = stringResource(R.string.driver_settings_description)) },
                    icon = { Icon(imageVector = Icons.Default.DeveloperBoard, null) },
                    onClick = { onClick("mw_general_driver") },
                )
                SettingsMenuLink(
                    colors = settingsTileColors(),
                    title = { Text(text = stringResource(R.string.driver_info_title)) },
                    subtitle = { Text(text = stringResource(R.string.driver_info_description)) },
                    icon = { Icon(imageVector = Icons.Default.DeveloperBoard, null) },
                    onClick = { onClick("mw_general_driver_info") },
                )
                SettingsMenuLink(
                    colors = settingsTileColors(),
                    title = { Text(text = stringResource(R.string.env_settings_title)) },
                    subtitle = { Text(text = stringResource(R.string.env_settings_description)) },
                    icon = { Icon(imageVector = Icons.Default.Public, null) },
                    onClick = { onClick("mw_general_environment") },
                )
            },
        )

        SettingsGroup(
            title = { Text("Controller") },
            content = {
                SettingsMenuLink(
                    colors = settingsTileColors(),
                    icon = { Icon(imageVector = Icons.Default.VideogameAsset, contentDescription = null) },
                    title = { Text(text = stringResource(R.string.controller_mapper_title)) },
                    subtitle = { Text(text = stringResource(R.string.controller_mapper_description)) },
                    onClick = { onClick("mw_controller_mapper") },
                )
                SettingsMenuLink(
                    colors = settingsTileColors(),
                    icon = { Icon(imageVector = Icons.Default.VideogameAsset, contentDescription = null) },
                    title = { Text(text = stringResource(R.string.virtual_controller_mapper_title)) },
                    subtitle = { Text(text = stringResource(R.string.controller_virtual_mapper_description)) },
                    onClick = { onClick("mw_controller_virtual_mapper") },
                )
            },
        )

        // TODO Verify
        val deviceArch by remember {
            val value = if (view.isInEditMode) "aarch64" else MiceWineUtils.deviceArch
            mutableStateOf(value)
        }
        if (deviceArch != "x86_64") {
            SettingsGroup(
                title = { Text(text = "Box64") },
                content = {
                    SettingsMenuLink(
                        colors = settingsTileColors(),
                        title = { Text(text = stringResource(R.string.box64_preset_manager_title)) },
                        subtitle = { Text(text = stringResource(R.string.box64_preset_manager_description)) },
                        onClick = { onClick("mw_box64_preset") },
                    )
                },
            )
        }

        SettingsGroup(
            title = { Text(text = "Rat Package") },
            content = {
                SettingsMenuLink(
                    colors = settingsTileColors(),
                    icon = { Icon(imageVector = Icons.Default.PestControlRodent, contentDescription = null) },
                    title = { Text(text = stringResource(R.string.rat_manager_title)) },
                    subtitle = { Text(text = stringResource(R.string.rat_manager_description)) },
                    onClick = { onClick("mw_rat_package_manager") },
                )
                SettingsMenuLink(
                    colors = settingsTileColors(),
                    icon = { Icon(imageVector = Icons.Default.Download, contentDescription = null) },
                    title = { Text(text = stringResource(R.string.rat_downloader_title)) },
                    subtitle = { Text(text = stringResource(R.string.rat_downloader_description)) },
                    onClick = { onClick("mw_rat_package_downloader") },
                )
            },
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
private fun Preview_SettingsGroupEmulation() {
    PluviaTheme {
        Surface {
            SettingsGroupEmulation(onClick = {})
        }
    }
}
