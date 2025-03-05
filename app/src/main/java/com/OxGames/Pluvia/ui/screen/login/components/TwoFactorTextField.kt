package com.OxGames.Pluvia.ui.screen.login.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

// Someday: Redo this with the possibly of fancy OTP boxes with proper autofilling.
@Composable
internal fun TwoFactorTextField(
    twoFactorText: String,
    onTwoFactorTextChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(true) {
        focusRequester.requestFocus()
    }

    OutlinedTextField(
        modifier = Modifier.focusRequester(focusRequester),
        value = twoFactorText,
        onValueChange = { value ->
            val filtered = value.filter { it.isLetterOrDigit() }.take(5)
            onTwoFactorTextChange(filtered)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
        ),
    )
}
