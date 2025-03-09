package com.OxGames.Pluvia.ui.component.settings

import android.content.res.Configuration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.OxGames.Pluvia.ui.theme.PluviaTheme
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.base.internal.LocalSettingsGroupEnabled
import com.alorma.compose.settings.ui.base.internal.SettingsTileColors
import com.alorma.compose.settings.ui.base.internal.SettingsTileDefaults
import com.alorma.compose.settings.ui.base.internal.SettingsTileScaffold

@Composable
fun SettingsCPUList(
    modifier: Modifier = Modifier,
    enabled: Boolean = LocalSettingsGroupEnabled.current,
    value: String,
    onValueChange: (String) -> Unit,
    title: @Composable () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    colors: SettingsTileColors = SettingsTileDefaults.colors(),
    tonalElevation: Dp = ListItemDefaults.Elevation,
    shadowElevation: Dp = ListItemDefaults.Elevation,
    action: @Composable (() -> Unit)? = null,
) {
    val scrollState = rememberScrollState()

    SettingsTileScaffold(
        modifier = modifier,
        enabled = enabled,
        title = title,
        icon = icon,
        colors = colors,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        action = action,
        subtitle = {
            val cpuAffinity = value.split(",").map { it.toInt() }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                for (cpu in 0 until Runtime.getRuntime().availableProcessors()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Checkbox(
                            checked = cpuAffinity.contains(cpu),
                            onCheckedChange = { value ->
                                val newAffinity = if (value) {
                                    (cpuAffinity + cpu).sorted()
                                } else {
                                    cpuAffinity.filter { it != cpu }
                                }
                                onValueChange(newAffinity.joinToString(","))
                            },
                        )
                        Text(text = "CPU$cpu")
                    }
                }
            }
        },
    )
}

// Preview looks off through AS, looks fine on device.
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
private fun Preview_SettingsCPUList() {
    PluviaTheme {
        SettingsGroup(title = { Text(text = "Test") }) {
            SettingsCPUList(
                value = "0,1,2,4,8",
                onValueChange = {},
                title = { Text("Processor Affinity") },
            )
        }
    }
}
