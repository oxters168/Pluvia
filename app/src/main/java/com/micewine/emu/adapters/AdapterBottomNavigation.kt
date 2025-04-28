package com.micewine.emu.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.micewine.emu.fragments.AboutFragment
import com.micewine.emu.fragments.FileManagerFragment
import com.micewine.emu.fragments.SettingsFragment
import com.micewine.emu.fragments.ShortcutsFragment

class AdapterBottomNavigation(activity: FragmentActivity?) : FragmentStateAdapter(activity!!) {
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ShortcutsFragment()
            1 -> SettingsFragment()
            2 -> FileManagerFragment()
            3 -> AboutFragment()
            else -> throw Exception("Invalid Fragment for Position $position")
        }
    }

    override fun getItemCount(): Int {
        return 4
    }
}