package com.winlator.core

import com.winlator.math.Mathf
import com.winlator.xserver.XServer
import java.util.Timer
import java.util.TimerTask
import kotlin.math.ceil
import kotlin.math.floor
import timber.log.Timber

class CursorLocker(private val xServer: XServer) : TimerTask() {

    var damping: Float = 0.25f

    var maxDistance: Short = (xServer.screenInfo.width * 0.05f).toInt().toShort()

    private var enabled = true

    private val pauseLock = Any()

    init {
        Timer().also {
            it.schedule(this, 0, (1000 / 60).toLong())
        }
    }

    fun setEnabled(enabled: Boolean) {
        if (enabled) {
            synchronized(pauseLock) {
                this.enabled = true
                (pauseLock as Object).notifyAll()
            }
        } else {
            this.enabled = enabled
        }
    }

    override fun run() {
        synchronized(pauseLock) {
            if (!enabled) {
                try {
                    (pauseLock as Object).wait()
                } catch (e: InterruptedException) {
                    Timber.w(e)
                }
            }
        }

        val x = Mathf.clamp(xServer.pointer.x.toInt(), -maxDistance, xServer.screenInfo.width + maxDistance).toShort()

        val y = Mathf.clamp(xServer.pointer.y.toInt(), -maxDistance, xServer.screenInfo.height + maxDistance).toShort()

        if (x < 0) {
            xServer.pointer.setX(ceil((x * damping).toDouble()).toInt().toShort().toInt())
        } else if (x >= xServer.screenInfo.width) {
            xServer.pointer.setX(
                floor((xServer.screenInfo.width + (x - xServer.screenInfo.width) * damping).toDouble()).toInt().toShort().toInt(),
            )
        }

        if (y < 0) {
            xServer.pointer.setY(ceil((y * damping).toDouble()).toInt().toShort().toInt())
        } else if (y >= xServer.screenInfo.height) {
            xServer.pointer.setY(
                floor((xServer.screenInfo.height + (y - xServer.screenInfo.height) * damping).toDouble()).toInt().toShort().toInt(),
            )
        }
    }
}
