package com.winlator.xenvironment

import android.content.Context
import com.winlator.core.FileUtils.chmod
import com.winlator.core.FileUtils.clear
import com.winlator.xenvironment.components.GuestProgramLauncherComponent
import java.io.File

class XEnvironment(val context: Context, val imageFs: ImageFs) : Iterable<EnvironmentComponent?> {

    private val components = ArrayList<EnvironmentComponent>()

    fun addComponent(environmentComponent: EnvironmentComponent) {
        environmentComponent.environment = this
        components.add(environmentComponent)
    }

    fun <T : EnvironmentComponent> getComponent(componentClass: Class<T>): T? {
        for (component in components) {
            if (component.javaClass == componentClass) {
                return component as T
            }
        }

        return null
    }

    override fun iterator(): MutableIterator<EnvironmentComponent> = components.iterator()

    val tmpDir: File
        get() {
            val tmpDir = File(context.filesDir, "tmp")

            if (!tmpDir.isDirectory) {
                tmpDir.mkdirs()
                chmod(tmpDir, 505)
            }

            return tmpDir
        }

    fun startEnvironmentComponents() {
        clear(tmpDir)

        for (environmentComponent in this) {
            environmentComponent.start()
        }
    }

    fun stopEnvironmentComponents() {
        for (environmentComponent in this) {
            environmentComponent.stop()
        }
    }

    fun onPause() {
        val guestProgramLauncherComponent = getComponent(GuestProgramLauncherComponent::class.java)
        guestProgramLauncherComponent?.suspendProcess()
    }

    fun onResume() {
        val guestProgramLauncherComponent = getComponent(GuestProgramLauncherComponent::class.java)
        guestProgramLauncherComponent?.resumeProcess()
    }
}
