package com.micewine.emu.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.micewine.emu.R
import com.micewine.emu.activities.EmulationActivity
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.SELECTED_BOX64
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.SELECTED_VULKAN_DRIVER
import com.micewine.emu.activities.MainActivity.Companion.ACTION_RUN_WINE
import com.micewine.emu.fragments.ShortcutsFragment.Companion.ADRENO_TOOLS_DRIVER
import com.micewine.emu.fragments.ShortcutsFragment.Companion.MESA_DRIVER
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getBox64Preset
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getBox64Version
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getCpuAffinity
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getD3DXRenderer
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getDXVKVersion
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getDisplaySettings
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getVKD3DVersion
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getVirtualControllerPreset
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getVulkanDriver
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getVulkanDriverType
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getWineD3DVersion
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getWineESync
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getWineServices
import com.micewine.emu.fragments.ShortcutsFragment.Companion.getWineVirtualDesktop
import com.micewine.emu.fragments.VirtualControllerPresetManagerFragment.Companion.preferences
import java.io.File

class AdapterGame(private val gameList: MutableList<GameItem>, private val activity: Activity) : RecyclerView.Adapter<AdapterGame.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.adapter_game_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sList = gameList[position]
        holder.titleGame.text = sList.name

        val imageFile = File(sList.iconPath)

        if (imageFile.exists() && imageFile.length() > 0) {
            val imageBitmap = BitmapFactory.decodeFile(sList.iconPath)

            if (imageBitmap != null) {
                holder.gameImage.setImageBitmap(
                    resizeBitmap(
                        imageBitmap, holder.gameImage.layoutParams.width, holder.gameImage.layoutParams.height
                    )
                )
            }
        } else if (sList.iconPath == "") {
            holder.gameImage.setImageBitmap(resizeBitmap(
                BitmapFactory.decodeResource(activity.resources, R.drawable.default_icon), holder.gameImage.layoutParams.width, holder.gameImage.layoutParams.height)
            )
        } else {
            holder.gameImage.setImageBitmap(
                Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888),
            )
        }
    }

    override fun getItemCount(): Int {
        return gameList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<GameItem>) {
        gameList.clear()
        gameList.addAll(newList)
        notifyDataSetChanged()
    }

    private fun resizeBitmap(originalBitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(originalBitmap, width, height, false)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
        val titleGame: TextView = itemView.findViewById(R.id.title_game_model)
        val gameImage: ImageView = itemView.findViewById(R.id.img_game)

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(v: View) {
            val gameModel = gameList[adapterPosition]

            selectedGameName = gameModel.name

            val exeFile = File(gameModel.exePath)
            var exePath = gameModel.exePath
            var exeArguments = gameModel.exeArguments

            if (!exeFile.exists()) {
                if (exeFile.path == activity.getString(R.string.desktop_mode_init)) {
                    exePath = ""
                    exeArguments = ""
                } else {
                    activity.runOnUiThread {
                        Toast.makeText(activity, "", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
            }

            val intent = Intent(activity, EmulationActivity::class.java)

            var driverName = getVulkanDriver(selectedGameName)
            var driverType = getVulkanDriverType(selectedGameName)
            if (driverName == "Global") {
                driverName = preferences?.getString(SELECTED_VULKAN_DRIVER, "").toString()
                driverType = if (driverName.startsWith("AdrenoToolsDriver-")) ADRENO_TOOLS_DRIVER else MESA_DRIVER
            }

            var box64Version = getBox64Version(selectedGameName)
            if (box64Version == "Global") {
                box64Version = preferences?.getString(SELECTED_BOX64, "").toString()
            }

            val runWineIntent = Intent(ACTION_RUN_WINE).apply {
                putExtra("exePath", exePath)
                putExtra("exeArguments", exeArguments)
                putExtra("driverName", driverName)
                putExtra("driverType", driverType)
                putExtra("box64Version", box64Version)
                putExtra("box64Preset", getBox64Preset(selectedGameName))
                putExtra("displayResolution", getDisplaySettings(selectedGameName)[1])
                putExtra("virtualControllerPreset", getVirtualControllerPreset(selectedGameName))
                putExtra("d3dxRenderer", getD3DXRenderer(selectedGameName))
                putExtra("wineD3D", getWineD3DVersion(selectedGameName))
                putExtra("dxvk", getDXVKVersion(selectedGameName))
                putExtra("vkd3d", getVKD3DVersion(selectedGameName))
                putExtra("esync", getWineESync(selectedGameName))
                putExtra("services", getWineServices(selectedGameName))
                putExtra("virtualDesktop", getWineVirtualDesktop(selectedGameName))
                putExtra("cpuAffinity", getCpuAffinity(selectedGameName))
            }

            activity.sendBroadcast(runWineIntent)
            activity.startActivity(intent)
        }

        override fun onLongClick(v: View): Boolean {
            val gameModel = gameList[adapterPosition]

            selectedGameName = gameModel.name

            return false
        }
    }

    class GameItem(
        var name: String,
        var exePath: String,
        var exeArguments: String,
        var iconPath: String
    )

    companion object {
        var selectedGameName = ""
    }
}
