package com.winlator.xenvironment.components

import android.os.Process
import com.winlator.core.AppUtils.archName
import com.winlator.core.FileUtils.chmod
import com.winlator.core.FileUtils.writeString
import com.winlator.core.ProcessHelper.exec
import com.winlator.xconnector.UnixSocketConfig
import com.winlator.xenvironment.EnvironmentComponent
import java.io.File

class PulseAudioComponent(private val socketConfig: UnixSocketConfig) : EnvironmentComponent() {

    companion object {
        private var pid = -1
        private val lock = Any()
    }

    override fun start() {
        synchronized(lock) {
            stop()
            pid = execPulseAudio()
        }
    }

    override fun stop() {
        synchronized(lock) {
            if (pid != -1) {
                Process.killProcess(pid)
                pid = -1
            }
        }
    }

    private fun execPulseAudio(): Int {
        val context = environment!!.context
        val nativeLibraryDir = context.applicationInfo.nativeLibraryDir
        val workingDir = File(context.filesDir, "/pulseaudio")

        if (!workingDir.isDirectory) {
            workingDir.mkdirs()
            chmod(workingDir, 505)
        }

        // TODO kotlin the `join`
        val configFile = File(workingDir, "default.pa")
        writeString(
            configFile,
            java.lang.String.join(
                "\n",
                "load-module module-native-protocol-unix auth-anonymous=1 auth-cookie-enabled=0 socket=\"" + socketConfig.path + "\"",
                "load-module module-aaudio-sink",
                "set-default-sink AAudioSink",
            ),
        )

        val archName = archName
        val modulesDir = File(workingDir, "modules/$archName")
        val systemLibPath = if (archName == "arm64") {
            "/system/lib64"
        } else {
            "system/lib"
        }

        val envVars = arrayListOf(
            "LD_LIBRARY_PATH=$systemLibPath:$nativeLibraryDir:$modulesDir",
            "HOME=$workingDir",
            "TMPDIR=" + environment!!.tmpDir,
        )

        var command = "$nativeLibraryDir/libpulseaudio.so"
        command += " --system=false"
        command += " --disable-shm=true"
        command += " --fail=false"
        command += " -n --file=default.pa"
        command += " --daemonize=false"
        command += " --use-pid-file=false"
        command += " --exit-idle-time=-1"

        return exec(command, envVars.toTypedArray(), workingDir)
    }
}
