package com.winlator.core

import android.content.Context
import android.net.wifi.WifiManager
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.UnknownHostException

class NetworkHelper(context: Context) {

    companion object {
        fun formatIpAddress(ipAddress: Int): String {
            return (ipAddress and 255).toString() + "." + ((ipAddress shr 8) and 255) + "." +
                ((ipAddress shr 16) and 255) + "." + ((ipAddress shr 24) and 255)
        }

        fun formatNetmask(netmask: Int): String {
            return if (netmask == 24) {
                "255.255.255.0"
            } else {
                if (netmask == 16) {
                    "255.255.0.0"
                } else {
                    if (netmask == 8) {
                        "255.0.0.0"
                    } else {
                        "0.0.0.0"
                    }
                }
            }
        }
    }

    private val wifiManager: WifiManager? =
        context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    val ipAddress: Int
        get() = wifiManager?.connectionInfo?.ipAddress ?: 0

    val netmask: Int
        get() {
            if (wifiManager == null) {
                return 0
            }

            val dhcpInfo = wifiManager.dhcpInfo ?: return 0

            var netmask = Integer.bitCount(dhcpInfo.netmask)

            if (dhcpInfo.netmask < 8 || dhcpInfo.netmask > 32) {
                try {
                    val inetAddress = InetAddress.getByName(formatIpAddress(ipAddress))
                    val networkInterface = NetworkInterface.getByInetAddress(inetAddress)

                    if (networkInterface != null) {
                        for (address in networkInterface.interfaceAddresses) {
                            if (inetAddress == address.address) {
                                netmask = address.networkPrefixLength.toInt()
                                break
                            }
                        }
                    }
                } catch (ignored: SocketException) {
                } catch (ignored: UnknownHostException) {
                }
            }

            return netmask
        }

    val gateway: Int
        get() {
            if (wifiManager == null) {
                return 0
            }

            return wifiManager.dhcpInfo?.gateway ?: 0
        }
}
