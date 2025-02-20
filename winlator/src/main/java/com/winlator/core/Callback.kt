package com.winlator.core

fun interface Callback<T> {
    fun call(`object`: T)
}
