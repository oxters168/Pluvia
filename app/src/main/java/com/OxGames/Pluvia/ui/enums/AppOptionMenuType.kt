package com.OxGames.Pluvia.ui.enums

import androidx.annotation.StringRes
import com.OxGames.Pluvia.R

enum class AppOptionMenuType(@StringRes val string: Int) {
    StorePage(R.string.app_option_menu_store),
    RunContainer(R.string.app_option_menu_open),
    EditContainer(R.string.app_option_menu_edit),
    Uninstall(R.string.app_option_menu_remove),
}
