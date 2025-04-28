package com.micewine.emu.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.micewine.emu.R
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.SPINNER
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.SWITCH
import com.micewine.emu.adapters.AdapterPreset.Companion.clickedPresetName
import com.micewine.emu.fragments.Box64PresetManagerFragment.Companion.editBox64Mapping
import com.micewine.emu.fragments.Box64PresetManagerFragment.Companion.getBox64Mapping
import com.micewine.emu.fragments.InfoDialogFragment

class AdapterSettingsBox64Preset(
    private val settingsList: MutableList<Box64ListSpinner>,
    private val activity: FragmentActivity
) :
    RecyclerView.Adapter<AdapterSettingsBox64Preset.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.adapter_settings_preferences_item, parent, false)
        return ViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sList = settingsList[position]
        holder.settingsName.setText(sList.titleSettings)
        holder.settingsDescription.setText(sList.descriptionSettings)

        if (activity.getString(sList.descriptionSettings) == " ") {
            holder.settingsDescription.visibility = View.GONE
        }

        val mapping = getBox64Mapping(clickedPresetName, sList.key)

        when (sList.type) {
            SWITCH -> {
                holder.spinnerOptions.visibility = View.GONE
                holder.settingsSwitch.visibility = View.VISIBLE
                holder.seekBar.visibility = View.GONE
                holder.seekBarValue.visibility = View.GONE

                holder.settingsSwitch.isChecked = mapping[0].toBoolean()
                holder.settingsSwitch.setOnClickListener {
                    editBox64Mapping(clickedPresetName, sList.key, (holder.settingsSwitch.isChecked).toString())
                }
            }
            SPINNER -> {
                holder.settingsSwitch.visibility = View.GONE
                holder.spinnerOptions.visibility = View.VISIBLE
                holder.seekBar.visibility = View.GONE
                holder.seekBarValue.visibility = View.GONE

                holder.spinnerOptions.adapter = ArrayAdapter(
                    activity,
                    android.R.layout.simple_spinner_dropdown_item,
                    sList.spinnerOptions!!
                )

                holder.spinnerOptions.setSelection(
                    sList.spinnerOptions!!.indexOf(
                        mapping[0]
                    )
                )

                holder.spinnerOptions.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            val selectedItem = parent?.getItemAtPosition(position).toString()

                            editBox64Mapping(clickedPresetName, sList.key, selectedItem)
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {
                        }
                    }
            }
        }
    }

    override fun getItemCount(): Int {
        return settingsList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val settingsName: TextView = itemView.findViewById(R.id.title_preferences_model)
        val settingsDescription: TextView = itemView.findViewById(R.id.description_preferences_model)
        val spinnerOptions: Spinner = itemView.findViewById(R.id.keyBindSpinner)
        val settingsSwitch: SwitchCompat = itemView.findViewById(R.id.optionSwitch)
        val seekBar: SeekBar = itemView.findViewById(R.id.seekBar)
        val seekBarValue: TextView = itemView.findViewById(R.id.seekBarValue)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val settingsModel = settingsList[getAdapterPosition()]

            InfoDialogFragment(
                settingsName.text.toString(),
                activity.getString(settingsModel.descriptionSettings)
            ).show(activity.supportFragmentManager, "")
        }
    }

    class Box64ListSpinner(
        var titleSettings: Int,
        var descriptionSettings: Int,
        var spinnerOptions: Array<String>?,
        var type: Int,
        var key: String
    )
}