package com.OxGames.Pluvia.ui.component.dialog

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.ui.theme.PluviaTheme

@Composable
fun MessageDialog(
    visible: Boolean,
    onDismissRequest: (() -> Unit)? = null,
    onConfirmClick: (() -> Unit)? = null,
    onDismissClick: (() -> Unit)? = null,
    @StringRes confirmBtnText: Int = R.string.confirm,
    @StringRes dismissBtnText: Int = R.string.dismiss,
    icon: ImageVector? = null,
    @StringRes title: Int? = null,
    message: String? = null,
    useHtmlInMsg: Boolean = false,
) {
    when {
        visible -> {
            AlertDialog(
                icon = icon?.let { { Icon(imageVector = icon, contentDescription = title?.let { stringResource(it) }) } },
                title = title?.let { { Text(text = stringResource(it)) } },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        message?.let {
                            if (useHtmlInMsg) {
                                Text(
                                    text = AnnotatedString.fromHtml(
                                        htmlString = it,
                                        linkStyles = TextLinkStyles(
                                            style = SpanStyle(
                                                textDecoration = TextDecoration.Underline,
                                                fontStyle = FontStyle.Italic,
                                                color = Color.Blue,
                                            ),
                                        ),
                                    ),
                                )
                            } else {
                                Text(text = it)
                            }
                        }
                    }
                },
                onDismissRequest = { onDismissRequest?.invoke() },
                dismissButton = onDismissClick?.let {
                    {
                        TextButton(onClick = it) {
                            Text(text = stringResource(dismissBtnText))
                        }
                    }
                },
                confirmButton = {
                    onConfirmClick?.let {
                        TextButton(onClick = it) {
                            Text(text = stringResource(confirmBtnText))
                        }
                    }
                },
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_MessageDialog() {
    PluviaTheme {
        MessageDialog(
            visible = true,
            icon = Icons.Default.Gamepad,
            title = R.string.dialog_title_unsaved_changes,
            message = stringResource(R.string.lorem),
            onDismissRequest = {},
            onDismissClick = {},
            onConfirmClick = {},
        )
    }
}
