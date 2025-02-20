package com.winlator.core

import android.content.Context
import android.net.Uri
import com.winlator.container.Container
import com.winlator.core.ElfHelper.is64Bit
import com.winlator.core.FileUtils.chmod
import com.winlator.core.FileUtils.copy
import com.winlator.core.FileUtils.delete
import com.winlator.core.FileUtils.readString
import com.winlator.core.FileUtils.symlink
import com.winlator.core.FileUtils.toRelativePath
import com.winlator.core.FileUtils.writeString
import com.winlator.core.ProcessHelper.addDebugCallback
import com.winlator.core.ProcessHelper.removeDebugCallback
import com.winlator.core.TarCompressorUtils.extract
import com.winlator.xenvironment.ImageFs
import com.winlator.xenvironment.XEnvironment
import com.winlator.xenvironment.components.GuestProgramLauncherComponent
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Pattern
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

object WineUtils {

    fun createDosdevicesSymlinks(container: Container) {
        val dosdevicesPath = (File(container.rootDir, ".wine/dosdevices")).path
        val files = (File(dosdevicesPath)).listFiles()

        if (files != null) {
            for (file in files) {
                if (file.name.matches("[a-z]:".toRegex())) {
                    file.delete()
                }
            }
        }

        symlink("../drive_c", "$dosdevicesPath/c:")
        symlink("/", "$dosdevicesPath/z:")

        for (drive in container.drivesIterator()) {
            val linkTarget = File(drive[1])
            val path = linkTarget.absolutePath

            if (!linkTarget.isDirectory && path.endsWith("/com.winlator/storage")) {
                linkTarget.mkdirs()
                chmod(linkTarget, 505)
            }

            symlink(path, dosdevicesPath + "/" + drive[0].lowercase() + ":")
        }
    }

    fun extractWineFileForInstallAsync(context: Context, uri: Uri?, callback: Callback<File?>?) {
        Executors.newSingleThreadExecutor().execute {
            val destination = File(ImageFs.find(context).installedWineDir, "/preinstall/wine")
            delete(destination)
            destination.mkdirs()

            val success = extract(TarCompressorUtils.Type.XZ, context, uri, destination)

            if (!success) {
                delete(destination)
            }

            callback?.call(if (success) destination else null)
        }
    }

    fun findWineVersionAsync(context: Context, wineDir: File, callback: Callback<WineInfo?>) {
        var wineDir = wineDir

        if (!wineDir.isDirectory) {
            callback.call(null)
            return
        }

        var files = wineDir.listFiles().orEmpty()

        if (files.isEmpty()) {
            callback.call(null)
            return
        }

        if (files.size == 1) {
            if (!files[0].isDirectory) {
                callback.call(null)
                return
            }

            wineDir = files[0]
            files = wineDir.listFiles().orEmpty()

            if (files.isEmpty()) {
                callback.call(null)
                return
            }
        }

        var binDir: File? = null
        for (file in files) {
            if (file.isDirectory && file.name == "bin") {
                binDir = file
                break
            }
        }

        if (binDir == null) {
            callback.call(null)
            return
        }

        val wineBin = File(binDir, "wine")
        val wineBin64 = File(binDir, "wine64")

        if (!wineBin.isFile) {
            callback.call(null)
            return
        }

        val arch = if ((wineBin64.isFile && is64Bit(wineBin64)) || is64Bit(wineBin)) {
            "x86_64"
        } else {
            "x86"
        }

        val imageFs = ImageFs.find(context)
        val rootDir = imageFs.rootDir
        val wineBinAbsPath = if (wineBin64.isFile) wineBin64.path else wineBin.path
        val wineBinRelPath = toRelativePath(rootDir.path, wineBinAbsPath)
        val winePath = wineDir.path

        val wineInfoRef = AtomicReference<WineInfo>()
        val debugCallback = Callback { line: String ->
            val pattern = Pattern.compile("^wine\\-([0-9\\.]+)\\-?([0-9\\.]+)?", Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(line)

            if (matcher.find()) {
                val version = matcher.group(1)
                val subversion = if (matcher.groupCount() >= 2) {
                    matcher.group(2)
                } else {
                    null
                }

                wineInfoRef.set(WineInfo(version, subversion, arch, winePath))
            }
        }

        addDebugCallback(debugCallback)

        val linkFile = File(rootDir, ImageFs.HOME_PATH)
        linkFile.delete()
        symlink(wineDir, linkFile)

        val environment = XEnvironment(context, imageFs)
        val guestProgramLauncherComponent = GuestProgramLauncherComponent()
        guestProgramLauncherComponent.guestExecutable = "$wineBinRelPath --version"
        guestProgramLauncherComponent.terminationCallback = Callback {
            callback.call(wineInfoRef.get())
            removeDebugCallback(debugCallback)
        }
        environment.addComponent(guestProgramLauncherComponent)
        environment.startEnvironmentComponents()
    }

    fun getInstalledWineInfos(context: Context): ArrayList<WineInfo> {
        val wineInfos = ArrayList<WineInfo>()

        wineInfos.add(WineInfo.MAIN_WINE_VERSION)

        val installedWineDir = ImageFs.find(context).installedWineDir

        val files = installedWineDir.listFiles()

        if (files != null) {
            for (file in files) {
                val name = file.name
                if (name.startsWith("wine")) {
                    wineInfos.add(WineInfo.fromIdentifier(context, name))
                }
            }
        }

        return wineInfos
    }

    private fun setWindowMetrics(registryEditor: WineRegistryEditor) {
        val fontNormalData = (MSLogFont()).toByteArray()
        val fontBoldData = (MSLogFont()).setWeight(700).toByteArray()
        registryEditor.setHexValue(
            "Control Panel\\Desktop\\WindowMetrics",
            "CaptionFont",
            fontBoldData,
        )
        registryEditor.setHexValue(
            "Control Panel\\Desktop\\WindowMetrics",
            "IconFont",
            fontNormalData,
        )
        registryEditor.setHexValue(
            "Control Panel\\Desktop\\WindowMetrics",
            "MenuFont",
            fontNormalData,
        )
        registryEditor.setHexValue(
            "Control Panel\\Desktop\\WindowMetrics",
            "MessageFont",
            fontNormalData,
        )
        registryEditor.setHexValue(
            "Control Panel\\Desktop\\WindowMetrics",
            "SmCaptionFont",
            fontNormalData,
        )
        registryEditor.setHexValue(
            "Control Panel\\Desktop\\WindowMetrics",
            "StatusFont",
            fontNormalData,
        )
    }

    fun applySystemTweaks(context: Context, wineInfo: WineInfo) {
        val rootDir = ImageFs.find(context).rootDir
        val systemRegFile = File(rootDir, ImageFs.WINEPREFIX + "/system.reg")
        val userRegFile = File(rootDir, ImageFs.WINEPREFIX + "/user.reg")

        WineRegistryEditor(systemRegFile).use { registryEditor ->
            registryEditor.setStringValue(
                key = "Software\\Classes\\.reg",
                name = null,
                value = "REGfile",
            )
            registryEditor.setStringValue(
                key = "Software\\Classes\\.reg",
                name = "Content Type",
                value = "application/reg",
            )
            registryEditor.setStringValue(
                key = "Software\\Classes\\REGfile\\Shell\\Open\\command",
                name = null,
                value = "C:\\windows\\regedit.exe /C \"%1\"",
            )

            registryEditor.setStringValue(
                key = "Software\\Classes\\dllfile\\DefaultIcon",
                name = null,
                value = "shell32.dll,-154",
            )
            registryEditor.setStringValue(
                key = "Software\\Classes\\lnkfile\\DefaultIcon",
                name = null,
                value = "shell32.dll,-30",
            )
            registryEditor.setStringValue(
                key = "Software\\Classes\\inifile\\DefaultIcon",
                name = null,
                value = "shell32.dll,-151",
            )
        }

        val direct3dLibs = arrayOf(
            "d3d8",
            "d3d9",
            "d3d10",
            "d3d10_1",
            "d3d10core",
            "d3d11",
            "d3d12",
            "d3d12core",
            "ddraw",
            "dxgi",
            "wined3d",
        )
        val xinputLibs = arrayOf(
            "dinput",
            "dinput8",
            "xinput1_1",
            "xinput1_2",
            "xinput1_3",
            "xinput1_4",
            "xinput9_1_0",
            "xinputuap",
        )
        val dllOverridesKey = "Software\\Wine\\DllOverrides"

        val isMainWineVersion = WineInfo.isMainWineVersion(wineInfo.identifier())

        WineRegistryEditor(userRegFile).use { registryEditor ->
            for (name in direct3dLibs) {
                registryEditor.setStringValue(
                    key = dllOverridesKey,
                    name = name,
                    value = "native,builtin",
                )
            }

            for (name in xinputLibs) {
                registryEditor.setStringValue(
                    key = dllOverridesKey,
                    name = name,
                    value = if (isMainWineVersion) "builtin,native" else "native,builtin",
                )
            }

            registryEditor.removeKey(
                key = "Software\\Winlator\\WFM\\ContextMenu\\7-Zip",
            )
            registryEditor.setStringValue(
                key = "Software\\Winlator\\WFM\\ContextMenu\\7-Zip",
                name = "Open Archive",
                value = "Z:\\opt\\apps\\7-Zip\\7zFM.exe \"%FILE%\"",
            )
            registryEditor.setStringValue(
                key = "Software\\Winlator\\WFM\\ContextMenu\\7-Zip",
                name = "Extract Here",
                value = "Z:\\opt\\apps\\7-Zip\\7zG.exe x \"%FILE%\" -r -o\"%DIR%\" -y",
            )
            registryEditor.setStringValue(
                key = "Software\\Winlator\\WFM\\ContextMenu\\7-Zip",
                name = "Extract to Folder",
                value = "Z:\\opt\\apps\\7-Zip\\7zG.exe x \"%FILE%\" -r -o\"%DIR%\\%BASENAME%\" -y",
            )

            setWindowMetrics(registryEditor)
        }

        val wineSystem32Dir = File(rootDir, "/opt/wine/lib/wine/x86_64-windows")
        val wineSysWoW64Dir = File(rootDir, "/opt/wine/lib/wine/i386-windows")
        val containerSystem32Dir = File(rootDir, ImageFs.WINEPREFIX + "/drive_c/windows/system32")
        val containerSysWoW64Dir = File(rootDir, ImageFs.WINEPREFIX + "/drive_c/windows/syswow64")

        val dlnames = arrayOf(
            "user32.dll", "shell32.dll", "dinput.dll", "dinput8.dll", "xinput1_1.dll", "xinput1_2.dll",
            "xinput1_3.dll", "xinput1_4.dll", "xinput9_1_0.dll", "xinputuap.dll", "winemenubuilder.exe", "explorer.exe",
        )
        val win64 = wineInfo.isWin64

        for (dlname in dlnames) {
            copy(
                srcFile = File(wineSysWoW64Dir, dlname),
                dstFile = File(if (win64) containerSysWoW64Dir else containerSystem32Dir, dlname),
            )

            if (win64) {
                copy(File(wineSystem32Dir, dlname), File(containerSystem32Dir, dlname))
            }
        }
    }

    fun overrideWinComponentDlls(context: Context, container: Container, wincomponents: String) {
        val dllOverridesKey = "Software\\Wine\\DllOverrides"
        val userRegFile = File(container.rootDir, ".wine/user.reg")
        val oldWinComponentsIter = KeyValueSet(container.getExtra("wincomponents", Container.FALLBACK_WINCOMPONENTS)!!).iterator()

        try {
            WineRegistryEditor(userRegFile).use { registryEditor ->
                val wincomponentsJSONObject = JSONObject(readString(context, "wincomponents/wincomponents.json"))

                for (wincomponent in KeyValueSet(wincomponents)) {
                    if (wincomponent[1] == oldWinComponentsIter.next()[1]) {
                        continue
                    }

                    val identifier = wincomponent[0]
                    val useNative = wincomponent[1] == "1"

                    val dlnames = wincomponentsJSONObject.getJSONArray(identifier)
                    for (i in 0..<dlnames.length()) {
                        val dlname = dlnames.getString(i)
                        if (useNative) {
                            registryEditor.setStringValue(dllOverridesKey, dlname, "native,builtin")
                        } else {
                            registryEditor.removeValue(dllOverridesKey, dlname)
                        }
                    }
                }
            }
        } catch (e: JSONException) {
            Timber.w(e)
        }
    }

    fun setWinComponentRegistryKeys(systemRegFile: File, identifier: String, useNative: Boolean) {
        if (identifier == "directsound") {
            WineRegistryEditor(systemRegFile).use { registryEditor ->
                val key64 =
                    "Software\\Classes\\CLSID\\{083863F1-70DE-11D0-BD40-00A0C911CE86}\\Instance\\{E30629D1-27E5-11CE-875D-00608CB78066}"
                val key32 =
                    "Software\\Classes\\Wow6432Node\\CLSID\\{083863F1-70DE-11D0-BD40-00A0C911CE86}\\Instance\\{E30629D1-27E5-11CE-875D-00608CB78066}"

                if (useNative) {
                    registryEditor.setStringValue(
                        key = key32,
                        name = "CLSID",
                        value = "{E30629D1-27E5-11CE-875D-00608CB78066}",
                    )
                    registryEditor.setHexValue(
                        key = key32,
                        name = "FilterData",
                        value = "02000000000080000100000000000000307069330200000000000000010000000000000000000000307479330000000038000000480000006175647300001000800000aa00389b710100000000001000800000aa00389b71",
                    )
                    registryEditor.setStringValue(
                        key = key32,
                        name = "FriendlyName",
                        value = "Wave Audio Renderer",
                    )

                    registryEditor.setStringValue(
                        key = key64,
                        name = "CLSID",
                        value = "{E30629D1-27E5-11CE-875D-00608CB78066}",
                    )
                    registryEditor.setHexValue(
                        key = key64,
                        name = "FilterData",
                        value = "02000000000080000100000000000000307069330200000000000000010000000000000000000000307479330000000038000000480000006175647300001000800000aa00389b710100000000001000800000aa00389b71",
                    )
                    registryEditor.setStringValue(
                        key = key64,
                        name = "FriendlyName",
                        value = "Wave Audio Renderer",
                    )
                } else {
                    registryEditor.removeKey(key32)
                    registryEditor.removeKey(key64)
                }
            }
        } else if (identifier == "wmdecoder") {
            WineRegistryEditor(systemRegFile).use { registryEditor ->
                if (useNative) {
                    registryEditor.setStringValue(
                        key = "Software\\Classes\\Wow6432Node\\CLSID\\{2EEB4ADF-4578-4D10-BCA7-BB955F56320A}\\InprocServer32",
                        name = null,
                        value = "C:\\windows\\system32\\wmadmod.dll",
                    )
                    registryEditor.setStringValue(
                        key = "Software\\Classes\\Wow6432Node\\CLSID\\{82D353DF-90BD-4382-8BC2-3F6192B76E34}\\InprocServer32",
                        name = null,
                        value = "C:\\windows\\system32\\wmvdecod.dll",
                    )
                } else {
                    registryEditor.setStringValue(
                        key = "Software\\Classes\\Wow6432Node\\CLSID\\{2EEB4ADF-4578-4D10-BCA7-BB955F56320A}\\InprocServer32",
                        name = null,
                        value = "C:\\windows\\system32\\winegstreamer.dll",
                    )
                    registryEditor.setStringValue(
                        key = "Software\\Classes\\Wow6432Node\\CLSID\\{82D353DF-90BD-4382-8BC2-3F6192B76E34}\\InprocServer32",
                        name = null,
                        value = "C:\\windows\\system32\\winegstreamer.dll",
                    )
                }
            }
        }
    }

    fun updateWineprefix(context: Context, terminationCallback: Callback<Int?>?) {
        val imageFs = ImageFs.find(context)
        val rootDir = imageFs.rootDir
        val tmpDir = imageFs.tmpDir

        if (!tmpDir.isDirectory) {
            tmpDir.mkdir()
        }

        writeString(File(rootDir, ImageFs.WINEPREFIX + "/.update-timestamp"), "0\n")

        val envVars = EnvVars()
        envVars.put("WINEPREFIX", ImageFs.WINEPREFIX)
        envVars.put("WINEDLLOVERRIDES", "mscoree,mshtml=d")

        val environment = XEnvironment(context, imageFs)
        val guestProgramLauncherComponent = GuestProgramLauncherComponent()
        guestProgramLauncherComponent.envVars = envVars
        guestProgramLauncherComponent.guestExecutable = WineInfo.MAIN_WINE_VERSION.getExecutable(context, true) + " wineboot -u"
        guestProgramLauncherComponent.terminationCallback = Callback<Int> { status: Int? ->
            writeString(File(rootDir, ImageFs.WINEPREFIX + "/.update-timestamp"), "disable\n")
            terminationCallback?.call(status)
        }
        environment.addComponent(guestProgramLauncherComponent)
        environment.startEnvironmentComponents()
    }

    fun changeServicesStatus(container: Container, onlyEssential: Boolean) {
        val services = arrayOf(
            "BITS:3", "Eventlog:2", "HTTP:3", "LanmanServer:3", "NDIS:2",
            "PlugPlay:2", "RpcSs:3", "scardsvr:3", "Schedule:3", "Spooler:3",
            "StiSvc:3", "TermService:3", "winebus:3", "winehid:3", "Winmgmt:3",
            "wuauserv:3",
        )
        val systemRegFile = File(container.rootDir, ".wine/system.reg")

        WineRegistryEditor(systemRegFile).use { registryEditor ->
            registryEditor.setCreateKeyIfNotExist(false)
            for (service in services) {
                val name = service.substring(0, service.indexOf(":"))
                val value = if (onlyEssential) {
                    4
                } else {
                    Character.getNumericValue(service[service.length - 1])
                }

                registryEditor.setDwordValue(
                    key = "System\\CurrentControlSet\\Services\\$name",
                    name = "Start",
                    value = value,
                )
            }
        }
    }
}
