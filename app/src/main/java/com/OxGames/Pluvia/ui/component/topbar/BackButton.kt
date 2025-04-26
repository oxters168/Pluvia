package com.OxGames.Pluvia.ui.component.topbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.ui.theme.PluviaTheme

@Composable
fun BackButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        content = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.desc_nav_back),
            )
        },
    )
}

@Preview
@Composable
private fun Preview_BackButton() {
    PluviaTheme {
        Surface {
            BackButton(onClick = {})
        }
    }
}
