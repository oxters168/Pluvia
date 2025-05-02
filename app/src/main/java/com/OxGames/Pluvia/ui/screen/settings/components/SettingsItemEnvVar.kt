package com.OxGames.Pluvia.ui.screen.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.OxGames.Pluvia.ui.theme.PluviaTheme

@Composable
fun SettingsItemListEnvVar(
    modifier: Modifier = Modifier,
    key: String,
    value: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        headlineContent = { Text(text = key) },
        supportingContent = { Text(text = value) },
        trailingContent = {
            IconButton(
                onClick = onDelete,
                content = { Icon(imageVector = Icons.Default.Delete, contentDescription = null) },
            )
        },
    )
}

@Preview
@Preview
@Composable
private fun Preview_SettingsItemListEnvVar() {
    PluviaTheme {
        Surface {
            SettingsItemListEnvVar(
                key = "Key Variable",
                value = "Value Variable",
                onClick = {},
                onDelete = {},
            )
        }
    }
}
