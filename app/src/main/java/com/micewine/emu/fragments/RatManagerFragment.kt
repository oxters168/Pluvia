package com.micewine.emu.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.micewine.emu.R
import com.micewine.emu.adapters.AdapterRatPackage
import com.micewine.emu.core.RatPackageManager.listRatPackages

class RatManagerFragment(private val prefix: String, private val type: Int, private val anotherPrefix: String = prefix) : Fragment() {
    private val ratList: MutableList<AdapterRatPackage.Item> = mutableListOf()
    private var rootView: View? = null
    private var recyclerView: RecyclerView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_general_settings, container, false)
        recyclerView = rootView?.findViewById(R.id.recyclerViewGeneralSettings)

        setAdapter()

        return rootView
    }

    private fun setAdapter() {
        recyclerView?.setAdapter(
            AdapterRatPackage(ratList, requireActivity())
        )

        ratList.clear()

        listRatPackages(prefix, anotherPrefix).forEach {
            addToAdapter(it.name!!, it.version!!, it.folderName!!, it.isUserInstalled!!)
        }
    }

    private fun addToAdapter(title: String, description: String, driverFolderId: String, canDelete: Boolean) {
        ratList.add(
            AdapterRatPackage.Item(title, description, driverFolderId, type, canDelete)
        )
    }
}
