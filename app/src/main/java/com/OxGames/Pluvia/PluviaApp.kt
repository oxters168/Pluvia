package com.OxGames.Pluvia

import android.app.Application
import android.os.StrictMode
import androidx.navigation.NavController
import com.OxGames.Pluvia.events.EventDispatcher
import com.OxGames.Pluvia.utils.application.CrashHandler
import com.OxGames.Pluvia.utils.application.ReleaseTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

typealias NavChangedListener = NavController.OnDestinationChangedListener

@HiltAndroidApp
class PluviaApp : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects() // Detect when Closeable objects are not properly closed
                    .penaltyLog() // Log violations to logcat
                    .build(),
            )

            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll() // Detect all violations (disk reads/writes, network operations, etc.)
                    .penaltyLog() // Log violations to logcat
                    .build(),
            )

            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        // Init our custom crash handler.
        CrashHandler.initialize(this)

        // Init our datastore preferences.
        PrefManager.init(this)
    }

    companion object {
        internal val events: EventDispatcher = EventDispatcher()
        internal var onDestinationChangedListener: NavChangedListener? = null
    }
}
