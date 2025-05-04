package com.micewine.emu.core

import android.content.Context
import android.os.Build
import com.OxGames.Pluvia.PrefManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.micewine.emu.MiceWineUtils
import com.micewine.emu.MiceWineUtils.Main.adrenoToolsDriverFile
import com.micewine.emu.MiceWineUtils.Main.appLang
import com.micewine.emu.MiceWineUtils.Main.appRootDir
import com.micewine.emu.MiceWineUtils.Main.box64Avx
import com.micewine.emu.MiceWineUtils.Main.box64DynarecAlignedAtomics
import com.micewine.emu.MiceWineUtils.Main.box64DynarecBigblock
import com.micewine.emu.MiceWineUtils.Main.box64DynarecBleedingEdge
import com.micewine.emu.MiceWineUtils.Main.box64DynarecCallret
import com.micewine.emu.MiceWineUtils.Main.box64DynarecDirty
import com.micewine.emu.MiceWineUtils.Main.box64DynarecFastnan
import com.micewine.emu.MiceWineUtils.Main.box64DynarecFastround
import com.micewine.emu.MiceWineUtils.Main.box64DynarecForward
import com.micewine.emu.MiceWineUtils.Main.box64DynarecNativeflags
import com.micewine.emu.MiceWineUtils.Main.box64DynarecPause
import com.micewine.emu.MiceWineUtils.Main.box64DynarecSafeflags
import com.micewine.emu.MiceWineUtils.Main.box64DynarecStrongmem
import com.micewine.emu.MiceWineUtils.Main.box64DynarecWait
import com.micewine.emu.MiceWineUtils.Main.box64DynarecWeakbarrier
import com.micewine.emu.MiceWineUtils.Main.box64DynarecX87double
import com.micewine.emu.MiceWineUtils.Main.box64LogLevel
import com.micewine.emu.MiceWineUtils.Main.box64Mmap32
import com.micewine.emu.MiceWineUtils.Main.box64NoSigSegv
import com.micewine.emu.MiceWineUtils.Main.box64NoSigill
import com.micewine.emu.MiceWineUtils.Main.box64ShowBt
import com.micewine.emu.MiceWineUtils.Main.box64ShowSegv
import com.micewine.emu.MiceWineUtils.Main.box64Sse42
import com.micewine.emu.MiceWineUtils.Main.enableDRI3
import com.micewine.emu.MiceWineUtils.Main.getLdPreloadWorkaround
import com.micewine.emu.MiceWineUtils.Main.homeDir
import com.micewine.emu.MiceWineUtils.Main.ratPackagesDir
import com.micewine.emu.MiceWineUtils.Main.selectedBox64
import com.micewine.emu.MiceWineUtils.Main.selectedDXVKHud
import com.micewine.emu.MiceWineUtils.Main.selectedGLProfile
import com.micewine.emu.MiceWineUtils.Main.selectedMesaVkWsiPresentMode
import com.micewine.emu.MiceWineUtils.Main.selectedTuDebugPreset
import com.micewine.emu.MiceWineUtils.Main.selectedWine
import com.micewine.emu.MiceWineUtils.Main.strBoolToNumStr
import com.micewine.emu.MiceWineUtils.Main.tmpDir
import com.micewine.emu.MiceWineUtils.Main.useAdrenoTools
import com.micewine.emu.MiceWineUtils.Main.usrDir
import com.micewine.emu.MiceWineUtils.Main.wineESync
import com.micewine.emu.MiceWineUtils.Main.wineLogLevel
import com.micewine.emu.MiceWineUtils.EnvVarsSettings.ENV_VARS_KEY

object EnvVars {

    fun initialize(context: Context) {
    }

    fun getEnv(): String {
        val vars = mutableListOf<String>()

        setEnv(vars)

        val savedVarsJson = PrefManager.getString(ENV_VARS_KEY, null)
        if (savedVarsJson != null) {
            val type = object : TypeToken<List<MiceWineUtils.EnvVarsSettings.EnvironmentVariable>>() {}.type

            Gson().fromJson<List<MiceWineUtils.EnvVarsSettings.EnvironmentVariable>>(savedVarsJson, type).forEach {
                vars.add("${it.key}=${it.value}")
            }
        }

        return "env ${vars.joinToString(" ")} "
    }

    private fun setEnv(vars: MutableList<String>) {
        vars.add("LANG=$appLang.UTF-8")
        vars.add("TMPDIR=$tmpDir")
        vars.add("HOME=$homeDir")
        vars.add("XDG_CONFIG_HOME=$homeDir/.config")
        vars.add("DISPLAY=:0")
        vars.add("PULSE_LATENCY_MSEC=60")
        vars.add("LD_LIBRARY_PATH=/system/lib64:$usrDir/lib")
        vars.add("PATH=\$PATH:$usrDir/bin:$ratPackagesDir/$selectedWine/files/wine/bin:$ratPackagesDir/$selectedBox64/files/usr/bin")
        vars.add("PREFIX=$usrDir")
        vars.add("MESA_SHADER_CACHE_DIR=$homeDir/.cache")
        vars.add("MESA_VK_WSI_PRESENT_MODE=$selectedMesaVkWsiPresentMode")

        val glVersionStr = selectedGLProfile!!.split(" ")[1]
        val glslVersion =
            when (val glVersionInt = glVersionStr.replace(".", "").toInt()) {
                in 33..46 -> "$glVersionInt" + "0"
                32 -> "150"
                31 -> "140"
                30 -> "130"
                21 -> "120"
                else -> null
            }

        vars.add("MESA_GL_VERSION_OVERRIDE=$glVersionStr")
        vars.add("MESA_GLSL_VERSION_OVERRIDE=$glslVersion")
        vars.add("VK_ICD_FILENAMES=$appRootDir/vulkan_icd.json")

        vars.add("GALLIUM_DRIVER=zink")
        vars.add("TU_DEBUG=$selectedTuDebugPreset")
        vars.add("ZINK_DEBUG=compact")
        vars.add("ZINK_DESCRIPTORS=lazy")

        if (!enableDRI3) {
            vars.add("MESA_VK_WSI_DEBUG=sw")
        }

        vars.add("DXVK_ASYNC=1")
        vars.add("DXVK_STATE_CACHE_PATH=$homeDir/.cache/dxvk-shader-cache")
        vars.add("DXVK_HUD=$selectedDXVKHud")
        vars.add("MANGOHUD=1")
        vars.add("MANGOHUD_CONFIGFILE=$usrDir/etc/MangoHud.conf")

        if (Build.SUPPORTED_ABIS[0] != "x86_64") {
            vars.add("BOX64_LOG=$box64LogLevel")
            vars.add("BOX64_CPUNAME=\"ARM64 CPU\"")
            vars.add("BOX64_MMAP32=$box64Mmap32")
            vars.add("BOX64_AVX=$box64Avx")
            vars.add("BOX64_SSE42=$box64Sse42")
            vars.add("BOX64_RCFILE=$usrDir/etc/box64.box64rc")
            vars.add("BOX64_DYNAREC_BIGBLOCK=$box64DynarecBigblock")
            vars.add("BOX64_DYNAREC_STRONGMEM=$box64DynarecStrongmem")
            vars.add("BOX64_DYNAREC_WEAKBARRIER=$box64DynarecWeakbarrier")
            vars.add("BOX64_DYNAREC_PAUSE=$box64DynarecPause")
            vars.add("BOX64_DYNAREC_X87DOUBLE=$box64DynarecX87double")
            vars.add("BOX64_DYNAREC_FASTNAN=$box64DynarecFastnan")
            vars.add("BOX64_DYNAREC_FASTROUND=$box64DynarecFastround")
            vars.add("BOX64_DYNAREC_SAFEFLAGS=$box64DynarecSafeflags")
            vars.add("BOX64_DYNAREC_CALLRET=$box64DynarecCallret")
            vars.add("BOX64_DYNAREC_ALIGNED_ATOMICS=$box64DynarecAlignedAtomics")
            vars.add("BOX64_DYNAREC_NATIVEFLAGS=$box64DynarecNativeflags")
            vars.add("BOX64_DYNAREC_BLEEDING_EDGE=$box64DynarecBleedingEdge")
            vars.add("BOX64_DYNAREC_WAIT=$box64DynarecWait")
            vars.add("BOX64_DYNAREC_DIRTY=$box64DynarecDirty")
            vars.add("BOX64_DYNAREC_FORWARD=$box64DynarecForward")
            vars.add("BOX64_SHOWSEGV=${strBoolToNumStr(box64ShowSegv)}")
            vars.add("BOX64_SHOWBT=${strBoolToNumStr(box64ShowBt)}")
            vars.add("BOX64_NOSIGSEGV=${strBoolToNumStr(box64NoSigSegv)}")
            vars.add("BOX64_NOSIGILL=${strBoolToNumStr(box64NoSigill)}")
        }

        vars.add("VKD3D_FEATURE_LEVEL=12_0")

        if (wineLogLevel == "disabled") {
            vars.add("WINEDEBUG=-all")
        }

        vars.add("WINE_Z_DISK=$appRootDir")
        vars.add("WINEESYNC=${strBoolToNumStr(wineESync)}")

        if (useAdrenoTools) {
            vars.add("USE_ADRENOTOOLS=1")
            vars.add("ADRENOTOOLS_CUSTOM_DRIVER_DIR=${adrenoToolsDriverFile?.parent}/")
            vars.add("ADRENOTOOLS_CUSTOM_DRIVER_NAME=${adrenoToolsDriverFile?.name}")
            // Workaround for dlopen error (at least on my device)
            vars.add(getLdPreloadWorkaround())
        }
    }
}
