package com.OxGames.Pluvia.ui.enums

import android.content.pm.ActivityInfo
import androidx.annotation.StringRes
import com.OxGames.Pluvia.R
import java.util.EnumSet

enum class Orientation(
    @StringRes val string: Int,
    val activityInfoValue: Int,
    val angleRanges: Array<IntRange>,
) {

    // 0° ± 30°
    PORTRAIT(R.string.orientation_portrait, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, arrayOf(330..360, 0..30)),

    // 90° ± 30°
    LANDSCAPE(R.string.orientation_landscape, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, arrayOf(60..120)),

    // 180° ± 30°
    REVERSE_PORTRAIT(R.string.orientation_rev_portrait, ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT, arrayOf(150..210)),

    // 270° ± 30°
    REVERSE_LANDSCAPE(R.string.orientation_rev_landscape, ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE, arrayOf(240..300)),

    // ...
    UNSPECIFIED(R.string.orientation_unspecified, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, arrayOf(0..360)),
    ;

    companion object {
        fun fromActivityInfoValue(value: Int): Orientation =
            Orientation.entries.firstOrNull { it.activityInfoValue == value } ?: UNSPECIFIED

        fun toInt(flags: EnumSet<Orientation>): Int =
            flags.fold(0) { acc, flag -> acc or (1 shl flag.ordinal) }

        fun fromInt(code: Int): EnumSet<Orientation> =
            EnumSet.copyOf(entries.filter { (code and (1 shl it.ordinal)) != 0 })
    }
}
