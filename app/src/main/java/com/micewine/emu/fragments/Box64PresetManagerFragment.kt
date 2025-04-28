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
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_AVX
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_ALIGNED_ATOMICS
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_BIGBLOCK
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_CALLRET
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_DIRTY
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_FASTNAN
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_FASTROUND
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_FORWARD
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_NATIVEFLAGS
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_PAUSE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_SAFEFLAGS
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_STRONGMEM
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_WAIT
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_WEAKBARRIER
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_X87DOUBLE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_MMAP32
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_SSE42
import com.micewine.emu.activities.PresetManagerActivity.Companion.SELECTED_BOX64_PRESET_KEY
import com.micewine.emu.adapters.AdapterPreset
import com.micewine.emu.adapters.AdapterPreset.Companion.selectedPresetId
import com.micewine.emu.fragments.CreatePresetFragment.Companion.BOX64_PRESET
import java.io.File
import java.util.Collections

class Box64PresetManagerFragment : Fragment() {
    private var rootView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_general_settings, container, false)
        recyclerView = rootView?.findViewById(R.id.recyclerViewGeneralSettings)

        initialize(requireContext())
        setAdapter()

        return rootView
    }

    private fun setAdapter() {
        recyclerView?.setAdapter(AdapterPreset(presetListNames, requireContext(), requireActivity().supportFragmentManager))

        presetListNames.clear()
        presetList.forEach {
            addToAdapter(it[0], BOX64_PRESET, true)
        }
    }

    private fun addToAdapter(titleSettings: String, type: Int, userPreset: Boolean) {
        presetListNames.add(
            AdapterPreset.Item(titleSettings, type, userPreset)
        )
    }

    companion object {
        private var recyclerView: RecyclerView? = null
        private val presetListNames: MutableList<AdapterPreset.Item> = mutableListOf()
        private var presetList: MutableList<MutableList<String>> = mutableListOf()
        private var preferences: SharedPreferences? = null

        private val gson = Gson()

        private val mappingMap = mapOf(
            BOX64_MMAP32 to 1,
            BOX64_AVX to 2,
            BOX64_SSE42 to 3,
            BOX64_DYNAREC_BIGBLOCK to 4,
            BOX64_DYNAREC_STRONGMEM to 5,
            BOX64_DYNAREC_WEAKBARRIER to 6,
            BOX64_DYNAREC_PAUSE to 7,
            BOX64_DYNAREC_X87DOUBLE to 8,
            BOX64_DYNAREC_FASTNAN to 9,
            BOX64_DYNAREC_FASTROUND to 10,
            BOX64_DYNAREC_SAFEFLAGS to 11,
            BOX64_DYNAREC_CALLRET to 12,
            BOX64_DYNAREC_ALIGNED_ATOMICS to 13,
            BOX64_DYNAREC_NATIVEFLAGS to 14,
            BOX64_DYNAREC_WAIT to 15,
            BOX64_DYNAREC_DIRTY to 16,
            BOX64_DYNAREC_FORWARD to 17,
        )

        fun initialize(context: Context) {
            preferences = PreferenceManager.getDefaultSharedPreferences(context)
            presetList = getBox64Presets()
        }

        fun getBox64Mapping(name: String, key: String): List<String> {
            val index = presetList.indexOfFirst { it[0] == name }

            if (index == -1) {
                return listOf("")
            }

            return presetList[index][mappingMap[key]!!].split(":")
        }

        fun editBox64Mapping(name: String, key: String, value: String) {
            var index = presetList.indexOfFirst { it[0] == name }

            if (index == -1) {
                presetList[0][0] = name
                index = 0
            }

            presetList[index][mappingMap[key]!!] = value

            saveBox64Preset()
        }

        fun addBox64Preset(context: Context, name: String) {
            if (presetListNames.firstOrNull { it.titleSettings == name } != null) {
                Toast.makeText(context, context.getString(R.string.executable_already_added), Toast.LENGTH_LONG).show()
                return
            }

            val defaultPreset = ArrayList(Collections.nCopies(mappingMap.size + 1, ":")).apply {
                this[0] = name
                this[mappingMap[BOX64_MMAP32]!!] = "true"
                this[mappingMap[BOX64_AVX]!!] = "2"
                this[mappingMap[BOX64_SSE42]!!] = "true"
                this[mappingMap[BOX64_DYNAREC_BIGBLOCK]!!] = "2"
                this[mappingMap[BOX64_DYNAREC_STRONGMEM]!!] = "1"
                this[mappingMap[BOX64_DYNAREC_WEAKBARRIER]!!] = "2"
                this[mappingMap[BOX64_DYNAREC_PAUSE]!!] = "0"
                this[mappingMap[BOX64_DYNAREC_X87DOUBLE]!!] = "false"
                this[mappingMap[BOX64_DYNAREC_FASTNAN]!!] = "true"
                this[mappingMap[BOX64_DYNAREC_FASTROUND]!!] = "true"
                this[mappingMap[BOX64_DYNAREC_SAFEFLAGS]!!] = "1"
                this[mappingMap[BOX64_DYNAREC_CALLRET]!!] = "true"
                this[mappingMap[BOX64_DYNAREC_ALIGNED_ATOMICS]!!] = "false"
                this[mappingMap[BOX64_DYNAREC_NATIVEFLAGS]!!] = "true"
                this[mappingMap[BOX64_DYNAREC_WAIT]!!] = "true"
                this[mappingMap[BOX64_DYNAREC_DIRTY]!!] = "false"
                this[mappingMap[BOX64_DYNAREC_FORWARD]!!] = "128"
            }

            presetList.add(defaultPreset)
            presetListNames.add(
                AdapterPreset.Item(name, BOX64_PRESET, true)
            )

            recyclerView?.adapter?.notifyItemInserted(presetListNames.size)

            saveBox64Preset()
        }

        fun deleteBox64Preset(name: String) {
            val index = presetList.indexOfFirst { it[0] == name }

            presetList.removeAt(index)
            presetListNames.removeAt(index)

            recyclerView?.adapter?.notifyItemRemoved(index)

            if (index == selectedPresetId) {
                preferences?.edit {
                    putString(SELECTED_BOX64_PRESET_KEY, presetListNames.first().titleSettings)
                    apply()
                }
                recyclerView?.adapter?.notifyItemChanged(0)
            }

            saveBox64Preset()
        }

        fun renameBox64Preset(name: String, newName: String) {
            val index = presetList.indexOfFirst { it[0] == name }

            presetList[index][0] = newName
            presetListNames[index].titleSettings = newName

            recyclerView?.adapter?.notifyItemChanged(index)

            saveBox64Preset()
        }

        fun importBox64Preset(activity: Activity, path: String) {
            val json = File(path).readLines()
            val listType = object : TypeToken<MutableList<String>>() {}.type

            if (json.size < 2 || json[0] != "box64Preset") {
                activity.runOnUiThread {
                    Toast.makeText(activity, activity.getString(R.string.invalid_box64_preset_file), Toast.LENGTH_LONG).show()
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
                AdapterPreset.Item(processed[0], BOX64_PRESET, true)
            )

            activity.runOnUiThread {
                recyclerView?.adapter?.notifyItemInserted(presetListNames.size)
            }

            saveBox64Preset()
        }

        fun exportBox64Preset(context: Context, name: String, path: String) {
            val index = presetList.indexOfFirst { it[0] == name }
            val file = File(path)

            file.writeText("box64Preset\n" + gson.toJson(presetList[index]))

            Toast.makeText(context, "Box64 Preset '$name' exported", Toast.LENGTH_LONG).show()
        }

        private fun saveBox64Preset() {
            preferences?.edit {
                putString("box64PresetList", gson.toJson(presetList))
                apply()
            }
        }

        fun getBox64Presets(): MutableList<MutableList<String>> {
            val json = preferences?.getString("box64PresetList", "")
            val listType = object : TypeToken<MutableList<MutableList<String>>>() {}.type

            return gson.fromJson(json, listType) ?: mutableListOf(
                ArrayList(Collections.nCopies(mappingMap.size + 1, ":")).apply {
                    this[0] = "default"
                    this[mappingMap[BOX64_MMAP32]!!] = "true"
                    this[mappingMap[BOX64_AVX]!!] = "2"
                    this[mappingMap[BOX64_SSE42]!!] = "true"
                    this[mappingMap[BOX64_DYNAREC_BIGBLOCK]!!] = "2"
                    this[mappingMap[BOX64_DYNAREC_STRONGMEM]!!] = "1"
                    this[mappingMap[BOX64_DYNAREC_WEAKBARRIER]!!] = "2"
                    this[mappingMap[BOX64_DYNAREC_PAUSE]!!] = "0"
                    this[mappingMap[BOX64_DYNAREC_X87DOUBLE]!!] = "false"
                    this[mappingMap[BOX64_DYNAREC_FASTNAN]!!] = "true"
                    this[mappingMap[BOX64_DYNAREC_FASTROUND]!!] = "true"
                    this[mappingMap[BOX64_DYNAREC_SAFEFLAGS]!!] = "1"
                    this[mappingMap[BOX64_DYNAREC_CALLRET]!!] = "true"
                    this[mappingMap[BOX64_DYNAREC_ALIGNED_ATOMICS]!!] = "false"
                    this[mappingMap[BOX64_DYNAREC_NATIVEFLAGS]!!] = "true"
                    this[mappingMap[BOX64_DYNAREC_WAIT]!!] = "true"
                    this[mappingMap[BOX64_DYNAREC_DIRTY]!!] = "false"
                    this[mappingMap[BOX64_DYNAREC_FORWARD]!!] = "128"
                }
            )
        }
    }
}
