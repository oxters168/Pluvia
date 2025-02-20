package com.winlator.core

import java.io.File

fun interface OnExtractFileListener {
    fun onExtractFile(destination: File?, size: Long): File?
}
