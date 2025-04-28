package com.micewine.emu.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.micewine.emu.R
import com.micewine.emu.fragments.WelcomeFragment

class AdapterWelcomeFragments(activity: FragmentActivity?) : FragmentStateAdapter(activity!!) {
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> WelcomeFragment(R.layout.fragment_welcome)
            1 -> WelcomeFragment(R.layout.fragment_welcome_2)
            else -> throw Exception("Invalid Fragment for Position $position")
        }
    }

    override fun getItemCount(): Int {
        return 2
    }
}