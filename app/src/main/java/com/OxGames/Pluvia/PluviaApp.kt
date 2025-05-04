package com.OxGames.Pluvia

import android.app.Application
import android.os.StrictMode
import com.OxGames.Pluvia.events.EventDispatcher
import com.OxGames.Pluvia.utils.application.CrashHandler
import com.OxGames.Pluvia.utils.application.ReleaseTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class PluviaApp : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.d("Debug enabled with StrictMode logging enabled")
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
            Timber.d("Release logging enabled")
            Timber.plant(ReleaseTree())
        }

        // Init our custom crash handler.
        CrashHandler.initialize(this)

        // Init our datastore preferences.
        PrefManager.init(this)
    }

    companion object {
        internal val events: EventDispatcher = EventDispatcher()
    }
}
