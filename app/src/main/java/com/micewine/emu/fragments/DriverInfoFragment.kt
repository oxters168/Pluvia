package com.micewine.emu.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.micewine.emu.R
import com.micewine.emu.activities.MainActivity.Companion.appRootDir
import com.micewine.emu.activities.MainActivity.Companion.ratPackagesDir
import com.micewine.emu.activities.MainActivity.Companion.setSharedVars
import com.micewine.emu.activities.RatManagerActivity.Companion.generateICDFile
import com.micewine.emu.core.EnvVars.getEnv
import com.micewine.emu.core.RatPackageManager.listRatPackages
import com.micewine.emu.core.RatPackageManager.listRatPackagesId
import com.micewine.emu.core.ShellLoader.runCommandWithOutput
import java.io.File

class DriverInfoFragment : Fragment() {
    private var rootView: View? = null
    private var driverSpinner: Spinner? = null
    private var driverInfoTextView: TextView? = null
    private var scrollView: ScrollView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_driver_info, container, false)

        driverSpinner = rootView?.findViewById(R.id.driverSpinner)
        driverInfoTextView = rootView?.findViewById(R.id.logsTextView)
        scrollView = rootView?.findViewById(R.id.scrollView)

        val vulkanDriversId = listRatPackagesId("VulkanDriver", "AdrenoToolsDriver")
        val vulkanDrivers = listRatPackages("VulkanDriver", "AdrenoToolsDriver").map { it.name + " " + it.version }

        driverSpinner?.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, vulkanDrivers)
        driverSpinner?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val driverId = vulkanDriversId[driverSpinner?.selectedItemPosition!!]

                val driverFile: String
                var adrenoToolsDriverPath: String? = null

                if (driverId.contains("AdrenoToolsDriver")) {
                    driverFile = File("$ratPackagesDir/${File("$appRootDir/packages").listFiles()?.first { it.name.startsWith("AdrenoTools-") }?.name}/pkg-header").readLines()[4].substringAfter("=")
                    adrenoToolsDriverPath = File("$ratPackagesDir/$driverId/pkg-header").readLines()[4].substringAfter("=")
                } else {
                    driverFile = File("$ratPackagesDir/$driverId/pkg-header").readLines()[4].substringAfter("=")
                }
                
                setSharedVars(requireActivity(), null, null, null, null, null, null, null, null, null, null, null, (driverId.contains("AdrenoToolsDriver")), adrenoToolsDriverPath)

                generateICDFile(driverFile, File("$appRootDir/vulkan_icd.json"))

                driverInfoTextView?.post {
                    driverInfoTextView?.text = runCommandWithOutput(getEnv() + "vulkaninfo", true)
                }

                scrollView?.post {
                    scrollView?.scrollTo(0, 0)
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }

        return rootView
    }
}
