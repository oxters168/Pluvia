package com.OxGames.Pluvia.ui.screen.settings.components

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.ui.theme.PluviaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

@Composable
internal fun SettingsLogViewerScreen(
    onClose: () -> Unit,
    onToast: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    /* Crash Log stuff */
    var latestCrashFile: File? by rememberSaveable { mutableStateOf(null) }
    var latestCrashText: String? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val crashDir = File(context.getExternalFilesDir(null), "crash_logs")
            latestCrashFile = crashDir.listFiles()
                ?.filter { it.name.startsWith("pluvia_crash_") }
                ?.maxByOrNull { it.lastModified() }

            latestCrashFile?.let {
                latestCrashText = it.readText()
            }
        }
    }

    /* Save crash log */
    val saveResultContract = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { resultUri ->
        resultUri?.let { uri ->
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        latestCrashFile?.inputStream()?.use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        val message = context.getString(R.string.toast_failed_crash_save)
                        Timber.e(e, message)
                        onToast(message)
                    }
                }
            }
        }
    }

    SettingsLogViewerContent(
        fileName = latestCrashFile?.name ?: stringResource(R.string.settings_default_crash_filename),
        fileText = latestCrashText ?: stringResource(R.string.settings_default_crash_message),
        onSave = { latestCrashFile?.let { file -> saveResultContract.launch(file.name) } },
        onClose = onClose,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsLogViewerContent(
    fileName: String,
    fileText: String,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = fileName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        content = { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.desc_close_dialog)) },
                    )
                },
                actions = {
                    IconButton(
                        onClick = onSave,
                        content = { Icon(Icons.Default.Save, stringResource(R.string.desc_save_crash)) },
                    )
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    top = WindowInsets.statusBars
                        .asPaddingValues()
                        .calculateTopPadding() + paddingValues.calculateTopPadding(),
                    bottom = 24.dp + paddingValues.calculateBottomPadding(),
                    start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                    end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
                ),
        ) {
            Text(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                fontSize = 12.sp,
                text = fileText,
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
private fun Preview_SettingsLogViewer() {
    PluviaTheme {
        SettingsLogViewerContent(
            fileName = "crash_log.txt",
            fileText = stringResource(R.string.lorem),
            onSave = {},
            onClose = {},
        )
    }
}
