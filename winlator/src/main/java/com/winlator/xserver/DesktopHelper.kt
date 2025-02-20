package com.winlator.xserver

import androidx.collection.arrayMapOf
import com.winlator.xserver.Atom.getId
import com.winlator.xserver.Pointer.OnPointerMotionListener
import com.winlator.xserver.WindowManager.OnWindowModificationListener

object DesktopHelper {

    fun attachTo(xServer: XServer) {
        setupXResources(xServer)

        xServer.pointer.addOnPointerMotionListener(
            object : OnPointerMotionListener {
                override fun onPointerButtonPress(button: Pointer.Button) {
                    updateFocusedWindow(xServer)
                }
            },
        )

        xServer.windowManager.addOnWindowModificationListener(
            object : OnWindowModificationListener {
                override fun onMapWindow(window: Window) {
                    setFocusedWindow(xServer, window)
                }
            },
        )
    }

    private fun updateFocusedWindow(xServer: XServer) {
        xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE).use {
            val focusedWindow = xServer.windowManager.focusedWindow
            val child = xServer.windowManager.findPointWindow(xServer.pointer.clampedX, xServer.pointer.clampedY)
            if (child == null && focusedWindow != xServer.windowManager.rootWindow) {
                xServer.windowManager.setFocus(xServer.windowManager.rootWindow, WindowManager.FocusRevertTo.NONE)
            } else if (child != null && child != focusedWindow) {
                setFocusedWindow(xServer, child)
            }
        }
    }

    private fun setFocusedWindow(xServer: XServer, window: Window) {
        if (window.isApplicationWindow) {
            val parentIsRoot = window.parent == xServer.windowManager.rootWindow
            xServer.windowManager.setFocus(
                window,
                if (parentIsRoot) WindowManager.FocusRevertTo.POINTER_ROOT else WindowManager.FocusRevertTo.PARENT,
            )
            xServer.winHandler!!.bringToFront(window.className, window.handle)
        }
    }

    private fun setupXResources(xServer: XServer) {
        val atom = getId("RESOURCE_MANAGER")
        val type = getId("STRING")

        val values = arrayMapOf(
            "size" to "20",
            "theme" to "dmz",
            "theme_core" to "true",
        )

        val sb = StringBuilder()
        for (entry in values.entries) {
            sb.append("Xcursor")
                .append('.')
                .append(entry.key)
                .append(':')
                .append('\t')
                .append(entry.value)
                .append('\n')
        }

        val data = sb.toString().toByteArray(XServer.LATIN1_CHARSET)
        xServer.windowManager.rootWindow.modifyProperty(atom, type, Property.Format.BYTE_ARRAY, Property.Mode.APPEND, data)
    }
}
