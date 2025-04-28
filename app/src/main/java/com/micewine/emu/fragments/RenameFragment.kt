package com.micewine.emu.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.micewine.emu.R
import com.micewine.emu.activities.MainActivity.Companion.selectedFile
import com.micewine.emu.adapters.AdapterPreset.Companion.PHYSICAL_CONTROLLER
import com.micewine.emu.adapters.AdapterPreset.Companion.VIRTUAL_CONTROLLER
import com.micewine.emu.adapters.AdapterPreset.Companion.clickedPresetName
import com.micewine.emu.adapters.AdapterPreset.Companion.clickedPresetType
import com.micewine.emu.fragments.Box64PresetManagerFragment.Companion.renameBox64Preset
import com.micewine.emu.fragments.ControllerPresetManagerFragment.Companion.renameControllerPreset
import com.micewine.emu.fragments.CreatePresetFragment.Companion.BOX64_PRESET
import com.micewine.emu.fragments.FileManagerFragment.Companion.renameFile
import com.micewine.emu.fragments.VirtualControllerPresetManagerFragment.Companion.renameVirtualControllerPreset
import java.io.File

class RenameFragment(private val type: Int, private val initialText: String) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.fragment_rename_game_item, null)
        val editTextNewName = view.findViewById<EditText>(R.id.editTextNewName)
        val buttonContinue = view.findViewById<Button>(R.id.buttonContinue)
        val buttonCancel = view.findViewById<Button>(R.id.buttonCancel)
        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog).setView(view).create()

        editTextNewName.setText(initialText)
        buttonContinue.setOnClickListener {
            val newName = editTextNewName.text.toString()

            if (newName == "") {
                dismiss()
            }

            when (type) {
                RENAME_PRESET -> {
                    when (clickedPresetType) {
                        PHYSICAL_CONTROLLER -> renameControllerPreset(clickedPresetName, newName)
                        VIRTUAL_CONTROLLER -> renameVirtualControllerPreset(clickedPresetName, newName)
                        BOX64_PRESET -> renameBox64Preset(clickedPresetName, newName)
                    }
                }
                RENAME_FILE -> {
                    val file = File(selectedFile)
                    val newFile = file.parent!! + "/" + newName

                    renameFile(selectedFile, newFile)
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
        const val RENAME_PRESET = 0
        const val RENAME_FILE = 1
    }
}