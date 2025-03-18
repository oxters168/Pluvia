package com.OxGames.Pluvia.ui.screen

import com.OxGames.Pluvia.PrefManager
import com.OxGames.Pluvia.enums.HomeDestination

data class HomeState(
    val currentDestination: HomeDestination = PrefManager.startScreen,
    val confirmExit: Boolean = false,
)
