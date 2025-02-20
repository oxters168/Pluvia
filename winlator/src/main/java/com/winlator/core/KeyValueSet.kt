package com.winlator.core

class KeyValueSet(data: String) : Iterable<Array<String>> {

    var data = data.ifEmpty { "" }
        private set

    private fun indexOfKey(key: String): IntArray? {
        var start = 0
        var end = data.indexOf(",")

        if (end == -1) {
            end = data.length
        }

        while (start < end) {
            val index = data.indexOf("=", start)
            val currKey = data.substring(start, index)

            if (currKey == key) {
                return intArrayOf(start, end)
            }

            start = end + 1
            end = data.indexOf(",", start)

            if (end == -1) {
                end = data.length
            }
        }

        return null
    }

    fun get(key: String): String {
        for (keyValue in this) {
            if (keyValue[0] == key) {
                return keyValue[1]
            }
        }

        return ""
    }

    fun put(key: String, value: Any) {
        val range = indexOfKey(key)

        data = if (range != null) {
            StringUtils.replace(data, range[0], range[1], "$key=$value")
        } else {
            (if (data.isNotEmpty()) "$data," else "") + key + "=" + value
        }
    }

    override fun iterator(): Iterator<Array<String>> {
        val start = intArrayOf(0)
        val end = intArrayOf(data.indexOf(","))
        val item = arrayOfNulls<String>(2)

        return object : Iterator<Array<String>> {
            override fun hasNext(): Boolean {
                return start[0] < end[0]
            }

            override fun next(): Array<String> {
                val index = data.indexOf("=", start[0])

                item[0] = data.substring(start[0], index)
                item[1] = data.substring(index + 1, end[0])

                start[0] = end[0] + 1
                end[0] = data.indexOf(",", start[0])

                if (end[0] == -1) {
                    end[0] = data.length
                }

                return item as Array<String>
            }
        }
    }

    override fun toString(): String = data
}
