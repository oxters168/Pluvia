package com.OxGames.Pluvia.enums

import androidx.annotation.StringRes
import com.OxGames.Pluvia.R
import timber.log.Timber

enum class ControllerSupport(val code: Int, @StringRes val string: Int) {
    none(0, R.string.controller_none),
    partial(1, R.string.controller_partial),
    full(2, R.string.controller_full),
    ;

    companion object {
        fun from(keyValue: String?): ControllerSupport {
            return when (keyValue?.lowercase()) {
                none.name -> none
                partial.name -> partial
                full.name -> full
                else -> {
                    if (keyValue != null) {
                        Timber.e("Could not find proper ControllerSupport from $keyValue")
                    }
                    none
                }
            }
        }

        fun from(code: Int): ControllerSupport {
            ControllerSupport.entries.forEach { appType ->
                if (code == appType.code) {
                    return appType
                }
            }
            return none
        }
    }
}
