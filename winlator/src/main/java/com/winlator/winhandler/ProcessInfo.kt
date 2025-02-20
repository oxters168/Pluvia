package com.winlator.winhandler

import com.winlator.core.StringUtils.formatBytes

data class ProcessInfo(
    val pid: Int,
    val name: String,
    val memoryUsage: Long,
    val affinityMask: Int,
    val wow64Process: Boolean,
) {
    val formattedMemoryUsage: String
        get() = formatBytes(memoryUsage)

    val cPUList: String
        get() {
            val numProcessors = Runtime.getRuntime().availableProcessors()
            val cpuList = ArrayList<String>()
            for (i in 0..<numProcessors) {
                if ((affinityMask and (1 shl i)) != 0) {
                    cpuList.add(i.toString())
                }
            }

            return cpuList.joinToString(",")
        }
}
