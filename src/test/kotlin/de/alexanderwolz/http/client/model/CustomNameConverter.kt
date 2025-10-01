package de.alexanderwolz.http.client.model

import de.alexanderwolz.http.client.model.converter.Converter
import de.alexanderwolz.http.client.model.type.ContentType

class CustomNameConverter : Converter<CustomName> {
    override fun serialize(element: CustomName, type: ContentType): ByteArray {
        return element.name.toByteArray()
    }

    override fun deserialize(bytes: ByteArray, type: ContentType): CustomName {
        return CustomName(bytes.decodeToString())
    }
}