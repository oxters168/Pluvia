package com.OxGames.Pluvia.enums

import androidx.annotation.StringRes
import com.OxGames.Pluvia.R

enum class AppTheme(@StringRes val string: Int) {
    AUTO(R.string.theme_auto),
    DAY(R.string.theme_day),
    NIGHT(R.string.theme_night),
    AMOLED(R.string.theme_amoled),
}
