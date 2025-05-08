package com.OxGames.Pluvia.events

import com.OxGames.Pluvia.enums.Orientation
import java.util.EnumSet

interface AndroidEvent<T> : Event<T> {
    data class KeyEvent(val event: android.view.KeyEvent) : AndroidEvent<Boolean>
    data class MotionEvent(val event: android.view.MotionEvent?) : AndroidEvent<Boolean>
    data class SetAllowedOrientation(val orientations: EnumSet<Orientation>) : AndroidEvent<Unit>
    data class SetSystemUIVisibility(val visible: Boolean) : AndroidEvent<Unit>
    data object ActivityDestroyed : AndroidEvent<Unit>

    @Deprecated("BackPressed doesn't seem to have a use case")
    data object BackPressed : AndroidEvent<Unit>

    data object EndProcess : AndroidEvent<Unit>
    data object GuestProgramTerminated : AndroidEvent<Unit>
    data object StartOrientator : AndroidEvent<Unit>
    // data class SetAppBarVisibility(val visible: Boolean) : AndroidEvent<Unit>
}
