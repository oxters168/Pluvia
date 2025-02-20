package com.winlator.core

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.DisplayMetrics
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.winlator.R
import com.winlator.core.MSBitmap.create
import com.winlator.xenvironment.ImageFs
import com.winlator.xserver.ScreenInfo
import java.io.File
import kotlin.math.ceil

object WineThemeManager {
    val DEFAULT_DESKTOP_THEME: String = Theme.LIGHT.toString() + "," + BackgroundType.IMAGE + ",#0277bd"

    fun apply(context: Context, themeInfo: ThemeInfo, screenInfo: ScreenInfo) {
        val rootDir = ImageFs.find(context).rootDir
        val userRegFile = File(rootDir, ImageFs.WINEPREFIX + "/user.reg")
        val background = Color.red(themeInfo.backgroundColor).toString() + " " +
            Color.green(themeInfo.backgroundColor) + " " + Color.blue(themeInfo.backgroundColor)

        if (themeInfo.backgroundType == BackgroundType.IMAGE) {
            createWallpaperBMPFile(context, screenInfo)
        }

        WineRegistryEditor(userRegFile).use { registryEditor ->
            if (themeInfo.backgroundType == BackgroundType.IMAGE) {
                registryEditor.setStringValue("Control Panel\\Desktop", "Wallpaper", ImageFs.CACHE_PATH + "/wallpaper.bmp")
            } else {
                registryEditor.removeValue("Control Panel\\Desktop", "Wallpaper")
            }

            if (themeInfo.theme == Theme.LIGHT) {
                registryEditor.setStringValue("Control Panel\\Colors", "ActiveBorder", "245 245 245")
                registryEditor.setStringValue("Control Panel\\Colors", "ActiveTitle", "96 125 139")
                registryEditor.setStringValue("Control Panel\\Colors", "Background", background)
                registryEditor.setStringValue("Control Panel\\Colors", "ButtonAlternateFace", "245 245 245")
                registryEditor.setStringValue("Control Panel\\Colors", "ButtonDkShadow", "158 158 158")
                registryEditor.setStringValue("Control Panel\\Colors", "ButtonFace", "245 245 245")
                registryEditor.setStringValue("Control Panel\\Colors", "ButtonHilight", "224 224 224")
                registryEditor.setStringValue("Control Panel\\Colors", "ButtonLight", "255 255 255")
                registryEditor.setStringValue("Control Panel\\Colors", "ButtonShadow", "158 158 158")
                registryEditor.setStringValue("Control Panel\\Colors", "ButtonText", "0 0 0")
                registryEditor.setStringValue("Control Panel\\Colors", "GradientActiveTitle", "96 125 139")
                registryEditor.setStringValue("Control Panel\\Colors", "GradientInactiveTitle", "117 117 117")
                registryEditor.setStringValue("Control Panel\\Colors", "GrayText", "158 158 158")
                registryEditor.setStringValue("Control Panel\\Colors", "Hilight", "2 136 209")
                registryEditor.setStringValue("Control Panel\\Colors", "HilightText", "255 255 255")
                registryEditor.setStringValue("Control Panel\\Colors", "HotTrackingColor", "2 136 209")
                registryEditor.setStringValue("Control Panel\\Colors", "InactiveBorder", "255 255 255")
                registryEditor.setStringValue("Control Panel\\Colors", "InactiveTitle", "117 117 117")
                registryEditor.setStringValue("Control Panel\\Colors", "InactiveTitleText", "200 200 200")
                registryEditor.setStringValue("Control Panel\\Colors", "InfoText", "0 0 0")
                registryEditor.setStringValue("Control Panel\\Colors", "InfoWindow", "255 255 255")
                registryEditor.setStringValue("Control Panel\\Colors", "Menu", "245 245 245")
                registryEditor.setStringValue("Control Panel\\Colors", "MenuBar", "245 245 245")
                registryEditor.setStringValue("Control Panel\\Colors", "MenuHilight", "2 136 209")
                registryEditor.setStringValue("Control Panel\\Colors", "MenuText", "0 0 0")
                registryEditor.setStringValue("Control Panel\\Colors", "Scrollbar", "245 245 245")
                registryEditor.setStringValue("Control Panel\\Colors", "TitleText", "255 255 255")
                registryEditor.setStringValue("Control Panel\\Colors", "Window", "245 245 245")
                registryEditor.setStringValue("Control Panel\\Colors", "WindowFrame", "158 158 158")
                registryEditor.setStringValue("Control Panel\\Colors", "WindowText", "0 0 0")
            } else if (themeInfo.theme == Theme.DARK) {
                registryEditor.setStringValue("Control Panel\\Colors", "ActiveBorder", "48 48 48")
                registryEditor.setStringValue("Control Panel\\Colors", "ActiveTitle", "33 33 33")
                registryEditor.setStringValue("Control Panel\\Colors", "Background", background)
                registryEditor.setStringValue("Control Panel\\Colors", "ButtonAlternateFace", "33 33 33")
                registryEditor.setStringValue("Control Panel\\Colors", "ButtonDkShadow", "0 0 0")
                registryEditor.setStringValue("Control Panel\\Colors", "ButtonFace", "33 33 33")
                registryEditor.setStringValue("Control Panel\\Colors", "ButtonHilight", "48 48 48")
                registryEditor.setStringValue("Control Panel\\Colors", "ButtonLight", "48 48 48")
                registryEditor.setStringValue("Control Panel\\Colors", "ButtonShadow", "0 0 0")
                registryEditor.setStringValue("Control Panel\\Colors", "ButtonText", "255 255 255")
                registryEditor.setStringValue("Control Panel\\Colors", "GradientActiveTitle", "33 33 33")
                registryEditor.setStringValue("Control Panel\\Colors", "GradientInactiveTitle", "33 33 33")
                registryEditor.setStringValue("Control Panel\\Colors", "GrayText", "117 117 117")
                registryEditor.setStringValue("Control Panel\\Colors", "Hilight", "2 136 209")
                registryEditor.setStringValue("Control Panel\\Colors", "HilightText", "255 255 255")
                registryEditor.setStringValue("Control Panel\\Colors", "HotTrackingColor", "2 136 209")
                registryEditor.setStringValue("Control Panel\\Colors", "InactiveBorder", "48 48 48")
                registryEditor.setStringValue("Control Panel\\Colors", "InactiveTitle", "33 33 33")
                registryEditor.setStringValue("Control Panel\\Colors", "InactiveTitleText", "117 117 117")
                registryEditor.setStringValue("Control Panel\\Colors", "InfoText", "255 255 255")
                registryEditor.setStringValue("Control Panel\\Colors", "InfoWindow", "255 255 255")
                registryEditor.setStringValue("Control Panel\\Colors", "Menu", "33 33 33")
                registryEditor.setStringValue("Control Panel\\Colors", "MenuBar", "48 48 48")
                registryEditor.setStringValue("Control Panel\\Colors", "MenuHilight", "2 136 209")
                registryEditor.setStringValue("Control Panel\\Colors", "MenuText", "255 255 255")
                registryEditor.setStringValue("Control Panel\\Colors", "Scrollbar", "48 48 48")
                registryEditor.setStringValue("Control Panel\\Colors", "TitleText", "255 255 255")
                registryEditor.setStringValue("Control Panel\\Colors", "Window", "48 48 48")
                registryEditor.setStringValue("Control Panel\\Colors", "WindowFrame", "0 0 0")
                registryEditor.setStringValue("Control Panel\\Colors", "WindowText", "255 255 255")
            }
        }
    }

    private fun createWallpaperBMPFile(context: Context, screenInfo: ScreenInfo) {
        val outputHeight = 480
        val outputWidth = ceil(((outputHeight.toFloat() / screenInfo.height) * screenInfo.width).toDouble()).toInt()

        val outputBitmap = createBitmap(outputWidth, outputHeight)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val canvas = Canvas(outputBitmap)

        val userWallpaperFile = getUserWallpaperFile(context)

        if (userWallpaperFile.isFile) {
            val image = BitmapFactory.decodeFile(userWallpaperFile.path)
            val srcRect = Rect(0, 0, image.width, image.height)
            val dstRect = Rect(0, 0, outputWidth, outputHeight)

            canvas.drawBitmap(image, srcRect, dstRect, paint)
        } else {
            val options = BitmapFactory.Options()

            options.inTargetDensity = DisplayMetrics.DENSITY_HIGH

            val wallpaperBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.wallpaper, options)

            paint.style = Paint.Style.FILL
            paint.color = -0xfea865

            canvas.drawRect(0f, 0f, outputWidth.toFloat(), outputHeight * 0.5f, paint)

            paint.color = -0xfd8843

            canvas.drawRect(0f, outputHeight * 0.5f, outputWidth.toFloat(), outputHeight.toFloat(), paint)

            val targetSize = outputHeight * (320.0f / 480.0f)
            val centerX = (outputWidth - targetSize) * 0.5f
            val centerY = (outputHeight - targetSize) * 0.5f
            val srcRect = Rect(0, 0, wallpaperBitmap.width, wallpaperBitmap.height)
            val dstRect = RectF(centerX, centerY, centerX + targetSize, centerY + targetSize)

            canvas.drawBitmap(wallpaperBitmap, srcRect, dstRect, paint)
        }

        val imageFs = ImageFs.find(context)

        create(outputBitmap, File(imageFs.rootDir, ImageFs.CACHE_PATH + "/wallpaper.bmp"))
    }

    fun getUserWallpaperFile(context: Context): File = File(ImageFs.find(context).rootDir, ImageFs.CONFIG_PATH + "/user-wallpaper.png")

    enum class Theme {
        LIGHT,
        DARK,
    }

    enum class BackgroundType {
        IMAGE,
        COLOR,
    }

    class ThemeInfo(value: String) {
        val theme: Theme
        var backgroundType: BackgroundType? = null
        var backgroundColor: Int = 0

        init {
            val values = value.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            theme = Theme.valueOf(values[0])

            if (values.size < 3) {
                backgroundColor = values[1].toColorInt()
                backgroundType = BackgroundType.IMAGE
            } else {
                backgroundType = BackgroundType.valueOf(values[1])
                backgroundColor = values[2].toColorInt()
            }
        }
    }
}
