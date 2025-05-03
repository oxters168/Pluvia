package com.micewine.emu.core

import android.os.Build
import com.OxGames.Pluvia.PrefManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.micewine.emu.MiceWineUtils

object EnvVars {
    fun getEnv(): String {
        val vars = mutableListOf<String>()

        setEnv(vars)

        val savedVarsJson = PrefManager.getString(MiceWineUtils.EnvVarsSettings.ENV_VARS_KEY, null)
        if (savedVarsJson != null) {
            val type = object : TypeToken<List<MiceWineUtils.EnvVarsSettings.EnvironmentVariable>>() {}.type

            Gson().fromJson<List<MiceWineUtils.EnvVarsSettings.EnvironmentVariable>>(savedVarsJson, type).forEach {
                vars.add("${it.key}=${it.value}")
            }
        }

        return "env ${vars.joinToString(" ")} "
    }

    private fun setEnv(vars: MutableList<String>) {
        vars.add("LANG=${MiceWineUtils.Main.appLang}.UTF-8")
        vars.add("TMPDIR=${MiceWineUtils.Main.tmpDir}")
        vars.add("HOME=${MiceWineUtils.Main.homeDir}")
        vars.add("XDG_CONFIG_HOME=${MiceWineUtils.Main.homeDir}/.config")
        vars.add("DISPLAY=:0")
        vars.add("PULSE_LATENCY_MSEC=60")
        vars.add("LD_LIBRARY_PATH=/system/lib64:${MiceWineUtils.Main.usrDir}/lib")
        vars.add(
            "PATH=\$PATH:${MiceWineUtils.Main.usrDir}/bin:${MiceWineUtils.Main.ratPackagesDir}/" +
                "${MiceWineUtils.Main.selectedWine}/files/wine/bin:${MiceWineUtils.Main.ratPackagesDir}/" +
                "${MiceWineUtils.Main.selectedBox64}/files/usr/bin",
        )
        vars.add("PREFIX=${MiceWineUtils.Main.usrDir}")
        vars.add("MESA_SHADER_CACHE_DIR=${MiceWineUtils.Main.homeDir}/.cache")
        vars.add("MESA_VK_WSI_PRESENT_MODE=${MiceWineUtils.Main.selectedMesaVkWsiPresentMode}")

        val glVersionStr = MiceWineUtils.Main.selectedGLProfile!!.split(" ")[1]
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
        vars.add("VK_ICD_FILENAMES=${MiceWineUtils.Main.appRootDir}/vulkan_icd.json")

        vars.add("GALLIUM_DRIVER=zink")
        vars.add("TU_DEBUG=${MiceWineUtils.Main.selectedTuDebugPreset}")
        vars.add("ZINK_DEBUG=compact")
        vars.add("ZINK_DESCRIPTORS=lazy")

        if (!MiceWineUtils.Main.enableDRI3) {
            vars.add("MESA_VK_WSI_DEBUG=sw")
        }

        vars.add("DXVK_ASYNC=1")
        vars.add("DXVK_STATE_CACHE_PATH=${MiceWineUtils.Main.homeDir}/.cache/dxvk-shader-cache")
        vars.add("DXVK_HUD=${MiceWineUtils.Main.selectedDXVKHud}")
        vars.add("MANGOHUD=1")
        vars.add("MANGOHUD_CONFIGFILE=${MiceWineUtils.Main.usrDir}/etc/MangoHud.conf")

        if (Build.SUPPORTED_ABIS[0] != "x86_64") {
            vars.add("BOX64_LOG=${MiceWineUtils.Main.box64LogLevel}")
            vars.add("BOX64_CPUNAME=\"ARM64 CPU\"")
            vars.add("BOX64_MMAP32=${MiceWineUtils.Main.box64Mmap32}")
            vars.add("BOX64_AVX=${MiceWineUtils.Main.box64Avx}")
            vars.add("BOX64_SSE42=${MiceWineUtils.Main.box64Sse42}")
            vars.add("BOX64_RCFILE=${MiceWineUtils.Main.usrDir}/etc/box64.box64rc")
            vars.add("BOX64_DYNAREC_BIGBLOCK=${MiceWineUtils.Main.box64DynarecBigblock}")
            vars.add("BOX64_DYNAREC_STRONGMEM=${MiceWineUtils.Main.box64DynarecStrongmem}")
            vars.add("BOX64_DYNAREC_WEAKBARRIER=${MiceWineUtils.Main.box64DynarecWeakbarrier}")
            vars.add("BOX64_DYNAREC_PAUSE=${MiceWineUtils.Main.box64DynarecPause}")
            vars.add("BOX64_DYNAREC_X87DOUBLE=${MiceWineUtils.Main.box64DynarecX87double}")
            vars.add("BOX64_DYNAREC_FASTNAN=${MiceWineUtils.Main.box64DynarecFastnan}")
            vars.add("BOX64_DYNAREC_FASTROUND=${MiceWineUtils.Main.box64DynarecFastround}")
            vars.add("BOX64_DYNAREC_SAFEFLAGS=${MiceWineUtils.Main.box64DynarecSafeflags}")
            vars.add("BOX64_DYNAREC_CALLRET=${MiceWineUtils.Main.box64DynarecCallret}")
            vars.add("BOX64_DYNAREC_ALIGNED_ATOMICS=${MiceWineUtils.Main.box64DynarecAlignedAtomics}")
            vars.add("BOX64_DYNAREC_NATIVEFLAGS=${MiceWineUtils.Main.box64DynarecNativeflags}")
            vars.add("BOX64_DYNAREC_BLEEDING_EDGE=${MiceWineUtils.Main.box64DynarecBleedingEdge}")
            vars.add("BOX64_DYNAREC_WAIT=${MiceWineUtils.Main.box64DynarecWait}")
            vars.add("BOX64_DYNAREC_DIRTY=${MiceWineUtils.Main.box64DynarecDirty}")
            vars.add("BOX64_DYNAREC_FORWARD=${MiceWineUtils.Main.box64DynarecForward}")
            vars.add("BOX64_SHOWSEGV=${MiceWineUtils.Main.box64ShowSegv}")
            vars.add("BOX64_SHOWBT=${MiceWineUtils.Main.box64ShowBt}")
            vars.add("BOX64_NOSIGSEGV=${MiceWineUtils.Main.box64NoSigSegv}")
            vars.add("BOX64_NOSIGILL=${MiceWineUtils.Main.box64NoSigill}")
        }

        vars.add("VKD3D_FEATURE_LEVEL=12_0")

        if (MiceWineUtils.Main.wineLogLevel == "disabled") {
            vars.add("WINEDEBUG=-all")
        }

        vars.add("WINE_Z_DISK=${MiceWineUtils.Main.appRootDir}")
        vars.add("WINEESYNC=${MiceWineUtils.Main.strBoolToNumStr(MiceWineUtils.Main.wineESync)}")

        if (MiceWineUtils.Main.useAdrenoTools) {
            vars.add("USE_ADRENOTOOLS=1")
            vars.add("ADRENOTOOLS_CUSTOM_DRIVER_DIR=${MiceWineUtils.Main.adrenoToolsDriverFile?.parent}/")
            vars.add("ADRENOTOOLS_CUSTOM_DRIVER_NAME=${MiceWineUtils.Main.adrenoToolsDriverFile?.name}")
            // Workaround for dlopen error (at least on my device)
            vars.add("LD_PRELOAD=/system/lib64/libEGL.so:/system/lib64/libGLESv1_CM.so")
        }
    }
}
