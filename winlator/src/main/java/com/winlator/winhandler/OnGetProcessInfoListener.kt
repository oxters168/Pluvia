package com.winlator.winhandler

fun interface OnGetProcessInfoListener {
    fun onGetProcessInfo(index: Int, count: Int, processInfo: ProcessInfo?)
}
