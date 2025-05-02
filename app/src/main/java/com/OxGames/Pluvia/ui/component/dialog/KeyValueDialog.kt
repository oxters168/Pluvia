package com.OxGames.Pluvia.ui.component.dialog

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.ui.theme.PluviaTheme

@Composable
fun KeyValueDialog(
    visible: Boolean,
    key: String?,
    value: String?,
    onKeyChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirmClick: () -> Unit,
    @StringRes confirmBtnText: Int = R.string.confirm,
    @StringRes dismissBtnText: Int = R.string.cancel,
) {
    if (!visible) {
        return
    }

    AlertDialog(
        icon = {
            if (key == null) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            } else {
                Icon(imageVector = Icons.Default.Edit, contentDescription = null)
            }
        },
        title = {
            Text(
                fontSize = 20.sp,
                text = if (key == null) stringResource(R.string.env_add_action) else stringResource(R.string.env_edit_action),
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = key.orEmpty(),
                    onValueChange = onKeyChange,
                    label = { Text(text = "Environment Key") },
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = value.orEmpty(),
                    onValueChange = onValueChange,
                    label = { Text(text = "Environment Value") },
                )
            }
        },
        onDismissRequest = onDismissRequest,
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(dismissBtnText))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirmClick) {
                Text(text = stringResource(confirmBtnText))
            }
        },
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_KeyValueDialog_New() {
    PluviaTheme {
        KeyValueDialog(
            visible = true,
            key = null,
            value = null,
            onKeyChange = {},
            onValueChange = {},
            onDismissRequest = {},
            onConfirmClick = {},
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_KeyValueDialog_Edit() {
    PluviaTheme {
        KeyValueDialog(
            visible = true,
            key = "Preview Key",
            value = "Preview Value",
            onKeyChange = {},
            onValueChange = {},
            onDismissRequest = {},
            onConfirmClick = {},
        )
    }
}
