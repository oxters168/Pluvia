package com.OxGames.Pluvia.ui.screen.login

import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.enums.LoginResult
import com.OxGames.Pluvia.enums.LoginScreen
import com.OxGames.Pluvia.ui.component.LoadingScreen
import com.OxGames.Pluvia.ui.screen.login.components.LoginTextField
import com.OxGames.Pluvia.ui.screen.login.components.QrCodeImage
import com.OxGames.Pluvia.ui.screen.login.components.TwoFactorAuthScreenContent
import com.OxGames.Pluvia.ui.theme.PluviaTheme

@Composable
fun UserLoginScreen(
    viewModel: LoginViewModel = viewModel(),
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val state by viewModel.loginState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.snackEvents.collect { message ->
            snackBarHostState.showSnackbar(message)
        }
    }

    UserLoginScreenContent(
        snackBarHostState = snackBarHostState,
        state = state,
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
    state: LoginState,
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
            if (state.loginResult == LoginResult.Failed) {
                val systemBarPadding = WindowInsets.systemBars
                    .only(WindowInsetsSides.End)
                    .asPaddingValues()

                val fabString = if (state.loginScreen == LoginScreen.QR) {
                    R.string.login_fab_credential
                } else {
                    R.string.login_fab_qr
                }

                ExtendedFloatingActionButton(
                    modifier = Modifier
                        .padding(end = systemBarPadding.calculateEndPadding(LayoutDirection.Ltr))
                        .displayCutoutPadding(),
                    onClick = {
                        when (state.loginScreen) {
                            LoginScreen.QR -> onShowLoginScreen(LoginScreen.CREDENTIAL)
                            LoginScreen.CREDENTIAL -> onShowLoginScreen(LoginScreen.QR)
                            else -> onShowLoginScreen(LoginScreen.CREDENTIAL)
                        }
                    },
                    text = {
                        Text(text = stringResource(fabString))
                    },
                    icon = {
                        val icon = if (state.loginScreen == LoginScreen.QR) {
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
                state.isLoggingIn.not() &&
                state.loginResult != LoginResult.Success
            ) {
                Crossfade(
                    modifier = Modifier.fillMaxSize(),
                    targetState = state.loginScreen,
                ) { screen ->
                    when (screen) {
                        LoginScreen.CREDENTIAL -> {
                            LoginTextField(
                                isSteamConnected = state.isSteamConnected,
                                username = state.username,
                                onUsername = onUsername,
                                password = state.password,
                                onPassword = onPassword,
                                rememberSession = state.rememberSession,
                                onRememberSession = onRememberSession,
                                onLoginBtnClick = onCredentialLogin,
                            )
                        }

                        LoginScreen.TWO_FACTOR -> {
                            TwoFactorAuthScreenContent(
                                loginState = state,
                                message = when {
                                    state.previousCodeIncorrect ->
                                        stringResource(R.string.steam_2fa_incorrect)

                                    state.loginResult == LoginResult.DeviceAuth ->
                                        stringResource(R.string.steam_2fa_device)

                                    state.loginResult == LoginResult.DeviceConfirm ->
                                        stringResource(R.string.steam_2fa_confirmation)

                                    state.loginResult == LoginResult.EmailAuth ->
                                        stringResource(
                                            R.string.steam_2fa_email,
                                            state.email ?: "...",
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
                                if (state.isQrFailed) {
                                    ElevatedButton(onClick = onQrRetry) { Text(text = stringResource(R.string.retry)) }
                                } else if (state.qrCode.isNullOrEmpty()) {
                                    CircularProgressIndicator()
                                } else {
                                    QrCodeImage(content = state.qrCode, size = 256.dp)
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

internal class UserLoginPreview : PreviewParameterProvider<LoginState> {
    override val values = sequenceOf(
        LoginState(isSteamConnected = true),
        LoginState(isSteamConnected = true, loginScreen = LoginScreen.QR, qrCode = "Hello World!"),
        LoginState(isSteamConnected = true, loginScreen = LoginScreen.QR, isQrFailed = true),
        LoginState(isSteamConnected = false),
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_UserLoginScreen(
    @PreviewParameter(UserLoginPreview::class) state: LoginState,
) {
    val snackBarHostState = remember { SnackbarHostState() }
    var innerState by remember { mutableStateOf(state) }

    PluviaTheme {
        Surface {
            UserLoginScreenContent(
                snackBarHostState = snackBarHostState,
                state = innerState,
                onUsername = { innerState = innerState.copy(username = it) },
                onPassword = { innerState = innerState.copy(password = it) },
                onRememberSession = { innerState = innerState.copy(rememberSession = it) },
                onCredentialLogin = { },
                onTwoFactorLogin = { },
                onQrRetry = { },
                onSetTwoFactor = { },
                onShowLoginScreen = { },
            )
        }
    }
}
