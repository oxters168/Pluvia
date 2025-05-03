package com.OxGames.Pluvia.ui.screen.settings.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.OxGames.Pluvia.PrefManager
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.ui.component.EmptyScreen
import com.OxGames.Pluvia.ui.component.dialog.KeyValueDialog
import com.OxGames.Pluvia.ui.component.topbar.BackButton
import com.OxGames.Pluvia.ui.theme.PluviaTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.micewine.emu.MiceWineUtils
import timber.log.Timber

// TODO strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMiceWineEnvVariables(
    onBack: () -> Unit,
) {
    val state = rememberLazyListState()
    val expanded by remember { derivedStateOf { state.firstVisibleItemIndex == 0 } }

    var envVarsList by remember {
        val list = listOf<MiceWineUtils.EnvVarsSettings.EnvironmentVariable>()
        mutableStateOf(list)
    }
    var dialogVisible by remember { mutableStateOf(false) }
    var tempKey by remember { mutableStateOf<String?>(null) }
    var tempVal by remember { mutableStateOf<String?>(null) }
    var tempPos by remember { mutableStateOf<Int?>(null) }

    val saveEnvironmentVariables: () -> Unit = {
        val varsJson = Gson().toJson(envVarsList)
        PrefManager.putString(MiceWineUtils.EnvVarsSettings.ENV_VARS_KEY, varsJson)
    }

    val showDialog: (Int?) -> Unit = { position ->
        tempPos = position
        val enVar = tempPos?.let { envVarsList[it] }
        tempKey = enVar?.key
        tempVal = enVar?.value
        dialogVisible = true
    }

    val deleteEnvironmentVar: (Int) -> Unit = { position ->
        // Oh no...
        val tempList = envVarsList.toMutableList()
        val result = tempList.removeAt(position)

        Timber.i("Removed Env Var: ${result.key} -> ${result.value}")

        envVarsList = tempList.toList()
        saveEnvironmentVariables()
    }

    LaunchedEffect(Unit) {
        val savedVarsJson = PrefManager.getString(MiceWineUtils.EnvVarsSettings.ENV_VARS_KEY, null)
        savedVarsJson?.let {
            val type = object : TypeToken<List<MiceWineUtils.EnvVarsSettings.EnvironmentVariable>>() {}.type
            val savedVars = Gson().fromJson<List<MiceWineUtils.EnvVarsSettings.EnvironmentVariable>>(it, type)
            envVarsList = savedVars
        }
    }

    KeyValueDialog(
        visible = dialogVisible,
        key = tempKey,
        value = tempVal,
        onKeyChange = { tempKey = it },
        onValueChange = { tempVal = it },
        onDismissRequest = {
            tempKey = null
            tempVal = null
            tempPos = null
            dialogVisible = false
        },
        onConfirmClick = {
            val key = tempKey.orEmpty().trim()
            val value = tempVal.orEmpty().trim()

            if (key.isNotEmpty() && value.isNotEmpty()) {
                val envVar = MiceWineUtils.EnvVarsSettings.EnvironmentVariable(key, value)
                val tempList = envVarsList.toMutableList()
                if (tempPos == null) {
                    tempList.add(envVar)
                } else {
                    tempList[tempPos!!] = envVar
                }
                envVarsList = tempList.toList()
                saveEnvironmentVariables()
            }

            tempKey = null
            tempVal = null
            tempPos = null
            dialogVisible = false
        },
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.env_settings_title)) },
                navigationIcon = { BackButton(onClick = onBack) },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(text = stringResource(R.string.desc_add_env_variable)) },
                icon = { Icon(imageVector = Icons.Default.Add, null) },
                expanded = expanded,
                onClick = { showDialog(null) },
            )
        },
    ) { paddingValues ->
        if (envVarsList.isEmpty()) {
            EmptyScreen(message = "No Environment Variables Added")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                state = state,
                content = {
                    itemsIndexed(items = envVarsList, key = { _, item -> item.key }) { idx, item ->
                        SettingsItemListEnvVar(
                            modifier = Modifier.animateItem(),
                            key = item.key,
                            value = item.value,
                            onClick = { showDialog(idx) },
                            onDelete = { deleteEnvironmentVar(idx) },
                        )

                        if (idx < envVarsList.size - 1) {
                            HorizontalDivider()
                        }
                    }
                },
            )
        }
    }
}

@Preview(device = "id:pixel_5", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
private fun Preview_SettingsMiceWineEnvVariables() {
    PluviaTheme {
        SettingsMiceWineEnvVariables(onBack = {})
    }
}
