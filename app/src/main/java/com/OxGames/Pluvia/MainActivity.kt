package com.OxGames.Pluvia

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color.TRANSPARENT
import android.hardware.input.InputManager
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import android.view.InputDevice
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.OxGames.Pluvia.events.AndroidEvent
import com.OxGames.Pluvia.service.SteamService
import com.OxGames.Pluvia.ui.PluviaMain
import com.OxGames.Pluvia.utils.decoders.AnimatedPngDecoder
import com.OxGames.Pluvia.utils.decoders.IconDecoder
import com.micewine.emu.MiceWineUtils
import com.micewine.emu.controller.ControllerUtils
import com.micewine.emu.core.ShellLoader
import com.micewine.emu.core.WineWrapper
import com.skydoves.landscapist.coil.LocalCoilImageLoader
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path.Companion.toOkioPath
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val onEndProcess: (AndroidEvent.EndProcess) -> Unit = {
        finishAndRemoveTask()
    }

    private var inputManager: InputManager? = null

    private val inputDeviceListener: InputManager.InputDeviceListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) {
            InputDevice.getDevice(deviceId)
        }

        override fun onInputDeviceChanged(deviceId: Int) {
            val inputDevice = InputDevice.getDevice(deviceId) ?: return

            if ((inputDevice.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) ||
                (inputDevice.sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)
            ) {
                if (ControllerUtils.connectedPhysicalControllers.indexOfFirst { it.id == deviceId } == -1) {
                    ControllerUtils.connectedPhysicalControllers.add(
                        ControllerUtils.PhysicalController(
                            name = inputDevice.name,
                            id = deviceId,
                            mappingType = -1,
                            virtualXInputId = -1,
                        ),
                    )
                }

                ControllerUtils.prepareButtonsAxisValues()
            }
        }

        override fun onInputDeviceRemoved(deviceId: Int) {
            val index = ControllerUtils.connectedPhysicalControllers.indexOfFirst { it.id == deviceId }
            if (index == -1) return

            ControllerUtils.GamePadServer.disconnectController(ControllerUtils.connectedPhysicalControllers[index].virtualXInputId)
            ControllerUtils.connectedPhysicalControllers.removeAt(index)
        }
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                MiceWineUtils.Main.ACTION_RUN_WINE -> {
                }

                MiceWineUtils.Main.ACTION_SELECT_FILE_MANAGER -> {
                }

                MiceWineUtils.Main.ACTION_SETUP -> {
                }

                MiceWineUtils.Main.ACTION_INSTALL_RAT -> {
                }

                MiceWineUtils.Main.ACTION_INSTALL_ADTOOLS_DRIVER -> {
                }

                MiceWineUtils.Main.ACTION_SELECT_ICON -> {
                }

                MiceWineUtils.Main.ACTION_CREATE_WINE_PREFIX -> {
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.light(TRANSPARENT, TRANSPARENT))

        super.onCreate(savedInstanceState)

        /* MiceWine Initialization */
        ControllerUtils.initialize(this@MainActivity)

        lifecycleScope.launch {
            ControllerUtils.controllerMouseEmulation()
        }

        inputManager = getSystemService(INPUT_SERVICE) as InputManager
        inputManager?.registerInputDeviceListener(inputDeviceListener, null)

        registerReceiver(
            receiver,
            object : IntentFilter() {
                init {
                    addAction(MiceWineUtils.Main.ACTION_RUN_WINE)
                    addAction(MiceWineUtils.Main.ACTION_SETUP)
                    addAction(MiceWineUtils.Main.ACTION_INSTALL_RAT)
                    addAction(MiceWineUtils.Main.ACTION_INSTALL_ADTOOLS_DRIVER)
                    addAction(MiceWineUtils.Main.ACTION_SELECT_FILE_MANAGER)
                    addAction(MiceWineUtils.Main.ACTION_SELECT_ICON)
                    addAction(MiceWineUtils.Main.ACTION_CREATE_WINE_PREFIX)
                }
            },
        )

        onNewIntent(intent) // TODO do we need?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (MiceWineUtils.Main.winePrefix?.exists() == true) {
                WineWrapper.clearDrives()

                (application.getSystemService(Context.STORAGE_SERVICE) as StorageManager).storageVolumes.forEach { volume ->
                    if (volume.isRemovable) {
                        WineWrapper.addDrive("${volume.directory}")
                    }
                }
            }
        }

        /* Back to Pluvia */
        PluviaApp.events.on<AndroidEvent.EndProcess, Unit>(onEndProcess)

        setContent {
            var hasNotificationPermission by remember { mutableStateOf(false) }
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { hasNotificationPermission = it },
            )

            LaunchedEffect(Unit) {
                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            val context = LocalContext.current
            val imageLoader = remember {
                val memoryCache = MemoryCache.Builder(context)
                    .maxSizePercent(0.1)
                    .strongReferencesEnabled(true)
                    .build()

                val diskCache = DiskCache.Builder()
                    .maxSizePercent(0.03)
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .build()

                ImageLoader.Builder(context)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .memoryCache(memoryCache)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .diskCache(diskCache)
                    .components {
                        add(IconDecoder.Factory())
                        add(AnimatedPngDecoder.Factory())
                    }
                    // .logger(if (BuildConfig.DEBUG) DebugLogger() else null)
                    .build()
            }

            CompositionLocalProvider(
                value = LocalCoilImageLoader provides imageLoader,
                content = { PluviaMain() },
            )
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        if (!MiceWineUtils.Main.usrDir.exists()) {
            // TODO go back to setup (WelcomeActivity)
        } else {
            MiceWineUtils.Main.setupDone = true
        }
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch { runXServer(":0") }
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(receiver)
        inputManager?.unregisterInputDeviceListener(inputDeviceListener)

        PluviaApp.events.emit(AndroidEvent.ActivityDestroyed)

        PluviaApp.events.off<AndroidEvent.EndProcess, Unit>(onEndProcess)

        Timber.d(
            "onDestroy - Connected: %b, Logged-In: %b, Changing-Config: %b",
            SteamService.isConnected,
            SteamService.isLoggedIn,
            isChangingConfigurations,
        )

        if (SteamService.isConnected && !SteamService.isLoggedIn && !isChangingConfigurations) {
            Timber.i("Stopping Steam Service")
            SteamService.stop()
        }
    }

    private suspend fun runXServer(display: String) = withContext(Dispatchers.IO) {
        if (MiceWineUtils.Main.runningXServer && !MiceWineUtils.Main.setupDone) {
            return@withContext
        }

        MiceWineUtils.Main.runningXServer = true

        ShellLoader.runCommand(
            "env CLASSPATH=${getClassPath()} /system/bin/app_process / com.micewine.emu.CmdEntryPoint $display &> /dev/null",
        )

        MiceWineUtils.Main.runningXServer = false
    }

    private fun getClassPath(): String {
        return File(getLibsPath()).parentFile?.parentFile?.absolutePath + "/base.apk"
    }

    private fun getLibsPath(): String {
        return this@MainActivity.applicationInfo.nativeLibraryDir
    }
}
