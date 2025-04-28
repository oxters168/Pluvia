package com.micewine.emu.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.database.DataSetObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.SpinnerAdapter
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.google.android.material.materialswitch.MaterialSwitch
import com.micewine.emu.R
import com.micewine.emu.activities.EmulationActivity
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.SELECTED_BOX64
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.SELECTED_VULKAN_DRIVER
import com.micewine.emu.activities.MainActivity.Companion.ACTION_RUN_WINE
import com.micewine.emu.activities.MainActivity.Companion.ACTION_SELECT_ICON
import com.micewine.emu.activities.MainActivity.Companion.getNativeResolutions
import com.micewine.emu.activities.MainActivity.Companion.resolutions16_9
import com.micewine.emu.activities.MainActivity.Companion.resolutions4_3
import com.micewine.emu.activities.MainActivity.Companion.selectedCpuAffinity
import com.micewine.emu.activities.MainActivity.Companion.usrDir
import com.micewine.emu.adapters.AdapterGame.Companion.selectedGameName
import com.micewine.emu.controller.ControllerUtils.getGameControllerNames
import com.micewine.emu.core.RatPackageManager.getPackageNameVersionById
import com.micewine.emu.core.RatPackageManager.listRatPackages
import com.micewine.emu.core.RatPackageManager.listRatPackagesId
import com.micewine.emu.fragments.Box64PresetManagerFragment.Companion.getBox64Presets
import com.micewine.emu.fragments.ControllerPresetManagerFragment.Companion.getControllerPresets
import com.micewine.emu.fragments.DebugSettingsFragment.Companion.availableCPUs
import com.micewine.emu.fragments.ShortcutsFragment.Companion.editGameFromList
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getBox64Preset
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getBox64Version
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getControllerPreset
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getControllerXInput
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getCpuAffinity
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getD3DXRenderer
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getDXVKVersion
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getDisplaySettings
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getGameExeArguments
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getGameIcon
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getVKD3DVersion
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getVirtualControllerPreset
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getVirtualControllerXInput
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getVulkanDriver
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getWineD3DVersion
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getWineESync
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getWineServices
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getWineVirtualDesktop
import com.micewine.emu.fragments.ShortcutsFragment.Companion.putBox64Preset
import com.micewine.emu.fragments.ShortcutsFragment.Companion.putBox64Version
import com.micewine.emu.fragments.ShortcutsFragment.Companion.putControllerPreset
import com.micewine.emu.fragments.ShortcutsFragment.Companion.putControllerXInput
import com.micewine.emu.fragments.ShortcutsFragment.Companion.putCpuAffinity
import com.micewine.emu.fragments.ShortcutsFragment.Companion.putD3DXRenderer
import com.micewine.emu.fragments.ShortcutsFragment.Companion.putDXVKVersion
import com.micewine.emu.fragments.ShortcutsFragment.Companion.putDisplaySettings
import com.micewine.emu.fragments.ShortcutsFragment.Companion.putVKD3DVersion
import com.micewine.emu.fragments.ShortcutsFragment.Companion.putVirtualControllerPreset
import com.micewine.emu.fragments.ShortcutsFragment.Companion.putVirtualControllerXInput
import com.micewine.emu.fragments.ShortcutsFragment.Companion.putVulkanDriver
import com.micewine.emu.fragments.ShortcutsFragment.Companion.putWineD3DVersion
import com.micewine.emu.fragments.ShortcutsFragment.Companion.putWineESync
import com.micewine.emu.fragments.ShortcutsFragment.Companion.putWineServices
import com.micewine.emu.fragments.ShortcutsFragment.Companion.putWineVirtualDesktop
import com.micewine.emu.fragments.VirtualControllerPresetManagerFragment.Companion.getVirtualControllerPresets
import java.io.File

class EditGamePreferencesFragment(private val type: Int, private val exePath: File? = null) : DialogFragment() {
    private var preferences: SharedPreferences? = null
    private var imageView: ImageView? = null

    @SuppressLint("SetTextI18n")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.fragment_edit_game_preferences, null)

        val editTextNewName = view.findViewById<EditText>(R.id.editTextNewName)
        val editTextArguments = view.findViewById<EditText>(R.id.appArgumentsEditText)
        val buttonContinue = view.findViewById<Button>(R.id.buttonContinue)
        val buttonCancel = view.findViewById<Button>(R.id.buttonCancel)
        val selectedDisplayModeSpinner = view.findViewById<Spinner>(R.id.selectedDisplayMode)
        val selectedDisplayResolutionSpinner = view.findViewById<Spinner>(R.id.selectedDisplayResolution)
        val selectedDriverSpinner = view.findViewById<Spinner>(R.id.selectedDriver)
        val selectedD3DXRendererSpinner = view.findViewById<Spinner>(R.id.selectedD3DXRenderer)
        val selectedDXVKSpinner = view.findViewById<Spinner>(R.id.selectedDXVK)
        val selectedWineD3DSpinner = view.findViewById<Spinner>(R.id.selectedWineD3D)
        val selectedVKD3DSpinner = view.findViewById<Spinner>(R.id.selectedVKD3D)
        val wineESyncSwitch = view.findViewById<MaterialSwitch>(R.id.wineESync)
        val wineServicesSwitch = view.findViewById<MaterialSwitch>(R.id.wineServices)
        val enableWineVirtualDesktopSwitch = view.findViewById<MaterialSwitch>(R.id.enableWineVirtualDesktop)
        val enableController0XInputSwitch = view.findViewById<MaterialSwitch>(R.id.enableController0XInput)
        val enableController1XInputSwitch = view.findViewById<MaterialSwitch>(R.id.enableController1XInput)
        val enableController2XInputSwitch = view.findViewById<MaterialSwitch>(R.id.enableController2XInput)
        val enableController3XInputSwitch = view.findViewById<MaterialSwitch>(R.id.enableController3XInput)
        val cpuAffinitySpinner = view.findViewById<Spinner>(R.id.cpuAffinity)
        val controller0Layout = view.findViewById<LinearLayout>(R.id.controller0Layout)
        val controller1Layout = view.findViewById<LinearLayout>(R.id.controller1Layout)
        val controller2Layout = view.findViewById<LinearLayout>(R.id.controller2Layout)
        val controller3Layout = view.findViewById<LinearLayout>(R.id.controller3Layout)
        val connectedController0 = view.findViewById<TextView>(R.id.controller0)
        val selectedController0ProfileSpinner = view.findViewById<Spinner>(R.id.selectedController0Profile)
        val connectedController1 = view.findViewById<TextView>(R.id.controller1)
        val selectedController1ProfileSpinner = view.findViewById<Spinner>(R.id.selectedController1Profile)
        val connectedController2 = view.findViewById<TextView>(R.id.controller2)
        val selectedController2ProfileSpinner = view.findViewById<Spinner>(R.id.selectedController2Profile)
        val connectedController3 = view.findViewById<TextView>(R.id.controller3)
        val noControllersConnectedText = view.findViewById<TextView>(R.id.noControllersConnected)
        val selectedController3ProfileSpinner = view.findViewById<Spinner>(R.id.selectedController3Profile)
        val enableVirtualControllerXInputSwitch = view.findViewById<MaterialSwitch>(R.id.enableVirtualControllerXInput)
        val selectedVirtualControllerProfileSpinner = view.findViewById<Spinner>(R.id.selectedVirtualControllerProfile)
        val selectedBox64Spinner = view.findViewById<Spinner>(R.id.selectedBox64)
        val selectedBox64ProfileSpinner = view.findViewById<Spinner>(R.id.selectedBox64Profile)

        imageView = view.findViewById(R.id.imageView)

        when (type) {
            EDIT_GAME_PREFERENCES -> {
                val imageBitmap = getGameIcon(selectedGameName)
                if (imageBitmap != null) {
                    imageView?.setImageBitmap(
                        resizeBitmap(
                            imageBitmap, imageView?.layoutParams?.width!!, imageView?.layoutParams?.height!!
                        )
                    )
                }
            }
            FILE_MANAGER_START_PREFERENCES -> {
                val iconFile = File("$usrDir/icons/${exePath?.nameWithoutExtension}-icon")
                if (iconFile.exists() && iconFile.length() > 0) {
                    imageView?.setImageBitmap(BitmapFactory.decodeFile(iconFile.path))
                } else {
                    imageView?.setImageResource(R.drawable.ic_log)
                }
            }
        }

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog).setView(view).create()

        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        when (type) {
            EDIT_GAME_PREFERENCES -> {
                editTextNewName.setText(selectedGameName)
                editTextArguments.setText(getGameExeArguments(selectedGameName))

                if (selectedGameName == getString(R.string.desktop_mode_init)) {
                    editTextNewName.isEnabled = false
                    editTextArguments.isEnabled = false
                    enableWineVirtualDesktopSwitch.isEnabled = false
                    enableWineVirtualDesktopSwitch.isChecked = true

                    imageView?.setImageBitmap(
                        resizeBitmap(
                            BitmapFactory.decodeResource(requireActivity().resources, R.drawable.default_icon), imageView?.layoutParams?.width!!, imageView?.layoutParams?.height!!
                        )
                    )
                }

                imageView?.setOnClickListener {
                    requireActivity().sendBroadcast(
                        Intent(ACTION_SELECT_ICON)
                    )
                }
            }
            FILE_MANAGER_START_PREFERENCES -> {
                editTextNewName.setText(exePath?.nameWithoutExtension)
                editTextNewName.isEnabled = false
                editTextArguments.setText("")

                selectedGameName = ""
            }
        }

        val controllerProfilesNames: List<String> = getControllerPresets().map { it[0] }
        val virtualControllerProfilesNames: List<String> = getVirtualControllerPresets(requireContext()).map { it.name }
        val box64Versions: List<String> = listRatPackages("Box64-").map { it.name + " " + it.version }.toMutableList().apply { add(0, "Global: ${getPackageNameVersionById(preferences?.getString(SELECTED_BOX64, ""))}") }
        val box64VersionsId: List<String> = listRatPackagesId("Box64-").toMutableList().apply { add(0, "Global") }
        val box64ProfilesNames: List<String> = getBox64Presets().map { it[0] }

        selectedDisplayModeSpinner.apply {
            val aspectRatios = listOf("16:9", "4:3", "Native")
            val displaySettings = getDisplaySettings(selectedGameName)

            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, aspectRatios)
            setSelection(aspectRatios.indexOf(displaySettings[0]))

            onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedItem = parent?.getItemAtPosition(position).toString()
                    var resolutionList: Array<String>? = null

                    when (selectedItem) {
                        "16:9" -> {
                            resolutionList = resolutions16_9
                        }
                        "4:3" -> {
                            resolutionList = resolutions4_3
                        }
                        "Native" -> {
                            resolutionList = getNativeResolutions(requireActivity()).toTypedArray()
                        }
                    }

                    selectedDisplayResolutionSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, resolutionList!!)
                    selectedDisplayResolutionSpinner.setSelection(resolutionList.indexOf(displaySettings[1]))

                    temporarySettings.displayMode = selectedItem
                    temporarySettings.displayResolution = resolutionList.first()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
        selectedDisplayResolutionSpinner.apply {
            onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedItem = parent?.getItemAtPosition(position).toString()

                    temporarySettings.displayResolution = selectedItem
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }

        val vulkanDriversId = listRatPackagesId("VulkanDriver", "AdrenoToolsDriver").toMutableList().apply { add(0, "Global") }

        selectedDriverSpinner.apply {
            val vulkanDrivers = listRatPackages("VulkanDriver", "AdrenoToolsDriver").map { it.name + " " + it.version }.toMutableList().apply { add(0, "Global: ${getPackageNameVersionById(preferences?.getString(SELECTED_VULKAN_DRIVER, ""))}") }
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, vulkanDrivers)
            val index = vulkanDriversId.indexOf(getVulkanDriver(selectedGameName))
            setSelection(if (index == -1) 0 else index)

            onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    temporarySettings.vulkanDriver = vulkanDriversId[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
        selectedD3DXRendererSpinner.apply {
            val d3dxRenderers = listOf("DXVK", "WineD3D")
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, d3dxRenderers)
            setSelection(d3dxRenderers.indexOf(getD3DXRenderer(selectedGameName)))

            onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedItem = d3dxRenderers[position]

                    when (selectedItem) {
                        "DXVK" -> {
                            selectedWineD3DSpinner.isEnabled = false
                            selectedDXVKSpinner.isEnabled = true
                        }
                        "WineD3D" -> {
                            selectedDXVKSpinner.isEnabled = false
                            selectedWineD3DSpinner.isEnabled = true
                        }
                    }

                    temporarySettings.d3dxRenderer = selectedItem
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
        selectedDXVKSpinner.apply {
            val dxvkVersions = listRatPackages("DXVK").map { it.name + " " + it.version }
            val dxvkVersionsId = listRatPackagesId("DXVK")
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, dxvkVersions)
            val index = dxvkVersionsId.indexOf(getDXVKVersion(selectedGameName))
            setSelection(if (index == -1) 0 else index)

            onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    temporarySettings.dxvk = dxvkVersionsId[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
        selectedWineD3DSpinner.apply {
            val wineD3DVersions = listRatPackages("WineD3D").map { it.name + " " + it.version }
            val wineD3DVersionsId = listRatPackagesId("WineD3D")
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, wineD3DVersions)
            val index = wineD3DVersionsId.indexOf(getWineD3DVersion(selectedGameName))
            setSelection(if (index == -1) 0 else index)

            onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    temporarySettings.wineD3D = wineD3DVersionsId[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
        selectedVKD3DSpinner.apply {
            val vkd3dVersions = listRatPackages("VKD3D").map { it.name + " " + it.version }
            val vkd3dVersionsId = listRatPackagesId("VKD3D")
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, vkd3dVersions)
            val index = vkd3dVersionsId.indexOf(getVKD3DVersion(selectedGameName))
            setSelection(if (index == -1) 0 else index)

            onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    temporarySettings.vkd3d = vkd3dVersionsId[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
        wineESyncSwitch.apply {
            isChecked = getWineESync(selectedGameName)

            setOnClickListener {
                temporarySettings.wineESync = isChecked
            }
        }
        wineServicesSwitch.apply {
            isChecked = getWineServices(selectedGameName)

            setOnClickListener {
                temporarySettings.wineServices = isChecked
            }
        }

        if (selectedGameName != getString(R.string.desktop_mode_init)) {
            enableWineVirtualDesktopSwitch.apply {
                isChecked = getWineVirtualDesktop(selectedGameName)

                setOnClickListener {
                    temporarySettings.wineVirtualDesktop = isChecked
                }
            }
        }

        cpuAffinitySpinner.apply {
            adapter = CPUAffinityAdapter(requireActivity(), availableCPUs, cpuAffinitySpinner, type)
        }

        val connectedControllers = getGameControllerNames()
        val controllerTextViews = listOf(connectedController0, connectedController1, connectedController2, connectedController3)
        val controllerLayoutViews = listOf(controller0Layout, controller1Layout, controller2Layout, controller3Layout)

        controllerLayoutViews.forEachIndexed { index, layout ->
            layout.visibility = if (connectedControllers.size > index) View.VISIBLE else View.GONE
        }

        noControllersConnectedText.visibility = if (connectedControllers.isEmpty()) View.VISIBLE else View.GONE

        connectedControllers.forEachIndexed { index, controller ->
            if (index < controllerTextViews.size) {
                controllerTextViews[index].text = "$index: ${controller.name}"
            }
        }
        enableController0XInputSwitch.apply {
            isChecked = getControllerXInput(selectedGameName, 0)
            selectedController0ProfileSpinner.isEnabled = !isChecked

            setOnClickListener {
                selectedController0ProfileSpinner.isEnabled = !isChecked

                temporarySettings.controllerXInput[0] = isChecked
            }
        }
        enableController1XInputSwitch.apply {
            isChecked = getControllerXInput(selectedGameName, 1)
            selectedController1ProfileSpinner.isEnabled = !isChecked

            setOnClickListener {
                selectedController1ProfileSpinner.isEnabled = !isChecked

                temporarySettings.controllerXInput[1] = isChecked
            }
        }
        enableController2XInputSwitch.apply {
            isChecked = getControllerXInput(selectedGameName, 2)
            selectedController2ProfileSpinner.isEnabled = !isChecked

            setOnClickListener {
                selectedController2ProfileSpinner.isEnabled = !isChecked

                temporarySettings.controllerXInput[2] = isChecked
            }
        }
        enableController3XInputSwitch.apply {
            isChecked = getControllerXInput(selectedGameName, 3)
            selectedController3ProfileSpinner.isEnabled = !isChecked

            setOnClickListener {
                selectedController3ProfileSpinner.isEnabled = !isChecked

                temporarySettings.controllerXInput[3] = isChecked
            }
        }
        enableVirtualControllerXInputSwitch.apply {
            isChecked = getVirtualControllerXInput(selectedGameName)
            selectedVirtualControllerProfileSpinner.isEnabled = !isChecked

            setOnClickListener {
                selectedVirtualControllerProfileSpinner.isEnabled = !isChecked

                temporarySettings.virtualXInputController = isChecked
            }
        }
        selectedController0ProfileSpinner.apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, controllerProfilesNames)
            setSelection(controllerProfilesNames.indexOf(getControllerPreset(selectedGameName, 0)))

            onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    temporarySettings.controllerPreset[0] = controllerProfilesNames[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
        selectedController1ProfileSpinner.apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, controllerProfilesNames)
            setSelection(controllerProfilesNames.indexOf(getControllerPreset(selectedGameName, 1)))

            onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    temporarySettings.controllerPreset[1] = controllerProfilesNames[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
        selectedController2ProfileSpinner.apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, controllerProfilesNames)
            setSelection(controllerProfilesNames.indexOf(getControllerPreset(selectedGameName, 2)))

            onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    temporarySettings.controllerPreset[2] = controllerProfilesNames[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
        selectedController3ProfileSpinner.apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, controllerProfilesNames)
            setSelection(controllerProfilesNames.indexOf(getControllerPreset(selectedGameName, 3)))

            onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    temporarySettings.controllerPreset[3] = controllerProfilesNames[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
        selectedVirtualControllerProfileSpinner.apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, virtualControllerProfilesNames)
            setSelection(virtualControllerProfilesNames.indexOf(getVirtualControllerPreset(selectedGameName)))

            onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    temporarySettings.virtualControllerPreset = virtualControllerProfilesNames[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
        selectedBox64Spinner.apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, box64Versions)
            val index = box64VersionsId.indexOf(getBox64Version(selectedGameName))
            setSelection(if (index == -1) 0 else index)

            onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    temporarySettings.box64Version = box64VersionsId[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
        selectedBox64ProfileSpinner.apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, box64ProfilesNames)
            setSelection(box64ProfilesNames.indexOf(getBox64Preset(selectedGameName)))

            onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    temporarySettings.box64Preset = box64ProfilesNames[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
        buttonContinue.setOnClickListener {
            if (type == EDIT_GAME_PREFERENCES) {
                val newName = editTextNewName.text.toString()
                val newArguments = editTextArguments.text.toString()

                if (newName == "") {
                    return@setOnClickListener
                }

                temporarySettings.let {
                    putDisplaySettings(selectedGameName, it.displayMode, it.displayResolution)
                    putVulkanDriver(selectedGameName, it.vulkanDriver)
                    putD3DXRenderer(selectedGameName, it.d3dxRenderer)
                    putDXVKVersion(selectedGameName, it.dxvk)
                    putWineD3DVersion(selectedGameName, it.wineD3D)
                    putVKD3DVersion(selectedGameName, it.vkd3d)
                    putWineESync(selectedGameName, it.wineESync)
                    putWineServices(selectedGameName, it.wineServices)
                    putWineVirtualDesktop(selectedGameName, it.wineVirtualDesktop)
                    putCpuAffinity(selectedGameName, it.cpuAffinity)
                    putVirtualControllerPreset(selectedGameName, it.virtualControllerPreset)
                    putVirtualControllerXInput(selectedGameName, it.virtualXInputController)

                    putControllerXInput(selectedGameName, it.controllerXInput[0], 0)
                    putControllerXInput(selectedGameName, it.controllerXInput[1], 1)
                    putControllerXInput(selectedGameName, it.controllerXInput[2], 2)
                    putControllerXInput(selectedGameName, it.controllerXInput[3], 3)

                    putControllerPreset(selectedGameName, it.controllerPreset[0], 0)
                    putControllerPreset(selectedGameName, it.controllerPreset[1], 1)
                    putControllerPreset(selectedGameName, it.controllerPreset[2], 2)
                    putControllerPreset(selectedGameName, it.controllerPreset[3], 3)

                    putBox64Version(selectedGameName, it.box64Version)
                    putBox64Preset(selectedGameName, it.box64Preset)
                }

                editGameFromList(selectedGameName, newName, newArguments)
            } else if (type == FILE_MANAGER_START_PREFERENCES) {
                val runWineIntent = Intent(ACTION_RUN_WINE).apply {
                    putExtra("exePath", exePath?.path)
                    putExtra("exeArguments", editTextArguments.text.toString())
                    putExtra("driverName", vulkanDriversId[selectedDriverSpinner.selectedItemPosition])
                    putExtra("box64Version", box64VersionsId[selectedBox64Spinner.selectedItemPosition])
                    putExtra("box64Preset", selectedBox64ProfileSpinner.selectedItem.toString())
                    putExtra("displayResolution", selectedDisplayResolutionSpinner.selectedItem.toString())
                    putExtra("virtualControllerPreset", selectedVirtualControllerProfileSpinner.selectedItem.toString())
                    putExtra("controllerPreset", selectedController0ProfileSpinner.selectedItem.toString())
                    putExtra("d3dxRenderer", selectedD3DXRendererSpinner.selectedItem.toString())
                    putExtra("wineD3D", selectedWineD3DSpinner.selectedItem.toString())
                    putExtra("dxvk", selectedDXVKSpinner.selectedItem.toString())
                    putExtra("vkd3d", selectedVKD3DSpinner.selectedItem.toString())
                    putExtra("esync", wineESyncSwitch.isChecked)
                    putExtra("services", wineServicesSwitch.isChecked)
                    putExtra("virtualDesktop", enableWineVirtualDesktopSwitch.isChecked)
                    putExtra("cpuAffinity", selectedCpuAffinity)
                }

                requireActivity().sendBroadcast(runWineIntent)
                requireActivity().startActivity(
                    Intent(requireActivity(), EmulationActivity::class.java)
                )
            }

            dismiss()
        }

        buttonCancel.setOnClickListener {
            dismiss()
        }

        return dialog
    }

    override fun onResume() {
        val imageBitmap = getGameIcon(selectedGameName)
        if (imageBitmap != null) {
            imageView?.setImageBitmap(
                resizeBitmap(
                    imageBitmap, imageView?.layoutParams?.width!!, imageView?.layoutParams?.height!!
                )
            )
        }

        super.onResume()
    }

    private fun resizeBitmap(originalBitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(originalBitmap, width, height, false)
    }

    companion object {
        const val EDIT_GAME_PREFERENCES = 0
        const val FILE_MANAGER_START_PREFERENCES = 1

        val temporarySettings = TemporarySettings()

        class TemporarySettings(
            var displayMode: String = getDisplaySettings("")[0],
            var displayResolution: String = getDisplaySettings("")[1],
            var vulkanDriver: String = "",
            var d3dxRenderer: String = "",
            var dxvk: String = "",
            var wineD3D: String = "",
            var vkd3d: String = "",
            var wineESync: Boolean = getWineESync(""),
            var wineServices: Boolean = getWineServices(""),
            var wineVirtualDesktop: Boolean = getWineVirtualDesktop(""),
            var cpuAffinity: String = getCpuAffinity(""),
            var virtualControllerPreset: String = "",
            var virtualXInputController: Boolean = getVirtualControllerXInput(""),
            var controllerXInput: BooleanArray = booleanArrayOf(false, false, false, false),
            var controllerPreset: MutableList<String> = mutableListOf("", "", "", ""),
            var box64Version: String = getBox64Version(""),
            var box64Preset: String = getBox64Preset(""),
        )

        class CPUAffinityAdapter(
            val activity: Activity,
            private val arrayElements: Array<String>,
            private val spinner: Spinner,
            private val type: Int
        ) : SpinnerAdapter {
            val checked = BooleanArray(count)

            override fun registerDataSetObserver(p0: DataSetObserver?) {
            }

            override fun unregisterDataSetObserver(p0: DataSetObserver?) {
            }

            override fun getCount(): Int {
                return arrayElements.count()
            }

            override fun getItem(p0: Int): Any {
                return arrayElements[p0]
            }

            override fun getItemId(p0: Int): Long {
                return p0.toLong()
            }

            override fun hasStableIds(): Boolean {
                return true
            }

            override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View? {
                val inflater = activity.layoutInflater
                val view = p1 ?: inflater.inflate(android.R.layout.simple_spinner_item, p2, false)

                view.findViewById<TextView>(android.R.id.text1).apply {
                    text = temporarySettings.cpuAffinity
                }

                return view
            }

            override fun getItemViewType(p0: Int): Int {
                return 0
            }

            override fun getViewTypeCount(): Int {
                return 1
            }

            override fun isEmpty(): Boolean {
                return arrayElements.isEmpty()
            }

            override fun getDropDownView(p0: Int, p1: View?, p2: ViewGroup?): View {
                val inflater = activity.layoutInflater
                val view = inflater.inflate(R.layout.item_checkbox, p2, false)
                val checkBox = view.findViewById<CheckBox>(R.id.checkbox)

                checked[p0] = temporarySettings.cpuAffinity.contains(arrayElements[p0]) == true

                checkBox.isChecked = checked[p0]
                checkBox.text = arrayElements[p0]

                checkBox.setOnClickListener {
                    checked[p0] = !checked[p0]

                    val builder: StringBuilder = StringBuilder()

                    for (i in checked.indices) {
                        if (checked[i]) {
                            builder.append(",")
                            builder.append(arrayElements[i])
                        }
                    }

                    if (builder.isNotEmpty()) {
                        builder.deleteCharAt(0)
                    }

                    if (type == EDIT_GAME_PREFERENCES) {
                        temporarySettings.cpuAffinity = builder.toString()
                    } else if (type == FILE_MANAGER_START_PREFERENCES) {
                        selectedCpuAffinity = builder.toString()
                    }

                    spinner.adapter = this
                }

                return view
            }
        }
    }
}