package com.micewine.emu.activities

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentManager
import com.micewine.emu.core.ShellLoader

/**
 * This is a Composed version of MiceWine's EmulationActivity
 */
class EmulationActivity : ComponentActivity() {

    // TODO

    fun handleKey(e: KeyEvent): Boolean {
        TODO()
    }

    fun setExternalKeyboardConnected(connected: Boolean) {
        externalKeyboardConnected = connected
        TODO()
        // lorieView!!.requestFocus()
    }

    fun clientConnectedStateChanged() {
        // TODO
    }

    companion object {
        const val KEY_BACK = 158

        var handler: Handler = Handler(Looper.getMainLooper())
        var inputMethodManager: InputMethodManager? = null
        private var showIMEWhileExternalConnected = false
        private var externalKeyboardConnected = false

        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: EmulationActivity

        @JvmStatic
        fun getInstance(): EmulationActivity {
            return instance
        }

        @JvmStatic
        fun getKeyboardConnected(): Boolean {
            return externalKeyboardConnected
        }

        @JvmStatic
        fun getDisplayDensity(): Float {
            return Resources.getSystem().displayMetrics.density
        }

        var sharedLogs: ShellLoader.ViewModelAppLogs? = null

        fun initSharedLogs(supportFragmentManager: FragmentManager) {
            sharedLogs = ShellLoader.ViewModelAppLogs(supportFragmentManager)
        }
    }
}
