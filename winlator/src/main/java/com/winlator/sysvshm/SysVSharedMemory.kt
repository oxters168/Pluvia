package com.winlator.sysvshm

import android.annotation.SuppressLint
import android.os.Build
import android.os.SharedMemory
import android.system.ErrnoException
import android.util.SparseArray
import com.winlator.xconnector.XConnectorEpoll
import java.lang.reflect.InvocationTargetException
import java.nio.ByteBuffer
import timber.log.Timber

class SysVSharedMemory {

    private val shmemories = SparseArray<SHMemory>()

    private var maxSHMemoryId = 0

    private class SHMemory {
        var fd: Int = 0
        var size: Long = 0
        var data: ByteBuffer? = null
    }

    fun getFd(shmid: Int): Int {
        synchronized(shmemories) {
            val shmemory = shmemories[shmid]
            return shmemory?.fd ?: -1
        }
    }

    operator fun get(size: Long): Int {
        synchronized(shmemories) {
            val index = shmemories.size()
            var fd = ashmemCreateRegion(index, size)

            if (fd < 0) {
                fd = createSharedMemory("sysvshm-$index", size.toInt())
            }

            if (fd < 0) {
                return -1
            }

            val shmemory = SHMemory()
            val id = ++maxSHMemoryId

            shmemory.fd = fd
            shmemory.size = size

            shmemories.put(id, shmemory)

            return id
        }
    }

    fun delete(shmid: Int) {
        val shmemory = shmemories[shmid]

        if (shmemory != null) {
            if (shmemory.fd != -1) {
                XConnectorEpoll.closeFd(shmemory.fd)
                shmemory.fd = -1
            }

            shmemories.remove(shmid)
        }
    }

    fun deleteAll() {
        synchronized(shmemories) {
            for (i in shmemories.size() - 1 downTo 0) {
                delete(shmemories.keyAt(i))
            }
        }
    }

    fun attach(shmid: Int): ByteBuffer? {
        synchronized(shmemories) {
            val shmemory = shmemories[shmid]
            if (shmemory != null) {
                if (shmemory.data == null) {
                    shmemory.data = mapSHMSegment(shmemory.fd, shmemory.size, 0, true)
                }

                return shmemory.data
            } else {
                return null
            }
        }
    }

    fun detach(data: ByteBuffer) {
        synchronized(shmemories) {
            for (i in 0..<shmemories.size()) {
                val shmemory = shmemories.valueAt(i)
                if (shmemory.data == data) {
                    unmapSHMSegment(shmemory.data!!, shmemory.size)
                    shmemory.data = null
                    break
                }
            }
        }
    }

    companion object {

        init {
            System.loadLibrary("winlator")
        }

        @SuppressLint("ObsoleteSdkInt")
        private fun createSharedMemory(name: String, size: Int): Int {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    val sharedMemory = SharedMemory.create(name, size)
                    try {
                        val method = sharedMemory.javaClass.getMethod("getFd")
                        val ret = method.invoke(sharedMemory)
                        if (ret != null) return ret as Int
                    } catch (e: NoSuchMethodException) {
                        Timber.w(e)
                    } catch (e: IllegalAccessException) {
                        Timber.w(e)
                    } catch (e: InvocationTargetException) {
                        Timber.w(e)
                    }
                }
            } catch (e: ErrnoException) {
                Timber.w(e)
            }

            return -1
        }

        /**
         * Native Methods
         */

        external fun createMemoryFd(name: String?, size: Int): Int

        external fun ashmemCreateRegion(index: Int, size: Long): Int

        external fun mapSHMSegment(fd: Int, size: Long, offset: Int, readonly: Boolean): ByteBuffer?

        external fun unmapSHMSegment(data: ByteBuffer, size: Long)
    }
}
