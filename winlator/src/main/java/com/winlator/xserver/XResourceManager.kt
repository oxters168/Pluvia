package com.winlator.xserver

abstract class XResourceManager {

    private val onResourceLifecycleListeners = ArrayList<OnResourceLifecycleListener>()

    interface OnResourceLifecycleListener {
        fun onCreateResource(resource: XResource?) {}

        fun onFreeResource(resource: XResource?) {}
    }

    fun addOnResourceLifecycleListener(onResourceLifecycleListener: OnResourceLifecycleListener) {
        onResourceLifecycleListeners.add(onResourceLifecycleListener)
    }

    fun removeOnResourceLifecycleListener(onResourceLifecycleListener: OnResourceLifecycleListener) {
        onResourceLifecycleListeners.remove(onResourceLifecycleListener)
    }

    fun triggerOnCreateResourceListener(resource: XResource?) {
        onResourceLifecycleListeners.indices.reversed().forEach { onResourceLifecycleListeners[it].onCreateResource(resource) }
    }

    fun triggerOnFreeResourceListener(resource: XResource?) {
        onResourceLifecycleListeners.indices.reversed().forEach { onResourceLifecycleListeners[it].onFreeResource(resource) }
    }
}
