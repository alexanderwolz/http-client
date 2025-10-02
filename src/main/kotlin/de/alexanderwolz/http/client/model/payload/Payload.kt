package de.alexanderwolz.http.client.model.payload

import de.alexanderwolz.http.client.model.type.BasicContentTypes
import de.alexanderwolz.http.client.model.type.ContentType

interface Payload {
    val type: ContentType
    val bytes: ByteArray
    val element: Any

    fun create(type: ContentType, bytes: ByteArray): Payload {
        return PayloadImpl(type, bytes)
    }

    fun create(type: ContentType, element: Any): Payload {
        return PayloadImpl(type, element)
    }

    companion object {
        val EMPTY = object : Payload {
            override val type = BasicContentTypes.EMPTY
            override val element = ""
            override val bytes = element.toByteArray()
        }
    }
}