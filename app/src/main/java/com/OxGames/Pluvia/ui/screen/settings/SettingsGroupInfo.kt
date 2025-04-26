package com.OxGames.Pluvia.ui.screen.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.OxGames.Pluvia.Constants
import com.OxGames.Pluvia.PrefManager
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.ui.component.dialog.MessageDialog
import com.OxGames.Pluvia.ui.theme.settingsTileColors
import com.OxGames.Pluvia.ui.theme.settingsTileColorsAlt
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSwitch

@Composable
fun SettingsGroupInfo() {
    SettingsGroup(title = { Text(text = stringResource(R.string.settings_group_info)) }) {
        val uriHandler = LocalUriHandler.current
        var askForTip by rememberSaveable { mutableStateOf(!PrefManager.tipped) }
        var showLibrariesDialog by rememberSaveable { mutableStateOf(false) }

        MessageDialog(
            visible = showLibrariesDialog,
            onDismissRequest = { showLibrariesDialog = false },
            onConfirmClick = { showLibrariesDialog = false },
            confirmBtnText = R.string.close,
            icon = Icons.Default.Info,
            title = R.string.dialog_title_libraries,
            message = stringResource(R.string.dialog_message_libraries),
        )

        SettingsMenuLink(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.settings_tip_title)) },
            subtitle = { Text(text = stringResource(R.string.settings_tip_subtitle)) },
            icon = { Icon(imageVector = Icons.Filled.MonetizationOn, contentDescription = stringResource(R.string.settings_tip_title)) },
            onClick = {
                uriHandler.openUri(Constants.Misc.KO_FI_LINK)
                askForTip = false
                PrefManager.tipped = !askForTip
            },
        )

        SettingsSwitch(
            colors = settingsTileColorsAlt(),
            state = askForTip,
            title = { Text(text = stringResource(R.string.settings_show_tip_title)) },
            subtitle = { Text(text = stringResource(R.string.settings_show_tip_subtitle)) },
            onCheckedChange = {
                askForTip = it
                PrefManager.tipped = !askForTip
            },
        )

        SettingsMenuLink(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.settings_show_source_title)) },
            subtitle = { Text(text = stringResource(R.string.settings_show_source_subtitle)) },
            onClick = { uriHandler.openUri(Constants.Misc.GITHUB_LINK) },
        )

        SettingsMenuLink(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.settings_show_libraries_title)) },
            subtitle = { Text(text = stringResource(R.string.settings_show_libraries_subtitle)) },
            onClick = { showLibrariesDialog = true },
        )

        SettingsMenuLink(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.settings_show_privacy_title)) },
            subtitle = { Text(text = stringResource(R.string.settings_show_privacy_subtitle)) },
            onClick = { uriHandler.openUri(Constants.Misc.PRIVACY_LINK) },
        )
    }
}
