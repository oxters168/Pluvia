package com.micewine.emu.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.micewine.emu.R
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_AVX
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_AVX_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_ALIGNED_ATOMICS
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_ALIGNED_ATOMICS_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_BIGBLOCK
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_BIGBLOCK_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_CALLRET
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_CALLRET_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_DIRTY
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_DIRTY_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_FASTNAN
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_FASTNAN_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_FASTROUND
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_FASTROUND_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_FORWARD
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_FORWARD_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_NATIVEFLAGS
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_NATIVEFLAGS_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_PAUSE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_PAUSE_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_SAFEFLAGS
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_SAFEFLAGS_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_STRONGMEM
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_STRONGMEM_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_WAIT
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_WAIT_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_WEAKBARRIER
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_WEAKBARRIER_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_X87DOUBLE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_DYNAREC_X87DOUBLE_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_MMAP32
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_MMAP32_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_SSE42
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.BOX64_SSE42_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.SPINNER
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.SWITCH
import com.micewine.emu.adapters.AdapterSettingsBox64Preset

class Box64SettingsFragment : Fragment() {
    private val settingsList: MutableList<AdapterSettingsBox64Preset.Box64ListSpinner> = ArrayList()
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
        val adapterSettingsPreferences = AdapterSettingsBox64Preset(settingsList, requireActivity())

        recyclerView?.setAdapter(adapterSettingsPreferences)

        settingsList.clear()

        addToAdapter(R.string.box64_bigblock_title, R.string.box64_bigblock_description, arrayOf("0", "1", "2", "3"), SPINNER, BOX64_DYNAREC_BIGBLOCK_DEFAULT_VALUE, BOX64_DYNAREC_BIGBLOCK)
        addToAdapter(R.string.box64_strongmem_title, R.string.box64_strongmem_description, arrayOf("0", "1", "2", "3"), SPINNER, BOX64_DYNAREC_STRONGMEM_DEFAULT_VALUE, BOX64_DYNAREC_STRONGMEM)
        addToAdapter(R.string.box64_weakbarrier_title, R.string.box64_weakbarrier_description, arrayOf("0", "1", "2"), SPINNER, BOX64_DYNAREC_WEAKBARRIER_DEFAULT_VALUE, BOX64_DYNAREC_WEAKBARRIER)
        addToAdapter(R.string.box64_pause_title, R.string.box64_pause_description, arrayOf("0", "1", "2", "3"), SPINNER, BOX64_DYNAREC_PAUSE_DEFAULT_VALUE, BOX64_DYNAREC_PAUSE)
        addToAdapter(R.string.box64_x87double_title, R.string.box64_x87double_description, null, SWITCH, BOX64_DYNAREC_X87DOUBLE_DEFAULT_VALUE, BOX64_DYNAREC_X87DOUBLE)
        addToAdapter(R.string.box64_fastnan_title, R.string.box64_fastnan_description, null, SWITCH, BOX64_DYNAREC_FASTNAN_DEFAULT_VALUE, BOX64_DYNAREC_FASTNAN)
        addToAdapter(R.string.box64_fastround_title, R.string.box64_fastround_description, null, SWITCH, BOX64_DYNAREC_FASTROUND_DEFAULT_VALUE, BOX64_DYNAREC_FASTROUND)
        addToAdapter(R.string.box64_safeflags_title, R.string.box64_safeflags_description, arrayOf("0", "1", "2"), SPINNER, BOX64_DYNAREC_SAFEFLAGS_DEFAULT_VALUE, BOX64_DYNAREC_SAFEFLAGS)
        addToAdapter(R.string.box64_callret_title, R.string.box64_callret_description, null, SWITCH, BOX64_DYNAREC_CALLRET_DEFAULT_VALUE, BOX64_DYNAREC_CALLRET)
        addToAdapter(R.string.box64_aligned_atomics_title, R.string.box64_aligned_atomics_description, null, SWITCH, BOX64_DYNAREC_ALIGNED_ATOMICS_DEFAULT_VALUE, BOX64_DYNAREC_ALIGNED_ATOMICS)
        addToAdapter(R.string.box64_nativeflags_title, R.string.box64_nativeflags_description, null, SWITCH, BOX64_DYNAREC_NATIVEFLAGS_DEFAULT_VALUE, BOX64_DYNAREC_NATIVEFLAGS)
        addToAdapter(R.string.box64_dynarec_wait_title, R.string.box64_dynarec_wait_description, null, SWITCH, BOX64_DYNAREC_WAIT_DEFAULT_VALUE, BOX64_DYNAREC_WAIT)
        addToAdapter(R.string.box64_dynarec_dirty_title, R.string.box64_dynarec_dirty_description, null, SWITCH, BOX64_DYNAREC_DIRTY_DEFAULT_VALUE, BOX64_DYNAREC_DIRTY)
        addToAdapter(R.string.box64_dynarec_forward_title, R.string.box64_dynarec_forward_description, arrayOf("0", "128", "256", "512", "1024"), SPINNER, BOX64_DYNAREC_FORWARD_DEFAULT_VALUE, BOX64_DYNAREC_FORWARD)
        addToAdapter(R.string.box64_avx_title, R.string.box64_avx_description, arrayOf("0", "1", "2"), SPINNER, BOX64_AVX_DEFAULT_VALUE, BOX64_AVX)
        addToAdapter(R.string.box64_sse42_title, R.string.box64_sse42_description, null, SWITCH, BOX64_SSE42_DEFAULT_VALUE, BOX64_SSE42)
        addToAdapter(R.string.box64_mmap32_title, R.string.box64_mmap32_description, null, SWITCH, BOX64_MMAP32_DEFAULT_VALUE, BOX64_MMAP32)
    }

    private fun addToAdapter(titleId: Int, descriptionId: Int, valuesArray: Array<String>?, type: Int, defaultValue: Any, keyId: String) {
        settingsList.add(
            AdapterSettingsBox64Preset.Box64ListSpinner(
                titleId,
                descriptionId,
                valuesArray,
                type,
                keyId
            )
        )
    }
}
