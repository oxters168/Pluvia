package com.OxGames.Pluvia.ui.screen.login.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.OxGames.Pluvia.Constants

@Composable
internal fun LoginTextFields(
    isSteamConnected: Boolean,
    username: String,
    onUsername: (String) -> Unit,
    password: String,
    onPassword: (String) -> Unit,
    rememberSession: Boolean,
    onRememberSession: (Boolean) -> Unit,
    onLoginBtnClick: () -> Unit,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedTextField(
            value = username,
            singleLine = true,
            onValueChange = onUsername,
            label = { Text(text = "Username") },
        )
        OutlinedTextField(
            value = password,
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            onValueChange = onPassword,
            label = { Text(text = "Password") },
            trailingIcon = {
                val image = if (passwordVisible) {
                    Icons.Filled.Visibility
                } else {
                    Icons.Filled.VisibilityOff
                }

                val description = if (passwordVisible) "Hide password" else "Show password"

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            },
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = rememberSession,
                    onCheckedChange = onRememberSession,
                )
                Text(text = "Remember session")
            }
            Spacer(modifier = Modifier.width(24.dp))
            ElevatedButton(
                onClick = onLoginBtnClick,
                enabled = username.isNotEmpty() && password.isNotEmpty() && isSteamConnected,
                content = { Text(text = "Login") },
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        /* No affiliation disclaimer */
        Text(
            textAlign = TextAlign.Center,
            text = buildAnnotatedString {
                append("Pluvia is an unofficial Steam® client\n")
                withLink(
                    LinkAnnotation.Url(
                        Constants.Misc.PRIVACY_LINK,
                        TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary)),
                    ),
                    block = { append("Privacy Policy ") },
                )
            },
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
