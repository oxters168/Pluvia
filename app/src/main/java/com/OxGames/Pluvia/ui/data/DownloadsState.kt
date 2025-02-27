package com.OxGames.Pluvia.ui.data

import com.OxGames.Pluvia.data.LibraryItem

data class DownloadsState(
    val isRefreshing: Boolean = false,
    val items: List<LibraryItem> = emptyList(),

)
