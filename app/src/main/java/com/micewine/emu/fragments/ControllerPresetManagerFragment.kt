package com.micewine.emu.fragments

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.micewine.emu.R
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.DEAD_ZONE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.MOUSE_SENSIBILITY
import com.micewine.emu.activities.PresetManagerActivity.Companion.AXIS_HAT_X_MINUS_KEY
import com.micewine.emu.activities.PresetManagerActivity.Companion.AXIS_HAT_X_PLUS_KEY
import com.micewine.emu.activities.PresetManagerActivity.Companion.AXIS_HAT_Y_MINUS_KEY
import com.micewine.emu.activities.PresetManagerActivity.Companion.AXIS_HAT_Y_PLUS_KEY
import com.micewine.emu.activities.PresetManagerActivity.Companion.AXIS_RZ_MINUS_KEY
import com.micewine.emu.activities.PresetManagerActivity.Companion.AXIS_RZ_PLUS_KEY
import com.micewine.emu.activities.PresetManagerActivity.Companion.AXIS_X_MINUS_KEY
import com.micewine.emu.activities.PresetManagerActivity.Companion.AXIS_X_PLUS_KEY
import com.micewine.emu.activities.PresetManagerActivity.Companion.AXIS_Y_MINUS_KEY
import com.micewine.emu.activities.PresetManagerActivity.Companion.AXIS_Y_PLUS_KEY
import com.micewine.emu.activities.PresetManagerActivity.Companion.AXIS_Z_MINUS_KEY
import com.micewine.emu.activities.PresetManagerActivity.Companion.AXIS_Z_PLUS_KEY
import com.micewine.emu.activities.PresetManagerActivity.Companion.BUTTON_A_KEY
import com.micewine.emu.activities.PresetManagerActivity.Companion.BUTTON_B_KEY
import com.micewine.emu.activities.PresetManagerActivity.Companion.BUTTON_L1_KEY
import com.micewine.emu.activities.PresetManagerActivity.Companion.BUTTON_L2_KEY
import com.micewine.emu.activities.PresetManagerActivity.Companion.BUTTON_R1_KEY
import com.micewine.emu.activities.PresetManagerActivity.Companion.BUTTON_R2_KEY
import com.micewine.emu.activities.PresetManagerActivity.Companion.BUTTON_SELECT_KEY
import com.micewine.emu.activities.PresetManagerActivity.Companion.BUTTON_START_KEY
import com.micewine.emu.activities.PresetManagerActivity.Companion.BUTTON_THUMBL_KEY
import com.micewine.emu.activities.PresetManagerActivity.Companion.BUTTON_THUMBR_KEY
import com.micewine.emu.activities.PresetManagerActivity.Companion.BUTTON_X_KEY
import com.micewine.emu.activities.PresetManagerActivity.Companion.BUTTON_Y_KEY
import com.micewine.emu.activities.PresetManagerActivity.Companion.SELECTED_CONTROLLER_PRESET_KEY
import com.micewine.emu.adapters.AdapterPreset
import com.micewine.emu.adapters.AdapterPreset.Companion.PHYSICAL_CONTROLLER
import com.micewine.emu.adapters.AdapterPreset.Companion.selectedPresetId
import java.io.File
import java.util.Collections

class ControllerPresetManagerFragment(private val editShortcut: Boolean) : Fragment() {
    private var rootView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_general_settings, container, false)
        recyclerView = rootView?.findViewById(R.id.recyclerViewGeneralSettings)

        initialize(requireContext(), editShortcut)
        setAdapter()

        return rootView
    }

    private fun setAdapter() {
        recyclerView?.setAdapter(
            AdapterPreset(presetListNames, requireContext(), requireActivity().supportFragmentManager)
        )

        presetListNames.clear()
        presetList.forEach {
            addToAdapter(it[0], PHYSICAL_CONTROLLER, true)
        }
    }

    private fun addToAdapter(titleSettings: String, type: Int, userPreset: Boolean) {
        presetListNames.add(
            AdapterPreset.Item(titleSettings, type, userPreset, editShortcut)
        )
    }

    companion object {
        private var recyclerView: RecyclerView? = null
        private val presetListNames: MutableList<AdapterPreset.Item> = mutableListOf()
        private var presetList: MutableList<MutableList<String>> = mutableListOf()
        private var preferences: SharedPreferences? = null
        private var editShortcut: Boolean = false

        private val gson = Gson()

        private val mappingMap = mapOf(
            BUTTON_A_KEY to 1,
            BUTTON_B_KEY to 2,
            BUTTON_X_KEY to 3,
            BUTTON_Y_KEY to 4,
            BUTTON_START_KEY to 5,
            BUTTON_SELECT_KEY to 6,
            BUTTON_R1_KEY to 7,
            BUTTON_R2_KEY to 8,
            BUTTON_L1_KEY to 9,
            BUTTON_L2_KEY to 10,
            BUTTON_THUMBL_KEY to 11,
            BUTTON_THUMBR_KEY to 12,
            AXIS_X_PLUS_KEY to 13,
            AXIS_X_MINUS_KEY to 14,
            AXIS_Y_PLUS_KEY to 15,
            AXIS_Y_MINUS_KEY to 16,
            AXIS_Z_PLUS_KEY to 17,
            AXIS_Z_MINUS_KEY to 18,
            AXIS_RZ_PLUS_KEY to 19,
            AXIS_RZ_MINUS_KEY to 20,
            AXIS_HAT_X_PLUS_KEY to 21,
            AXIS_HAT_X_MINUS_KEY to 22,
            AXIS_HAT_Y_PLUS_KEY to 23,
            AXIS_HAT_Y_MINUS_KEY to 24,
            DEAD_ZONE to 25,
            MOUSE_SENSIBILITY to 26
        )

        fun initialize(context: Context, boolean: Boolean = false) {
            preferences = PreferenceManager.getDefaultSharedPreferences(context)
            presetList = getControllerPresets()
            editShortcut = boolean
        }

        fun getMouseSensibility(name: String): Int {
            val index = presetList.indexOfFirst { it[0] == name }

            if (index == -1) {
                return 100
            }

            return presetList[index][mappingMap[MOUSE_SENSIBILITY]!!].toInt()
        }

        fun putMouseSensibility(name: String, value: Int) {
            val index = presetList.indexOfFirst { it[0] == name }

            if (index == -1) {
                return
            }

            presetList[index][mappingMap[MOUSE_SENSIBILITY]!!] = value.toString()

            saveControllerPresets()
        }

        fun getDeadZone(name: String): Int {
            val index = presetList.indexOfFirst { it[0] == name }

            if (index == -1) {
                return 25
            }

            return presetList[index][mappingMap[DEAD_ZONE]!!].toInt()
        }

        fun putDeadZone(name: String, value: Int) {
            val index = presetList.indexOfFirst { it[0] == name }

            if (index == -1) {
                return
            }

            presetList[index][mappingMap[DEAD_ZONE]!!] = value.toString()

            saveControllerPresets()
        }

        fun getMapping(name: String, key: String): List<String> {
            val index = presetList.indexOfFirst { it[0] == name }

            if (index == -1) {
                return listOf("", "")
            }

            return presetList[index][mappingMap[key]!!].split(":")
        }

        fun editControllerPreset(name: String, key: String, selectedItem: String) {
            var index = presetList.indexOfFirst { it[0] == name }

            if (index == -1) {
                presetList[0][0] = name
                index = 0
            }

            presetList[index][mappingMap[key]!!] = selectedItem

            saveControllerPresets()
        }

        fun addControllerPreset(context: Context, name: String) {
            if (presetListNames.firstOrNull { it.titleSettings == name } != null) {
                Toast.makeText(context, context.getString(R.string.executable_already_added), Toast.LENGTH_LONG).show()
                return
            }

            val defaultPreset = ArrayList(Collections.nCopies(mappingMap.size + 1, ":")).apply {
                this[0] = name
                this[mappingMap[DEAD_ZONE]!!] = "25"
                this[mappingMap[MOUSE_SENSIBILITY]!!] = "100"
            }

            presetList.add(defaultPreset)
            presetListNames.add(
                AdapterPreset.Item(name, PHYSICAL_CONTROLLER, true, editShortcut)
            )

            recyclerView?.adapter?.notifyItemInserted(presetListNames.size)

            saveControllerPresets()
        }

        fun deleteControllerPreset(name: String) {
            val index = presetList.indexOfFirst { it[0] == name }

            presetList.removeAt(index)
            presetListNames.removeAt(index)

            recyclerView?.adapter?.notifyItemRemoved(index)

            if (index == selectedPresetId) {
                preferences?.edit {
                    putString(SELECTED_CONTROLLER_PRESET_KEY, presetListNames.first().titleSettings)
                    apply()
                }
                recyclerView?.adapter?.notifyItemChanged(0)
            }

            saveControllerPresets()
        }

        fun renameControllerPreset(name: String, newName: String) {
            val index = presetList.indexOfFirst { it[0] == name }

            presetList[index][0] = newName
            presetListNames[index].titleSettings = newName

            recyclerView?.adapter?.notifyItemChanged(index)

            saveControllerPresets()
        }

        fun importControllerPreset(activity: Activity, path: String) {
            val json = File(path).readLines()
            val listType = object : TypeToken<MutableList<String>>() {}.type

            if (json.size < 2 || json[0] != "controllerPreset") {
                activity.runOnUiThread {
                    Toast.makeText(activity, activity.getString(R.string.invalid_controller_preset_file), Toast.LENGTH_LONG).show()
                }
                return
            }

            val processed = gson.fromJson<MutableList<String>>(json[1], listType)

            var presetName = processed[0]
            var count = 1

            while (presetList.any { it[0] == presetName }) {
                presetName = "${processed[0]}-$count"
                count++
            }

            processed[0] = presetName

            presetList.add(processed)
            presetListNames.add(
                AdapterPreset.Item(processed[0], PHYSICAL_CONTROLLER, true)
            )

            activity.runOnUiThread {
                recyclerView?.adapter?.notifyItemInserted(presetListNames.size)
            }

            saveControllerPresets()
        }

        fun exportControllerPreset(name: String, path: String) {
            val index = presetList.indexOfFirst { it[0] == name }
            val file = File(path)

            file.writeText("controllerPreset\n" + gson.toJson(presetList[index]))
        }

        private fun saveControllerPresets() {
            preferences?.edit {
                putString("controllerPresetList", gson.toJson(presetList))  
                apply()
            }
        }

        fun getControllerPresets(): MutableList<MutableList<String>> {
            val json = preferences?.getString("controllerPresetList", "")
            val listType = object : TypeToken<MutableList<List<String>>>() {}.type

            return gson.fromJson(json, listType) ?: mutableListOf(ArrayList(
                Collections.nCopies(mappingMap.size + 1, ":")).apply {
                this[0] = "default"
                this[mappingMap[DEAD_ZONE]!!] = "25"
                this[mappingMap[MOUSE_SENSIBILITY]!!] = "100"
            })
        }
    }
}
