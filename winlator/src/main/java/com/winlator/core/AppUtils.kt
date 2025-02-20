package com.winlator.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Rect
import android.os.Build
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.lang.ref.WeakReference

object AppUtils {

    val archName: String
        get() {
            for (arch in Build.SUPPORTED_ABIS) {
                when (arch) {
                    "arm64-v8a" -> return "arm64"
                    "armeabi-v7a" -> return "armhf"
                    "x86_64" -> return "x86_64"
                    "x86" -> return "x86"
                }
            }
            return "armhf"
        }

    private val globalToastReference: WeakReference<Toast>? = null

    fun restartActivity(activity: AppCompatActivity) {
        val intent = activity.intent
        activity.finish()
        activity.startActivity(intent)
        activity.overridePendingTransition(0, 0)
    }

    @JvmOverloads
    fun restartApplication(context: Context, selectedMenuItemId: Int = 0) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val mainIntent = Intent.makeRestartActivityTask(intent!!.component)

        if (selectedMenuItemId > 0) {
            mainIntent.putExtra("selected_menu_item_id", selectedMenuItemId)
        }

        context.startActivity(mainIntent)

        Runtime.getRuntime().exit(0)
    }

    fun showKeyboard(activity: AppCompatActivity) {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            activity.window.decorView.postDelayed({ imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0) }, 500L)
        } else {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }
    }

    fun hideSystemUI(activity: Activity, hide: Boolean = true) {
        val window = activity.window
        val decorView = window.decorView

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            val insetsController = decorView.windowInsetsController
            if (insetsController != null) {
                if (hide) {
                    insetsController.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    insetsController.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    insetsController.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    insetsController.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
                }
            }
        } else {
            val flags = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

            decorView.systemUiVisibility = flags
            decorView.setOnSystemUiVisibilityChangeListener { visibility: Int ->
                if ((visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.systemUiVisibility = flags
                }
            }
        }
    }

    val isUiThread: Boolean
        get() = Looper.getMainLooper().thread == Thread.currentThread()

    val screenWidth: Int
        get() = Resources.getSystem().displayMetrics.widthPixels

    val screenHeight: Int
        get() = Resources.getSystem().displayMetrics.heightPixels

    fun getVersionCode(context: Context): Int {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            return pInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            return 0
        }
    }

    fun observeSoftKeyboardVisibility(rootView: View, callback: Callback<Boolean?>) {
        val visible = booleanArrayOf(false)

        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()

            rootView.getWindowVisibleDisplayFrame(rect)

            val screenHeight = rootView.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            if (keypadHeight > screenHeight * 0.15f) {
                if (!visible[0]) {
                    visible[0] = true
                    callback.call(true)
                }
            } else {
                if (visible[0]) {
                    visible[0] = false
                    callback.call(false)
                }
            }
        }
    }
}
