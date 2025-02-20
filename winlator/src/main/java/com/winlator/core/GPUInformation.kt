package com.winlator.core

import android.content.Context
import android.opengl.EGL14
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import java.util.Objects
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.opengles.GL10

object GPUInformation {
    private fun loadGPUInformation(context: Context): ArrayMap<String, String> {
        val thread = Thread.currentThread()

        val gpuInfo = arrayMapOf(
            "renderer" to "",
            "vendor" to "",
            "version" to "",
        )

        (
            Thread {
                var attribList = intArrayOf(
                    EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
                    EGL10.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL10.EGL_RED_SIZE, 8,
                    EGL10.EGL_GREEN_SIZE, 8,
                    EGL10.EGL_BLUE_SIZE, 8,
                    EGL10.EGL_ALPHA_SIZE, 0,
                    EGL10.EGL_NONE,
                )
                val configs = arrayOfNulls<EGLConfig>(1)
                val configCounts = IntArray(1)

                val egl = EGLContext.getEGL() as EGL10
                val eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)

                val version = IntArray(2)
                egl.eglInitialize(eglDisplay, version)
                egl.eglChooseConfig(eglDisplay, attribList, configs, 1, configCounts)

                attribList = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE)
                val eglContext = egl.eglCreateContext(eglDisplay, configs[0], EGL10.EGL_NO_CONTEXT, attribList)

                egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, eglContext)

                val gl = eglContext.gl as GL10
                val gpuRenderer = Objects.toString(gl.glGetString(GL10.GL_RENDERER), "")
                val gpuVendor = Objects.toString(gl.glGetString(GL10.GL_VENDOR), "")
                val gpuVersion = Objects.toString(gl.glGetString(GL10.GL_VERSION), "")

                gpuInfo["renderer"] = gpuRenderer
                gpuInfo["vendor"] = gpuVendor
                gpuInfo["version"] = gpuVersion

                PreferenceManager.getDefaultSharedPreferences(context).edit {
                    putString("gpu_renderer", gpuRenderer)
                    putString("gpu_vendor", gpuVendor)
                    putString("gpu_version", gpuVersion)
                }
                synchronized(thread) {
                    (thread as Object).notify()
                }
            }
            ).start()

        synchronized(thread) {
            try {
                (thread as Object).wait()
            } catch (_: InterruptedException) {
            }
        }

        return gpuInfo
    }

    fun getRenderer(context: Context): String? {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val value = preferences.getString("gpu_renderer", "")!!

        if (value.isNotEmpty()) {
            return value
        }

        val gpuInfo = loadGPUInformation(context)

        return gpuInfo["renderer"]
    }

    fun getVendor(context: Context): String? {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val value = preferences.getString("gpu_vendor", "")!!

        if (value.isNotEmpty()) {
            return value
        }

        val gpuInfo = loadGPUInformation(context)

        return gpuInfo["vendor"]
    }

    fun getVersion(context: Context): String? {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val value = preferences.getString("gpu_version", "")!!

        if (value.isNotEmpty()) {
            return value
        }

        val gpuInfo = loadGPUInformation(context)

        return gpuInfo["version"]
    }

    fun isAdreno6xx(context: Context): Boolean {
        return getRenderer(context)!!.lowercase().matches(".*adreno[^6]+6[0-9]{2}.*".toRegex())
    }
}
