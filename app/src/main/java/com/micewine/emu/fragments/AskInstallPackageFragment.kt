package com.micewine.emu.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.micewine.emu.R
import com.micewine.emu.activities.MainActivity.Companion.ACTION_INSTALL_ADTOOLS_DRIVER
import com.micewine.emu.activities.MainActivity.Companion.ACTION_INSTALL_RAT
import com.micewine.emu.adapters.AdapterPreset.Companion.PHYSICAL_CONTROLLER
import com.micewine.emu.adapters.AdapterPreset.Companion.VIRTUAL_CONTROLLER
import com.micewine.emu.core.RatPackageManager
import com.micewine.emu.fragments.Box64PresetManagerFragment.Companion.importBox64Preset
import com.micewine.emu.fragments.ControllerPresetManagerFragment.Companion.importControllerPreset
import com.micewine.emu.fragments.CreatePresetFragment.Companion.BOX64_PRESET
import com.micewine.emu.fragments.VirtualControllerPresetManagerFragment.Companion.importVirtualControllerPreset

class AskInstallPackageFragment(private val packageType: Int) : DialogFragment() {
    private var preferences: SharedPreferences? = null

    @SuppressLint("SetTextI18n")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.fragment_ask_install_rat, null)

        val buttonContinue = view.findViewById<Button>(R.id.buttonContinue)
        val buttonCancel = view.findViewById<Button>(R.id.buttonCancel)

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog).setView(view).create()

        val askInstallText = view.findViewById<TextView>(R.id.askInstallText)

        when (packageType) {
            RAT_PACKAGE -> {
                askInstallText.text = "${activity?.getString(R.string.install_rat_package_warning)} ${ratCandidate?.name} (${ratCandidate?.version})?"
            }
            ADTOOLS_DRIVER_PACKAGE -> {
                askInstallText.text = "${activity?.getString(R.string.install_rat_package_warning)} ${adToolsDriverCandidate?.name} (${adToolsDriverCandidate?.version})?"
            }
            MWP_PRESET_PACKAGE -> {
                askInstallText.text = "${activity?.getString(R.string.install_rat_package_warning)} ${mwpPresetCandidate?.second?.substringAfterLast("/")}?"
            }
        }

        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        buttonContinue.setOnClickListener {
            when (packageType) {
                RAT_PACKAGE -> {
                    context?.sendBroadcast(
                        Intent(ACTION_INSTALL_RAT)
                    )
                }
                ADTOOLS_DRIVER_PACKAGE -> {
                    context?.sendBroadcast(
                        Intent(ACTION_INSTALL_ADTOOLS_DRIVER)
                    )
                }
                MWP_PRESET_PACKAGE -> {
                    when (mwpPresetCandidate?.first) {
                        VIRTUAL_CONTROLLER -> {
                            importVirtualControllerPreset(requireActivity(), mwpPresetCandidate?.second!!)
                        }
                        PHYSICAL_CONTROLLER -> {
                            importControllerPreset(requireActivity(), mwpPresetCandidate?.second!!)
                        }
                        BOX64_PRESET -> {
                            importBox64Preset(requireActivity(), mwpPresetCandidate?.second!!)
                        }
                    }
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
        var ratCandidate: RatPackageManager.RatPackage? = null
        var adToolsDriverCandidate: RatPackageManager.AdrenoToolsPackage? = null
        var mwpPresetCandidate: Pair<Int, String>? = null

        const val RAT_PACKAGE = 0
        const val ADTOOLS_DRIVER_PACKAGE = 1
        const val MWP_PRESET_PACKAGE = 2
    }
}