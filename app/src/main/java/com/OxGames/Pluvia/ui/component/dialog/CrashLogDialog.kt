package com.OxGames.Pluvia.ui.component.dialog

import android.content.res.Configuration
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.OxGames.Pluvia.ui.theme.PluviaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashLogDialog(
    visible: Boolean = true,
    fileName: String,
    fileText: String,
    onSave: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    if (visible) {
        val scrollState = rememberScrollState()

        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = false,
            ),
            content = {
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
                                    onClick = onDismissRequest,
                                    content = { Icon(Icons.Default.Close, null) },
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = onSave,
                                    content = { Icon(Icons.Default.Save, null) },
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
            },
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
private fun Preview_CrashLogDialog() {
    PluviaTheme {
        CrashLogDialog(
            fileName = "crash_log.txt",
            fileText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed " +
                "do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
                "Ut enim ad minim veniam, quis nostrud exercitation ullamco " +
                "laboris nisi ut aliquip ex ea commodo consequat. Duis aute " +
                "irure dolor in reprehenderit in voluptate velit esse cillum " +
                "dolore eu fugiat nulla pariatur. Excepteur sint occaecat " +
                "cupidatat non proident, sunt in culpa qui officia deserunt " +
                "mollit anim id est laborum.",
            onSave = {},
            onDismissRequest = {},
        )
    }
}
