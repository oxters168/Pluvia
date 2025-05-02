package com.OxGames.Pluvia.ui.screen.settings.components

import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.OxGames.Pluvia.BuildConfig
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.service.SteamService
import com.OxGames.Pluvia.ui.theme.PluviaTheme
import com.OxGames.Pluvia.ui.theme.settingsTileColors
import com.OxGames.Pluvia.ui.theme.settingsTileColorsDebug
import com.OxGames.Pluvia.utils.application.CrashHandler
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalCoilApi::class)
@Composable
fun SettingsGroupDebug(
    onShowLog: () -> Unit,
) {
    val context = LocalContext.current

    /* Crash Log stuff */
    var hasCrashFile: Boolean by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val crashDir = File(context.getExternalFilesDir(null), "crash_logs")
            hasCrashFile = crashDir.listFiles()?.isNotEmpty() ?: false
        }
    }

    /* Save log cat */
    val saveLogCat = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { resultUri ->
        try {
            resultUri?.let {
                val logs = CrashHandler.getAppLogs(1000)
                context.contentResolver.openOutputStream(resultUri)?.use { outputStream ->
                    outputStream.write(logs.toByteArray())
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.toast_failed_log_save), Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsMenuLink(
            colors = settingsTileColors(),
            icon = { Icon(imageVector = Icons.Default.Save, contentDescription = null) },
            title = { Text(text = stringResource(R.string.settings_save_logcat_title)) },
            subtitle = { Text(text = stringResource(R.string.settings_save_logcat_subtitle)) },
            onClick = { saveLogCat.launch("app_logs_${CrashHandler.timestamp}.txt") },
        )

        SettingsMenuLink(
            colors = settingsTileColors(),
            icon = { Icon(imageVector = Icons.Default.Description, contentDescription = null) },
            title = { Text(text = stringResource(R.string.settings_view_logcat_title)) },
            subtitle = {
                val string = if (hasCrashFile) {
                    R.string.settings_view_logcat_subtitle
                } else {
                    R.string.settings_view_logcat_subtitle_alt
                }
                Text(text = stringResource(string))
            },
            enabled = hasCrashFile,
            onClick = onShowLog,
        )

        SettingsGroup(
            title = { Text(text = "Advanced") },
            content = {
                SettingsMenuLink(
                    modifier = Modifier.combinedClickable(
                        onLongClick = {
                            SteamService.logOut()
                            (context as ComponentActivity).finishAffinity()
                        },
                        onClick = {
                            Toast.makeText(context, context.getString(R.string.toast_settings_activate), Toast.LENGTH_SHORT).show()
                        },
                    ),
                    colors = settingsTileColorsDebug(),
                    icon = { Icon(imageVector = Icons.Default.Warning, contentDescription = null) },
                    title = { Text(text = stringResource(R.string.settings_clear_pref_title)) },
                    subtitle = { Text(text = stringResource(R.string.settings_clear_pref_subtitle)) },
                    onClick = {},
                )

                SettingsMenuLink(
                    modifier = Modifier.combinedClickable(
                        onLongClick = {
                            SteamService.stop()
                            SteamService.clearDatabase()
                            (context as ComponentActivity).finishAffinity()
                        },
                        onClick = {
                            Toast.makeText(context, context.getString(R.string.toast_settings_activate), Toast.LENGTH_SHORT).show()
                        },
                    ),
                    colors = settingsTileColorsDebug(),
                    icon = { Icon(imageVector = Icons.Default.Warning, contentDescription = null) },
                    title = { Text(text = stringResource(R.string.settings_clear_db_title)) },
                    subtitle = { Text(text = stringResource(R.string.settings_clear_db_subtitle)) },
                    onClick = {},
                )

                var containerResetVerify by remember { mutableStateOf(false) } // Okay to not be rememberSavable
                SettingsMenuLink(
                    modifier = Modifier.combinedClickable(
                        onLongClick = {
                            if (!containerResetVerify) {
                                Toast.makeText(context, context.getString(R.string.toast_container_reset_verify), Toast.LENGTH_SHORT).show()
                                containerResetVerify = true
                            } else {
                                TODO()
                            }
                        },
                        onClick = {
                            Toast.makeText(context, context.getString(R.string.toast_settings_activate), Toast.LENGTH_SHORT).show()
                            containerResetVerify = false
                        },
                    ),
                    colors = settingsTileColorsDebug(),
                    icon = { Icon(imageVector = Icons.Default.Warning, contentDescription = null) },
                    title = { Text(text = stringResource(R.string.settings_reset_containers_title)) },
                    subtitle = { Text(text = stringResource(R.string.settings_reset_containers_subtitle)) },
                    onClick = {},
                )

                SettingsMenuLink(
                    modifier = Modifier.combinedClickable(
                        onLongClick = {
                            context.imageLoader.diskCache?.clear()
                            context.imageLoader.memoryCache?.clear()
                        },
                        onClick = {
                            Toast.makeText(context, context.getString(R.string.toast_settings_activate), Toast.LENGTH_SHORT).show()
                        },
                    ),
                    colors = settingsTileColorsDebug(),
                    icon = { Icon(imageVector = Icons.Default.Warning, contentDescription = null) },
                    title = { Text(text = stringResource(R.string.settings_reset_image_cache_title)) },
                    subtitle = { Text(text = stringResource(R.string.settings_reset_image_cache_subtitle)) },
                    onClick = {},
                )

                if (BuildConfig.DEBUG) {
                    SettingsMenuLink(
                        title = { Text(text = "Crash App") },
                        icon = { Icon(imageVector = Icons.Default.Warning, contentDescription = null) },
                        onClick = {
                            throw NotImplementedError("Debug crash test")
                        },
                    )
                }
            },
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
private fun Preview_SettingsGroupDebug() {
    PluviaTheme {
        Surface {
            SettingsGroupDebug(onShowLog = {})
        }
    }
}
