package com.micewine.emu.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.micewine.emu.R
import com.micewine.emu.activities.GeneralSettingsActivity
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.ACTION_PREFERENCE_SELECT
import com.micewine.emu.activities.PresetManagerActivity
import com.micewine.emu.activities.RatDownloaderActivity
import com.micewine.emu.activities.RatManagerActivity
import com.micewine.emu.adapters.AdapterPreset.Companion.PHYSICAL_CONTROLLER
import com.micewine.emu.adapters.AdapterPreset.Companion.VIRTUAL_CONTROLLER
import com.micewine.emu.fragments.CreatePresetFragment.Companion.BOX64_PRESET

class AdapterSettings(private val settingsList: List<SettingsList>, private val context: Context) :
    RecyclerView.Adapter<AdapterSettings.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.adapter_settings_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sList = settingsList[position]
        holder.settingsName.text = sList.titleSettings
        holder.settingsDescription.text = sList.descriptionSettings
        holder.imageSettings.setImageResource(sList.imageSettings)
    }

    override fun getItemCount(): Int {
        return settingsList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        val settingsName: TextView = itemView.findViewById(R.id.title_preferences_model)
        val settingsDescription: TextView = itemView.findViewById(R.id.description_preferences_model)
        val imageSettings: ImageView = itemView.findViewById(R.id.set_img)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val settingsModel = settingsList[getAdapterPosition()]

            when (settingsModel.titleSettings) {
                context.getString(R.string.general_settings) -> {
                    val intent = Intent(context, GeneralSettingsActivity::class.java)
                    context.startActivity(intent)
                }

                context.getString(R.string.controller_mapper_title) -> {
                    val intent = Intent(context, PresetManagerActivity::class.java).apply {
                        putExtra("presetType", PHYSICAL_CONTROLLER)
                    }
                    context.startActivity(intent)
                }

                context.getString(R.string.virtual_controller_mapper_title) -> {
                    val intent = Intent(context, PresetManagerActivity::class.java).apply {
                        putExtra("presetType", VIRTUAL_CONTROLLER)
                    }
                    context.startActivity(intent)
                }

                context.getString(R.string.box64_preset_manager_title) -> {
                    val intent = Intent(context, PresetManagerActivity::class.java).apply {
                        putExtra("presetType", BOX64_PRESET)
                    }
                    context.startActivity(intent)
                }

                context.getString(R.string.rat_manager_title) -> {
                    val intent = Intent(context, RatManagerActivity::class.java)
                    context.startActivity(intent)
                }

                context.getString(R.string.rat_downloader_title) -> {
                    val intent = Intent(context, RatDownloaderActivity::class.java)
                    context.startActivity(intent)
                }

                else -> {
                    val intent = Intent(ACTION_PREFERENCE_SELECT).apply {
                        putExtra("preference", settingsModel.titleSettings)
                    }
                    context.sendBroadcast(intent)
                }
            }
        }
    }

    class SettingsList(var titleSettings: String, var descriptionSettings: String, var imageSettings: Int)
}