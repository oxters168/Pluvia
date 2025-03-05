package com.OxGames.Pluvia.ui.screen.login

import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
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
import com.OxGames.Pluvia.ui.data.UserLoginState
import com.OxGames.Pluvia.ui.model.UserLoginViewModel
import com.OxGames.Pluvia.ui.screen.login.components.LoginTextFields
import com.OxGames.Pluvia.ui.screen.login.components.QrCodeImage
import com.OxGames.Pluvia.ui.theme.PluviaTheme
import com.materialkolor.ktx.isLight

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

    LoginScreenContent(
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
private fun LoginScreenContent(
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
    val configuration = LocalConfiguration.current
    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) },
        topBar = {
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                CenterAlignedTopAppBar(
                    title = { Text(text = stringResource(R.string.app_name)) },
                )
            }
        },
        floatingActionButton = {
            // Scaffold seems not to calculate 'end' padding when using 3-Button Nav Bar in landscape.
            if (userLoginState.loginResult == LoginResult.Failed) {
                val systemBarPadding = WindowInsets.systemBars
                    .only(WindowInsetsSides.End)
                    .asPaddingValues()

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
                        val text = if (userLoginState.loginScreen == LoginScreen.QR) {
                            "Credential Sign In"
                        } else {
                            "QR Sign In"
                        }
                        Text(text = text)
                    },
                    icon = {
                        val icon = if (userLoginState.loginScreen == LoginScreen.QR) {
                            Icons.Filled.Keyboard
                        } else {
                            Icons.Filled.QrCode2
                        }
                        Icon(imageVector = icon, contentDescription = null)
                    },
                )
            }
        },
    ) { paddingValues ->
        when (configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> LoginScreenContent(
                paddingValues = paddingValues,
                userLoginState = userLoginState,
                onCredentialLogin = onCredentialLogin,
                onPassword = onPassword,
                onQrRetry = onQrRetry,
                onRememberSession = onRememberSession,
                onSetTwoFactor = onSetTwoFactor,
                onTwoFactorLogin = onTwoFactorLogin,
                onUsername = onUsername,
            )

            else -> LoginScreenLandscapeContent(
                paddingValues = paddingValues,
                userLoginState = userLoginState,
                onCredentialLogin = onCredentialLogin,
                onPassword = onPassword,
                onQrRetry = onQrRetry,
                onRememberSession = onRememberSession,
                onSetTwoFactor = onSetTwoFactor,
                onTwoFactorLogin = onTwoFactorLogin,
                onUsername = onUsername,
            )
        }
    }
}

@Composable
private fun LoginScreenLandscapeContent(
    paddingValues: PaddingValues,
    userLoginState: UserLoginState,
    onCredentialLogin: () -> Unit,
    onPassword: (String) -> Unit,
    onQrRetry: () -> Unit,
    onRememberSession: (Boolean) -> Unit,
    onSetTwoFactor: (String) -> Unit,
    onTwoFactorLogin: () -> Unit,
    onUsername: (String) -> Unit,
) {
    Row(modifier = Modifier.padding(paddingValues)) {
        Column(
            modifier = Modifier
                .weight(.33f)
                .border(1.dp, Color.Red)
                .fillMaxSize()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val isLight = MaterialTheme.colorScheme.background.isLight()
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(
                        if (isLight) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                    ),
                painter = painterResource(R.drawable.ic_login_logo),
                contentDescription = "Pluvia Logo",
            )
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
            )
        }
        Column(
            modifier = Modifier
                .weight(.66f)
                .border(1.dp, Color.Blue)
                .fillMaxSize(),
        ) {
            LoginScreenContent(
                paddingValues = PaddingValues(),
                horizontalAlignment = Alignment.Start,
                userLoginState = userLoginState,
                onCredentialLogin = onCredentialLogin,
                onPassword = onPassword,
                onQrRetry = onQrRetry,
                onRememberSession = onRememberSession,
                onSetTwoFactor = onSetTwoFactor,
                onTwoFactorLogin = onTwoFactorLogin,
                onUsername = onUsername,
            )
        }
    }
}

@Composable
private fun LoginScreenContent(
    paddingValues: PaddingValues,
    userLoginState: UserLoginState,
    onCredentialLogin: () -> Unit,
    onPassword: (String) -> Unit,
    onQrRetry: () -> Unit,
    onRememberSession: (Boolean) -> Unit,
    onSetTwoFactor: (String) -> Unit,
    onTwoFactorLogin: () -> Unit,
    onUsername: (String) -> Unit,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.Center,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
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
                        LoginTextFields(
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
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                userLoginState.isQrFailed -> ElevatedButton(onClick = onQrRetry) { Text(text = "Retry") }
                                userLoginState.qrCode.isNullOrEmpty() -> CircularProgressIndicator()
                                else -> QrCodeImage(content = userLoginState.qrCode, size = 256.dp)
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

internal class UserLoginPreview : PreviewParameterProvider<UserLoginState> {
    override val values = sequenceOf(
        UserLoginState(isSteamConnected = true),
        UserLoginState(isSteamConnected = true, loginScreen = LoginScreen.TWO_FACTOR, loginResult = LoginResult.DeviceConfirm),
        UserLoginState(isSteamConnected = true, loginScreen = LoginScreen.QR, qrCode = "Hello World!"),
        UserLoginState(isSteamConnected = true, loginScreen = LoginScreen.QR, isQrFailed = true),
        UserLoginState(isSteamConnected = false),
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    device = "spec:parent=pixel_5,orientation=landscape",
)
@Composable
private fun Preview_LoginScreenContent(
    @PreviewParameter(UserLoginPreview::class) state: UserLoginState,
) {
    val snackBarHostState = remember { SnackbarHostState() }

    PluviaTheme {
        Surface {
            LoginScreenContent(
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
