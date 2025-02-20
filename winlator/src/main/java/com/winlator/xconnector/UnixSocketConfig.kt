package com.winlator.xconnector

import com.winlator.core.FileUtils.delete
import com.winlator.core.FileUtils.getDirname
import java.io.File

class UnixSocketConfig private constructor(val path: String?) {

    companion object {
        const val SYSVSHM_SERVER_PATH: String = "/tmp/.sysvshm/SM0"
        const val ALSA_SERVER_PATH: String = "/tmp/.sound/AS0"
        const val PULSE_SERVER_PATH: String = "/tmp/.sound/PS0"
        const val XSERVER_PATH: String = "/tmp/.X11-unix/X0"
        const val VIRGL_SERVER_PATH: String = "/tmp/.virgl/V0"

        fun createSocket(rootPath: String?, relativePath: String): UnixSocketConfig {
            val socketFile = File(rootPath, relativePath)

            val dirname = getDirname(relativePath)
            if (dirname.lastIndexOf("/") > 0) {
                val socketDir = File(rootPath, getDirname(relativePath))
                delete(socketDir)
                socketDir.mkdirs()
            } else {
                socketFile.delete()
            }

            return UnixSocketConfig(socketFile.path)
        }
    }
}
