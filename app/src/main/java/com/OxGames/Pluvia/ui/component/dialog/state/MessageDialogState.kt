package com.OxGames.Pluvia.ui.component.dialog.state

import androidx.annotation.StringRes
import androidx.compose.runtime.saveable.mapSaver
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.ui.enums.DialogType

data class MessageDialogState(
    val visible: Boolean,
    val type: DialogType = DialogType.NONE,
    @StringRes val confirmBtnText: Int = R.string.confirm,
    @StringRes val dismissBtnText: Int = R.string.dismiss,
    @StringRes val title: Int? = null,
    val message: String? = null,
) {
    companion object {
        val Saver = mapSaver(
            save = { state ->
                mapOf(
                    "visible" to state.visible,
                    "type" to state.type,
                    "confirmBtnText" to state.confirmBtnText,
                    "dismissBtnText" to state.dismissBtnText,
                    "title" to state.title,
                    "message" to state.message,
                )
            },
            restore = { savedMap ->
                MessageDialogState(
                    visible = savedMap["visible"] as Boolean,
                    type = savedMap["type"] as DialogType,
                    confirmBtnText = savedMap["confirmBtnText"] as Int,
                    dismissBtnText = savedMap["dismissBtnText"] as Int,
                    title = savedMap["title"] as Int?,
                    message = savedMap["message"] as String?,
                )
            },
        )
    }
}
