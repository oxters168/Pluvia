package com.OxGames.Pluvia.ui.screen.xserver

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.text.format.DateUtils
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.ui.component.dialog.MessageDialog
import com.OxGames.Pluvia.ui.theme.PluviaTheme
import com.OxGames.Pluvia.utils.ContainerUtils
import com.winlator.container.ContainerManager
import com.winlator.inputcontrols.TouchMouse
import com.winlator.widget.XServerView
import com.winlator.winhandler.WinHandler
import com.winlator.xserver.Keyboard
import com.winlator.xserver.Property
import com.winlator.xserver.ScreenInfo
import com.winlator.xserver.Window
import com.winlator.xserver.WindowManager
import com.winlator.xserver.XServer
import java.nio.file.Paths
import kotlin.io.path.name
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun XServerScreen(
    appId: Int,
    bootToContainer: Boolean,
    navigateBack: () -> Unit,
    onExit: () -> Unit,
    onWindowMapped: ((Window) -> Unit)? = null,
    onWindowUnmapped: ((Window) -> Unit)? = null,
    viewModel: XServerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isExitDialogOpen by remember { mutableStateOf(false) }
    val xServerState by viewModel.state.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // imm.showSoftInput() doesn't work
    // Compose LocalSoftwareKeyboardController doesn't work.
    @Suppress("DEPRECATION")
    val showKeyboard = {
        // Old school way just like in Winlator, with extra Compose hackery.
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            (context as ComponentActivity).window.decorView.postDelayed(
                {
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                },
                500L,
            )
        } else {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                XServerViewModel.XServerUiEvent.OnExit -> onExit()
                XServerViewModel.XServerUiEvent.OnNavigateBack -> navigateBack()
            }
        }
    }

    MessageDialog(
        visible = isExitDialogOpen,
        onDismissRequest = { isExitDialogOpen = false },
        onConfirmClick = {
            scope.launch {
                isExitDialogOpen = false
                viewModel.exit()
            }
        },
        onDismissClick = { isExitDialogOpen = false },
        confirmBtnText = R.string.close,
        dismissBtnText = R.string.cancel,
        icon = Icons.AutoMirrored.Filled.ExitToApp,
        title = R.string.dialog_title_exit_game,
        message = context.getString(
            R.string.dialog_message_exit_game,
            xServerState.gameName.ifEmpty { context.getString(R.string.xserver_no_game_name) },
        ),
    )

    BackHandler {
        scope.launch {
            if (drawerState.isClosed) {
                drawerState.open()
            } else {
                drawerState.close()
            }

            Timber.i("Drawer state is now: ${drawerState.currentValue}")
        }
    }

    // Verify
    val touchHandler = remember {
        { event: MotionEvent ->
            viewModel.touchMouse?.onTouchEvent(event)
            true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            XServerDrawer(
                state = xServerState,
                onKeyboard = {
                    scope.launch {
                        Timber.i("Show keyboard called")
                        drawerState.close()
                        showKeyboard()
                    }
                },
                onExit = {
                    scope.launch {
                        Timber.i("Exit Dialog called")
                        drawerState.close()
                        isExitDialogOpen = true
                    }
                },
            )
        },
        content = {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerHoverIcon(PointerIcon(0))
                    .pointerInteropFilter(onTouchEvent = touchHandler),
                factory = {
                    Timber.i("Creating XServerView and XServer")

                    viewModel.init(appId)

                    XServerView(context, XServer(ScreenInfo(xServerState.screenSize))).apply {
                        viewModel.xServerView = this

                        renderer.isCursorVisible = false

                        xServer.renderer = renderer
                        xServer.winHandler = WinHandler(xServer, this)

                        viewModel.touchMouse = TouchMouse(xServer)
                        viewModel.keyboard = Keyboard(xServer)

                        if (!bootToContainer) {
                            renderer.setUnviewableWMClasses("explorer.exe")
                            // TODO: make 'force fullscreen' be an option of the app being launched
                            viewModel.appLaunchInfo?.let { renderer.forceFullscreenWMClass = Paths.get(it.executable).name }
                        }

                        xServer.windowManager.addOnWindowModificationListener(
                            object : WindowManager.OnWindowModificationListener {
                                override fun onUpdateWindowContent(window: Window) {
                                    if (!xServerState.winStarted && window.isApplicationWindow()) {
                                        renderer.setCursorVisible(true)
                                        viewModel.winStarted(true)
                                    }
                                }

                                override fun onModifyWindowProperty(window: Window, property: Property) {
                                }

                                override fun onMapWindow(window: Window) {
                                    Timber.i(
                                        "onMapWindow:" +
                                            "\n\twindowName: ${window.name}" +
                                            "\n\twindowClassName: ${window.className}" +
                                            "\n\tprocessId: ${window.processId}" +
                                            "\n\thasParent: ${window.parent != null}" +
                                            "\n\tchildrenSize: ${window.children.size}",
                                    )

                                    viewModel.assignTaskAffinity(window, xServer.winHandler)

                                    onWindowMapped?.invoke(window)
                                }

                                override fun onUnmapWindow(window: Window) {
                                    Timber.i(
                                        "onUnmapWindow:" +
                                            "\n\twindowName: ${window.name}" +
                                            "\n\twindowClassName: ${window.className}" +
                                            "\n\tprocessId: ${window.processId}" +
                                            "\n\thasParent: ${window.parent != null}" +
                                            "\n\tchildrenSize: ${window.children.size}",
                                    )

                                    // changeFrameRatingVisibility(window, null)
                                    onWindowUnmapped?.invoke(window)
                                }
                            },
                        )

                        if (viewModel.xEnvironment != null) {
                            viewModel.xEnvironment = viewModel.shiftXEnvironmentToContext(xServer = xServer)
                        } else {
                            val containerManager = ContainerManager(context)
                            val container = ContainerUtils.getContainer(context, appId)
                            containerManager.activateContainer(container)

                            viewModel.setBootConfig(container, containerManager, bootToContainer)
                        }
                    }
                },
                update = { },
                onRelease = {
                    Timber.w("AndroidView Release")
                },
            )
        },
    )
}

@Composable
private fun XServerDrawer(
    state: XServerState,
    onKeyboard: () -> Unit,
    onExit: () -> Unit,
) {
    ModalDrawerSheet {
        // Header
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceDim)
                .padding(all = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                modifier = Modifier.size(96.dp),
                painter = painterResource(id = R.drawable.ic_logo_color),
                contentDescription = null,
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    fontSize = 32.sp,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (state.gameName.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.friend_playing_game, state.gameName),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.surfaceTint,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = DateUtils.formatDateTime(LocalContext.current, state.currentTime, DateUtils.FORMAT_SHOW_TIME),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.surfaceTint,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        // Items
        NavigationDrawerItem(
            label = { Text(text = stringResource(R.string.xserver_drawer_show_keyboard)) },
            icon = { Icon(imageVector = Icons.Default.Keyboard, contentDescription = null) },
            selected = false,
            onClick = onKeyboard,
        )
        NavigationDrawerItem(
            label = { Text(text = stringResource(R.string.exit)) },
            icon = { Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
            selected = false,
            onClick = onExit,
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
private fun Preview_XServerDrawer() {
    PluviaTheme {
        Surface {
            XServerDrawer(
                state = XServerState(gameName = "The Game"),
                onKeyboard = {},
                onExit = {},
            )
        }
    }
}
