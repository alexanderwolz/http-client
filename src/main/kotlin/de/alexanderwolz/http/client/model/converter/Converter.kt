package de.alexanderwolz.http.client.model.converter

import de.alexanderwolz.http.client.model.type.ContentType

interface Converter<T:Any> {
    fun serialize(element: T, type: ContentType): ByteArray
    fun deserialize(bytes: ByteArray, type: ContentType): T
}