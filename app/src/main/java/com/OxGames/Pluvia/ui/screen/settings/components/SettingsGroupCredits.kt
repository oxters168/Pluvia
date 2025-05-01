package com.OxGames.Pluvia.ui.screen.settings.components

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import com.OxGames.Pluvia.ui.theme.PluviaTheme
import com.OxGames.Pluvia.ui.theme.settingsTileColors
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink

// TODO strings and urls

@Composable
internal fun SettingsGroupCredits() {
    val uriHandler = LocalUriHandler.current

    SettingsGroup(
        title = { Text(text = "Credits") },
        content = {
            SettingsMenuLink(
                colors = settingsTileColors(),
                icon = { Icon(imageVector = Icons.Default.Link, contentDescription = "Web Link") },
                title = { Text(text = "Longi94 - JavaSteam") },
                onClick = {
                    uriHandler.openUri("https://github.com/Longi94/JavaSteam")
                },
            )
            SettingsMenuLink(
                colors = settingsTileColors(),
                icon = { Icon(imageVector = Icons.Default.Link, contentDescription = "Web Link") },
                title = { Text(text = "KreitinnSoftware - MiceWine") },
                onClick = {
                    uriHandler.openUri("https://github.com/KreitinnSoftware/MiceWine-Application")
                },
            )
        },
    )
}

@Preview(showBackground = false, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
private fun Preview_SettingsCredits() {
    PluviaTheme {
        Surface {
            SettingsGroupCredits()
        }
    }
}
