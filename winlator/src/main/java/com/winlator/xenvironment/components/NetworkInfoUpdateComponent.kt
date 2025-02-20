package com.winlator.xenvironment.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import com.winlator.core.FileUtils.writeString
import com.winlator.core.NetworkHelper
import com.winlator.xenvironment.EnvironmentComponent
import java.io.File

class NetworkInfoUpdateComponent : EnvironmentComponent() {

    private var broadcastReceiver: BroadcastReceiver? = null

    override fun start() {
        val context = environment!!.context
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkHelper = NetworkHelper(context)

        updateAdapterInfoFile(0, 0, 0)

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                var ipAddress = 0
                var netmask = 0
                var gateway = 0

                val networkInfo = connectivityManager.activeNetworkInfo
                if (networkInfo != null &&
                    networkInfo.isAvailable &&
                    networkInfo.isConnected &&
                    networkInfo.type == ConnectivityManager.TYPE_WIFI
                ) {
                    ipAddress = networkHelper.ipAddress
                    netmask = networkHelper.netmask
                    gateway = networkHelper.gateway
                }

                updateAdapterInfoFile(ipAddress, netmask, gateway)
                updateEtcHostsFile(ipAddress)
            }
        }

        val filter = IntentFilter()
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        context.registerReceiver(broadcastReceiver, filter)
    }

    override fun stop() {
        if (broadcastReceiver != null) {
            environment!!.context.unregisterReceiver(broadcastReceiver)
            broadcastReceiver = null
        }
    }

    private fun updateAdapterInfoFile(ipAddress: Int, netmask: Int, gateway: Int) {
        val file = File(environment!!.imageFs.tmpDir, "adapterinfo")
        writeString(
            file = file,
            data = "Android Wi-Fi Adapter," + NetworkHelper.formatIpAddress(ipAddress) + "," +
                NetworkHelper.formatNetmask(netmask) + "," + NetworkHelper.formatIpAddress(gateway),
        )
    }

    private fun updateEtcHostsFile(ipAddress: Int) {
        val ip = if (ipAddress != 0) NetworkHelper.formatIpAddress(ipAddress) else "127.0.0.1"
        val file = File(environment!!.imageFs.rootDir, "etc/hosts")

        writeString(file, "$ip\tlocalhost\n")
    }
}
