package com.OxGames.Pluvia.ui.component.topbar

import android.content.res.Configuration
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.OxGames.Pluvia.PluviaApp
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.events.SteamEvent
import com.OxGames.Pluvia.service.SteamService
import com.OxGames.Pluvia.ui.component.ListItemImage
import com.OxGames.Pluvia.ui.component.dialog.ProfileDialog
import com.OxGames.Pluvia.ui.theme.PluviaTheme
import com.OxGames.Pluvia.utils.SteamUtils
import `in`.dragonbra.javasteam.enums.EPersonaState
import kotlinx.coroutines.launch

@Composable
fun AccountButton(
    onSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val persona by SteamService.personaState.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }
    ProfileDialog(
        openDialog = showDialog,
        name = persona?.name.orEmpty(),
        avatarHash = persona?.avatarHash.orEmpty(),
        state = persona?.state ?: EPersonaState.Offline,
        onStatusChange = {
            scope.launch {
                PluviaApp.events.emit(SteamEvent.PersonaStateChange(it))
            }
        },
        onSettings = {
            onSettings()
            showDialog = false
        },
        onLogout = {
            onLogout()
            showDialog = false
        },
        onDismiss = {
            showDialog = false
        },
    )

    IconButton(
        onClick = { showDialog = true },
        content = {
            ListItemImage(
                image = { SteamUtils.getAvatarURL(persona?.avatarHash) },
                contentDescription = stringResource(R.string.desc_account_button),
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_AccountButton() {
    PluviaTheme {
        CenterAlignedTopAppBar(
            title = { Text(text = "Top App Bar") },
            actions = {
                AccountButton(
                    onSettings = {},
                    onLogout = {},
                )
            },
        )
    }
}
