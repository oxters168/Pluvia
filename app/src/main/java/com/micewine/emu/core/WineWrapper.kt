package com.micewine.emu.core

import android.os.Build
import com.micewine.emu.activities.MainActivity.Companion.selectedCpuAffinity
import com.micewine.emu.activities.MainActivity.Companion.wineDisksFolder
import com.micewine.emu.activities.MainActivity.Companion.winePrefix
import com.micewine.emu.core.EnvVars.getEnv
import com.micewine.emu.core.ShellLoader.runCommand
import com.micewine.emu.core.ShellLoader.runCommandWithOutput
import java.io.File
import kotlin.math.abs

object WineWrapper {
    private var IS_BOX64 = if (Build.SUPPORTED_ABIS[0] == "x86_64") "" else "box64"

    fun getCpuHexMask(): String {
        val availCpus = Runtime.getRuntime().availableProcessors()
        val cpuMask = MutableList(availCpus) { '0' }
        val cpuAffinity = selectedCpuAffinity!!.replace(",", "")

        for (element in cpuAffinity) {
            cpuMask[abs(element.toString().toInt() - availCpus) - 1] = '1'
        }

        return Integer.toHexString(cpuMask.joinToString("").toInt(2))
    }

    fun waitFor(name: String) {
        while (!wine("tasklist", true).contains(name)) {
            Thread.sleep(100)
        }
    }

    fun wine(args: String) {
        runCommand(
            getEnv() + "WINEPREFIX='$winePrefix' $IS_BOX64 wine $args"
        )
    }

    fun wine(args: String, retLog: Boolean): String {
        if (retLog) {
            return runCommandWithOutput(
                getEnv() + "BOX64_LOG=0 WINEPREFIX='$winePrefix' $IS_BOX64 wine $args"
            )
        }
        return ""
    }

    fun wine(args: String, cwd: String) {
        runCommand(
            "cd $cwd;" + getEnv() + "WINEPREFIX='$winePrefix' $IS_BOX64 wine $args"
        )
    }

    fun clearDrives() {
        var letter = 'e'

        while (letter <= 'y') {
            val disk = File("$wineDisksFolder/$letter:")
            if (disk.exists()) {
                disk.delete()
            }
            letter++
        }
    }

    fun addDrive(path: String) {
        runCommand("ln -sf $path $wineDisksFolder/${getAvailableDisks()[0]}:")
    }

    private fun getAvailableDisks(): List<String> {
        var letter = 'c'
        val availableDisks = mutableListOf<String>()

        while (letter <= 'z') {
            if (!File("$wineDisksFolder/$letter:").exists()) {
                availableDisks.add("$letter")
            }
            letter++
        }

        return availableDisks
    }

    fun extractIcon(exeFile: File, output: String) {
        if (exeFile.extension.lowercase() == "exe") {
            runCommand(
                getEnv() + "wrestool -x -t 14 '${getSanitizedPath(exeFile.path)}' > '$output'"
            )
        }
    }

    fun getSanitizedPath(filePath: String) : String {
        return filePath.replace("'", "'\\''")
    }
}