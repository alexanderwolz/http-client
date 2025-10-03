package de.alexanderwolz.http.client.model.payload

import de.alexanderwolz.http.client.model.content.BasicContentTypes
import de.alexanderwolz.http.client.model.content.ContentResolver
import de.alexanderwolz.http.client.model.content.ContentType

interface Payload<T> {
    val type: ContentType
    val bytes: ByteArray
    val element: T

    companion object {

        val EMPTY = object : Payload<String> {
            override val type = BasicContentTypes.EMPTY
            override val element = ""
            override val bytes = element.toByteArray()
        }

        internal fun create(type: ContentType, bytes: ByteArray, resolver: ContentResolver? = null): Payload<Any> {
            return object : AbstractPayload<Any>(type, bytes = bytes, customResolver = resolver) {}
        }

        fun <T : Any> create(type: ContentType, element: T, resolver: ContentResolver? = null): Payload<T> {
            return object : AbstractPayload<T>(type, element = element, customResolver = resolver) {}
        }
    }
}