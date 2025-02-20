package com.winlator.renderer

import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.winlator.R
import com.winlator.math.Mathf
import com.winlator.math.XForm.identity
import com.winlator.math.XForm.instance
import com.winlator.math.XForm.makeTransform
import com.winlator.math.XForm.multiply
import com.winlator.math.XForm.set
import com.winlator.renderer.GPUImage.Companion.checkIsSupported
import com.winlator.renderer.material.CursorMaterial
import com.winlator.renderer.material.ShaderMaterial
import com.winlator.renderer.material.WindowMaterial
import com.winlator.widget.XServerView
import com.winlator.xserver.Bitmask
import com.winlator.xserver.Drawable
import com.winlator.xserver.Pointer.OnPointerMotionListener
import com.winlator.xserver.Window
import com.winlator.xserver.WindowAttributes
import com.winlator.xserver.WindowManager.OnWindowModificationListener
import com.winlator.xserver.XServer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.min

class GLRenderer(val xServerView: XServerView, private val xServer: XServer) :
    GLSurfaceView.Renderer, OnWindowModificationListener, OnPointerMotionListener {
    private val quadVertices = VertexAttribute("position", 2)
    private val tmpXForm1 = instance
    private val tmpXForm2 = instance
    private val cursorMaterial = CursorMaterial()
    private val windowMaterial = WindowMaterial()
    val viewTransformation: ViewTransformation = ViewTransformation()
    private val rootCursorDrawable: Drawable
    private val renderableWindows = ArrayList<RenderableWindow>()
    var forceFullscreenWMClass: String? = null
    var isFullscreen: Boolean = false
        private set
    private var toggleFullscreen = false
    private var viewportNeedsUpdate = true
    var cursorVisible = true
        private set
    var screenOffsetYRelativeToCursor = false
        private set
    var unviewableWMClasses: Array<out String>? = null
        private set
    var magnifierZoom = 1.0f
        private set
    private val magnifierEnabled = true
    private var surfaceWidth = 0
    private var surfaceHeight = 0

    init {
        rootCursorDrawable = createRootCursorDrawable()

        quadVertices.put(
            floatArrayOf(
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                1.0f, 1.0f,
            ),
        )

        xServer.windowManager.addOnWindowModificationListener(this)
        xServer.pointer.addOnPointerMotionListener(this)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        checkIsSupported()

        GLES20.glFrontFace(GLES20.GL_CCW)
        GLES20.glDisable(GLES20.GL_CULL_FACE)

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        viewTransformation.update(
            width,
            height,
            xServer.screenInfo.width.toInt(),
            xServer.screenInfo.height.toInt(),
        )
    }

    override fun onDrawFrame(gl: GL10?) {
        if (toggleFullscreen) {
            this.isFullscreen = !this.isFullscreen
            toggleFullscreen = false
            viewportNeedsUpdate = true
        }

        drawFrame()
    }

    private fun drawFrame() {
        val xrFrame = false

        if (viewportNeedsUpdate && magnifierEnabled) {
            if (this.isFullscreen) {
                GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
            } else {
                GLES20.glViewport(
                    viewTransformation.viewOffsetX,
                    viewTransformation.viewOffsetY,
                    viewTransformation.viewWidth,
                    viewTransformation.viewHeight,
                )
            }
            viewportNeedsUpdate = false
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        if (magnifierEnabled) {
            var pointerX = 0f
            var pointerY = 0f
            val magnifierZoom = if (!screenOffsetYRelativeToCursor) this.magnifierZoom else 1.0f

            if (magnifierZoom != 1.0f) {
                pointerX = Mathf.clamp(
                    xServer.pointer.x * magnifierZoom - xServer.screenInfo.width * 0.5f,
                    0f,
                    (xServer.screenInfo.width * abs((1.0f - magnifierZoom).toDouble())).toFloat(),
                )
            }

            if (screenOffsetYRelativeToCursor || magnifierZoom != 1.0f) {
                val scaleY = if (magnifierZoom != 1.0f) abs((1.0f - magnifierZoom).toDouble()).toFloat() else 0.5f
                val offsetY = xServer.screenInfo.height * (if (screenOffsetYRelativeToCursor) 0.25f else 0.5f)
                pointerY = Mathf.clamp(
                    xServer.pointer.y * magnifierZoom - offsetY,
                    0f,
                    xServer.screenInfo.height * scaleY,
                )
            }

            makeTransform(tmpXForm2, -pointerX, -pointerY, magnifierZoom, magnifierZoom, 0f)
        } else {
            if (!this.isFullscreen) {
                var pointerY = 0
                if (screenOffsetYRelativeToCursor) {
                    val halfScreenHeight = (xServer.screenInfo.height / 2).toShort()
                    pointerY = Mathf.clamp(xServer.pointer.y - halfScreenHeight / 2, 0, halfScreenHeight.toInt())
                }

                makeTransform(
                    tmpXForm2,
                    viewTransformation.sceneOffsetX,
                    viewTransformation.sceneOffsetY - pointerY,
                    viewTransformation.sceneScaleX,
                    viewTransformation.sceneScaleY,
                    0f,
                )

                GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
                GLES20.glScissor(
                    viewTransformation.viewOffsetX,
                    viewTransformation.viewOffsetY,
                    viewTransformation.viewWidth,
                    viewTransformation.viewHeight,
                )
            } else {
                identity(tmpXForm2)
            }
        }

        renderWindows()
        if (cursorVisible) renderCursor()

        if (!magnifierEnabled && !this.isFullscreen) GLES20.glDisable(GLES20.GL_SCISSOR_TEST)

        if (xrFrame) {
            xServerView.requestRender()
        }
    }

    override fun onMapWindow(window: Window) {
        xServerView.queueEvent(Runnable { this.updateScene() })
        xServerView.requestRender()
    }

    override fun onUnmapWindow(window: Window) {
        xServerView.queueEvent(Runnable { this.updateScene() })
        xServerView.requestRender()
    }

    override fun onChangeWindowZOrder(window: Window) {
        xServerView.queueEvent(Runnable { this.updateScene() })
        xServerView.requestRender()
    }

    override fun onUpdateWindowContent(window: Window) {
        xServerView.requestRender()
    }

    override fun onUpdateWindowGeometry(window: Window?, resized: Boolean) {
        if (resized) {
            xServerView.queueEvent(Runnable { this.updateScene() })
        } else {
            xServerView.queueEvent(Runnable { updateWindowPosition(window) })
        }
        xServerView.requestRender()
    }

    override fun onUpdateWindowAttributes(window: Window, mask: Bitmask) {
        if (mask.isSet(WindowAttributes.FLAG_CURSOR)) xServerView.requestRender()
    }

    override fun onPointerMove(x: Short, y: Short) {
        xServerView.requestRender()
    }

    private fun renderDrawable(
        drawable: Drawable,
        x: Int,
        y: Int,
        material: ShaderMaterial,
        forceFullscreen: Boolean = false,
    ) {
        synchronized(drawable.renderLock) {
            val texture = drawable.texture
            texture!!.updateFromDrawable(drawable)

            if (forceFullscreen) {
                val newHeight = min(
                    xServer.screenInfo.height.toDouble(),
                    ((xServer.screenInfo.width.toFloat() / drawable.width) * drawable.height).toDouble(),
                ).toInt().toShort()
                val newWidth =
                    ((newHeight.toFloat() / drawable.height) * drawable.width).toInt().toShort()
                set(
                    tmpXForm1,
                    (xServer.screenInfo.width - newWidth) * 0.5f,
                    (xServer.screenInfo.height - newHeight) * 0.5f,
                    newWidth.toFloat(),
                    newHeight.toFloat(),
                )
            } else {
                set(
                    tmpXForm1,
                    x.toFloat(),
                    y.toFloat(),
                    drawable.width.toFloat(),
                    drawable.height.toFloat(),
                )
            }

            multiply(tmpXForm1, tmpXForm1, tmpXForm2)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture.textureId)
            GLES20.glUniform1i(material.getUniformLocation("texture"), 0)
            GLES20.glUniform1fv(material.getUniformLocation("xform"), tmpXForm1.size, tmpXForm1, 0)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, quadVertices.count())
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }
    }

    private fun renderWindows() {
        windowMaterial.use()
        GLES20.glUniform2f(
            windowMaterial.getUniformLocation("viewSize"),
            xServer.screenInfo.width.toFloat(),
            xServer.screenInfo.height.toFloat(),
        )
        quadVertices.bind(windowMaterial.programId)

        xServer.lock(XServer.Lockable.DRAWABLE_MANAGER).use { lock ->
            for (window in renderableWindows) {
                renderDrawable(
                    window.content,
                    window.rootX.toInt(),
                    window.rootY.toInt(),
                    windowMaterial,
                    window.forceFullscreen,
                )
            }
        }
        quadVertices.disable()
    }

    private fun renderCursor() {
        cursorMaterial.use()
        GLES20.glUniform2f(
            cursorMaterial.getUniformLocation("viewSize"),
            xServer.screenInfo.width.toFloat(),
            xServer.screenInfo.height.toFloat(),
        )
        quadVertices.bind(cursorMaterial.programId)

        xServer.lock(XServer.Lockable.DRAWABLE_MANAGER).use { lock ->
            val pointWindow = xServer.inputDeviceManager.pointWindow

            val cursor = pointWindow?.attributes?.getCursor()

            val x = xServer.pointer.clampedX
            val y = xServer.pointer.clampedY

            if (cursor != null) {
                if (cursor.isVisible) {
                    renderDrawable(
                        drawable = cursor.cursorImage!!,
                        x = x - cursor.hotSpotX,
                        y = y - cursor.hotSpotY,
                        material = cursorMaterial,
                    )
                }
            } else {
                renderDrawable(rootCursorDrawable, x.toInt(), y.toInt(), cursorMaterial)
            }
        }

        quadVertices.disable()
    }

    fun toggleFullscreen() {
        toggleFullscreen = true
        xServerView.requestRender()
    }

    private fun createRootCursorDrawable(): Drawable {
        val context = xServerView.context

        val options = BitmapFactory.Options()
        options.inScaled = false

        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.cursor, options)

        return Drawable.fromBitmap(bitmap!!)
    }

    private fun updateScene() {
        xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.DRAWABLE_MANAGER).use {
            renderableWindows.clear()
            collectRenderableWindows(
                window = xServer.windowManager.rootWindow,
                x = xServer.windowManager.rootWindow.x.toInt(),
                y = xServer.windowManager.rootWindow.y.toInt(),
            )
        }
    }

    private fun collectRenderableWindows(window: Window, x: Int, y: Int) {
        if (!window.attributes.isMapped) {
            return
        }

        if (window != xServer.windowManager.rootWindow) {
            var viewable = true

            if (unviewableWMClasses != null) {
                val wmClass = window.className
                for (unviewableWMClass in unviewableWMClasses) {
                    if (wmClass.contains(unviewableWMClass)) {
                        if (window.attributes.isEnabled) {
                            window.disableAllDescendants()
                        }

                        viewable = false
                        break
                    }
                }
            }

            if (viewable) {
                if (forceFullscreenWMClass != null) {
                    val width = window.width
                    val height = window.height
                    var forceFullscreen = false

                    if (width >= 320 && height >= 200 && width < xServer.screenInfo.width && height < xServer.screenInfo.height) {
                        val parent = window.parent
                        val parentHasWMClass = parent!!.className.contains(forceFullscreenWMClass!!)
                        val hasWMClass = window.className.contains(forceFullscreenWMClass!!)

                        if (hasWMClass) {
                            forceFullscreen = !parentHasWMClass && window.childCount == 0
                        } else {
                            val borderX = (parent.width - width).toShort()
                            val borderY = (parent.height - height).toShort()

                            if (parent.childCount == 1 && borderX > 0 && borderY > 0 && borderX <= 12) {
                                forceFullscreen = true
                                removeRenderableWindow(parent)
                            }
                        }
                    }

                    renderableWindows.add(RenderableWindow(window.content!!, x, y, forceFullscreen))
                } else {
                    renderableWindows.add(RenderableWindow(window.content!!, x, y))
                }
            }
        }

        window.getChildren().forEach { child ->
            collectRenderableWindows(child!!, child.x + x, child.y + y)
        }
    }

    private fun removeRenderableWindow(window: Window) {
        renderableWindows.indices.forEach {
            if (renderableWindows[it].content == window.content) {
                renderableWindows.removeAt(it)
                return
            }
        }
    }

    private fun updateWindowPosition(window: Window?) {
        renderableWindows.forEach { renderableWindow ->
            if (renderableWindow.content == window!!.content) {
                renderableWindow.rootX = window.rootX
                renderableWindow.rootY = window.rootY
                return
            }
        }
    }

    fun setCursorVisible(cursorVisible: Boolean) {
        this.cursorVisible = cursorVisible
        xServerView.requestRender()
    }

    fun setScreenOffsetYRelativeToCursor(screenOffsetYRelativeToCursor: Boolean) {
        this.screenOffsetYRelativeToCursor = screenOffsetYRelativeToCursor
        xServerView.requestRender()
    }

    fun setUnviewableWMClasses(vararg unviewableWMNames: String) {
        this.unviewableWMClasses = unviewableWMNames
    }

    fun setMagnifierZoom(magnifierZoom: Float) {
        this.magnifierZoom = magnifierZoom
        xServerView.requestRender()
    }
}
