package com.winlator.xserver

object IDGenerator {
    private var id = 0

    fun generate(): Int = ++id
}
