package com.OxGames.Pluvia.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import timber.log.Timber

class ServiceConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    var serviceConnection: SteamServiceInterface? = null
        private set

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as? ServiceBinder)?.service
            serviceConnection = service
            Timber.i("Service connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceConnection = null
            Timber.i("Service disconnected")

            // Attempt to reconnect
            bindToService()
        }
    }

    init {
        // Bind to service when this class is instantiated
        bindToService()
    }

    private fun bindToService() {
        val intent = Intent(context, SteamService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService() {
        try {
            context.unbindService(connection)
        } catch (e: Exception) {
            Timber.i("Error unbinding service", e)
        }
    }

    fun ensureServiceStarted() {
        val intent = Intent(context, SteamService::class.java)
        context.startForegroundService(intent)
    }
}
