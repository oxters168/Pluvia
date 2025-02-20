package com.winlator.xserver

import android.util.SparseArray
import com.winlator.sysvshm.SysVSharedMemory
import java.nio.ByteBuffer

class SHMSegmentManager(private val sysVSharedMemory: SysVSharedMemory) {

    private val shmSegments = SparseArray<ByteBuffer?>()

    fun attach(xid: Int, shmid: Int) {
        if (shmSegments.indexOfKey(xid) >= 0) {
            detach(xid)
        }

        sysVSharedMemory.attach(shmid)?.let { data ->
            shmSegments.put(xid, data)
        }
    }

    fun detach(xid: Int) {
        shmSegments.get(xid)?.let { data ->
            sysVSharedMemory.detach(data)
            shmSegments.remove(xid)
        }
    }

    fun getData(xid: Int): ByteBuffer? = shmSegments.get(xid)
}
