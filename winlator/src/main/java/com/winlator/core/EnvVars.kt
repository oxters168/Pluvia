package com.winlator.core

class EnvVars : Iterable<String?> {

    private val data = LinkedHashMap<String, String>()

    constructor()

    constructor(values: String?) {
        putAll(values)
    }

    fun put(name: String, value: Any) {
        data[name] = value.toString()
    }

    fun putAll(values: String?) {
        if (values.isNullOrEmpty()) {
            return
        }

        val parts = values.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        for (part in parts) {
            val index = part.indexOf("=")
            val name = part.substring(0, index)
            val value = part.substring(index + 1)
            data[name] = value
        }
    }

    fun putAll(envVars: EnvVars) {
        data.putAll(envVars.data)
    }

    fun get(name: String): String = data.getOrDefault(name, "")

    fun remove(name: String) {
        data.remove(name)
    }

    fun has(name: String): Boolean = data.containsKey(name)

    fun clear() {
        data.clear()
    }

    val isEmpty: Boolean
        get() = data.isEmpty()

    override fun toString(): String = toStringArray().joinToString(" ")

    fun toEscapedString(): String {
        var result = ""

        for (key in data.keys) {
            if (result.isNotEmpty()) {
                result += " "
            }

            val value = data[key]

            result += key + "=" + value!!.replace(" ", "\\ ")
        }

        return result
    }

    fun toStringArray(): Array<String?> {
        val stringArray = arrayOfNulls<String>(data.size)
        var index = 0

        for (key in data.keys) {
            stringArray[index++] = key + "=" + data[key]
        }

        return stringArray
    }

    override fun iterator(): MutableIterator<String> = data.keys.iterator()
}
