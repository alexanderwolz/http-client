package de.alexanderwolz.http.client.model.payload

import de.alexanderwolz.http.client.model.content.type.BasicContentTypes
import de.alexanderwolz.http.client.model.content.type.ContentType

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
            return object : AbstractPayload(type, bytes) {}
        }

        fun create(type: ContentType, element: Any): Payload {
            return object : AbstractPayload(type, element) {}
        }
    }
}