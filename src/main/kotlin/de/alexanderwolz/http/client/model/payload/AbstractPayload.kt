package de.alexanderwolz.http.client.model.payload

import de.alexanderwolz.commons.log.Logger
import de.alexanderwolz.http.client.model.converter.Converter
import de.alexanderwolz.http.client.model.type.ContentType
import kotlin.reflect.KClass

abstract class AbstractPayload : Payload {

    protected val logger = Logger(javaClass)

    override lateinit var type: ContentType
        protected set

    override lateinit var bytes: ByteArray
        protected set

    override lateinit var element: Any
        protected set

    constructor(type: ContentType, bytes: ByteArray) {
        this.type = type
        this.bytes = bytes
        this.element = deserialize(bytes)
        typeCheck()
    }

    constructor(type: ContentType, element: Any) {
        this.type = type
        this.bytes = serialize(element)
        this.element = element
        typeCheck()
    }

    private fun typeCheck() {
        if (!type.clazz.java.isAssignableFrom(element::class.java)) {
            throw IllegalStateException("Element does not match content-type class: ${element::class.java} -> ${type.clazz.java}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun serialize(element: Any): ByteArray {
        val converter = type.converter as Converter<Any>
        val clazz = type.clazz as KClass<Any>
        return converter.serialize(element, clazz).apply {
            logger.trace {
                "Serialized from element ${element::class.java} into ByteArray " +
                        "(media type '${type.mediaType}', contentType class=${type.clazz.java})"
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun deserialize(bytes: ByteArray): Any {
        val converter = type.converter as Converter<Any>
        val clazz = type.clazz as KClass<Any>
        return converter.deserialize(bytes, clazz).apply {
            logger.trace {
                "Deserialized from media type '${type.mediaType}' into " +
                        "${this::class.java} (contentType class=${type.clazz.java})"
            }
        }
    }

}