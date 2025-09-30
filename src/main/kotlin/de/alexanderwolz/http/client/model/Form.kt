package de.alexanderwolz.http.client.model

import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*

class Form(map: Map<String, String>) {

    private val map = HashMap<String, String>()

    init {
        this.map.putAll(map)
    }

    constructor(encodedString: String) : this(
        encodedString.split("&").associate { pair ->
            val (key, value) = pair.split("=", limit = 2)
            URLDecoder.decode(key, "UTF-8") to URLDecoder.decode(value, "UTF-8")
        }
    )

    fun encodeToString(): String {
        return map.entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }
    }

    fun add(name: String, value: String) {
        map[name] = value
    }

    val entries: Set<Map.Entry<String, String>> = Collections.unmodifiableSet(map.entries)

}