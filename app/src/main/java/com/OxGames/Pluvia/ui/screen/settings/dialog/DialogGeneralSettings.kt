package com.OxGames.Pluvia.ui.screen.settings.dialog

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.OxGames.Pluvia.MiceWineUtils
import com.OxGames.Pluvia.PrefManager
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.ui.screen.settings.components.SettingsItemList
import com.OxGames.Pluvia.ui.screen.settings.components.SettingsItemType
import com.OxGames.Pluvia.ui.theme.PluviaTheme
import com.alorma.compose.settings.ui.SettingsMenuLink

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
                val context = LocalContext.current
                val scrollState = rememberScrollState()
                var debugSettingsExpanded by rememberSaveable { mutableStateOf(false) }
                var soundSettingsExpanded by rememberSaveable { mutableStateOf(false) }
                var driversSettingsExpanded by rememberSaveable { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(scrollState),
                ) {
                    // item {
                    // TODO `Box64 Settings` ??
                    //  I see it in code, but not on actual screen.
                    // }
                    GeneralSettingsItem(
                        title = stringResource(R.string.debug_settings_title),
                        description = stringResource(R.string.debug_settings_description),
                        expanded = debugSettingsExpanded,
                        icon = Icons.Default.Settings,
                        onClick = { debugSettingsExpanded = !debugSettingsExpanded },
                    )
                    AnimatedVisibility(debugSettingsExpanded) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                            SettingsItemList(
                                title = stringResource(R.string.wine_log_level_title),
                                description = stringResource(R.string.wine_log_level_description),
                                spinnerOptions = arrayOf("disabled", "default"),
                                type = SettingsItemType.SPINNER,
                                defaultValue = MiceWineUtils.WINE_LOG_LEVEL_DEFAULT_VALUE,
                                key = MiceWineUtils.WINE_LOG_LEVEL,
                            )
                            SettingsItemList(
                                title = stringResource((R.string.box64_log_title)),
                                description = stringResource(R.string.box64_log_description),
                                spinnerOptions = arrayOf("0", "1"),
                                type = SettingsItemType.SPINNER,
                                defaultValue = MiceWineUtils.BOX64_LOG_DEFAULT_VALUE,
                                key = MiceWineUtils.BOX64_LOG,
                            )
                            SettingsItemList(
                                title = stringResource(R.string.box64_show_segv_title),
                                description = stringResource(R.string.box64_show_segv_description),
                                type = SettingsItemType.SWITCH,
                                defaultValue = "${MiceWineUtils.BOX64_SHOWSEGV_DEFAULT_VALUE}",
                                key = MiceWineUtils.BOX64_SHOWSEGV,
                            )
                            SettingsItemList(
                                title = stringResource(R.string.box64_no_sigsegv_title),
                                description = stringResource(R.string.box64_no_sigsegv_description),
                                type = SettingsItemType.SWITCH,
                                defaultValue = "${MiceWineUtils.BOX64_NOSIGSEGV_DEFAULT_VALUE}",
                                key = MiceWineUtils.BOX64_NOSIGSEGV,
                            )
                            SettingsItemList(
                                title = stringResource(R.string.box64_no_sigill_title),
                                description = stringResource(R.string.box64_no_sigill_description),
                                type = SettingsItemType.SWITCH,
                                defaultValue = "${MiceWineUtils.BOX64_NOSIGILL_DEFAULT_VALUE}",
                                key = MiceWineUtils.BOX64_NOSIGILL,
                            )
                            SettingsItemList(
                                title = stringResource(R.string.box64_show_bt_title),
                                description = stringResource(R.string.box64_show_bt_description),
                                type = SettingsItemType.SWITCH,
                                defaultValue = "${MiceWineUtils.BOX64_SHOWBT_DEFAULT_VALUE}",
                                key = MiceWineUtils.BOX64_SHOWBT,
                            )
                            SettingsItemList(
                                title = stringResource(R.string.enable_ram_counter),
                                description = stringResource(R.string.enable_ram_counter_description),
                                type = SettingsItemType.SWITCH,
                                defaultValue = "${MiceWineUtils.RAM_COUNTER_DEFAULT_VALUE}",
                                key = MiceWineUtils.RAM_COUNTER,
                            )
                            SettingsItemList(
                                title = stringResource(R.string.enable_cpu_counter),
                                description = stringResource(R.string.enable_cpu_counter_description),
                                type = SettingsItemType.SWITCH,
                                defaultValue = "${MiceWineUtils.CPU_COUNTER_DEFAULT_VALUE}",
                                key = MiceWineUtils.CPU_COUNTER,
                            )
                            SettingsItemList(
                                title = stringResource(R.string.enable_debug_info),
                                description = stringResource(R.string.enable_debug_info_description),
                                type = SettingsItemType.SWITCH,
                                defaultValue = "${MiceWineUtils.ENABLE_DEBUG_INFO_DEFAULT_VALUE}",
                                key = MiceWineUtils.ENABLE_DEBUG_INFO,
                            )
                        }
                    }

                    GeneralSettingsItem(
                        title = stringResource(R.string.sound_settings_title),
                        description = stringResource(R.string.sound_settings_description),
                        expanded = soundSettingsExpanded,
                        icon = Icons.Default.Settings,
                        onClick = { soundSettingsExpanded = !soundSettingsExpanded },
                    )
                    AnimatedVisibility(soundSettingsExpanded) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                            SettingsItemList(
                                title = stringResource(R.string.select_audio_sink),
                                description = null,
                                spinnerOptions = arrayOf("SLES", "AAudio"),
                                type = SettingsItemType.SPINNER,
                                defaultValue = MiceWineUtils.PA_SINK_DEFAULT_VALUE,
                                key = MiceWineUtils.PA_SINK,
                            )
                        }
                    }
                    GeneralSettingsItem(
                        title = stringResource(R.string.driver_settings_title),
                        description = stringResource(R.string.driver_settings_description),
                        expanded = driversSettingsExpanded,
                        icon = Icons.Default.Settings,
                        onClick = { driversSettingsExpanded = !driversSettingsExpanded },
                    )
                    AnimatedVisibility(driversSettingsExpanded) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                            SettingsItemList(
                                title = stringResource(R.string.enable_dri3),
                                description = null,
                                type = SettingsItemType.SWITCH,
                                defaultValue = "${MiceWineUtils.ENABLE_DRI3_DEFAULT_VALUE}",
                                key = MiceWineUtils.ENABLE_DRI3,
                            )
                            SettingsItemList(
                                title = stringResource(R.string.select_dxvk_hud_preset_title),
                                description = null,
                                spinnerOptions = arrayOf(
                                    "fps", "gpuload",
                                    "devinfo", "version", "api",
                                    "memory", "cs", "compiler",
                                    "allocations", "pipelines", "frametimes",
                                    "descriptors", "drawcalls", "submissions",
                                ),
                                type = SettingsItemType.CHECKBOX,
                                defaultValue = MiceWineUtils.SELECTED_DXVK_HUD_PRESET_DEFAULT_VALUE,
                                key = MiceWineUtils.SELECTED_DXVK_HUD_PRESET,
                            )
                            SettingsItemList(
                                title = stringResource(R.string.mesa_vk_wsi_present_mode_title),
                                description = null,
                                spinnerOptions = arrayOf("fifo", "relaxed", "mailbox", "immediate"),
                                type = SettingsItemType.SPINNER,
                                defaultValue = MiceWineUtils.SELECTED_MESA_VK_WSI_PRESENT_MODE_DEFAULT_VALUE,
                                key = MiceWineUtils.SELECTED_MESA_VK_WSI_PRESENT_MODE,
                            )
                            SettingsItemList(
                                title = stringResource(R.string.tu_debug_title),
                                description = null,
                                spinnerOptions = arrayOf(
                                    "noconform", "flushall", "syncdraw",
                                    "sysmem", "gmem", "nolrz",
                                    "noubwc", "nomultipos", "forcebin",
                                ),
                                type = SettingsItemType.CHECKBOX,
                                defaultValue = MiceWineUtils.SELECTED_TU_DEBUG_PRESET_DEFAULT_VALUE,
                                key = MiceWineUtils.SELECTED_TU_DEBUG_PRESET,
                            )
                            SettingsItemList(
                                title = stringResource(R.string.select_gl_profile_title),
                                description = null,
                                spinnerOptions = arrayOf(
                                    "GL 2.1", "GL 3.0", "GL 3.1",
                                    "GL 3.2", "GL 3.3", "GL 4.0",
                                    "GL 4.1", "GL 4.2", "GL 4.3",
                                    "GL 4.4", "GL 4.5", "GL 4.6",
                                ),
                                type = SettingsItemType.SPINNER,
                                defaultValue = MiceWineUtils.SELECTED_GL_PROFILE_DEFAULT_VALUE,
                                key = MiceWineUtils.SELECTED_GL_PROFILE,
                            )
                        }
                    }
                    GeneralSettingsItem(
                        title = stringResource(R.string.driver_info_title),
                        description = stringResource(R.string.driver_info_description),
                        icon = Icons.Default.Settings,
                        onClick = {
                            // TODO
                            Toast.makeText(context, "Not Implemented Yet", Toast.LENGTH_SHORT).show()
                        },
                    )
                    GeneralSettingsItem(
                        title = stringResource(R.string.env_settings_title),
                        description = stringResource(R.string.env_settings_description),
                        icon = Icons.Default.Settings,
                        onClick = {
                            // TODO
                            Toast.makeText(context, "Not Implemented Yet", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun GeneralSettingsItem(
    title: String,
    description: String,
    onClick: () -> Unit,
    expanded: Boolean? = null,
    icon: ImageVector? = null,
) {
    SettingsMenuLink(
        title = { Text(text = title) },
        subtitle = { Text(text = description) },
        icon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                ) // TODO
            }
        },
        onClick = onClick,
        action = expanded?.let {
            {
                Icon(
                    imageVector = if (it) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                ) // TODO
            }
        },
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
private fun Preview_DialogGeneralSettings() {
    val context = LocalContext.current
    PrefManager.init(context)
    PluviaTheme {
        DialogGeneralSettings(visible = true, onDismissRequest = {})
    }
}
