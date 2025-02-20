package com.winlator.winhandler

import android.view.KeyEvent
import android.view.MotionEvent
import com.winlator.core.StringUtils.fromANSIString
import com.winlator.inputcontrols.ExternalController
import com.winlator.widget.XServerView
import com.winlator.xserver.XServer
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayDeque
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import timber.log.Timber

class WinHandler(private val xServer: XServer, private val xServerView: XServerView) {

    companion object {
        private const val SERVER_PORT: Short = 7947
        private const val CLIENT_PORT: Short = 7946
        const val DINPUT_MAPPER_TYPE_STANDARD: Byte = 0
        const val DINPUT_MAPPER_TYPE_XINPUT: Byte = 1
    }

    private var socket: DatagramSocket? = null

    private val sendData: ByteBuffer = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN)

    private val receiveData: ByteBuffer = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN)

    private val sendPacket = DatagramPacket(sendData.array(), 64)

    private val receivePacket = DatagramPacket(receiveData.array(), 64)

    private val actions = ArrayDeque<Runnable>()

    private var initReceived = false

    private var running = false

    var onGetProcessInfoListener: OnGetProcessInfoListener? = null

    var currentController: ExternalController? = null
        private set

    private var localhost: InetAddress? = null

    var dInputMapperType: Byte = DINPUT_MAPPER_TYPE_XINPUT

    private val gamepadClients: MutableList<Int> = CopyOnWriteArrayList()

    private fun sendPacket(port: Int): Boolean {
        try {
            val size = sendData.position()

            if (size == 0) {
                return false
            }

            sendPacket.address = localhost
            sendPacket.port = port

            socket!!.send(sendPacket)

            return true
        } catch (e: IOException) {
            return false
        }
    }

    fun exec(command: String) {
        var command = command

        command = command.trim { it <= ' ' }

        if (command.isEmpty()) {
            return
        }

        val cmdList = command.split(" ".toRegex(), limit = 2).toTypedArray()
        val filename = cmdList[0]
        val parameters = if (cmdList.size > 1) cmdList[1] else ""

        addAction {
            val filenameBytes = filename.toByteArray()
            val parametersBytes = parameters.toByteArray()

            sendData.rewind()
            sendData.put(RequestCodes.EXEC)
            sendData.putInt(filenameBytes.size + parametersBytes.size + 8)
            sendData.putInt(filenameBytes.size)
            sendData.putInt(parametersBytes.size)
            sendData.put(filenameBytes)
            sendData.put(parametersBytes)

            sendPacket(CLIENT_PORT.toInt())
        }
    }

    fun killProcess(processName: String) {
        addAction {
            sendData.rewind()
            sendData.put(RequestCodes.KILL_PROCESS)

            val bytes = processName.toByteArray()

            sendData.putInt(bytes.size)
            sendData.put(bytes)

            sendPacket(CLIENT_PORT.toInt())
        }
    }

    fun listProcesses() {
        addAction {
            sendData.rewind()
            sendData.put(RequestCodes.LIST_PROCESSES)
            sendData.putInt(0)

            if (!sendPacket(CLIENT_PORT.toInt()) && onGetProcessInfoListener != null) {
                onGetProcessInfoListener!!.onGetProcessInfo(0, 0, null)
            }
        }
    }

    fun setProcessAffinity(processName: String, affinityMask: Int) {
        addAction {
            val bytes = processName.toByteArray()

            sendData.rewind()
            sendData.put(RequestCodes.SET_PROCESS_AFFINITY)
            sendData.putInt(9 + bytes.size)
            sendData.putInt(0)
            sendData.putInt(affinityMask)
            sendData.put(bytes.size.toByte())
            sendData.put(bytes)

            sendPacket(CLIENT_PORT.toInt())
        }
    }

    fun setProcessAffinity(pid: Int, affinityMask: Int) {
        addAction {
            sendData.rewind()
            sendData.put(RequestCodes.SET_PROCESS_AFFINITY)
            sendData.putInt(9)
            sendData.putInt(pid)
            sendData.putInt(affinityMask)
            sendData.put(0.toByte())

            sendPacket(CLIENT_PORT.toInt())
        }
    }

    fun mouseEvent(flags: Int, dx: Int, dy: Int, wheelDelta: Int) {
        if (!initReceived) return
        addAction {
            sendData.rewind()
            sendData.put(RequestCodes.MOUSE_EVENT)
            sendData.putInt(10)
            sendData.putInt(flags)
            sendData.putShort(dx.toShort())
            sendData.putShort(dy.toShort())
            sendData.putShort(wheelDelta.toShort())
            sendData.put((if ((flags and MouseEventFlags.MOVE) != 0) 1 else 0).toByte()) // cursor pos feedback

            sendPacket(CLIENT_PORT.toInt())
        }
    }

    fun keyboardEvent(vkey: Byte, flags: Int) {
        if (!initReceived) return
        addAction {
            sendData.rewind()
            sendData.put(RequestCodes.KEYBOARD_EVENT)
            sendData.put(vkey)
            sendData.putInt(flags)

            sendPacket(CLIENT_PORT.toInt())
        }
    }

    @JvmOverloads
    fun bringToFront(processName: String, handle: Long = 0) {
        addAction {
            sendData.rewind()
            sendData.put(RequestCodes.BRING_TO_FRONT)

            val bytes = processName.toByteArray()

            sendData.putInt(bytes.size)
            sendData.put(bytes)
            sendData.putLong(handle)

            sendPacket(CLIENT_PORT.toInt())
        }
    }

    private fun addAction(action: Runnable) {
        synchronized(actions) {
            actions.add(action)
            (actions as Object).notify()
        }
    }

    private fun startSendThread() {
        Executors.newSingleThreadExecutor().execute {
            while (running) {
                synchronized(actions) {
                    while (initReceived && !actions.isEmpty()) {
                        actions.poll().run()
                    }

                    try {
                        (actions as Object).wait()
                    } catch (_: InterruptedException) {
                    }
                }
            }
        }
    }

    fun stop() {
        running = false

        if (socket != null) {
            socket!!.close()
            socket = null
        }

        synchronized(actions) {
            (actions as Object).notify()
        }
    }

    private fun handleRequest(requestCode: Byte, port: Int) {
        when (requestCode) {
            RequestCodes.INIT -> {
                initReceived = true

                synchronized(actions) {
                    (actions as Object).notify()
                }
            }

            RequestCodes.GET_PROCESS -> {
                if (onGetProcessInfoListener == null) {
                    return
                }

                receiveData.position(receiveData.position() + 4)

                val numProcesses = receiveData.getShort().toInt()
                val index = receiveData.getShort().toInt()
                val pid = receiveData.getInt()
                val memoryUsage = receiveData.getLong()
                val affinityMask = receiveData.getInt()
                val wow64Process = receiveData.get().toInt() == 1

                val bytes = ByteArray(32)

                receiveData[bytes]

                val name = fromANSIString(bytes)

                onGetProcessInfoListener!!.onGetProcessInfo(
                    index = index,
                    count = numProcesses,
                    processInfo = ProcessInfo(pid, name, memoryUsage, affinityMask, wow64Process),
                )
            }

            RequestCodes.GET_GAMEPAD -> {
                val isXInput = receiveData.get().toInt() == 1
                val notify = receiveData.get().toInt() == 1

                if (currentController == null || !currentController!!.isConnected) {
                    currentController = ExternalController.getController(0)
                }

                val enabled = currentController != null

                if (enabled && notify) {
                    if (!gamepadClients.contains(port)) {
                        gamepadClients.add(port)
                    }
                } else {
                    gamepadClients.remove(port)
                }

                addAction {
                    sendData.rewind()
                    sendData.put(RequestCodes.GET_GAMEPAD)

                    if (enabled) {
                        sendData.putInt(currentController!!.getDeviceId())
                        sendData.put(dInputMapperType)

                        val bytes = currentController!!.name!!.toByteArray()

                        sendData.putInt(bytes.size)
                        sendData.put(bytes)
                    } else {
                        sendData.putInt(0)
                    }

                    sendPacket(port)
                }
            }

            RequestCodes.GET_GAMEPAD_STATE -> {
                val gamepadId = receiveData.getInt()
                val enabled = currentController != null

                if (currentController != null && currentController!!.getDeviceId() != gamepadId) {
                    currentController = null
                }

                addAction {
                    sendData.rewind()
                    sendData.put(RequestCodes.GET_GAMEPAD_STATE)
                    sendData.put((if (enabled) 1 else 0).toByte())

                    if (enabled) {
                        sendData.putInt(gamepadId)
                        currentController!!.state.writeTo(sendData)
                    }

                    sendPacket(port)
                }
            }

            RequestCodes.RELEASE_GAMEPAD -> {
                currentController = null
                gamepadClients.clear()
            }

            RequestCodes.CURSOR_POS_FEEDBACK -> {
                val x = receiveData.getShort()
                val y = receiveData.getShort()

                xServer.pointer.setX(x.toInt())
                xServer.pointer.setY(y.toInt())

                xServerView.requestRender()
            }
        }
    }

    // TODO: Coroutine?
    fun start() {
        try {
            localhost = InetAddress.getLocalHost()
        } catch (e: UnknownHostException) {
            try {
                localhost = InetAddress.getByName("127.0.0.1")
            } catch (ex: UnknownHostException) {
                Timber.w(ex)
            }
        }

        running = true
        startSendThread()

        Executors.newSingleThreadExecutor().execute {
            try {
                socket = DatagramSocket(null)
                socket!!.reuseAddress = true
                socket!!.bind(InetSocketAddress(null as InetAddress?, SERVER_PORT.toInt()))

                while (running) {
                    socket!!.receive(receivePacket)

                    synchronized(actions) {
                        receiveData.rewind()

                        val requestCode = receiveData.get()

                        handleRequest(requestCode, receivePacket.port)
                    }
                }
            } catch (_: IOException) {
            }
        }
    }

    fun sendGamepadState() {
        if (!initReceived || gamepadClients.isEmpty()) {
            return
        }

        val enabled = currentController != null

        for (port in gamepadClients) {
            addAction {
                sendData.rewind()
                sendData.put(RequestCodes.GET_GAMEPAD_STATE)
                sendData.put((if (enabled) 1 else 0).toByte())

                if (enabled) {
                    sendData.putInt(currentController!!.getDeviceId())
                    currentController!!.state.writeTo(sendData)
                }

                sendPacket(port)
            }
        }
    }

    fun onGenericMotionEvent(event: MotionEvent): Boolean {
        var handled = false

        if (currentController != null && currentController!!.getDeviceId() == event.deviceId) {
            handled = currentController!!.updateStateFromMotionEvent(event)

            if (handled) {
                sendGamepadState()
            }
        }

        return handled
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        var handled = false
        if (currentController != null && currentController!!.getDeviceId() == event.deviceId && event.repeatCount == 0) {
            val action = event.action

            if (action == KeyEvent.ACTION_DOWN) {
                handled = currentController!!.updateStateFromKeyEvent(event)
            } else if (action == KeyEvent.ACTION_UP) {
                handled = currentController!!.updateStateFromKeyEvent(event)
            }

            if (handled) {
                sendGamepadState()
            }
        }

        return handled
    }
}
