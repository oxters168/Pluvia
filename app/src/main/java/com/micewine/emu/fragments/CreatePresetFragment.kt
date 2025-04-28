package com.micewine.emu.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.micewine.emu.R
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.SELECTED_WINE_PREFIX
import com.micewine.emu.activities.MainActivity.Companion.ACTION_CREATE_WINE_PREFIX
import com.micewine.emu.activities.MainActivity.Companion.ratPackagesDir
import com.micewine.emu.activities.MainActivity.Companion.setSharedVars
import com.micewine.emu.activities.MainActivity.Companion.winePrefix
import com.micewine.emu.fragments.Box64PresetManagerFragment.Companion.addBox64Preset
import com.micewine.emu.fragments.ControllerPresetManagerFragment.Companion.addControllerPreset
import com.micewine.emu.fragments.VirtualControllerPresetManagerFragment.Companion.addVirtualControllerPreset
import java.io.File

class CreatePresetFragment(private val presetType: Int) : DialogFragment() {
    var preferences: SharedPreferences? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.fragment_create_preset, null)

        val editTextNewName = view.findViewById<EditText>(R.id.editTextNewName)
        val buttonContinue = view.findViewById<Button>(R.id.buttonContinue)
        val buttonCancel = view.findViewById<Button>(R.id.buttonCancel)
        val wineVersionText = view.findViewById<TextView>(R.id.wineVersionText)
        val wineVersionSpinner = view.findViewById<Spinner>(R.id.wineVersionSpinner)
        val wineVersionsNames: MutableList<String> = mutableListOf()
        val wineVersions: MutableList<String> = mutableListOf()

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog).setView(view).create()

        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())!!

        when (presetType) {
            WINEPREFIX_PRESET -> {
                wineVersionText.visibility = View.VISIBLE
                wineVersionSpinner.visibility = View.VISIBLE

                ratPackagesDir.listFiles()?.sorted()?.forEach {
                    if (it.isDirectory && it.name.startsWith("Wine-")) {
                        val wineName = File("$it/pkg-header").readLines()[0].substringAfter("=")

                        wineVersionsNames.add(wineName)
                        wineVersions.add(it.name)
                    }
                }

                wineVersionSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, wineVersionsNames)
            }
            CONTROLLER_PRESET, VIRTUAL_CONTROLLER_PRESET, BOX64_PRESET -> {
                wineVersionText.visibility = View.GONE
                wineVersionSpinner.visibility = View.GONE
            }
        }

        buttonContinue.setOnClickListener {
            val newName = editTextNewName.text.toString()

            if (newName == "") {
                dismiss()
            }

            when (presetType) {
                WINEPREFIX_PRESET -> {
                    preferences!!.edit().apply {
                        putString(SELECTED_WINE_PREFIX, newName)
                        apply()
                    }

                    setSharedVars(requireActivity())

                    val createWinePrefixIntent = Intent(ACTION_CREATE_WINE_PREFIX).apply {
                        putExtra("winePrefix", winePrefix?.path)
                        putExtra("wine", wineVersions[wineVersionSpinner.selectedItemPosition])
                    }

                    requireContext().sendBroadcast(createWinePrefixIntent)
                }
                CONTROLLER_PRESET -> {
                    addControllerPreset(requireContext(), newName)
                }
                VIRTUAL_CONTROLLER_PRESET -> {
                    addVirtualControllerPreset(requireContext(), newName)
                }
                BOX64_PRESET -> {
                    addBox64Preset(requireContext(), newName)
                }
            }

            dismiss()
        }

        buttonCancel.setOnClickListener {
            dismiss()
        }

        return dialog
    }

    companion object {
        const val WINEPREFIX_PRESET = 1
        const val CONTROLLER_PRESET = 2
        const val VIRTUAL_CONTROLLER_PRESET = 3
        const val BOX64_PRESET = 4
    }
}