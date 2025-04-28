package com.micewine.emu.activities

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.micewine.emu.R
import com.micewine.emu.controller.XKeyCodes.getXKeyScanCodes
import com.micewine.emu.databinding.ActivityVirtualControllerMapperBinding
import com.micewine.emu.fragments.EditVirtualButtonFragment
import com.micewine.emu.views.OverlayView
import com.micewine.emu.views.OverlayView.Companion.SHAPE_CIRCLE
import com.micewine.emu.views.OverlayView.Companion.analogList
import com.micewine.emu.views.OverlayView.Companion.buttonList
import com.micewine.emu.views.OverlayViewCreator

class VirtualControllerOverlayMapper : AppCompatActivity() {
    private var binding: ActivityVirtualControllerMapperBinding? = null
    private var overlayView: OverlayViewCreator? = null
    private var virtualControllerMapperDrawerLayout: DrawerLayout? = null
    private var navigationView: NavigationView? = null
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_EDIT_VIRTUAL_BUTTON) {
                EditVirtualButtonFragment().show(supportFragmentManager, "")
            } else if (intent.action == ACTION_INVALIDATE) {
                overlayView?.invalidate()
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityVirtualControllerMapperBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        overlayView = findViewById(R.id.overlayView)

        virtualControllerMapperDrawerLayout = findViewById(R.id.virtualControllerMapperDrawerLayout)
        virtualControllerMapperDrawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        navigationView = findViewById(R.id.navigationView)
        navigationView?.setNavigationItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.addButton -> {
                    overlayView?.addButton(
                        OverlayView.VirtualButton(
                            buttonList.count() + 1,
                            overlayView?.width!! / 2F,
                            overlayView?.height!! / 2F,
                            180F,
                            "--",
                            null,
                            -1,
                            false,
                            SHAPE_CIRCLE
                        )
                    )

                    virtualControllerMapperDrawerLayout?.closeDrawers()
                }

                R.id.addVAxis -> {
                    overlayView?.addAnalog(
                        OverlayView.VirtualAnalog(
                            analogList.count() + 1,
                            overlayView?.width!! / 2F,
                            overlayView?.height!! / 2F,
                            0F,
                            0F,
                            275F,
                            "--",
                            getXKeyScanCodes("--"),
                            "--",
                            getXKeyScanCodes("--"),
                            "--",
                            getXKeyScanCodes("--"),
                            "--",
                            getXKeyScanCodes("--"),
                            false,
                            -1,
                            0.75F
                        )
                    )

                    virtualControllerMapperDrawerLayout?.closeDrawers()
                }

                R.id.addDPad -> {
                    overlayView?.addDPad(
                        OverlayView.VirtualDPad(
                            analogList.count() + 1,
                            overlayView?.width!! / 2F,
                            overlayView?.height!! / 2F,
                            275F,
                            "--",
                            getXKeyScanCodes("--"),
                            "--",
                            getXKeyScanCodes("--"),
                            "--",
                            getXKeyScanCodes("--"),
                            "--",
                            getXKeyScanCodes("--"),
                            -1,
                            false,
                            0F,
                            0F,
                            0
                        )
                    )

                    virtualControllerMapperDrawerLayout?.closeDrawers()
                }

                R.id.exitButton -> {
                    overlayView?.saveOnPreferences()

                    finish()
                }
            }

            true
        }

        registerReceiver(receiver, object : IntentFilter(ACTION_EDIT_VIRTUAL_BUTTON) {
            init {
                addAction(ACTION_INVALIDATE)
            }
        })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (virtualControllerMapperDrawerLayout?.isOpen!!) {
                virtualControllerMapperDrawerLayout?.closeDrawers()
            } else {
                virtualControllerMapperDrawerLayout?.openDrawer(GravityCompat.START)
            }

            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
    }

    companion object {
        const val ACTION_EDIT_VIRTUAL_BUTTON = "com.micewine.emu.ACTION_EDIT_VIRTUAL_BUTTON"
        const val ACTION_INVALIDATE = "com.micewine.emu.ACTION_INVALIDATE"
    }
}