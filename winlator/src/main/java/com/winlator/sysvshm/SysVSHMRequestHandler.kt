package com.winlator.sysvshm

import com.winlator.xconnector.Client
import com.winlator.xconnector.RequestHandler
import java.io.IOException

class SysVSHMRequestHandler : RequestHandler {
    @Throws(IOException::class)
    override fun handleRequest(client: Client?): Boolean {
        val sysVSharedMemory = client!!.tag as SysVSharedMemory
        val inputStream = client.inputStream!!
        val outputStream = client.outputStream!!

        if (inputStream.available() < 5) {
            return false
        }

        val requestCode = inputStream.readByte()

        when (requestCode) {
            RequestCodes.SHMGET.code -> {
                val size = inputStream.readUnsignedInt()
                val shmid = sysVSharedMemory[size]
                outputStream.lock().use {
                    outputStream.writeInt(shmid)
                }
            }

            RequestCodes.GET_FD.code -> {
                val shmid = inputStream.readInt()
                outputStream.lock().use {
                    outputStream.writeByte(0.toByte())
                    outputStream.setAncillaryFd(sysVSharedMemory.getFd(shmid))
                }
            }

            RequestCodes.DELETE.code -> {
                val shmid = inputStream.readInt()
                sysVSharedMemory.delete(shmid)
            }
        }

        return true
    }
}
