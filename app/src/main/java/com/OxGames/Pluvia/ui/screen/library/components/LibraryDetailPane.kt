package com.OxGames.Pluvia.ui.screen.library.components

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.service.SteamService
import com.OxGames.Pluvia.ui.component.EmptyScreen
import com.OxGames.Pluvia.ui.screen.library.AppScreen
import com.OxGames.Pluvia.ui.theme.PluviaTheme

@Composable
internal fun LibraryDetailPane(
    appId: Int,
    onClickPlay: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    Surface {
        if (appId == SteamService.INVALID_APP_ID) {
            EmptyScreen(message = stringResource(R.string.library_no_selection))
        } else {
            AppScreen(
                appId = appId,
                onClickPlay = onClickPlay,
                onBack = onBack,
            )
        }
    }
}

/***********
 * PREVIEW *
 ***********/

@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES or android.content.res.Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
private fun Preview_LibraryDetailPane() {
    PluviaTheme {
        LibraryDetailPane(
            appId = Int.MAX_VALUE,
            onClickPlay = { },
            onBack = { },
        )
    }
}
