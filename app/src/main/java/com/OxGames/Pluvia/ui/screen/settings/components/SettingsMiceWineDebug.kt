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
import com.OxGames.Pluvia.MiceWineUtils
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.ui.component.topbar.BackButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsMiceWineDebug(
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
}
