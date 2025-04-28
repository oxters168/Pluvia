package com.micewine.emu.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import androidx.recyclerview.widget.RecyclerView
import com.micewine.emu.R
import com.micewine.emu.adapters.AdapterPreset.Companion.clickedPresetName
import com.micewine.emu.controller.XKeyCodes
import com.micewine.emu.fragments.ControllerPresetManagerFragment.Companion.editControllerPreset
import com.micewine.emu.fragments.ControllerPresetManagerFragment.Companion.getMapping

class AdapterSettingsController(private val settingsControllerList: List<SettingsController>, private val context: Context) :
    RecyclerView.Adapter<AdapterSettingsController.ViewHolder>() {

    private val allEntries: List<String> = XKeyCodes.getKeyNames(true)
    private val keyEntries: List<String> = XKeyCodes.getKeyNames(false)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.adapter_settings_controller_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sList = settingsControllerList[position]

        holder.image.setImageResource(sList.image)
        
        val mapping = getMapping(clickedPresetName, sList.key)

        when (sList.image) {
            R.drawable.l_up, R.drawable.l_down, R.drawable.l_left, R.drawable.l_right,
            R.drawable.r_up, R.drawable.r_down, R.drawable.r_left, R.drawable.r_right -> {
                holder.keyBindSpinner.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, keyEntries)
                holder.keyBindSpinner.setSelection(keyEntries.indexOf(mapping[0]))
            }

            else -> {
                holder.keyBindSpinner.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, allEntries)
                holder.keyBindSpinner.setSelection(allEntries.indexOf(mapping[0]))
            }
        }

        holder.keyBindSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    editControllerPreset(
                        clickedPresetName,
                        sList.key,
                        parent?.selectedItem.toString()
                    )
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
    }

    override fun getItemCount(): Int {
        return settingsControllerList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val keyBindSpinner: Spinner = itemView.findViewById(R.id.keyBindSpinner)
        val image: ImageView = itemView.findViewById(R.id.buttonImageView)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
        }
    }

    class SettingsController(var image: Int, var key: String)
}