package com.micewine.emu.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.micewine.emu.R
import com.micewine.emu.activities.PresetManagerActivity.Companion.ACTION_EDIT_BOX64_PRESET
import com.micewine.emu.activities.PresetManagerActivity.Companion.ACTION_EDIT_CONTROLLER_MAPPING
import com.micewine.emu.activities.VirtualControllerOverlayMapper
import com.micewine.emu.adapters.AdapterGame.Companion.selectedGameName
import com.micewine.emu.fragments.CreatePresetFragment.Companion.BOX64_PRESET
import com.micewine.emu.fragments.DeleteItemFragment
import com.micewine.emu.fragments.DeleteItemFragment.Companion.DELETE_PRESET
import com.micewine.emu.fragments.FloatingFileManagerFragment
import com.micewine.emu.fragments.FloatingFileManagerFragment.Companion.OPERATION_EXPORT_PRESET
import com.micewine.emu.fragments.RenameFragment
import com.micewine.emu.fragments.RenameFragment.Companion.RENAME_PRESET
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getBox64Preset
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getControllerPreset
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getVirtualControllerPreset
import com.micewine.emu.fragments.ShortcutsFragment.Companion.putBox64Preset
import com.micewine.emu.fragments.ShortcutsFragment.Companion.putControllerPreset
import com.micewine.emu.fragments.ShortcutsFragment.Companion.putVirtualControllerPreset

class AdapterPreset(private val settingsList: MutableList<Item>, private val context: Context, private val supportFragmentManager: FragmentManager) :
    RecyclerView.Adapter<AdapterPreset.ViewHolder>() {

    val preferences = PreferenceManager.getDefaultSharedPreferences(context)!!

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.adapter_preset_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sList = settingsList[position]

        holder.settingsName.text = sList.titleSettings

        if (sList.userPreset) {
            holder.moreButton.visibility = View.VISIBLE
        } else {
            holder.moreButton.visibility = View.GONE
        }

        if (sList.showRadioButton) {
            when (sList.type) {
                PHYSICAL_CONTROLLER -> {
                    if (sList.titleSettings == getControllerPreset(selectedGameName, 0)) {
                        selectedPresetId = position
                    }
                    holder.radioButton.setOnClickListener {
                        putControllerPreset(selectedGameName, holder.settingsName.text.toString(), 0)
                        selectedPresetId = holder.adapterPosition
                        notifyItemRangeChanged(0, settingsList.size)
                    }
                }
                VIRTUAL_CONTROLLER -> {
                    if (sList.titleSettings == getVirtualControllerPreset(selectedGameName)) {
                        selectedPresetId = position
                    }
                    holder.radioButton.setOnClickListener {
                        putVirtualControllerPreset(selectedGameName, holder.settingsName.text.toString())
                        selectedPresetId = holder.adapterPosition
                        notifyItemRangeChanged(0, settingsList.size)
                    }
                }
                BOX64_PRESET -> {
                    if (sList.titleSettings == getBox64Preset(selectedGameName)) {
                        selectedPresetId = position
                    }
                    holder.radioButton.setOnClickListener {
                        putBox64Preset(selectedGameName, holder.settingsName.text.toString())
                        selectedPresetId = holder.adapterPosition
                        notifyItemRangeChanged(0, settingsList.size)
                    }
                }
            }

            holder.radioButton.visibility = View.VISIBLE
            holder.radioButton.isChecked = position == selectedPresetId
        } else {
            holder.radioButton.visibility = View.GONE
        }

        holder.moreButton.setOnClickListener {
            val popup = PopupMenu(context, holder.moreButton)
            val inflater: MenuInflater = popup.menuInflater
            inflater.inflate(R.menu.preset_more_options_menu, popup.menu)

            popup.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.editPreset -> {
                        clickedPresetName = sList.titleSettings
                        clickedPresetType = sList.type

                        when (sList.type) {
                            PHYSICAL_CONTROLLER -> {
                                context.sendBroadcast(
                                    Intent(ACTION_EDIT_CONTROLLER_MAPPING)
                                )
                            }
                            VIRTUAL_CONTROLLER -> {
                                val intent = Intent(context, VirtualControllerOverlayMapper::class.java)
                                context.startActivity(intent)
                            }
                            BOX64_PRESET -> {
                                context.sendBroadcast(
                                    Intent(ACTION_EDIT_BOX64_PRESET)
                                )
                            }
                        }

                        true
                    }
                    R.id.deletePreset -> {
                        clickedPresetName = sList.titleSettings
                        clickedPresetType = sList.type

                        DeleteItemFragment(DELETE_PRESET, context).show(supportFragmentManager, "")

                        true
                    }
                    R.id.renamePreset -> {
                        clickedPresetName = sList.titleSettings
                        clickedPresetType = sList.type

                        RenameFragment(RENAME_PRESET, clickedPresetName).show(supportFragmentManager, "")

                        true
                    }
                    R.id.exportPreset -> {
                        clickedPresetName = sList.titleSettings
                        clickedPresetType = sList.type

                        FloatingFileManagerFragment(OPERATION_EXPORT_PRESET).show(supportFragmentManager, "")

                        true
                    }

                    else -> false
                }
            }

            popup.show()
        }
    }

    override fun getItemCount(): Int {
        return settingsList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val radioButton: RadioButton = itemView.findViewById(R.id.radioButton)
        val settingsName: TextView = itemView.findViewById(R.id.presetTitle)
        val moreButton: ImageButton = itemView.findViewById(R.id.moreButton)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
        }
    }

    class Item(var titleSettings: String, var type: Int, var userPreset: Boolean, var showRadioButton: Boolean = false)

    companion object {
        const val PHYSICAL_CONTROLLER = 0
        const val VIRTUAL_CONTROLLER = 1
        var clickedPresetName = ""
        var clickedPresetType = -1
        var selectedPresetId = -1
    }
}