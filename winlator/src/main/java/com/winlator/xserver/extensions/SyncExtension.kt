package com.winlator.xserver.extensions

import android.util.SparseBooleanArray
import com.winlator.xconnector.XInputStream
import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.XClient
import com.winlator.xserver.errors.BadFence
import com.winlator.xserver.errors.BadIdChoice
import com.winlator.xserver.errors.BadImplementation
import com.winlator.xserver.errors.BadMatch
import com.winlator.xserver.errors.XRequestError
import java.io.IOException

class SyncExtension : Extension {

    companion object {
        const val MAJOR_OPCODE: Byte = -104
    }

    private val fences = SparseBooleanArray()

    private object ClientOpcodes {
        const val CREATE_FENCE: Byte = 14
        const val TRIGGER_FENCE: Byte = 15
        const val RESET_FENCE: Byte = 16
        const val DESTROY_FENCE: Byte = 17
        const val AWAIT_FENCE: Byte = 19
    }

    override val name: String
        get() = "SYNC"

    override val majorOpcode: Byte
        get() = MAJOR_OPCODE

    override val firstErrorId: Byte
        get() = Byte.MIN_VALUE

    override val firstEventId: Byte
        get() = 0

    fun setTriggered(id: Int) {
        synchronized(fences) {
            if (fences.indexOfKey(id) >= 0) {
                fences.put(id, true)
            }
        }
    }

    @Throws(IOException::class, XRequestError::class)
    private fun createFence(
        client: XClient,
        inputStream: XInputStream,
        outputStream: XOutputStream,
    ) {
        synchronized(fences) {
            inputStream.skip(4)
            val id = inputStream.readInt()

            if (fences.indexOfKey(id) >= 0) {
                throw BadIdChoice(id)
            }

            val initiallyTriggered = inputStream.readByte().toInt() == 1

            inputStream.skip(3)

            fences.put(id, initiallyTriggered)
        }
    }

    @Throws(IOException::class, XRequestError::class)
    private fun triggerFence(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        synchronized(fences) {
            val id = inputStream.readInt()

            if (fences.indexOfKey(id) < 0) {
                throw BadFence(id)
            }

            fences.put(id, true)
        }
    }

    @Throws(IOException::class, XRequestError::class)
    private fun resetFence(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        synchronized(fences) {
            val id = inputStream.readInt()

            if (fences.indexOfKey(id) < 0) {
                throw BadFence(id)
            }

            val triggered = fences[id]

            if (!triggered) {
                throw BadMatch()
            }

            fences.put(id, false)
        }
    }

    @Throws(IOException::class, XRequestError::class)
    private fun destroyFence(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        synchronized(fences) {
            val id = inputStream.readInt()

            if (fences.indexOfKey(id) < 0) {
                throw BadFence(id)
            }

            fences.delete(id)
        }
    }

    @Throws(IOException::class, XRequestError::class)
    private fun awaitFence(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        synchronized(fences) {
            var length = client.remainingRequestLength
            val ids = IntArray(length / 4)
            var i = 0

            while (length != 0) {
                ids[i++] = inputStream.readInt()
                length -= 4
            }

            var anyTriggered = false
            do {
                for (id in ids) {
                    if (fences.indexOfKey(id) < 0) {
                        throw BadFence(id)
                    }

                    anyTriggered = fences[id]

                    if (anyTriggered) {
                        break
                    }
                }
            } while (!anyTriggered)
        }
    }

    @Throws(IOException::class, XRequestError::class)
    override fun handleRequest(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        val opcode = client.requestData.toInt()
        when (opcode) {
            ClientOpcodes.CREATE_FENCE.toInt() -> createFence(client, inputStream, outputStream)
            ClientOpcodes.TRIGGER_FENCE.toInt() -> triggerFence(client, inputStream, outputStream)
            ClientOpcodes.RESET_FENCE.toInt() -> resetFence(client, inputStream, outputStream)
            ClientOpcodes.DESTROY_FENCE.toInt() -> destroyFence(client, inputStream, outputStream)
            ClientOpcodes.AWAIT_FENCE.toInt() -> awaitFence(client, inputStream, outputStream)
            else -> throw BadImplementation()
        }
    }
}
