package com.winlator.xserver

class Bitmask : Iterable<Int?> {

    var bits: Int = 0
        private set

    val isEmpty: Boolean
        get() = bits == 0

    constructor()

    constructor(bits: Int) {
        this.bits = bits
    }

    fun isSet(flag: Int): Boolean = (flag and this.bits) != 0

    fun intersects(mask: Bitmask): Boolean = (mask.bits and this.bits) != 0

    fun set(flag: Int) {
        bits = bits or flag
    }

    fun set(flag: Int, value: Boolean) {
        if (value) {
            set(flag)
        } else {
            unset(flag)
        }
    }

    fun unset(flag: Int) {
        bits = bits and flag.inv()
    }

    fun join(mask: Bitmask) {
        this.bits = mask.bits or this.bits
    }

    override fun iterator(): Iterator<Int> {
        val bits = intArrayOf(this.bits)
        return object : Iterator<Int> {
            override fun hasNext(): Boolean = bits[0] != 0

            override fun next(): Int {
                val index = Integer.lowestOneBit(bits[0])
                bits[0] = bits[0] and index.inv()
                return index
            }
        }
    }

    override fun hashCode(): Int = bits
}
