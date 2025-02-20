package com.winlator.xserver

import androidx.collection.ArraySet

class ResourceIDs(maxClients: Int) {

    private val idBases = ArraySet<Int?>()

    val idMask: Int

    init {
        var clientsBits = 32 - Integer.numberOfLeadingZeros(maxClients)
        clientsBits = if (Integer.bitCount(maxClients) == 1) clientsBits - 1 else clientsBits

        val base = 29 - clientsBits

        idMask = (1 shl base) - 1

        for (i in 1..<maxClients) {
            idBases.add(i shl base)
        }
    }

    @Synchronized
    fun get(): Int {
        if (idBases.isEmpty()) {
            return -1
        }

        val iter = idBases.iterator()
        val idBase: Int = iter.next()!!

        iter.remove()

        return idBase
    }

    fun isInInterval(value: Int, idBase: Int): Boolean = (value or idMask) == (idBase or idMask)

    @Synchronized
    fun free(idBase: Int?) {
        idBases.add(idBase)
    }
}
