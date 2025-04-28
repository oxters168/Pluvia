package com.micewine.emu.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.micewine.emu.adapters.AdapterRatPackage.Companion.BOX64
import com.micewine.emu.adapters.AdapterRatPackage.Companion.DXVK
import com.micewine.emu.adapters.AdapterRatPackage.Companion.VKD3D
import com.micewine.emu.adapters.AdapterRatPackage.Companion.VK_DRIVER
import com.micewine.emu.adapters.AdapterRatPackage.Companion.WINE
import com.micewine.emu.adapters.AdapterRatPackage.Companion.WINED3D
import com.micewine.emu.fragments.RatDownloaderFragment

class AdapterTabPagerDownloader(activity: FragmentActivity?) : FragmentStateAdapter(activity!!) {
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> RatDownloaderFragment("VulkanDriver", VK_DRIVER ,"AdrenoToolsDriver-")
            1 -> RatDownloaderFragment("Box64", BOX64)
            2 -> RatDownloaderFragment("Wine", WINE)
            3 -> RatDownloaderFragment("DXVK", DXVK)
            4 -> RatDownloaderFragment("WineD3D", WINED3D)
            5 -> RatDownloaderFragment("VKD3D", VKD3D)
            else -> throw Exception("Invalid Fragment for Position $position")
        }
    }

    override fun getItemCount(): Int {
        return 6
    }

    fun getItemName(position: Int): String {
        return when (position) {
            0 -> "Drivers"
            1 -> "Box64"
            2 -> "Wine"
            3 -> "DXVK"
            4 -> "WineD3D"
            5 -> "VKD3D"
            else -> ""
        }
    }
}