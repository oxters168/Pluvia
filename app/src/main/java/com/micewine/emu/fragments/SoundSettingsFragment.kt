package com.micewine.emu.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.micewine.emu.R
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.PA_SINK
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.PA_SINK_DEFAULT_VALUE
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.SPINNER
import com.micewine.emu.activities.MainActivity.Companion.paSink
import com.micewine.emu.activities.MainActivity.Companion.usrDir
import com.micewine.emu.adapters.AdapterSettingsPreferences
import com.micewine.emu.adapters.AdapterSettingsPreferences.SettingsListSpinner
import java.io.File

class SoundSettingsFragment : Fragment() {
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

        addToAdapter(R.string.select_audio_sink, R.string.null_description, arrayOf("SLES", "AAudio"), SPINNER, PA_SINK_DEFAULT_VALUE, PA_SINK)
    }

    private fun addToAdapter(titleId: Int, descriptionId: Int, valuesArray: Array<String>?, type: Int, defaultValue: Any, keyId: String) {
        settingsList.add(SettingsListSpinner(titleId, descriptionId, valuesArray, null, type, "$defaultValue", keyId))
    }

    companion object {
        fun generatePAFile() {
            val paFile = File("$usrDir/etc/pulse/default.pa")

            paFile.writeText("" +
                    "#!/data/data/com.micewine.emu/files/usr/bin/pulseaudio -nF\n" +
                    ".fail\n" +
                    "\n" +
                    "load-module module-device-restore\n" +
                    "load-module module-stream-restore\n" +
                    "load-module module-card-restore\n" +
                    "load-module module-augment-properties\n" +
                    "load-module module-switch-on-port-available\n" +
                    "\n" +
                    ".ifexists module-esound-protocol-unix.so\n" +
                    "load-module module-esound-protocol-unix\n" +
                    ".endif\n" +
                    "load-module module-native-protocol-unix\n" +
                    "load-module module-default-device-restore\n" +
                    "load-module module-always-sink\n" +
                    "load-module module-intended-roles\n" +
                    "load-module module-position-event-sounds\n" +
                    "load-module module-role-cork\n" +
                    "load-module module-filter-heuristics\n" +
                    "load-module module-filter-apply\n" +
                    "\n" +
                    ".nofail\n" +
                    ".include /data/data/com.micewine.emu/files/usr/etc/pulse/default.pa.d\n" +
                    "\n" +
                    "load-module module-$paSink-sink\n"
            )
        }
    }
}
