package com.OxGames.Pluvia.ui.screen.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.ui.theme.PluviaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DialogGeneralSettings(
    visible: Boolean = true,
    onDismissRequest: () -> Unit,
) {
    if (!visible) {
        return
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
        ),
        content = {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text(text = stringResource(R.string.general_settings)) },
                        actions = {
                            IconButton(
                                onClick = onDismissRequest,
                                content = {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = "Close General Settings Dialog", // TODO
                                    )
                                },
                            )
                        },
                    )
                },
            ) { paddingValues ->
                var debugSettingsExpanded by rememberSaveable { mutableStateOf(false) }
                var soundSettingsExpanded by rememberSaveable { mutableStateOf(false) }
                var driversSettingsExpanded by rememberSaveable { mutableStateOf(false) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    // item {
                    // TODO `Box64 Settings` ??
                    //  I see it in code, but not on actual screen.
                    // }
                    item {
                        Column {
                            GeneralSettingsItem(
                                title = stringResource(R.string.debug_settings_title),
                                description = stringResource(R.string.debug_settings_description),
                                icon = Icons.Default.Settings,
                                onClick = { debugSettingsExpanded = !debugSettingsExpanded },
                            )
                            AnimatedVisibility(debugSettingsExpanded) {
                                // Expand sublist
                            }
                        }
                    }
                    item {
                        Column {
                            GeneralSettingsItem(
                                title = stringResource(R.string.sound_settings_title),
                                description = stringResource(R.string.sound_settings_description),
                                icon = Icons.Default.Settings,
                                onClick = { soundSettingsExpanded = !soundSettingsExpanded },
                            )
                            AnimatedVisibility(soundSettingsExpanded) {

                            }
                        }
                    }
                    item {
                       Column {
                           GeneralSettingsItem(
                               title = stringResource(R.string.driver_settings_title),
                               description = stringResource(R.string.driver_settings_description),
                               icon = Icons.Default.Settings,
                               onClick = { driversSettingsExpanded = !driversSettingsExpanded },
                           )
                           AnimatedVisibility(driversSettingsExpanded) {

                           }
                       }
                    }
                    item {
                        GeneralSettingsItem(
                            title = stringResource(R.string.driver_info_title),
                            description = stringResource(R.string.driver_info_description),
                            icon = Icons.Default.Settings,
                            onClick = { },
                        )
                    }
                    item {
                        GeneralSettingsItem(
                            title = stringResource(R.string.env_settings_title),
                            description = stringResource(R.string.env_settings_description),
                            icon = Icons.Default.Settings,
                            onClick = { },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun GeneralSettingsItem(
    title: String,
    description: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(text = title)
        },
        supportingContent = {
            Text(text = description)
        },
        leadingContent = icon?.let {
            {
                Icon(imageVector = it, contentDescription = null)
            }
        },
    )
}

@Preview
@Preview
@Composable
private fun Preview_DialogGeneralSettings() {
    PluviaTheme {
        DialogGeneralSettings(visible = true, onDismissRequest = {})
    }
}
