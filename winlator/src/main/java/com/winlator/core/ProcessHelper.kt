package com.winlator.core

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.Executors
import kotlin.math.pow
import timber.log.Timber

object ProcessHelper {
    const val PRINT_DEBUG: Boolean = false // FIXME change to false

    private val debugCallbacks = ArrayList<Callback<String>>()
    private const val SIGCONT: Byte = 18
    private const val SIGSTOP: Byte = 19

    fun suspendProcess(pid: Int) {
        android.os.Process.sendSignal(pid, SIGSTOP.toInt())
    }

    fun resumeProcess(pid: Int) {
        android.os.Process.sendSignal(pid, SIGCONT.toInt())
    }

    @JvmOverloads
    fun exec(
        command: String,
        envp: Array<String?>? = null,
        workingDir: File? = null,
        terminationCallback: Callback<Int>? = null,
    ): Int {
        var pid = -1

        try {
            val process = Runtime.getRuntime().exec(splitCommand(command), envp, workingDir)
            val pidField = process.javaClass.getDeclaredField("pid")

            pidField.isAccessible = true
            pid = pidField.getInt(process)
            pidField.isAccessible = false

            if (debugCallbacks.isNotEmpty()) {
                createDebugThread(process.inputStream)
                createDebugThread(process.errorStream)
            }

            if (terminationCallback != null) {
                createWaitForThread(process, terminationCallback)
            }
        } catch (e: Exception) {
            Timber.w(e)
        }

        return pid
    }

    private fun createDebugThread(inputStream: InputStream) {
        Executors.newSingleThreadExecutor().execute {
            try {
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String
                    while ((reader.readLine().also { line = it }) != null) {
                        if (PRINT_DEBUG) {
                            println(line)
                        }

                        synchronized(debugCallbacks) {
                            if (debugCallbacks.isNotEmpty()) {
                                for (callback in debugCallbacks) {
                                    callback.call(line)
                                }
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                Timber.w(e)
            }
        }
    }

    private fun createWaitForThread(process: Process, terminationCallback: Callback<Int>) {
        Executors.newSingleThreadExecutor().execute {
            try {
                val status = process.waitFor()
                terminationCallback.call(status)
            } catch (_: InterruptedException) {
            }
        }
    }

    fun removeAllDebugCallbacks() {
        synchronized(debugCallbacks) {
            debugCallbacks.clear()
        }
    }

    fun addDebugCallback(callback: Callback<String>) {
        synchronized(debugCallbacks) {
            if (!debugCallbacks.contains(callback)) debugCallbacks.add(callback)
        }
    }

    fun removeDebugCallback(callback: Callback<String>) {
        synchronized(debugCallbacks) {
            debugCallbacks.remove(callback)
        }
    }

    fun splitCommand(command: String): Array<String> {
        val result = ArrayList<String>()
        var startedQuotes = false
        var value = ""
        var currChar: Char
        var nextChar: Char
        var i = 0
        val count = command.length

        while (i < count) {
            currChar = command[i]

            if (startedQuotes) {
                if (currChar == '"') {
                    startedQuotes = false

                    if (value.isNotEmpty()) {
                        value += '"'
                        result.add(value)
                        value = ""
                    }
                } else {
                    value += currChar
                }
            } else if (currChar == '"') {
                startedQuotes = true
                value += '"'
            } else {
                nextChar = if (i < count - 1) command[i + 1] else '\u0000'

                if (currChar == ' ' || (currChar == '\\' && nextChar == ' ')) {
                    if (currChar == '\\') {
                        value += ' '
                        i++
                    } else if (!value.isEmpty()) {
                        result.add(value)
                        value = ""
                    }
                } else {
                    value += currChar

                    if (i == count - 1) {
                        result.add(value)
                        value = ""
                    }
                }
            }

            i++
        }

        return result.toTypedArray<String>()
    }

    fun getAffinityMaskAsHexString(cpuList: String): String {
        val values = cpuList.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var affinityMask = 0

        for (value in values) {
            val index = value.toByte()
            affinityMask = affinityMask or 2.0.pow(index.toDouble()).toInt()
        }

        return Integer.toHexString(affinityMask)
    }

    fun getAffinityMask(cpuList: String?): Int {
        if (cpuList.isNullOrEmpty()) {
            return 0
        }

        val values = cpuList.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var affinityMask = 0

        for (value in values) {
            val index = value.toByte()
            affinityMask = affinityMask or 2.0.pow(index.toDouble()).toInt()
        }

        return affinityMask
    }

    fun getAffinityMask(cpuList: BooleanArray): Int {
        var affinityMask = 0

        for (i in cpuList.indices) {
            if (cpuList[i]) affinityMask = affinityMask or 2.0.pow(i.toDouble()).toInt()
        }

        return affinityMask
    }

    fun getAffinityMask(from: Int, to: Int): Int {
        var affinityMask = 0

        for (i in from..<to) {
            affinityMask = affinityMask or 2.0.pow(i.toDouble()).toInt()
        }

        return affinityMask
    }
}
