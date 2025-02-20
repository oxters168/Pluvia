package com.winlator.xenvironment.components

import android.os.Process
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.winlator.box86_64.Box86_64Preset
import com.winlator.box86_64.Box86_64PresetManager.getEnvVars
import com.winlator.core.Callback
import com.winlator.core.DefaultVersion
import com.winlator.core.EnvVars
import com.winlator.core.ProcessHelper
import com.winlator.core.ProcessHelper.exec
import com.winlator.core.ProcessHelper.resumeProcess
import com.winlator.core.ProcessHelper.suspendProcess
import com.winlator.core.TarCompressorUtils
import com.winlator.core.TarCompressorUtils.extract
import com.winlator.xconnector.UnixSocketConfig
import com.winlator.xenvironment.EnvironmentComponent
import com.winlator.xenvironment.ImageFs
import java.io.File

class GuestProgramLauncherComponent : EnvironmentComponent() {

    companion object {
        private var pid = -1
        private val lock = Any()
    }

    var guestExecutable: String? = null

    var bindingPaths: Array<String> = arrayOf()

    var envVars: EnvVars? = null

    var box86Preset: String = Box86_64Preset.COMPATIBILITY

    var box64Preset: String = Box86_64Preset.COMPATIBILITY

    var terminationCallback: Callback<Int>? = null

    var isWoW64Mode: Boolean = true

    override fun start() {
        synchronized(lock) {
            stop()
            extractBox86_64Files()
            pid = execGuestProgram()
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

    private fun execGuestProgram(): Int {
        val context = environment!!.context
        val imageFs = environment!!.imageFs
        val rootDir = imageFs.rootDir
        val tmpDir = environment!!.tmpDir
        val nativeLibraryDir = context.applicationInfo.nativeLibraryDir

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val enableBox86_64Logs = preferences.getBoolean("enable_box86_64_logs", false)

        val envVars = EnvVars()

        if (!isWoW64Mode) {
            addBox86EnvVars(envVars, enableBox86_64Logs)
        }

        addBox64EnvVars(envVars, enableBox86_64Logs)
        envVars.put("HOME", ImageFs.HOME_PATH)
        envVars.put("USER", ImageFs.USER)
        envVars.put("TMPDIR", "/tmp")
        envVars.put("LC_ALL", "en_US.utf8")
        envVars.put("DISPLAY", ":0")
        envVars.put("PATH", imageFs.winePath + "/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin")
        envVars.put("LD_LIBRARY_PATH", "/usr/lib/aarch64-linux-gnu:/usr/lib/arm-linux-gnueabihf")
        envVars.put("ANDROID_SYSVSHM_SERVER", UnixSocketConfig.SYSVSHM_SERVER_PATH)

        if ((File(imageFs.lib64Dir, "libandroid-sysvshm.so")).exists() ||
            (File(imageFs.lib32Dir, "libandroid-sysvshm.so")).exists()
        ) {
            envVars.put("LD_PRELOAD", "libandroid-sysvshm.so")
        }

        if (this.envVars != null) {
            envVars.putAll(this.envVars!!)
        }

        val bindSHM = envVars.get("WINEESYNC") == "1"

        var command = "$nativeLibraryDir/libproot.so"
        command += " --kill-on-exit"
        command += " --rootfs=$rootDir"
        command += " --cwd=" + ImageFs.HOME_PATH
        command += " --bind=/dev"

        if (bindSHM) {
            val shmDir = File(rootDir, "/tmp/shm")
            shmDir.mkdirs()
            command += " --bind=" + shmDir.absolutePath + ":/dev/shm"
        }

        command += " --bind=/proc"
        command += " --bind=/sys"

        for (path in bindingPaths) {
            command += " --bind=" + (File(path)).absolutePath
        }

        command += " /usr/bin/env " + envVars.toEscapedString() + " box64 " + guestExecutable

        envVars.clear()
        envVars.put("PROOT_TMP_DIR", tmpDir)
        envVars.put("PROOT_LOADER", "$nativeLibraryDir/libproot-loader.so")

        if (!isWoW64Mode) {
            envVars.put("PROOT_LOADER_32", "$nativeLibraryDir/libproot-loader32.so")
        }

        return exec(
            command, envVars.toStringArray(), rootDir,
        ) { status: Int ->
            synchronized(lock) {
                pid = -1
            }

            terminationCallback?.call(status)
        }
    }

    private fun extractBox86_64Files() {
        val imageFs = environment!!.imageFs
        val context = environment!!.context
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val box86Version = preferences.getString("box86_version", DefaultVersion.BOX86)!!
        val box64Version = preferences.getString("box64_version", DefaultVersion.BOX64)!!
        val currentBox86Version = preferences.getString("current_box86_version", "")!!
        val currentBox64Version = preferences.getString("current_box64_version", "")!!
        val rootDir = imageFs.rootDir

        if (isWoW64Mode) {
            val box86File = File(rootDir, "/usr/local/bin/box86")
            if (box86File.isFile) {
                box86File.delete()
                preferences.edit { putString("current_box86_version", "") }
            }
        } else if (box86Version != currentBox86Version) {
            extract(TarCompressorUtils.Type.ZSTD, context, "box86_64/box86-$box86Version.tzst", rootDir)
            preferences.edit { putString("current_box86_version", box86Version) }
        }

        if (box64Version != currentBox64Version) {
            extract(TarCompressorUtils.Type.ZSTD, context, "box86_64/box64-$box64Version.tzst", rootDir)
            preferences.edit { putString("current_box64_version", box64Version) }
        }
    }

    private fun addBox86EnvVars(envVars: EnvVars, enableLogs: Boolean) {
        envVars.put("BOX86_NOBANNER", if (ProcessHelper.PRINT_DEBUG && enableLogs) "0" else "1")
        envVars.put("BOX86_DYNAREC", "1")

        if (enableLogs) {
            envVars.put("BOX86_LOG", "1")
            envVars.put("BOX86_DYNAREC_MISSING", "1")
        }

        envVars.putAll(getEnvVars("box86", environment!!.context, box86Preset))
        envVars.put("BOX86_X11GLX", "1")
        envVars.put("BOX86_NORCFILES", "1")
    }

    private fun addBox64EnvVars(envVars: EnvVars, enableLogs: Boolean) {
        envVars.put("BOX64_NOBANNER", if (ProcessHelper.PRINT_DEBUG && enableLogs) "0" else "1")
        envVars.put("BOX64_DYNAREC", "1")

        if (isWoW64Mode) {
            envVars.put("BOX64_MMAP32", "1")
        }

        envVars.put("BOX64_AVX", "1")

        if (enableLogs) {
            envVars.put("BOX64_LOG", "1")
            envVars.put("BOX64_DYNAREC_MISSING", "1")
        }

        envVars.putAll(getEnvVars("box64", environment!!.context, box64Preset))
        envVars.put("BOX64_X11GLX", "1")
        envVars.put("BOX64_NORCFILES", "1")
    }

    fun suspendProcess() {
        synchronized(lock) {
            if (pid != -1) suspendProcess(pid)
        }
    }

    fun resumeProcess() {
        synchronized(lock) {
            if (pid != -1) resumeProcess(pid)
        }
    }
}
