package com.OxGames.Pluvia.ui.screen.settings.components

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.OxGames.Pluvia.Constants
import com.OxGames.Pluvia.PrefManager
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.ui.theme.PluviaTheme
import com.OxGames.Pluvia.ui.theme.settingsTileColors
import com.OxGames.Pluvia.ui.theme.settingsTileColorsAlt
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSwitch

// TODO strings

@Composable
fun SettingsGroupInfo() {
    SettingsGroup(title = { Text(text = stringResource(R.string.settings_group_info)) }) {
        val uriHandler = LocalUriHandler.current
        var askForTip by rememberSaveable { mutableStateOf(!PrefManager.tipped) }

        SettingsMenuLink(
            colors = settingsTileColors(),
            icon = { Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = stringResource(R.string.settings_tip_title)) },
            title = { Text(text = stringResource(R.string.settings_tip_title)) },
            subtitle = { Text(text = stringResource(R.string.settings_tip_subtitle)) },
            onClick = {
                uriHandler.openUri(Constants.Misc.KO_FI_LINK)
                askForTip = false
                PrefManager.tipped = !askForTip
            },
        )

        SettingsSwitch(
            colors = settingsTileColorsAlt(),
            icon = { Icon(imageVector = Icons.Default.MoneyOff, contentDescription = stringResource(R.string.settings_tip_title)) },
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
            icon = { Icon(imageVector = Icons.Default.Link, contentDescription = "Web Link") },
            title = { Text(text = stringResource(R.string.settings_show_source_title)) },
            subtitle = { Text(text = stringResource(R.string.settings_show_source_subtitle)) },
            onClick = { uriHandler.openUri(Constants.Misc.GITHUB_LINK) },
        )

        SettingsMenuLink(
            colors = settingsTileColors(),
            icon = { Icon(imageVector = Icons.Default.Link, contentDescription = "Web Link") },
            title = { Text(text = stringResource(R.string.settings_show_privacy_title)) },
            subtitle = { Text(text = stringResource(R.string.settings_show_privacy_subtitle)) },
            onClick = { uriHandler.openUri(Constants.Misc.PRIVACY_LINK) },
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
private fun Preview_SettingsGroupInfo() {
    val context = LocalContext.current
    PrefManager.init(context)
    PluviaTheme {
        Surface {
            SettingsGroupInfo()
        }
    }
}
