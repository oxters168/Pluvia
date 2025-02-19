package com.winlator.box86_64

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.winlator.R
import com.winlator.core.EnvVars
import kotlin.math.max

object Box86_64PresetManager {

    fun getEnvVars(prefix: String, context: Context, id: String): EnvVars {
        val ucPrefix = prefix.uppercase()
        val envVars = EnvVars()

        if (id == Box86_64Preset.STABILITY) {
            envVars.apply {
                put(ucPrefix + "_DYNAREC_SAFEFLAGS", "2")
                put(ucPrefix + "_DYNAREC_FASTNAN", "0")
                put(ucPrefix + "_DYNAREC_FASTROUND", "0")
                put(ucPrefix + "_DYNAREC_X87DOUBLE", "1")
                put(ucPrefix + "_DYNAREC_BIGBLOCK", "0")
                put(ucPrefix + "_DYNAREC_STRONGMEM", "2")
                put(ucPrefix + "_DYNAREC_FORWARD", "128")
                put(ucPrefix + "_DYNAREC_CALLRET", "0")
                put(ucPrefix + "_DYNAREC_WAIT", "0")
            }
        } else if (id == Box86_64Preset.COMPATIBILITY) {
            envVars.apply {
                put(ucPrefix + "_DYNAREC_SAFEFLAGS", "2")
                put(ucPrefix + "_DYNAREC_FASTNAN", "0")
                put(ucPrefix + "_DYNAREC_FASTROUND", "0")
                put(ucPrefix + "_DYNAREC_X87DOUBLE", "1")
                put(ucPrefix + "_DYNAREC_BIGBLOCK", "0")
                put(ucPrefix + "_DYNAREC_STRONGMEM", "1")
                put(ucPrefix + "_DYNAREC_FORWARD", "128")
                put(ucPrefix + "_DYNAREC_CALLRET", "0")
                put(ucPrefix + "_DYNAREC_WAIT", "1")
            }
        } else if (id == Box86_64Preset.INTERMEDIATE) {
            envVars.apply {
                put(ucPrefix + "_DYNAREC_SAFEFLAGS", "2")
                put(ucPrefix + "_DYNAREC_FASTNAN", "1")
                put(ucPrefix + "_DYNAREC_FASTROUND", "0")
                put(ucPrefix + "_DYNAREC_X87DOUBLE", "1")
                put(ucPrefix + "_DYNAREC_BIGBLOCK", "1")
                put(ucPrefix + "_DYNAREC_STRONGMEM", "0")
                put(ucPrefix + "_DYNAREC_FORWARD", "128")
                put(ucPrefix + "_DYNAREC_CALLRET", "0")
                put(ucPrefix + "_DYNAREC_WAIT", "1")
            }
        } else if (id == Box86_64Preset.PERFORMANCE) {
            envVars.apply {
                put(ucPrefix + "_DYNAREC_SAFEFLAGS", "1")
                put(ucPrefix + "_DYNAREC_FASTNAN", "1")
                put(ucPrefix + "_DYNAREC_FASTROUND", "1")
                put(ucPrefix + "_DYNAREC_X87DOUBLE", "0")
                put(ucPrefix + "_DYNAREC_BIGBLOCK", "3")
                put(ucPrefix + "_DYNAREC_STRONGMEM", "0")
                put(ucPrefix + "_DYNAREC_FORWARD", "512")
                put(ucPrefix + "_DYNAREC_CALLRET", "1")
                put(ucPrefix + "_DYNAREC_WAIT", "1")
            }
        } else if (id.startsWith(Box86_64Preset.CUSTOM)) {
            for (preset in customPresetsIterator(prefix, context)) {
                if (preset[0] == id) {
                    envVars.putAll(preset[2])
                    break
                }
            }
        }

        return envVars
    }

    fun getPresets(prefix: String?, context: Context): ArrayList<Box86_64Preset> {
        val presets = arrayListOf(
            Box86_64Preset(Box86_64Preset.STABILITY, context.getString(R.string.stability)),
            Box86_64Preset(Box86_64Preset.COMPATIBILITY, context.getString(R.string.compatibility)),
            Box86_64Preset(Box86_64Preset.INTERMEDIATE, context.getString(R.string.intermediate)),
            Box86_64Preset(Box86_64Preset.PERFORMANCE, context.getString(R.string.performance)),
        )

        for (preset in customPresetsIterator(prefix, context)) {
            presets.add(Box86_64Preset(preset[0], preset[1]))
        }

        return presets
    }

    fun getPreset(prefix: String?, context: Context, id: String?): Box86_64Preset? {
        for (preset in getPresets(prefix, context)) {
            if (preset.id == id) {
                return preset
            }
        }

        return null
    }

    private fun customPresetsIterator(
        prefix: String?,
        context: Context,
    ): Iterable<Array<String>> {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val customPresetsStr = preferences.getString(prefix + "_custom_presets", "")!!
        val customPresets = customPresetsStr.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val index = intArrayOf(0)

        return Iterable {
            object : Iterator<Array<String>> {
                override fun hasNext(): Boolean = index[0] < customPresets.size && customPresetsStr.isNotEmpty() == true

                override fun next(): Array<String> = customPresets[index[0]++].split("|").toTypedArray()
            }
        }
    }

    fun getNextPresetId(context: Context, prefix: String?): Int {
        var maxId = 0
        for (preset in customPresetsIterator(prefix, context)) {
            maxId = max(
                maxId.toDouble(),
                preset[0].replace(Box86_64Preset.CUSTOM + "-", "").toInt().toDouble(),
            ).toInt()
        }

        return maxId + 1
    }

    fun editPreset(
        prefix: String?,
        context: Context,
        id: String?,
        name: String?,
        envVars: EnvVars,
    ) {
        val key = prefix + "_custom_presets"
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        var customPresetsStr: String = preferences.getString(key, "")!!

        if (id != null) {
            val customPresets = customPresetsStr.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            for (i in customPresets.indices) {
                val preset = customPresets[i].split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                if (preset[0] == id) {
                    customPresets[i] = "$id|$name|$envVars"
                    break
                }
            }

            customPresetsStr = customPresets.joinToString(",")
        } else {
            val preset = Box86_64Preset.CUSTOM + "-" + getNextPresetId(context, prefix) + "|" + name + "|" + envVars.toString()

            customPresetsStr += (if (!customPresetsStr.isEmpty()) "," else "") + preset
        }

        preferences.edit { putString(key, customPresetsStr) }
    }

    fun duplicatePreset(prefix: String, context: Context, id: String?) {
        val presets = getPresets(prefix, context)
        var originPreset: Box86_64Preset? = null
        for (preset in presets) {
            if (preset.id == id) {
                originPreset = preset
                break
            }
        }
        if (originPreset == null) return

        var newName: String?
        var i = 1

        while (true) {
            newName = originPreset.name + " (" + i + ")"

            var found = false

            for (preset in presets) {
                if (preset.name == newName) {
                    found = true
                    break
                }
            }

            if (!found) break

            i++
        }

        editPreset(prefix = prefix, context = context, id = null, name = newName, envVars = getEnvVars(prefix, context, originPreset.id))
    }

    fun removePreset(prefix: String?, context: Context, id: String?) {
        val key = prefix + "_custom_presets"
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val oldCustomPresetsStr = preferences.getString(key, "")!!

        var newCustomPresetsStr = ""

        val customPresets = oldCustomPresetsStr.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        for (i in customPresets.indices) {
            val preset = customPresets[i].split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            if (preset[0] != id) {
                newCustomPresetsStr += (if (!newCustomPresetsStr.isEmpty()) "," else "") + customPresets[i]
            }
        }

        preferences.edit { putString(key, newCustomPresetsStr) }
    }
}
