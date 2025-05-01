package com.OxGames.Pluvia

import android.os.Build

object MiceWineUtils {

    val isX86 = Common.deviceArch == "x86_64"

    object Common {
        val deviceArch = Build.SUPPORTED_ABIS[0].replace("arm64-v8a", "aarch64")
    }

    object GeneralSettings {
        const val ACTION_PREFERENCE_SELECT = "com.micewine.emu.ACTION_PREFERENCE_SELECT"
        const val SWITCH = 1
        const val SPINNER = 2
        const val CHECKBOX = 3
        const val SEEKBAR = 4

        const val BOX64_LOG = "BOX64_LOG"
        const val BOX64_LOG_DEFAULT_VALUE = "1"
        const val BOX64_MMAP32 = "BOX64_MMAP32"
        const val BOX64_MMAP32_DEFAULT_VALUE = true
        const val BOX64_AVX = "BOX64_AVX"
        const val BOX64_AVX_DEFAULT_VALUE = "2"
        const val BOX64_SSE42 = "BOX64_SSE42"
        const val BOX64_SSE42_DEFAULT_VALUE = true
        const val BOX64_DYNAREC_BIGBLOCK = "BOX64_DYNAREC_BIGBLOCK"
        const val BOX64_DYNAREC_BIGBLOCK_DEFAULT_VALUE = "1"
        const val BOX64_DYNAREC_STRONGMEM = "BOX64_DYNAREC_STRONGMEM"
        const val BOX64_DYNAREC_STRONGMEM_DEFAULT_VALUE = "1"
        const val BOX64_DYNAREC_WEAKBARRIER = "BOX64_DYNAREC_WEAKBARRIER"
        const val BOX64_DYNAREC_WEAKBARRIER_DEFAULT_VALUE = "1"
        const val BOX64_DYNAREC_PAUSE = "BOX64_DYNAREC_PAUSE"
        const val BOX64_DYNAREC_PAUSE_DEFAULT_VALUE = "0"
        const val BOX64_DYNAREC_X87DOUBLE = "BOX64_DYNAREC_X87DOUBLE"
        const val BOX64_DYNAREC_X87DOUBLE_DEFAULT_VALUE = false
        const val BOX64_DYNAREC_FASTNAN = "BOX64_DYNAREC_FASTNAN"
        const val BOX64_DYNAREC_FASTNAN_DEFAULT_VALUE = true
        const val BOX64_DYNAREC_FASTROUND = "BOX64_DYNAREC_FASTROUND"
        const val BOX64_DYNAREC_FASTROUND_DEFAULT_VALUE = true
        const val BOX64_DYNAREC_SAFEFLAGS = "BOX64_DYNAREC_SAFEFLAGS"
        const val BOX64_DYNAREC_SAFEFLAGS_DEFAULT_VALUE = "1"
        const val BOX64_DYNAREC_CALLRET = "BOX64_DYNAREC_CALLRET"
        const val BOX64_DYNAREC_CALLRET_DEFAULT_VALUE = true
        const val BOX64_DYNAREC_ALIGNED_ATOMICS = "BOX64_DYNAREC_ALIGNED_ATOMICS"
        const val BOX64_DYNAREC_ALIGNED_ATOMICS_DEFAULT_VALUE = false
        const val BOX64_DYNAREC_NATIVEFLAGS = "BOX64_DYNAREC_NATIVEFLAGS"
        const val BOX64_DYNAREC_NATIVEFLAGS_DEFAULT_VALUE = true
        const val BOX64_DYNAREC_WAIT = "BOX64_DYNAREC_WAIT"
        const val BOX64_DYNAREC_WAIT_DEFAULT_VALUE = true
        const val BOX64_DYNAREC_DIRTY = "BOX64_DYNAREC_DIRTY"
        const val BOX64_DYNAREC_DIRTY_DEFAULT_VALUE = false
        const val BOX64_DYNAREC_FORWARD = "BOX64_DYNAREC_FORWARD"
        const val BOX64_DYNAREC_FORWARD_DEFAULT_VALUE = "128"
        const val BOX64_SHOWSEGV = "BOX64_SHOWSEGV"
        const val BOX64_SHOWSEGV_DEFAULT_VALUE = false
        const val BOX64_SHOWBT = "BOX64_SHOWBT"
        const val BOX64_SHOWBT_DEFAULT_VALUE = false
        const val BOX64_NOSIGSEGV = "BOX64_NOSIGSEGV"
        const val BOX64_NOSIGSEGV_DEFAULT_VALUE = false
        const val BOX64_NOSIGILL = "BOX64_NOSIGILL"
        const val BOX64_NOSIGILL_DEFAULT_VALUE = false

        const val SELECTED_BOX64 = "selectedBox64"
        const val SELECTED_VULKAN_DRIVER = "selectedVulkanDriver"
        const val SELECTED_WINE_PREFIX = "selectedWinePrefix"
        const val SELECTED_TU_DEBUG_PRESET = "selectedTuDebugPreset"
        const val SELECTED_TU_DEBUG_PRESET_DEFAULT_VALUE = "noconform,sysmem"
        const val ENABLE_DRI3 = "enableDRI3"
        const val ENABLE_DRI3_DEFAULT_VALUE = true
        const val ENABLE_MANGOHUD = "enableMangoHUD"
        const val ENABLE_MANGOHUD_DEFAULT_VALUE = true
        const val WINE_LOG_LEVEL = "wineLogLevel"
        const val WINE_LOG_LEVEL_DEFAULT_VALUE = "default"
        const val SELECTED_GL_PROFILE = "selectedGLProfile"
        const val SELECTED_GL_PROFILE_DEFAULT_VALUE = "GL 3.2"
        const val SELECTED_DXVK_HUD_PRESET = "selectedDXVKHudPreset"
        const val SELECTED_DXVK_HUD_PRESET_DEFAULT_VALUE = ""
        const val SELECTED_MESA_VK_WSI_PRESENT_MODE = "MESA_VK_WSI_PRESENT_MODE"
        const val SELECTED_MESA_VK_WSI_PRESENT_MODE_DEFAULT_VALUE = "mailbox"
        const val DEAD_ZONE = "deadZone"
        const val MOUSE_SENSIBILITY = "mouseSensibility"
        const val FPS_LIMIT = "fpsLimit"
        const val PA_SINK = "pulseAudioSink"
        const val PA_SINK_DEFAULT_VALUE = "SLES"
    }
}
