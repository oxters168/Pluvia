package com.micewine.emu.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import com.micewine.emu.activities.MainActivity.Companion.enableCpuCounter
import com.micewine.emu.activities.MainActivity.Companion.enableDebugInfo
import com.micewine.emu.activities.MainActivity.Companion.enableRamCounter
import com.micewine.emu.activities.MainActivity.Companion.memoryStats
import com.micewine.emu.activities.MainActivity.Companion.miceWineVersion
import com.micewine.emu.activities.MainActivity.Companion.selectedD3DXRenderer
import com.micewine.emu.activities.MainActivity.Companion.selectedDXVK
import com.micewine.emu.activities.MainActivity.Companion.selectedVKD3D
import com.micewine.emu.activities.MainActivity.Companion.selectedWineD3D
import com.micewine.emu.activities.MainActivity.Companion.totalCpuUsage
import com.micewine.emu.activities.MainActivity.Companion.vulkanDriverDeviceName
import com.micewine.emu.activities.MainActivity.Companion.vulkanDriverDriverVersion
import com.micewine.emu.core.RatPackageManager.getPackageNameVersionById
import com.micewine.emu.core.RatPackageManager.listRatPackages

class OnScreenInfoView @JvmOverloads constructor (context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0): View(context, attrs, defStyleAttr) {
    private val paint: Paint = Paint().apply {
        textSize = 30F
        strokeWidth = 8F
    }

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            invalidate()
            handler.postDelayed(this, 800)
        }
    }

    private val textOffset = 40F
    private var textCount = 0
    private var vkd3dVersion: String? = getPackageNameVersionById(selectedVKD3D)
    private var dxvkVersion: String? = getPackageNameVersionById(selectedDXVK)
    private var wineD3DVersion: String? = getPackageNameVersionById(selectedWineD3D)

    init {
        handler.post(updateRunnable)

        if (vkd3dVersion == null) {
            vkd3dVersion = listRatPackages("VKD3D-").map { it.name + " " + it.version }.first()
        }
        if (dxvkVersion == null) {
            dxvkVersion = listRatPackages("DXVK-").map { it.name + " " + it.version }.first()
        }
        if (wineD3DVersion == null) {
            wineD3DVersion = listRatPackages("WineD3D-").map { it.name + " " + it.version }.first()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        textCount = 0

        if (enableRamCounter) {
            drawText("RAM: $memoryStats", 20F, canvas)
        }
        if (enableCpuCounter) {
            drawText("CPU: $totalCpuUsage", 20F, canvas)
        }
        if (enableDebugInfo) {
            textCount = 0
            onScreenInfo(canvas)
        }
    }

    private fun onScreenInfo(c: Canvas) {
        drawText(miceWineVersion, getTextEndX(c, miceWineVersion), c)
        drawText(vkd3dVersion!!, getTextEndX(c, vkd3dVersion!!), c)

        if (selectedD3DXRenderer == "DXVK") {
            drawText(dxvkVersion!!, getTextEndX(c, dxvkVersion!!), c)
        } else if (selectedD3DXRenderer == "WineD3D") {
            drawText(wineD3DVersion!!, getTextEndX(c, wineD3DVersion!!), c)
        }

        drawText(vulkanDriverDeviceName!!, getTextEndX(c, vulkanDriverDeviceName!!), c)
        drawText(vulkanDriverDriverVersion!!, getTextEndX(c, vulkanDriverDriverVersion!!), c)
    }

    private fun drawText(text: String, x: Float, c: Canvas) {
        textCount++

        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        c.drawText(text, x, textCount * textOffset, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        c.drawText(text, x, textCount * textOffset, paint)
    }

    private fun getTextEndX(canvas: Canvas, string: String): Float {
        return canvas.width - paint.measureText(string) - 20F
    }
}
