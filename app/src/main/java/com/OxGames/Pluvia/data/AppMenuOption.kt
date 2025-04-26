package com.OxGames.Pluvia.data

import com.OxGames.Pluvia.enums.AppOptionMenuType

data class AppMenuOption(
    val optionType: AppOptionMenuType,
    val onClick: () -> Unit,
)
