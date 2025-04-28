package com.micewine.emu.activities

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.micewine.emu.R
import com.micewine.emu.adapters.AdapterPreset.Companion.PHYSICAL_CONTROLLER
import com.micewine.emu.adapters.AdapterPreset.Companion.VIRTUAL_CONTROLLER
import com.micewine.emu.adapters.AdapterPreset.Companion.clickedPresetType
import com.micewine.emu.controller.ControllerUtils.connectedPhysicalControllers
import com.micewine.emu.databinding.ActivityPresetManagerBinding
import com.micewine.emu.fragments.Box64PresetManagerFragment
import com.micewine.emu.fragments.Box64SettingsFragment
import com.micewine.emu.fragments.ControllerMapperFragment
import com.micewine.emu.fragments.ControllerPresetManagerFragment
import com.micewine.emu.fragments.CreatePresetFragment
import com.micewine.emu.fragments.CreatePresetFragment.Companion.BOX64_PRESET
import com.micewine.emu.fragments.CreatePresetFragment.Companion.CONTROLLER_PRESET
import com.micewine.emu.fragments.CreatePresetFragment.Companion.VIRTUAL_CONTROLLER_PRESET
import com.micewine.emu.fragments.FloatingFileManagerFragment
import com.micewine.emu.fragments.FloatingFileManagerFragment.Companion.OPERATION_IMPORT_PRESET
import com.micewine.emu.fragments.VirtualControllerPresetManagerFragment

class PresetManagerActivity : AppCompatActivity() {
    private var binding: ActivityPresetManagerBinding? = null
    private var backButton: ImageButton? = null
    private var controllerConnected: TextView? = null
    private var addPresetFAB: FloatingActionButton? = null
    private var importPresetFAB: FloatingActionButton? = null
    private var preferences: SharedPreferences? = null
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_EDIT_CONTROLLER_MAPPING -> {
                    fragmentLoader(ControllerMapperFragment(), false)

                    addPresetFAB?.visibility = View.GONE
                    importPresetFAB?.visibility = View.GONE
                }
                ACTION_EDIT_BOX64_PRESET -> {
                    fragmentLoader(Box64SettingsFragment(), false)

                    addPresetFAB?.visibility = View.GONE
                    importPresetFAB?.visibility = View.GONE
                }
            }
        }
    }

    @SuppressLint("SetTextI18n", "UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferences = PreferenceManager.getDefaultSharedPreferences(this)!!

        binding = ActivityPresetManagerBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        controllerConnected = findViewById(R.id.controllerConnected)

        backButton = findViewById(R.id.backButton)
        backButton?.setOnClickListener {
            onKeyDown(KeyEvent.KEYCODE_BACK, null)
        }

        addPresetFAB = findViewById(R.id.addPresetFAB)
        importPresetFAB = findViewById(R.id.importPresetFAB)

        val presetType = intent?.getIntExtra("presetType", -1)
        val editShortcut = intent?.getBooleanExtra("editShortcut", false)

        when (presetType) {
            PHYSICAL_CONTROLLER -> {
                fragmentLoader(ControllerPresetManagerFragment(editShortcut!!), true)

                findViewById<Toolbar>(R.id.controllerMapperToolbar).title = getString(R.string.controller_mapper_title)

                if (connectedPhysicalControllers.isNotEmpty()) {
                    controllerConnected?.text = getString(R.string.connected_controller, connectedPhysicalControllers.joinToString(", ") { it.name })
                } else {
                    controllerConnected?.text = getString(R.string.no_controllers_connected)
                }

                addPresetFAB?.setOnClickListener {
                    CreatePresetFragment(CONTROLLER_PRESET).show(supportFragmentManager, "")
                }
                importPresetFAB?.setOnClickListener {
                    clickedPresetType = PHYSICAL_CONTROLLER
                    FloatingFileManagerFragment(OPERATION_IMPORT_PRESET).show(supportFragmentManager, "")
                }
            }
            VIRTUAL_CONTROLLER -> {
                fragmentLoader(VirtualControllerPresetManagerFragment(editShortcut!!), true)

                findViewById<Toolbar>(R.id.controllerMapperToolbar).title = getString(R.string.virtual_controller_mapper_title)

                controllerConnected?.visibility = View.GONE

                addPresetFAB?.setOnClickListener {
                    CreatePresetFragment(VIRTUAL_CONTROLLER_PRESET).show(supportFragmentManager, "")
                }
                importPresetFAB?.setOnClickListener {
                    clickedPresetType = VIRTUAL_CONTROLLER
                    FloatingFileManagerFragment(OPERATION_IMPORT_PRESET).show(supportFragmentManager, "")
                }
            }
            BOX64_PRESET -> {
                fragmentLoader(Box64PresetManagerFragment(), true)

                findViewById<Toolbar>(R.id.controllerMapperToolbar).title = getString(R.string.box64_preset_manager_title)

                controllerConnected?.visibility = View.GONE

                addPresetFAB?.setOnClickListener {
                    CreatePresetFragment(BOX64_PRESET).show(supportFragmentManager, "")
                }
                importPresetFAB?.setOnClickListener {
                    clickedPresetType = BOX64_PRESET
                    FloatingFileManagerFragment(OPERATION_IMPORT_PRESET).show(supportFragmentManager, "")
                }
            }
        }

        registerReceiver(receiver, object : IntentFilter() {
            init {
                addAction(ACTION_EDIT_CONTROLLER_MAPPING)
                addAction(ACTION_EDIT_BOX64_PRESET)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
                addPresetFAB?.visibility = View.VISIBLE
                importPresetFAB?.visibility = View.VISIBLE
            } else {
                finish()
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun fragmentLoader(fragment: Fragment, init: Boolean) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.presetManagerContent, fragment)

            if (!init) {
                addToBackStack(null)
            }

            commit()
        }
    }

    companion object {
        const val BUTTON_A_KEY = "buttonA"
        const val BUTTON_B_KEY = "buttonB"
        const val BUTTON_X_KEY = "buttonX"
        const val BUTTON_Y_KEY = "buttonY"
        const val BUTTON_START_KEY = "buttonStart"
        const val BUTTON_SELECT_KEY = "buttonSelect"
        const val BUTTON_R1_KEY = "buttonR1"
        const val BUTTON_R2_KEY = "buttonR2"
        const val BUTTON_L1_KEY = "buttonL1"
        const val BUTTON_L2_KEY = "buttonL2"
        const val BUTTON_THUMBL_KEY = "thumbLKey"
        const val BUTTON_THUMBR_KEY = "thumbRKey"
        const val AXIS_X_PLUS_KEY = "axisX+"
        const val AXIS_X_MINUS_KEY = "axisX-"
        const val AXIS_Y_PLUS_KEY = "axisY+"
        const val AXIS_Y_MINUS_KEY = "axisY-"
        const val AXIS_Z_PLUS_KEY = "axisZ+"
        const val AXIS_Z_MINUS_KEY = "axisZ-"
        const val AXIS_RZ_PLUS_KEY = "axisRZ+"
        const val AXIS_RZ_MINUS_KEY = "axisRZ-"
        const val AXIS_HAT_X_PLUS_KEY = "axisHatX+"
        const val AXIS_HAT_X_MINUS_KEY = "axisHatX-"
        const val AXIS_HAT_Y_PLUS_KEY = "axisHatY+"
        const val AXIS_HAT_Y_MINUS_KEY = "axisHatY-"

        const val SELECTED_CONTROLLER_PRESET_KEY = "selectedControllerPreset"
        const val SELECTED_VIRTUAL_CONTROLLER_PRESET_KEY = "selectedVirtualControllerPreset"
        const val SELECTED_BOX64_PRESET_KEY = "selectedBox64Preset"

        const val ACTION_EDIT_CONTROLLER_MAPPING = "com.micewine.emu.ACTION_EDIT_CONTROLLER_MAPPING"
        const val ACTION_EDIT_BOX64_PRESET = "com.micewine.emu.ACTION_EDIT_BOX64_PRESET"
    }
}