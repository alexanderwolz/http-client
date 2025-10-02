package de.alexanderwolz.http.client.model.payload

import de.alexanderwolz.http.client.instance.Settings
import de.alexanderwolz.http.client.model.type.BasicContentTypes
import de.alexanderwolz.http.client.model.type.ContentType

interface Payload {
    val type: ContentType
    val bytes: ByteArray
    val element: Any

    companion object {

        val EMPTY = object : Payload {
            override val type = BasicContentTypes.EMPTY
            override val element = ""
            override val bytes = element.toByteArray()
        }

        fun create(type: ContentType, bytes: ByteArray): Payload {
            if (Settings.httpLibrary == Settings.HttpLibrary.OK) {
                return PayloadImpl(type, bytes)
            }
            throw NoSuchElementException("Unknown HTTP library '${Settings.httpLibrary}'")
        }

        fun create(type: ContentType, element: Any): Payload {
            if (Settings.httpLibrary == Settings.HttpLibrary.OK) {
                return PayloadImpl(type, element)
            }
            throw NoSuchElementException("Unknown HTTP library '${Settings.httpLibrary}'")
        }
    }
}