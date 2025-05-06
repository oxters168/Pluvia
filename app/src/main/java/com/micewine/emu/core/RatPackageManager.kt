package com.micewine.emu.core

import android.annotation.SuppressLint
import android.content.Context
import com.OxGames.Pluvia.PrefManager
import com.OxGames.Pluvia.R
import com.google.gson.Gson
import com.micewine.emu.MiceWineUtils.GeneralSettings.SELECTED_BOX64
import com.micewine.emu.MiceWineUtils.GeneralSettings.SELECTED_VULKAN_DRIVER
import com.micewine.emu.MiceWineUtils.Main.appRootDir
import com.micewine.emu.MiceWineUtils.Main.ratPackagesDir
import com.micewine.emu.MiceWineUtils.Setup.dialogTitleText
import com.micewine.emu.MiceWineUtils.Setup.progressBarIsIndeterminate
import com.micewine.emu.MiceWineUtils.Setup.progressBarValue
import com.micewine.emu.core.ShellLoader.runCommand
import java.io.File
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.progress.ProgressMonitor

object RatPackageManager {
    @SuppressLint("SetTextI18n")
    fun installRat(ratPackage: RatPackage, context: Context) {
        progressBarIsIndeterminate = false

        var extractDir = appRootDir.parent

        ratPackage.ratFile.use { ratFile ->
            ratFile?.isRunInThread = true

            if (ratPackage.category == "rootfs") {
                installingRootFS = true
            } else {
                extractDir = "$ratPackagesDir/${ratPackage.category}-${java.util.UUID.randomUUID()}"
                File(extractDir!!).mkdirs()
            }

            ratFile?.extractAll(extractDir)

            while (!ratFile?.progressMonitor?.state?.equals(ProgressMonitor.State.READY)!!) {
                progressBarValue = ratFile.progressMonitor.percentDone

                Thread.sleep(100)
            }
        }

        progressBarValue = 0

        runCommand("chmod -R 700 $extractDir")
        runCommand("sh $extractDir/makeSymlinks.sh").also {
            File("$extractDir/makeSymlinks.sh").delete()
        }

        when (ratPackage.category) {
            "rootfs" -> {
                File("$extractDir/pkg-header").renameTo(File("$ratPackagesDir/rootfs-pkg-header"))

                val adrenoToolsFolder = File("$extractDir/adrenoTools")
                val vulkanDriversFolder = File("$extractDir/vulkanDrivers")
                val box64Folder = File("$extractDir/box64")
                val wineFolder = File("$extractDir/wine")

                File("$appRootDir/wine-utils/DXVK").listFiles()?.forEach {
                    val dxvkDir = File("$ratPackagesDir/DXVK-${java.util.UUID.randomUUID()}")
                    dxvkDir.mkdirs()
                    val dxvkFilesDir = File("$dxvkDir/files")
                    dxvkFilesDir.mkdirs()
                    val dxvkVersion = it.name.substringAfter("-")

                    it.renameTo(dxvkFilesDir)

                    File("$dxvkDir/pkg-header").writeText("name=DXVK\ncategory=DXVK\nversion=$dxvkVersion\narchitecture=any\nvkDriverLib=\n")
                }

                File("$appRootDir/wine-utils/WineD3D").listFiles()?.forEach {
                    val wineD3DDir = File("$ratPackagesDir/WineD3D-${java.util.UUID.randomUUID()}")
                    wineD3DDir.mkdirs()
                    val wineD3DFilesFir = File("$wineD3DDir/files")
                    wineD3DFilesFir.mkdirs()
                    val wineD3DVersion = it.name.substringAfter("-")

                    it.renameTo(wineD3DFilesFir)

                    File("$wineD3DDir/pkg-header").writeText("name=WineD3D\ncategory=WineD3D\nversion=$wineD3DVersion\narchitecture=any\nvkDriverLib=\n")
                }

                File("$appRootDir/wine-utils/VKD3D").listFiles()?.forEach {
                    val vkd3dDir = File("$ratPackagesDir/VKD3D-${java.util.UUID.randomUUID()}")
                    vkd3dDir.mkdirs()
                    val vkd3dFilesDir = File("$vkd3dDir/files")
                    vkd3dFilesDir.mkdirs()
                    val vkd3dVersion = it.name.substringAfter("-")

                    it.renameTo(vkd3dFilesDir)

                    File("$vkd3dDir/pkg-header").writeText("name=VKD3D\ncategory=VKD3D\nversion=$vkd3dVersion\narchitecture=any\nvkDriverLib=\n")
                }

                dialogTitleText = context.getString(R.string.installing_drivers)

                if (vulkanDriversFolder.exists()) {
                    vulkanDriversFolder.listFiles()?.sorted()?.forEach { ratFile ->
                        installRat(RatPackage(ratFile.path), context)
                    }

                    vulkanDriversFolder.deleteRecursively()
                }

                if (adrenoToolsFolder.exists()) {
                    adrenoToolsFolder.listFiles()?.sorted()?.forEach { ratFile ->
                        installRat(RatPackage(ratFile.path), context)
                    }

                    adrenoToolsFolder.deleteRecursively()
                }

                dialogTitleText = context.getString(R.string.installing_box64)

                if (box64Folder.exists()) {
                    box64Folder.listFiles()?.sorted()?.forEach { ratFile ->
                        installRat(RatPackage(ratFile.path), context)
                    }

                    box64Folder.deleteRecursively()
                }

                dialogTitleText = context.getString(R.string.installing_wine)

                if (wineFolder.exists()) {
                    wineFolder.listFiles()?.sorted()?.forEach { ratFile ->
                        installRat(RatPackage(ratFile.path), context)
                    }

                    wineFolder.deleteRecursively()
                }

                installingRootFS = false
            }

            "Box64" -> {
                if (PrefManager.getString(SELECTED_BOX64, "") == "") {
                    PrefManager.putString(SELECTED_BOX64, File(extractDir!!).name)
                }

                File("$extractDir/pkg-header").writeText("name=${ratPackage.name}\ncategory=${ratPackage.category}\nversion=${ratPackage.version}\narchitecture=${ratPackage.architecture}\nvkDriverLib=\n")

                if (!installingRootFS) {
                    File("$extractDir/pkg-external").writeText("")
                }
            }

            "VulkanDriver", "AdrenoTools" -> {
                if (PrefManager.getString(SELECTED_VULKAN_DRIVER, "") == "") {
                    PrefManager.putString(SELECTED_VULKAN_DRIVER, File(extractDir!!).name)
                }

                val driverLibPath = "$extractDir/files/usr/lib/${ratPackage.driverLib}"

                File("$extractDir/pkg-header").writeText("name=${ratPackage.name}\ncategory=${ratPackage.category}\nversion=${ratPackage.version}\narchitecture=${ratPackage.architecture}\nvkDriverLib=$driverLibPath\n")

                if (!installingRootFS) {
                    File("$extractDir/pkg-external").writeText("")
                }
            }

            "Wine", "DXVK", "WineD3D", "VKD3D" -> {
                File("$extractDir/pkg-header").writeText("name=${ratPackage.name}\ncategory=${ratPackage.category}\nversion=${ratPackage.version}\narchitecture=${ratPackage.architecture}\nvkDriverLib=\n")

                if (!installingRootFS) {
                    File("$extractDir/pkg-external").writeText("")
                }
            }
        }
    }

    fun listRatPackages(type: String = "", anotherType: String = "?"): List<RatPackage> {
        val packagesList: MutableList<RatPackage> = mutableListOf()

        File("$appRootDir/packages").listFiles()?.forEach { file ->
            if (file.isDirectory && (file.name.startsWith(type) || file.name.startsWith(anotherType))) {
                val pkgHeader = File("$file/pkg-header")
                if (!pkgHeader.exists()) {
                    file.deleteRecursively()
                    return@forEach
                }

                val lines = pkgHeader.readLines()
                var pkgName = lines[0].substringAfter("=")

                if (file.name.startsWith("AdrenoToolsDriver-")) {
                    pkgName += " (AdrenoTools)"
                }

                val pkgCategory = lines[1].substringAfter("=")
                val pkgVersion = lines[2].substringAfter("=")
                val pkgDriverLib = lines[3].substringAfter("=")

                packagesList.add(
                    RatPackage().apply {
                        name = pkgName
                        category = pkgCategory
                        version = pkgVersion
                        driverLib = pkgDriverLib
                        folderName = file.name

                        isUserInstalled = File("$file/pkg-external").exists()
                    },
                )
            }
        }

        return packagesList
    }

    fun listRatPackagesId(type: String = "", anotherType: String = "?"): List<String> {
        val packagesIdList: MutableList<String> = mutableListOf()

        File("$appRootDir/packages").listFiles()?.forEach { file ->
            if (file.isDirectory && (file.name.startsWith(type) || file.name.startsWith(anotherType))) {
                packagesIdList.add(file.name)
            }
        }

        return packagesIdList
    }

    fun getPackageNameVersionById(id: String?): String? {
        val pkgHeader = File("$ratPackagesDir/$id/pkg-header")

        if (pkgHeader.exists() && id != null) {
            val lines = pkgHeader.readLines()
            return lines[0].substringAfter("=") + " " + lines[2].substringAfter("=")
        }

        return null
    }

    fun checkPackageInstalled(name: String, category: String, version: String): Boolean {
        listRatPackages().forEach {
            if (it.name == name && it.category == category && it.version == version) {
                return true
            }
        }

        return false
    }

    fun installADToolsDriver(adrenoToolsPackage: AdrenoToolsPackage) {
        progressBarIsIndeterminate = false

        var extractDir: String

        adrenoToolsPackage.adrenoToolsFile.use {
            it.isRunInThread = true

            extractDir = "$ratPackagesDir/AdrenoToolsDriver-${java.util.UUID.randomUUID()}"

            it.extractAll(extractDir)

            while (!it.progressMonitor.state.equals(ProgressMonitor.State.READY)) {
                progressBarValue = it.progressMonitor.percentDone

                Thread.sleep(100)
            }
        }

        progressBarValue = 0

        runCommand("chmod -R 700 $extractDir")

        val driverLibPath = "$extractDir/${adrenoToolsPackage.driverLib}"

        File("$extractDir/pkg-header").writeText("name=${adrenoToolsPackage.name}\ncategory=AdrenoToolsDriver\nversion=${adrenoToolsPackage.version}\narchitecture=aarch64\nvkDriverLib=$driverLibPath\n")

        if (!installingRootFS) {
            File("$extractDir/pkg-external").writeText("")
        }
    }

    class AdrenoToolsPackage(path: String) {
        var name: String? = null
        var version: String? = null
        var description: String? = null
        var driverLib: String? = null
        var author: String? = null
        var adrenoToolsFile: ZipFile = ZipFile(path)

        init {
            val metaHeader = if (adrenoToolsFile.isValidZipFile) adrenoToolsFile.getFileHeader("meta.json") else null
            if (metaHeader != null) {
                adrenoToolsFile.getInputStream(metaHeader).use { inputStream ->
                    val json = inputStream.reader().readLines().joinToString("\n")
                    val meta = Gson().fromJson(json, AdrenoToolsMetaInfo::class.java)

                    name = meta.name
                    version = meta.driverVersion
                    description = meta.description
                    driverLib = meta.libraryName
                    author = meta.author
                }
            }
        }
    }

    data class AdrenoToolsMetaInfo(
        val name: String,
        val description: String,
        val author: String,
        val driverVersion: String,
        val libraryName: String,
    )

    class RatPackage(ratPath: String? = null) {
        var name: String? = null
        var category: String? = null
        var version: String? = null
        var architecture: String? = null
        var driverLib: String? = null
        var isUserInstalled: Boolean? = null
        var folderName: String? = null
        var ratFile: ZipFile? = null

        init {
            if (ratPath != null) {
                ratFile = ZipFile(ratPath)

                val ratHeader = if (ratFile?.isValidZipFile!!) ratFile?.getFileHeader("pkg-header") else null
                if (ratHeader != null) {
                    ratFile?.getInputStream(ratHeader).use { inputStream ->
                        val lines = inputStream?.reader()?.readLines()!!

                        name = lines[0].substringAfter("=")
                        category = lines[1].substringAfter("=")
                        version = lines[2].substringAfter("=")
                        architecture = lines[3].substringAfter("=")
                        driverLib = lines[4].substringAfter("=")
                    }
                }
            }
        }
    }

    private var installingRootFS: Boolean = false

    val installablePackagesCategories = setOf("VulkanDriver", "Box64", "Wine", "DXVK", "WineD3D", "VKD3D")
}
