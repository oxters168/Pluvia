package com.micewine.emu.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.micewine.emu.R
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.SELECTED_WINE_PREFIX
import com.micewine.emu.activities.MainActivity.Companion.selectedFile
import com.micewine.emu.activities.MainActivity.Companion.selectedFragmentId
import com.micewine.emu.activities.MainActivity.Companion.setupDone
import com.micewine.emu.activities.MainActivity.Companion.winePrefix
import com.micewine.emu.activities.MainActivity.Companion.winePrefixesDir
import com.micewine.emu.adapters.AdapterGame.Companion.selectedGameName
import com.micewine.emu.adapters.AdapterPreset.Companion.PHYSICAL_CONTROLLER
import com.micewine.emu.adapters.AdapterPreset.Companion.VIRTUAL_CONTROLLER
import com.micewine.emu.adapters.AdapterPreset.Companion.clickedPresetName
import com.micewine.emu.adapters.AdapterPreset.Companion.clickedPresetType
import com.micewine.emu.core.ShellLoader.runCommand
import com.micewine.emu.fragments.Box64PresetManagerFragment.Companion.deleteBox64Preset
import com.micewine.emu.fragments.ControllerPresetManagerFragment.Companion.deleteControllerPreset
import com.micewine.emu.fragments.CreatePresetFragment.Companion.BOX64_PRESET
import com.micewine.emu.fragments.FileManagerFragment.Companion.deleteFile
import com.micewine.emu.fragments.SetupFragment.Companion.dialogTitleText
import com.micewine.emu.fragments.SetupFragment.Companion.progressBarIsIndeterminate
import com.micewine.emu.fragments.ShortcutsFragment.Companion.ACTION_UPDATE_WINE_PREFIX_SPINNER
import com.micewine.emu.fragments.ShortcutsFragment.Companion.removeGameFromList
import com.micewine.emu.fragments.VirtualControllerPresetManagerFragment.Companion.deleteVirtualControllerPreset

class DeleteItemFragment(private val deleteType: Int, private val context: Context) : DialogFragment() {
    private var preferences: SharedPreferences? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.fragment_delete_item, null)

        val buttonContinue = view.findViewById<Button>(R.id.buttonContinue)
        val buttonCancel = view.findViewById<Button>(R.id.buttonCancel)

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog).setView(view).create()

        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        buttonContinue.setOnClickListener {
            when (deleteType) {
                DELETE_GAME_ITEM -> {
                    if (selectedFragmentId == 0) {
                        removeGameFromList(selectedGameName)
                    } else if (selectedFragmentId == 2) {
                        deleteFile(selectedFile)
                    }
                }
                DELETE_PRESET -> {
                    when (clickedPresetType) {
                        PHYSICAL_CONTROLLER -> deleteControllerPreset(clickedPresetName)
                        VIRTUAL_CONTROLLER -> deleteVirtualControllerPreset(clickedPresetName)
                        BOX64_PRESET -> deleteBox64Preset(clickedPresetName)
                    }
                }
                DELETE_WINE_PREFIX -> {
                    if (winePrefixesDir.listFiles()!!.count() == 1) {
                        Toast.makeText(context, getText(R.string.remove_last_wine_prefix_error), Toast.LENGTH_LONG).show()
                        dismiss()
                        return@setOnClickListener
                    }

                    val firstPrefix = winePrefixesDir.listFiles()!!.first()

                    preferences!!.edit().apply {
                        putString(SELECTED_WINE_PREFIX, firstPrefix.name)
                        apply()
                    }

                    Thread {
                        setupDone = false

                        SetupFragment().show(requireActivity().supportFragmentManager, "")

                        dialogTitleText = getString(R.string.deleting_wine_prefix)
                        progressBarIsIndeterminate = true

                        runCommand("rm -rf $winePrefix")

                        setupDone = true

                        context.sendBroadcast(
                            Intent(ACTION_UPDATE_WINE_PREFIX_SPINNER)
                        )
                    }.start()
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
        const val DELETE_GAME_ITEM = 0
        const val DELETE_PRESET = 1
        const val DELETE_WINE_PREFIX = 2
    }
}