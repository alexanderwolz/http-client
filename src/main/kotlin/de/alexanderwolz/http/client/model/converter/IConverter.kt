package de.alexanderwolz.http.client.model.converter

import de.alexanderwolz.http.client.model.payload.Payload
import de.alexanderwolz.http.client.model.type.ContentType

interface IConverter<T : Any> {
    fun serialize(payload: Payload<T>): ByteArray
    fun deserialize(type: ContentType, bytes: ByteArray): Payload<T>
}