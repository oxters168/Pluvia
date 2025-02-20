package com.winlator.xenvironment

abstract class EnvironmentComponent {
    var environment: XEnvironment? = null

    abstract fun start()

    abstract fun stop()
}
