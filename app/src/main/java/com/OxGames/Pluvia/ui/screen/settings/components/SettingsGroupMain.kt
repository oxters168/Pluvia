package com.OxGames.Pluvia.ui.screen.settings.components

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.OxGames.Pluvia.ui.screen.settings.SettingsCurrentPane
import com.OxGames.Pluvia.ui.theme.PluviaTheme
import com.OxGames.Pluvia.ui.theme.settingsTileColors
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink

// TODO strings

@Composable
fun SettingsGroupMain(
    onClick: (SettingsCurrentPane) -> Unit,
) {
    SettingsGroup(
        title = { Text("Main Settings") },
        content = {
            SettingsMenuLink(
                colors = settingsTileColors(),
                title = { Text(text = "Emulation (MiceWine)") },
                icon = { Icon(imageVector = Icons.Default.Terminal, contentDescription = null) },
                onClick = { onClick(SettingsCurrentPane.DETAIL_EMULATION) },
            )
            SettingsMenuLink(
                colors = settingsTileColors(),
                title = { Text(text = "Interface") },
                icon = { Icon(imageVector = Icons.Default.Extension, contentDescription = null) },
                onClick = { onClick(SettingsCurrentPane.DETAIL_INTERFACE) },
            )
            SettingsMenuLink(
                colors = settingsTileColors(),
                title = { Text(text = "Debug") },
                icon = { Icon(imageVector = Icons.Default.BugReport, contentDescription = null) },
                onClick = { onClick(SettingsCurrentPane.DETAIL_DEBUG) },
            )
        },
    )
}

@Preview(showSystemUi = false, showBackground = false, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
private fun Preview_SettingsGroupMain() {
    PluviaTheme {
        Surface {
            SettingsGroupMain(onClick = {})
        }
    }
}
