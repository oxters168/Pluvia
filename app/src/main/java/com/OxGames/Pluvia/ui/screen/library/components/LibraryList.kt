package com.OxGames.Pluvia.ui.screen.library.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.data.LibraryItem
import com.OxGames.Pluvia.ui.component.EmptyScreen

@Composable
internal fun LibraryList(
    modifier: Modifier = Modifier,
    contentPaddingValues: PaddingValues,
    listState: LazyListState,
    list: List<LibraryItem>,
    onItemClick: (Int) -> Unit,
) {
    if (list.isEmpty()) {
        EmptyScreen(message = stringResource(R.string.library_no_items))
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            state = listState,
            contentPadding = contentPaddingValues,
        ) {
            items(items = list, key = { it.index }) { item ->
                AppItem(
                    modifier = Modifier.animateItem(),
                    appInfo = item,
                    onClick = { onItemClick(item.appId) },
                )

                if (item.index < list.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

/***********
 * PREVIEW *
 ***********/
