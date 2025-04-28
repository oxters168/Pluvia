package com.micewine.emu.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.micewine.emu.R
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_LOG
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_LOG_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_NOSIGILL
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_NOSIGILL_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_NOSIGSEGV
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_NOSIGSEGV_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_SHOWBT
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_SHOWBT_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_SHOWSEGV
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_SHOWSEGV_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.SPINNER
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.SWITCH
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.WINE_LOG_LEVEL
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.WINE_LOG_LEVEL_DEFAULT_VALUE
import com.micewine.emu.activities.MainActivity.Companion.CPU_COUNTER
import com.micewine.emu.activities.MainActivity.Companion.CPU_COUNTER_DEFAULT_VALUE
import com.micewine.emu.activities.MainActivity.Companion.ENABLE_DEBUG_INFO
import com.micewine.emu.activities.MainActivity.Companion.ENABLE_DEBUG_INFO_DEFAULT_VALUE
import com.micewine.emu.activities.MainActivity.Companion.RAM_COUNTER
import com.micewine.emu.activities.MainActivity.Companion.RAM_COUNTER_DEFAULT_VALUE
import com.micewine.emu.adapters.AdapterSettingsPreferences
import com.micewine.emu.adapters.AdapterSettingsPreferences.SettingsListSpinner

class DebugSettingsFragment : Fragment() {
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

        addToAdapter(R.string.wine_log_level_title, R.string.wine_log_level_description, arrayOf("disabled", "default"),
            SPINNER, WINE_LOG_LEVEL_DEFAULT_VALUE, WINE_LOG_LEVEL
        )
        addToAdapter(R.string.box64_log_title, R.string.box64_log_description, arrayOf("0", "1"),
            SPINNER, BOX64_LOG_DEFAULT_VALUE, BOX64_LOG
        )
        addToAdapter(R.string.box64_show_segv_title, R.string.box64_show_segv_description, null,
            SWITCH, BOX64_SHOWSEGV_DEFAULT_VALUE, BOX64_SHOWSEGV
        )
        addToAdapter(R.string.box64_no_sigsegv_title, R.string.box64_no_sigsegv_description, null,
            SWITCH, BOX64_NOSIGSEGV_DEFAULT_VALUE, BOX64_NOSIGSEGV
        )
        addToAdapter(R.string.box64_no_sigill_title, R.string.box64_no_sigill_description, null,
            SWITCH, BOX64_NOSIGILL_DEFAULT_VALUE, BOX64_NOSIGILL
        )
        addToAdapter(R.string.box64_show_bt_title, R.string.box64_show_bt_description, null,
            SWITCH, BOX64_SHOWBT_DEFAULT_VALUE, BOX64_SHOWBT
        )
        addToAdapter(R.string.enable_ram_counter, R.string.enable_ram_counter_description, null,
            SWITCH, RAM_COUNTER_DEFAULT_VALUE, RAM_COUNTER
        )
        addToAdapter(R.string.enable_cpu_counter, R.string.enable_cpu_counter_description, null,
            SWITCH, CPU_COUNTER_DEFAULT_VALUE, CPU_COUNTER
        )
        addToAdapter(R.string.enable_debug_info, R.string.enable_debug_info_description, null,
            SWITCH, ENABLE_DEBUG_INFO_DEFAULT_VALUE, ENABLE_DEBUG_INFO
        )
    }

    private fun addToAdapter(titleId: Int, descriptionId: Int, valuesArray: Array<String>?, type: Int, defaultValue: Any, keyId: String) {
        settingsList.add(SettingsListSpinner(titleId, descriptionId, valuesArray, null, type, "$defaultValue", keyId))
    }

    companion object {
        val availableCPUs = (0 until Runtime.getRuntime().availableProcessors()).map { it.toString() }.toTypedArray()
    }
}
