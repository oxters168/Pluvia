package com.OxGames.Pluvia.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _serviceBinder = MutableStateFlow<SteamService.LocalBinder?>(null)
    val serviceBinder: StateFlow<SteamService.LocalBinder?> = _serviceBinder.asStateFlow()

    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            _serviceBinder.value = service as? SteamService.LocalBinder
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _serviceBinder.value = null
        }
    }

    fun bindService() {
        if (!isBound) {
            Intent(context, SteamService::class.java).also { intent ->
                context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                isBound = true
            }
        }
    }

    fun unbindService() {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
            _serviceBinder.value = null
        }
    }
}
