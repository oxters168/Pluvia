package com.winlator.core

import org.json.JSONArray
import org.json.JSONException
import timber.log.Timber

object ArrayUtils {

    @JvmStatic
    fun concat(vararg elements: ByteArray): ByteArray {
        var result = elements[0].copyOf(elements[0].size)

        for (i in 1..<elements.size) {
            val newArray = result.copyOf(result.size + elements[i].size)
            System.arraycopy(elements[i], 0, newArray, result.size, elements[i].size)
            result = newArray
        }

        return result
    }

    @SafeVarargs
    fun <T> concat(vararg elements: Array<T>): Array<T?> {
        var result = elements[0].copyOf(elements[0].size)

        for (i in 1..<elements.size) {
            val newArray = result.copyOf(result.size + elements[i].size)
            System.arraycopy(elements[i], 0, newArray, result.size, elements[i].size)
            result = newArray
        }

        return result
    }

    fun toStringArray(data: JSONArray): Array<String?> {
        val stringArray = arrayOfNulls<String>(data.length())

        for (i in 0..<data.length()) {
            try {
                stringArray[i] = data.getString(i)
            } catch (e: JSONException) {
                Timber.w(e)
            }
        }

        return stringArray
    }
}
