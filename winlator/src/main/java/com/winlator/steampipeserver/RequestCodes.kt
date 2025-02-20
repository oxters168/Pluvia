package com.winlator.steampipeserver

object RequestCodes {
    const val MSG_INIT: Int = 1
    const val MSG_SHUTDOWN: Int = 2
    const val MSG_RESTART_APP: Int = 3
    const val MSG_IS_RUNNING: Int = 4
    const val MSG_REGISTER_CALLBACK: Int = 5
    const val MSG_UNREGISTER_CALLBACK: Int = 6
    const val MSG_RUN_CALLBACKS: Int = 7
}
