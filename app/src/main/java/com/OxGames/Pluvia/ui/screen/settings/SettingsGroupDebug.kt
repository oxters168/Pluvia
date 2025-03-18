package com.OxGames.Pluvia.ui.screen.settings

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.service.SteamService
import com.OxGames.Pluvia.ui.component.dialog.CrashLogDialog
import com.OxGames.Pluvia.ui.theme.settingsTileColors
import com.OxGames.Pluvia.ui.theme.settingsTileColorsDebug
import com.OxGames.Pluvia.utils.application.CrashHandler
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.winlator.core.FileUtils
import java.io.File
import timber.log.Timber

@Suppress("UnnecessaryOptInAnnotation") // ExperimentalFoundationApi
@OptIn(ExperimentalCoilApi::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsGroupDebug() {
    val context = LocalContext.current

    /* Crash Log stuff */
    var showLogcatDialog by rememberSaveable { mutableStateOf(false) }
    var latestCrashFile: File? by rememberSaveable { mutableStateOf(null) }
    LaunchedEffect(Unit) {
        val crashDir = File(context.getExternalFilesDir(null), "crash_logs")
        latestCrashFile = crashDir.listFiles()
            ?.filter { it.name.startsWith("pluvia_crash_") }
            ?.maxByOrNull { it.lastModified() }
    }

    /* Save crash log */
    val saveResultContract = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { resultUri ->
        try {
            resultUri?.let { uri ->
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    latestCrashFile?.inputStream()?.use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.toast_failed_crash_save), Toast.LENGTH_SHORT).show()
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

    CrashLogDialog(
        visible = showLogcatDialog && latestCrashFile != null,
        fileName = latestCrashFile?.name ?: stringResource(R.string.settings_default_crash_filename),
        fileText = latestCrashFile?.readText() ?: stringResource(R.string.settings_default_crash_message),
        onSave = { latestCrashFile?.let { file -> saveResultContract.launch(file.name) } },
        onDismissRequest = { showLogcatDialog = false },
    )

    SettingsGroup(title = { Text(text = stringResource(R.string.settings_group_debug)) }) {
        SettingsMenuLink(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.settings_save_logcat_title)) },
            subtitle = { Text(text = stringResource(R.string.settings_save_logcat_subtitle)) },
            onClick = { saveLogCat.launch("app_logs_${CrashHandler.timestamp}.txt") },
        )

        SettingsMenuLink(
            colors = settingsTileColors(),
            title = { Text(text = stringResource(R.string.settings_view_logcat_title)) },
            subtitle = {
                val string = if (latestCrashFile != null) {
                    R.string.settings_view_logcat_subtitle
                } else {
                    R.string.settings_view_logcat_subtitle_alt
                }
                Text(text = stringResource(string))
            },
            enabled = latestCrashFile != null,
            onClick = { showLogcatDialog = true },
        )

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
                        File(context.filesDir, "imagefs").also { file ->
                            file.walkTopDown().forEach { FileUtils.delete(it) }
                            val result = file.deleteRecursively()
                            Timber.i("imagefs deleted? $result")
                        }
                        File(context.filesDir, "pulseaudio").also { file ->
                            val result = file.deleteRecursively()
                            Timber.i("pulseaudio deleted? $result")
                        }
                        File(context.filesDir, "splitcompat").also { file ->
                            val result = file.deleteRecursively()
                            Timber.i("splitcompat deleted? $result")
                        }
                        com.winlator.PrefManager.clear(context)
                        Toast.makeText(context, context.getString(R.string.toast_containers_reset), Toast.LENGTH_SHORT).show()
                        containerResetVerify = false
                    }
                },
                onClick = {
                    Toast.makeText(context, context.getString(R.string.toast_settings_activate), Toast.LENGTH_SHORT).show()
                    containerResetVerify = false
                },
            ),
            colors = settingsTileColorsDebug(),
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
            title = { Text(text = stringResource(R.string.settings_reset_image_cache_title)) },
            subtitle = { Text(text = stringResource(R.string.settings_reset_image_cache_subtitle)) },
            onClick = {},
        )
    }
}
