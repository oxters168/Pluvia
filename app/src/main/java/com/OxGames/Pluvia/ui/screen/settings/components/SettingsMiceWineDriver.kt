package com.OxGames.Pluvia.ui.screen.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.ui.component.topbar.BackButton
import com.micewine.emu.MiceWineUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsMiceWineDriver(
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.debug_settings_title)) },
                navigationIcon = { BackButton(onClick = onBack) },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsItemList(
                title = stringResource(R.string.enable_dri3),
                description = null,
                type = SettingsItemType.SWITCH,
                defaultValue = "${MiceWineUtils.GeneralSettings.ENABLE_DRI3_DEFAULT_VALUE}",
                key = MiceWineUtils.GeneralSettings.ENABLE_DRI3,
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
                defaultValue = MiceWineUtils.GeneralSettings.SELECTED_DXVK_HUD_PRESET_DEFAULT_VALUE,
                key = MiceWineUtils.GeneralSettings.SELECTED_DXVK_HUD_PRESET,
            )
            SettingsItemList(
                title = stringResource(R.string.mesa_vk_wsi_present_mode_title),
                description = null,
                spinnerOptions = arrayOf("fifo", "relaxed", "mailbox", "immediate"),
                type = SettingsItemType.SPINNER,
                defaultValue = MiceWineUtils.GeneralSettings.SELECTED_MESA_VK_WSI_PRESENT_MODE_DEFAULT_VALUE,
                key = MiceWineUtils.GeneralSettings.SELECTED_MESA_VK_WSI_PRESENT_MODE,
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
                defaultValue = MiceWineUtils.GeneralSettings.SELECTED_TU_DEBUG_PRESET_DEFAULT_VALUE,
                key = MiceWineUtils.GeneralSettings.SELECTED_TU_DEBUG_PRESET,
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
                defaultValue = MiceWineUtils.GeneralSettings.SELECTED_GL_PROFILE_DEFAULT_VALUE,
                key = MiceWineUtils.GeneralSettings.SELECTED_GL_PROFILE,
            )
        }
    }
}
