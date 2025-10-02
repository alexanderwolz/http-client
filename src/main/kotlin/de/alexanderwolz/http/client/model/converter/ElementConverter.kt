package de.alexanderwolz.http.client.model.converter

import de.alexanderwolz.http.client.model.type.ContentType

interface ElementConverter<T : Any> {
    fun serialize(type: ContentType, element: T): ByteArray
    fun deserialize(type: ContentType, bytes: ByteArray): T
}