package com.OxGames.Pluvia.ui.screen.login

import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.OxGames.Pluvia.Constants
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.enums.LoginResult
import com.OxGames.Pluvia.enums.LoginScreen
import com.OxGames.Pluvia.ui.component.LoadingScreen
import com.OxGames.Pluvia.ui.screen.login.components.QrCodeImage
import com.OxGames.Pluvia.ui.screen.login.components.TwoFactorAuthScreenContent
import com.OxGames.Pluvia.ui.theme.PluviaTheme

@Composable
fun UserLoginScreen(
    viewModel: UserLoginViewModel = viewModel(),
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val userLoginState by viewModel.loginState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.snackEvents.collect { message ->
            snackBarHostState.showSnackbar(message)
        }
    }

    UserLoginScreenContent(
        snackBarHostState = snackBarHostState,
        userLoginState = userLoginState,
        onUsername = viewModel::setUsername,
        onPassword = viewModel::setPassword,
        onShowLoginScreen = viewModel::onShowLoginScreen,
        onRememberSession = viewModel::setRememberSession,
        onCredentialLogin = viewModel::onCredentialLogin,
        onTwoFactorLogin = viewModel::submit,
        onQrRetry = viewModel::onQrRetry,
        onSetTwoFactor = viewModel::setTwoFactorCode,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserLoginScreenContent(
    snackBarHostState: SnackbarHostState,
    userLoginState: UserLoginState,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onShowLoginScreen: (LoginScreen) -> Unit,
    onRememberSession: (Boolean) -> Unit,
    onCredentialLogin: () -> Unit,
    onTwoFactorLogin: () -> Unit,
    onQrRetry: () -> Unit,
    onSetTwoFactor: (String) -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) },
        topBar = {
            CenterAlignedTopAppBar(title = { Text(text = stringResource(R.string.app_name)) })
        },
        floatingActionButton = {
            // Scaffold seems not to calculate 'end' padding when using 3-Button Nav Bar in landscape.
            if (userLoginState.loginResult == LoginResult.Failed) {
                val systemBarPadding = WindowInsets.systemBars
                    .only(WindowInsetsSides.End)
                    .asPaddingValues()

                val fabString = if (userLoginState.loginScreen == LoginScreen.QR) {
                    R.string.login_fab_credential
                } else {
                    R.string.login_fab_qr
                }

                ExtendedFloatingActionButton(
                    modifier = Modifier
                        .padding(end = systemBarPadding.calculateEndPadding(LayoutDirection.Ltr))
                        .displayCutoutPadding(),
                    onClick = {
                        when (userLoginState.loginScreen) {
                            LoginScreen.QR -> onShowLoginScreen(LoginScreen.CREDENTIAL)
                            LoginScreen.CREDENTIAL -> onShowLoginScreen(LoginScreen.QR)
                            else -> onShowLoginScreen(LoginScreen.CREDENTIAL)
                        }
                    },
                    text = {
                        Text(text = stringResource(fabString))
                    },
                    icon = {
                        val icon = if (userLoginState.loginScreen == LoginScreen.QR) {
                            Icons.Filled.Keyboard
                        } else {
                            Icons.Filled.QrCode2
                        }
                        Icon(imageVector = icon, contentDescription = stringResource(fabString))
                    },
                )
            }
        },
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (
                userLoginState.isLoggingIn.not() &&
                userLoginState.loginResult != LoginResult.Success
            ) {
                Crossfade(
                    modifier = Modifier.fillMaxSize(),
                    targetState = userLoginState.loginScreen,
                ) { screen ->
                    when (screen) {
                        LoginScreen.CREDENTIAL -> {
                            UsernamePassword(
                                isSteamConnected = userLoginState.isSteamConnected,
                                username = userLoginState.username,
                                onUsername = onUsername,
                                password = userLoginState.password,
                                onPassword = onPassword,
                                rememberSession = userLoginState.rememberSession,
                                onRememberSession = onRememberSession,
                                onLoginBtnClick = onCredentialLogin,
                            )
                        }

                        LoginScreen.TWO_FACTOR -> {
                            TwoFactorAuthScreenContent(
                                userLoginState = userLoginState,
                                message = when {
                                    userLoginState.previousCodeIncorrect ->
                                        stringResource(R.string.steam_2fa_incorrect)

                                    userLoginState.loginResult == LoginResult.DeviceAuth ->
                                        stringResource(R.string.steam_2fa_device)

                                    userLoginState.loginResult == LoginResult.DeviceConfirm ->
                                        stringResource(R.string.steam_2fa_confirmation)

                                    userLoginState.loginResult == LoginResult.EmailAuth ->
                                        stringResource(
                                            R.string.steam_2fa_email,
                                            userLoginState.email ?: "...",
                                        )

                                    else -> ""
                                },
                                onSetTwoFactor = onSetTwoFactor,
                                onLogin = onTwoFactorLogin,
                            )
                        }

                        LoginScreen.QR -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                if (userLoginState.isQrFailed) {
                                    ElevatedButton(onClick = onQrRetry) { Text(text = stringResource(R.string.retry)) }
                                } else if (userLoginState.qrCode.isNullOrEmpty()) {
                                    CircularProgressIndicator()
                                } else {
                                    QrCodeImage(content = userLoginState.qrCode, size = 256.dp)
                                }
                            }
                        }
                    }
                }
            } else {
                LoadingScreen()
            }
        }
    }
}

@Composable
private fun UsernamePassword(
    isSteamConnected: Boolean,
    username: String,
    onUsername: (String) -> Unit,
    password: String,
    onPassword: (String) -> Unit,
    rememberSession: Boolean,
    onRememberSession: (Boolean) -> Unit,
    onLoginBtnClick: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        OutlinedTextField(
            value = username,
            singleLine = true,
            onValueChange = onUsername,
            label = { Text(text = stringResource(R.string.username)) },
        )
        OutlinedTextField(
            value = password,
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            onValueChange = onPassword,
            label = { Text(text = stringResource(R.string.password)) },
            trailingIcon = {
                val image = if (passwordVisible) {
                    Icons.Filled.Visibility
                } else {
                    Icons.Filled.VisibilityOff
                }

                val description = if (passwordVisible) R.string.desc_login_hide_password else R.string.desc_login_show_password

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = stringResource(description))
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
                Text(text = stringResource(R.string.login_remember_session))
            }
            Spacer(modifier = Modifier.width(24.dp))
            ElevatedButton(
                onClick = onLoginBtnClick,
                enabled = username.isNotEmpty() && password.isNotEmpty() && isSteamConnected,
                content = { Text(text = stringResource(R.string.login)) },
            )
        }

        /* Footer + Privacy Policy */
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            textAlign = TextAlign.Center,
            text = buildAnnotatedString {
                append(text = stringResource(R.string.login_footer))
                withLink(
                    LinkAnnotation.Url(
                        Constants.Misc.PRIVACY_LINK,
                        TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary)),
                    ),
                    block = { append(text = stringResource(R.string.login_privacy_policy)) },
                )
            },
        )
    }
}

internal class UserLoginPreview : PreviewParameterProvider<UserLoginState> {
    override val values = sequenceOf(
        UserLoginState(isSteamConnected = true),
        UserLoginState(isSteamConnected = true, loginScreen = LoginScreen.QR, qrCode = "Hello World!"),
        UserLoginState(isSteamConnected = true, loginScreen = LoginScreen.QR, isQrFailed = true),
        UserLoginState(isSteamConnected = false),
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_UserLoginScreen(
    @PreviewParameter(UserLoginPreview::class) state: UserLoginState,
) {
    val snackBarHostState = remember { SnackbarHostState() }

    PluviaTheme {
        Surface {
            UserLoginScreenContent(
                snackBarHostState = snackBarHostState,
                userLoginState = state,
                onUsername = { },
                onPassword = { },
                onRememberSession = { },
                onCredentialLogin = { },
                onTwoFactorLogin = { },
                onQrRetry = { },
                onSetTwoFactor = { },
                onShowLoginScreen = { },
            )
        }
    }
}
