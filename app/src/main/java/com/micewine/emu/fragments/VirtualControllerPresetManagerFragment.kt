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
import com.micewine.emu.activities.MainActivity.Companion.getNativeResolution
import com.micewine.emu.activities.PresetManagerActivity.Companion.SELECTED_VIRTUAL_CONTROLLER_PRESET_KEY
import com.micewine.emu.adapters.AdapterPreset
import com.micewine.emu.adapters.AdapterPreset.Companion.VIRTUAL_CONTROLLER
import com.micewine.emu.adapters.AdapterPreset.Companion.selectedPresetId
import com.micewine.emu.views.OverlayView
import com.micewine.emu.views.OverlayViewCreator.Companion.GRID_SIZE
import java.io.File
import kotlin.math.roundToInt

class VirtualControllerPresetManagerFragment(private val editShortcut: Boolean) : Fragment() {
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
        recyclerView?.setAdapter(AdapterPreset(presetListNames, requireContext(), requireActivity().supportFragmentManager))

        presetListNames.clear()
        presetList.forEach {
            addToAdapter(it.name, VIRTUAL_CONTROLLER, true)
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
        private var presetList: MutableList<VirtualControllerPreset> = mutableListOf()
        var preferences: SharedPreferences? = null
        private var editShortcut: Boolean = false

        private val gson = Gson()

        fun initialize(context: Context, boolean: Boolean = false) {
            preferences = PreferenceManager.getDefaultSharedPreferences(context)
            presetList = getVirtualControllerPresets(context)
            editShortcut = boolean
        }

        fun getMapping(name: String): VirtualControllerPreset? {
            val index = presetList.indexOfFirst { it.name == name }

            if (index == -1) return null

            return presetList[index]
        }

        fun putMapping(name: String, resolution: String, buttonList: MutableList<OverlayView.VirtualButton>, analogList: MutableList<OverlayView.VirtualAnalog>, dpadList: MutableList<OverlayView.VirtualDPad>) {
            val index = presetList.indexOfFirst { it.name == name }

            if (index == -1) return

            presetList[index] = VirtualControllerPreset(name, resolution, mutableListOf(), mutableListOf(), mutableListOf())

            buttonList.forEach {
                presetList[index].buttons.add(it)
            }
            analogList.forEach {
                presetList[index].analogs.add(it)
            }
            dpadList.forEach {
                presetList[index].dpads.add(it)
            }

            saveVirtualControllerPresets()
        }

        fun addVirtualControllerPreset(context: Context, name: String) {
            if (presetListNames.firstOrNull { it.titleSettings == name } != null) {
                Toast.makeText(context, context.getString(R.string.executable_already_added), Toast.LENGTH_LONG).show()
                return
            }

            val defaultPreset = VirtualControllerPreset(name, "", mutableListOf(), mutableListOf(), mutableListOf())

            presetList.add(defaultPreset)
            presetListNames.add(
                AdapterPreset.Item(name, VIRTUAL_CONTROLLER, true, editShortcut)
            )

            recyclerView?.adapter?.notifyItemInserted(presetListNames.size)

            saveVirtualControllerPresets()
        }

        fun deleteVirtualControllerPreset(name: String) {
            val index = presetList.indexOfFirst { it.name == name }

            presetList.removeAt(index)
            presetListNames.removeAt(index)

            recyclerView?.adapter?.notifyItemRemoved(index)

            if (index == selectedPresetId) {
                preferences?.edit {
                    putString(SELECTED_VIRTUAL_CONTROLLER_PRESET_KEY, presetListNames.first().titleSettings)
                    apply()
                }
                recyclerView?.adapter?.notifyItemChanged(0)
            }

            saveVirtualControllerPresets()
        }

        fun renameVirtualControllerPreset(name: String, newName: String) {
            val index = presetList.indexOfFirst { it.name == name }

            presetList[index].name = newName
            presetListNames[index].titleSettings = newName

            recyclerView?.adapter?.notifyItemChanged(index)

            saveVirtualControllerPresets()
        }

        fun importVirtualControllerPreset(activity: Activity, path: String) {
            val lines = File(path).readLines()
            val canAutoAdjust = lines[1].contains("resolution")
            val listType = object : TypeToken<VirtualControllerPreset>() {}.type

            if (lines.size < 2 || lines[0] != "virtualControllerPreset") {
                activity.runOnUiThread {
                    Toast.makeText(activity, activity.getString(R.string.invalid_virtual_controller_preset_file), Toast.LENGTH_LONG).show()
                }
                return
            }

            val processed = gson.fromJson<VirtualControllerPreset>(lines[1], listType)

            var presetName = processed.name
            var count = 1

            while (presetList.any { it.name == presetName }) {
                presetName = "${processed.name}-$count"
                count++
            }

            processed.name = presetName

            if (canAutoAdjust) {
                val nativeResolution = getNativeResolution(activity)

                if (processed.resolution != nativeResolution) {
                    val nativeSplit = nativeResolution.split("x").map { it.toFloat() }
                    val processedSplit = processed.resolution.split("x").map { it.toFloat() }

                    val multiplierX = nativeSplit[0] / processedSplit[0] * 100F
                    val multiplierY = nativeSplit[1] / processedSplit[1] * 100F

                    processed.buttons.forEach {
                        it.x = (it.x / 100F * multiplierX / GRID_SIZE).roundToInt() * GRID_SIZE.toFloat()
                        it.y = (it.y / 100F * multiplierY / GRID_SIZE).roundToInt() * GRID_SIZE.toFloat()
                    }
                    processed.analogs.forEach {
                        it.x = (it.x / 100F * multiplierX / GRID_SIZE).roundToInt() * GRID_SIZE.toFloat()
                        it.y = (it.y / 100F * multiplierY / GRID_SIZE).roundToInt() * GRID_SIZE.toFloat()
                    }
                    processed.dpads.forEach {
                        it.x = (it.x / 100F * multiplierX / GRID_SIZE).roundToInt() * GRID_SIZE.toFloat()
                        it.y = (it.y / 100F * multiplierY / GRID_SIZE).roundToInt() * GRID_SIZE.toFloat()
                    }
                }
            }

            presetList.add(processed)
            presetListNames.add(
                AdapterPreset.Item(processed.name, VIRTUAL_CONTROLLER, true)
            )

            recyclerView?.post {
                recyclerView?.adapter?.notifyItemInserted(presetListNames.size)
            }

            saveVirtualControllerPresets()
        }

        fun exportVirtualControllerPreset(name: String, path: String) {
            val index = presetList.indexOfFirst { it.name == name }
            val file = File(path)

            file.writeText("virtualControllerPreset\n" + gson.toJson(presetList[index]))
        }

        private fun saveVirtualControllerPresets() {
            preferences?.edit {
                putString("virtualControllerPresetList", gson.toJson(presetList))
                apply()
            }
        }

        fun getVirtualControllerPresets(context: Context): MutableList<VirtualControllerPreset> {
            val json = preferences?.getString("virtualControllerPresetList", "")
            val listType = object : TypeToken<MutableList<VirtualControllerPreset>>() {}.type

            return gson.fromJson(json, listType) ?: mutableListOf(
                VirtualControllerPreset("default", getNativeResolution(context), mutableListOf(), mutableListOf(), mutableListOf())
            )
        }

        data class VirtualControllerPreset(
            var name: String,
            var resolution: String,
            var analogs: MutableList<OverlayView.VirtualAnalog>,
            var buttons: MutableList<OverlayView.VirtualButton>,
            var dpads: MutableList<OverlayView.VirtualDPad>
        )
    }
}
