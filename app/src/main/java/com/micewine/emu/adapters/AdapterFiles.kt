package com.micewine.emu.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.micewine.emu.R
import com.micewine.emu.activities.MainActivity.Companion.ACTION_SELECT_FILE_MANAGER
import com.micewine.emu.activities.MainActivity.Companion.customRootFSPath
import com.micewine.emu.activities.MainActivity.Companion.fileManagerCwd
import com.micewine.emu.activities.MainActivity.Companion.fileManagerDefaultDir
import com.micewine.emu.activities.MainActivity.Companion.selectedFile
import com.micewine.emu.activities.MainActivity.Companion.usrDir
import com.micewine.emu.core.WineWrapper.extractIcon
import com.micewine.emu.fragments.FloatingFileManagerFragment.Companion.outputFile
import com.micewine.emu.fragments.FloatingFileManagerFragment.Companion.refreshFiles
import com.micewine.emu.utils.DriveUtils
import mslinks.ShellLink
import mslinks.ShellLinkException
import java.io.File
import kotlin.math.round

class AdapterFiles(private val fileList: List<FileList>, private val context: Context, private val isFloatFilesDialog: Boolean) : RecyclerView.Adapter<AdapterFiles.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.adapter_files_item, parent, false)
        return ViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sList = fileList[position]

        if (fileManagerCwd == fileManagerDefaultDir) {
            holder.fileName.text = sList.file.name.uppercase()
        } else {
            holder.fileName.text = sList.file.name
        }

        holder.fileName.isSelected = true

        if (sList.file.isDirectory) {
            holder.icon.setImageResource(R.drawable.ic_folder)

            val count = sList.file.listFiles()?.count()

            if (count == null) {
                holder.fileDescription.visibility = View.GONE
            } else {
                holder.fileDescription.visibility = View.VISIBLE

                holder.fileDescription.text = when (count) {
                    0 -> {
                        context.getString(R.string.empty_text)
                    }
                    1 -> {
                        "$count ${context.getString(R.string.item_text)}"
                    }
                    else -> {
                        "$count ${context.getString(R.string.items_text)}"
                    }
                }
            }
        } else if (sList.file.isFile) {
            val fileSize = sList.file.length().toDouble()

            holder.fileDescription.text = formatSize(fileSize)

            val fileExtension = sList.file.extension.lowercase()

            if (fileExtension == "exe") {
                val output = File("$usrDir/icons/${sList.file.nameWithoutExtension}-icon")

                extractIcon(sList.file, output.path)

                if (output.exists() && output.length() > 0) {
                    holder.icon.setImageBitmap(BitmapFactory.decodeFile(output.path))
                } else {
                    holder.icon.setImageResource(R.drawable.ic_log)
                }
            } else if (fileExtension == "lnk") {
                try {
                    val shell = ShellLink(sList.file)
                    val drive = DriveUtils.parseWindowsPath(shell.resolveTarget())
                    if (drive != null) {
                        val filePath = File(drive.getUnixPath())

                        val output = File("$usrDir/icons/${filePath.nameWithoutExtension}-icon")

                        extractIcon(filePath, output.path)

                        if (output.exists() && output.length() > 0) {
                            holder.icon.setImageBitmap(BitmapFactory.decodeFile(output.path))
                        } else {
                            holder.icon.setImageResource(R.drawable.ic_log)
                        }
                    } else {
                        holder.icon.setImageResource(R.drawable.ic_log)
                    }
                } catch (_: ShellLinkException) {
                    holder.icon.setImageResource(R.drawable.ic_log)
                }
            } else if (fileExtension == "rat" || fileExtension == "mwp") {
                holder.icon.setImageResource(R.drawable.ic_rat_package)
            } else {
                holder.icon.setImageResource(R.drawable.ic_log)
            }
        }
    }

    override fun getItemCount(): Int {
        return fileList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
        val fileName: TextView = itemView.findViewById(R.id.title_preferences_model)
        val fileDescription: TextView = itemView.findViewById(R.id.description_preferences_model)
        val icon: ImageView = itemView.findViewById(R.id.set_img)

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(v: View) {
            if (adapterPosition < 0) {
                return
            }

            val settingsModel = fileList[adapterPosition]

            if (isFloatFilesDialog) {
                if (settingsModel.file.name == "..") {
                    fileManagerCwd = File(fileManagerCwd!!).parent!!

                    refreshFiles()
                } else if (settingsModel.file.isFile) {
                    if (settingsModel.file.name.contains(".rat")) {
                        customRootFSPath = settingsModel.file.path
                    } else {
                        outputFile = settingsModel.file.path
                    }
                } else if (settingsModel.file.isDirectory) {
                    fileManagerCwd = settingsModel.file.path

                    refreshFiles()
                }
            } else {
                val intent = Intent(ACTION_SELECT_FILE_MANAGER).apply {
                    putExtra("selectedFile", settingsModel.file.path)
                }

                context.sendBroadcast(intent)
            }
        }

        override fun onLongClick(v: View): Boolean {
            val settingsModel = fileList[adapterPosition]

            selectedFile = settingsModel.file.path

            return (fileManagerCwd == fileManagerDefaultDir) || selectedFile == ".."
        }
    }

    class FileList(var file: File)

    companion object {
        private const val GIGABYTE = 1024 * 1024 * 1024
        private const val MEGABYTE = 1024 * 1024
        private const val KILOBYTE = 1024

        private fun formatSize(value: Double): String {
            return when {
                value < KILOBYTE -> "${round(value * 100) / 100}B"
                value < MEGABYTE -> "${round(value / KILOBYTE * 100) / 100}KB"
                value < GIGABYTE -> "${round(value / MEGABYTE * 100) / 100}MB"
                else -> "${round(value / GIGABYTE * 100) / 100}GB"
            }
        }
    }
}