package com.winlator.widget

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import com.winlator.renderer.GLRenderer
import com.winlator.xserver.XServer

@SuppressLint("ViewConstructor")
class XServerView(context: Context, val xServer: XServer) : GLSurfaceView(context) {

    val renderer: GLRenderer = GLRenderer(this, xServer)

    init {
        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        preserveEGLContextOnPause = true
        setEGLConfigChooser(8, 8, 8, 8, 0, 0)
        setEGLContextClientVersion(3)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }
}
