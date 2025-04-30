package com.OxGames.Pluvia.ui.component.dialog

import android.content.res.Configuration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.OxGames.Pluvia.ui.theme.PluviaTheme

// TODO
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Box64PresetsDialog(
    visible: Boolean = true,
    onDismissRequest: () -> Unit,
) {
//    if (visible) {
//        val context = LocalContext.current
//        val prefix = "box64"
//        val scrollState = rememberScrollState()
//
//        Dialog(
//            onDismissRequest = onDismissRequest,
//            properties = DialogProperties(
//                usePlatformDefaultWidth = false,
//                dismissOnClickOutside = false,
//            ),
//            content = {
//                Scaffold(
//                    topBar = {
//                        CenterAlignedTopAppBar(
//                            title = { Text(text = stringResource(R.string.title_box64_presets)) },
//                            actions = {
//                                IconButton(
//                                    onClick = onDismissRequest,
//                                    content = {
//                                        Icon(
//                                            imageVector = Icons.Default.Done,
//                                            contentDescription = stringResource(R.string.desc_box64_close),
//                                        )
//                                    },
//                                )
//                            },
//                        )
//                    },
//                ) { paddingValues ->
//                    val getPresets: () -> ArrayList<Box86_64Preset> = { Box86_64PresetManager.getPresets(prefix, context) }
//                    val getPreset: (String) -> Box86_64Preset = { id -> getPresets().first { it.id == id } }
//                    var showPresets by rememberSaveable { mutableStateOf(false) }
//                    var presetId by rememberSaveable { mutableStateOf(getPresets().first().id) }
//                    var presetName by rememberSaveable { mutableStateOf(getPreset(presetId).name) }
//                    var envVars by rememberSaveable {
//                        mutableStateOf(
//                            Box86_64PresetManager.getEnvVars(prefix, context, getPreset(presetId).id).toString(),
//                        )
//                    }
//                    val isCustom: () -> Boolean = { getPreset(presetId).isCustom }
//
//                    Column(
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .padding(paddingValues),
//                    ) {
//                        OutlinedTextField(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(horizontal = 16.dp),
//                            value = presetName,
//                            enabled = isCustom(),
//                            onValueChange = {
//                                presetName = it.replace("|", "")
//                                Box86_64PresetManager.editPreset(prefix, context, presetId, presetName, EnvVars(envVars))
//                            },
//                            label = { Text(text = stringResource(R.string.box64_name_label)) },
//                            trailingIcon = {
//                                IconButton(
//                                    colors = IconButtonDefaults.iconButtonColors()
//                                        .copy(contentColor = MaterialTheme.colorScheme.onSurface),
//                                    onClick = { showPresets = true },
//                                    content = {
//                                        Icon(
//                                            imageVector = Icons.AutoMirrored.Outlined.ViewList,
//                                            contentDescription = stringResource(R.string.desc_box64_list_preset),
//                                        )
//                                    },
//                                )
//                                DropdownMenu(
//                                    expanded = showPresets,
//                                    onDismissRequest = { showPresets = false },
//                                    content = {
//                                        for (preset in getPresets()) {
//                                            DropdownMenuItem(
//                                                text = { Text(preset.name) },
//                                                onClick = {
//                                                    presetId = preset.id
//                                                    presetName = getPreset(presetId).name
//                                                    envVars = Box86_64PresetManager
//                                                        .getEnvVars(prefix, context, getPreset(presetId).id)
//                                                        .toString()
//                                                    showPresets = false
//                                                },
//                                            )
//                                        }
//                                    },
//                                )
//                            },
//                        )
//                        Row(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(horizontal = 16.dp),
//                            horizontalArrangement = Arrangement.SpaceBetween,
//                            verticalAlignment = Alignment.CenterVertically,
//                        ) {
//                            Text(text = stringResource(R.string.box64_env_variables))
//                            Row {
//                                IconButton(
//                                    onClick = {
//                                        presetId = Box86_64PresetManager.duplicatePreset(prefix, context, presetId)
//                                        presetName = getPreset(presetId).name
//                                        envVars = Box86_64PresetManager.getEnvVars(prefix, context, getPreset(presetId).id).toString()
//                                    },
//                                    content = {
//                                        Icon(
//                                            imageVector = Icons.Filled.ContentCopy,
//                                            contentDescription = stringResource(R.string.desc_box64_copy_preset),
//                                        )
//                                    },
//                                )
//                                IconButton(
//                                    onClick = {
//                                        val defaultEnvVars = EnvVarInfo.KNOWN_BOX64_VARS.values.joinToString(" ") {
//                                            "${it.identifier}=${it.possibleValues.first()}"
//                                        }
//                                        presetId = Box86_64PresetManager.editPreset(
//                                            prefix,
//                                            context,
//                                            null,
//                                            context.getString(R.string.unnamed),
//                                            EnvVars(defaultEnvVars),
//                                        )
//                                        presetName = getPreset(presetId).name
//                                        envVars = Box86_64PresetManager.getEnvVars(prefix, context, getPreset(presetId).id).toString()
//                                    },
//                                    content = {
//                                        Icon(
//                                            imageVector = Icons.Outlined.AddCircle,
//                                            contentDescription = stringResource(R.string.desc_box64_create_preset),
//                                        )
//                                    },
//                                )
//                                IconButton(
//                                    enabled = isCustom(),
//                                    onClick = {
//                                        val idToBeDeleted = presetId
//                                        presetId = getPresets().first().id
//                                        presetName = getPreset(presetId).name
//                                        envVars = Box86_64PresetManager.getEnvVars(prefix, context, getPreset(presetId).id).toString()
//                                        Box86_64PresetManager.removePreset(prefix, context, idToBeDeleted)
//                                    },
//                                    content = {
//                                        Icon(
//                                            imageVector = Icons.Filled.Delete,
//                                            contentDescription = stringResource(R.string.desc_box64_delete_preset),
//                                        )
//                                    },
//                                )
//                            }
//                        }
//                        Column(modifier = Modifier.verticalScroll(scrollState)) {
//                            var infoMsg by rememberSaveable { mutableStateOf("") }
//                            MessageDialog(
//                                visible = infoMsg.isNotEmpty(),
//                                onDismissRequest = { infoMsg = "" },
//                                message = infoMsg,
//                                useHtmlInMsg = true,
//                            )
//                            SettingsEnvVars(
//                                colors = settingsTileColors(),
//                                enabled = isCustom(),
//                                envVars = EnvVars(envVars),
//                                onEnvVarsChange = {
//                                    envVars = it.toString()
//                                    Box86_64PresetManager.editPreset(prefix, context, presetId, presetName, it)
//                                },
//                                knownEnvVars = EnvVarInfo.KNOWN_BOX64_VARS,
//                                envVarAction = { varName ->
//                                    IconButton(
//                                        onClick = {
//                                            val resName = varName.replace(prefix.uppercase(), "box86_64_env_var_help_").lowercase()
//                                            StringUtils.getString(context, resName)
//                                                ?.let { infoMsg = it }
//                                                ?: Timber.w("Could not find string resource of $resName")
//                                        },
//                                        content = {
//                                            Icon(
//                                                imageVector = Icons.Outlined.Info,
//                                                contentDescription = stringResource(R.string.desc_box64_variable_info),
//                                            )
//                                        },
//                                    )
//                                },
//                            )
//                        }
//                    }
//                }
//            },
//        )
//    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
private fun Preview_Box64PresetsDialog() {
    PluviaTheme {
        Box64PresetsDialog(visible = true, onDismissRequest = {})
    }
}
