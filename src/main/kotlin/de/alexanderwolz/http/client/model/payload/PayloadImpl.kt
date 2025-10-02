package de.alexanderwolz.http.client.model.payload

import de.alexanderwolz.commons.log.Logger
import de.alexanderwolz.http.client.model.converter.Converter
import de.alexanderwolz.http.client.model.type.ContentType

internal class PayloadImpl : Payload {

    private val logger = Logger(javaClass)

    override val type: ContentType
    override val bytes: ByteArray
    override val element: Any

    constructor(type: ContentType, bytes: ByteArray) {
        this.type = type
        this.bytes = bytes
        this.element = deserialize()
        typeCheck()
    }

    constructor(type: ContentType, element: Any) {
        this.type = type
        this.element = element
        this.bytes = serialize()
        typeCheck()
    }

    private fun typeCheck() {
        if (!type.clazz.java.isAssignableFrom(element::class.java)) {
            throw IllegalStateException("Element does not match content-type class: ${element::class.java} -> ${type.clazz.java}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun serialize(): ByteArray {
        val converter = type.converter as Converter<Any>
        return converter.serialize(element, type).apply {
            logger.trace {
                "Serialized from element ${element::class.java} into ByteArray " +
                        "(media type '${type.mediaType}', contentType class=${type.clazz.java})"
            }
        }
    }

    private fun deserialize(): Any {
        return type.converter.deserialize(bytes, type).apply {
            logger.trace {
                "Deserialized from media type '${type.mediaType}' into " +
                        "${this::class.java} (contentType class=${type.clazz.java})"
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Payload

        if (type != other.type) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}