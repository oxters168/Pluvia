package com.OxGames.Pluvia.ui.screen.settings.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.OxGames.Pluvia.MiceWineUtils
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.ui.component.topbar.BackButton
import com.OxGames.Pluvia.ui.theme.PluviaTheme
import com.alorma.compose.settings.ui.base.internal.SettingsTileDefaults
import com.micewine.emu.core.EnvVars
import com.micewine.emu.core.RatPackageManager.listRatPackages
import com.micewine.emu.core.RatPackageManager.listRatPackagesId
import com.micewine.emu.core.ShellLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

// TODO incomplete

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsMiceWineDriverInfo(
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current

    val logsScrollState = rememberScrollState()
    var logsText by remember {
        val text = if (view.isInEditMode) context.getString(R.string.lorem) else ""
        mutableStateOf(text)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.driver_info_title)) },
                navigationIcon = { BackButton(onClick = onBack) },
            )
        },
    ) { paddingValues ->
        var expanded by remember { mutableStateOf(false) }
        val vulkanDriversId by remember {
            val value = listRatPackagesId("VulkanDriver", "AdrenoToolsDriver")
            println("vulkanDrivers $value")
            mutableStateOf(value)
        }
        val vulkanDrivers by remember {
            val value = listRatPackages("VulkanDriver", "AdrenoToolsDriver").map { it.name + " " + it.version }
            println("vulkanDrivers $value")
            mutableStateOf(value)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(text = stringResource(R.string.select_driver_title))
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true }
                        .border(1.dp, SettingsTileDefaults.colors().titleColor, RoundedCornerShape(8))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Beans", fontSize = 14.sp)
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null, // TODO
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    content = {
                        vulkanDrivers.forEachIndexed { index, s ->
                            DropdownMenuItem(
                                text = { Text(text = s) },
                                onClick = {
                                    expanded = false

                                    scope.launch(Dispatchers.IO) {
                                        val driverId = vulkanDriversId[index]

                                        val driverFile: String
                                        var adrenoToolsDriverPath: String? = null
                                        val ratPackagesDir = MiceWineUtils.ratPackagesDir
                                        val appRootDir = MiceWineUtils.appRootDir

                                        if (driverId.contains("AdrenoToolsDriver")) {
                                            driverFile = File(
                                                "$ratPackagesDir/${
                                                    File("$appRootDir/packages").listFiles()
                                                        ?.first { it.name.startsWith("AdrenoTools-") }?.name
                                                }/pkg-header",
                                            )
                                                .readLines()[4]
                                                .substringAfter("=")

                                            adrenoToolsDriverPath = File("$ratPackagesDir/$driverId/pkg-header")
                                                .readLines()[4]
                                                .substringAfter("=")
                                        } else {
                                            driverFile = File("$ratPackagesDir/$driverId/pkg-header")
                                                .readLines()[4]
                                                .substringAfter("=")
                                        }

                                        MiceWineUtils.setSharedVars(
                                            context = context,
                                            box64Version = null,
                                            box64Preset = null,
                                            d3dxRenderer = null,
                                            wineD3D = null,
                                            dxvk = null,
                                            vkd3d = null,
                                            displayResolution = null,
                                            esync = null,
                                            services = null,
                                            virtualDesktop = null,
                                            cpuAffinity = null,
                                            adrenoTools = (driverId.contains("AdrenoToolsDriver")),
                                            adrenoToolsDriverPath = adrenoToolsDriverPath,
                                        )

                                        MiceWineUtils.generateICDFile(
                                            driverLib = driverFile,
                                            destIcd = File("$appRootDir/vulkan_icd.json"),
                                        )

                                        logsText = ShellLoader.runCommandWithOutput(
                                            cmd = EnvVars.getEnv() + "vulkaninfo",
                                            enableStdErr = true,
                                        )

                                        logsScrollState.animateScrollTo(value = 0)
                                    }
                                },
                            )
                        }
                    },
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .verticalScroll(logsScrollState)
                    .background(Color.DarkGray),
            ) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    color = Color.White,
                    text = logsText.ifEmpty { "No data available" },
                )
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
private fun Preview_SettingsMiceWineDriverInfo() {
    PluviaTheme {
        Surface {
            SettingsMiceWineDriverInfo(onBack = {})
        }
    }
}
