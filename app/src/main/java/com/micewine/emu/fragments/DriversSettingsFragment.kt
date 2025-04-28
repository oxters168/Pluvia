package com.micewine.emu.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.micewine.emu.R
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.CHECKBOX
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.ENABLE_DRI3
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.ENABLE_DRI3_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.SELECTED_DXVK_HUD_PRESET
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.SELECTED_DXVK_HUD_PRESET_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.SELECTED_GL_PROFILE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.SELECTED_GL_PROFILE_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.SELECTED_MESA_VK_WSI_PRESENT_MODE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.SELECTED_MESA_VK_WSI_PRESENT_MODE_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.SELECTED_TU_DEBUG_PRESET
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.SELECTED_TU_DEBUG_PRESET_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.SPINNER
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.SWITCH
import com.micewine.emu.adapters.AdapterSettingsPreferences
import com.micewine.emu.adapters.AdapterSettingsPreferences.SettingsListSpinner

class DriversSettingsFragment : Fragment() {
    private val settingsList: MutableList<SettingsListSpinner> = ArrayList()
    private var rootView: View? = null
    private var recyclerView: RecyclerView? = null
    private var layoutManager: GridLayoutManager? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_settings_model, container, false)
        recyclerView = rootView?.findViewById(R.id.recyclerViewSettingsModel)

        layoutManager = recyclerView?.layoutManager as GridLayoutManager?
        layoutManager?.spanCount = 1

        setAdapter()

        return rootView
    }

    private fun setAdapter() {
        recyclerView?.setAdapter(AdapterSettingsPreferences(settingsList, requireActivity()))

        settingsList.clear()

        addToAdapter(R.string.enable_dri3, R.string.null_description, null, SWITCH, ENABLE_DRI3_DEFAULT_VALUE, ENABLE_DRI3)
        addToAdapter(R.string.select_dxvk_hud_preset_title, R.string.null_description, arrayOf("fps", "gpuload", "devinfo", "version", "api", "memory", "cs", "compiler", "allocations", "pipelines", "frametimes", "descriptors", "drawcalls", "submissions"), CHECKBOX, SELECTED_DXVK_HUD_PRESET_DEFAULT_VALUE, SELECTED_DXVK_HUD_PRESET)
        addToAdapter(R.string.mesa_vk_wsi_present_mode_title, R.string.null_description, arrayOf("fifo", "relaxed", "mailbox", "immediate"), SPINNER, SELECTED_MESA_VK_WSI_PRESENT_MODE_DEFAULT_VALUE, SELECTED_MESA_VK_WSI_PRESENT_MODE)
        addToAdapter(R.string.tu_debug_title, R.string.null_description, arrayOf("noconform", "flushall", "syncdraw", "sysmem", "gmem", "nolrz", "noubwc", "nomultipos", "forcebin"), CHECKBOX, SELECTED_TU_DEBUG_PRESET_DEFAULT_VALUE, SELECTED_TU_DEBUG_PRESET)
        addToAdapter(R.string.select_gl_profile_title, R.string.null_description, arrayOf(
            "GL 2.1", "GL 3.0",
            "GL 3.1", "GL 3.2",
            "GL 3.3", "GL 4.0",
            "GL 4.1", "GL 4.2",
            "GL 4.3", "GL 4.4",
            "GL 4.5", "GL 4.6"),
            SPINNER, SELECTED_GL_PROFILE_DEFAULT_VALUE, SELECTED_GL_PROFILE)
    }

    private fun addToAdapter(titleId: Int, descriptionId: Int, valuesArray: Array<String>?, type: Int, defaultValue: Any, keyId: String) {
        settingsList.add(
            SettingsListSpinner(titleId, descriptionId, valuesArray, null, type, "$defaultValue", keyId)
        )
    }
}
