package de.alexanderwolz.http.client.model.payload

import de.alexanderwolz.commons.log.Logger
import de.alexanderwolz.http.client.model.converter.ElementConverter
import de.alexanderwolz.http.client.model.converter.ParentConverter
import de.alexanderwolz.http.client.model.type.ContentType

@Suppress("UNCHECKED_CAST")
abstract class AbstractPayload : Payload {

    protected val logger = Logger(javaClass)

    var parentBytes: ByteArray? = null

    override lateinit var type: ContentType
        protected set

    override lateinit var bytes: ByteArray
        protected set

    override lateinit var element: Any
        protected set

    constructor(type: ContentType, bytes: ByteArray) {
        this.type = type
        if (type.parentConverter != null) {
            logger.debug { "Using parent converter: ${type.parentConverter}" }
            val converter = type.parentConverter as ParentConverter<Any, Any>
            val parent = converter.decode(bytes)
            this.element = converter.unwrap(parent)
            this.bytes = serialize(type, this.element)
            this.parentBytes = bytes
        } else {
            this.bytes = bytes
            this.element = deserialize(type, bytes)
        }
        typeCheck()
    }

    constructor(type: ContentType, element: Any) {
        this.type = type
        if (type.parentConverter != null) {
            val converter = type.parentConverter as ParentConverter<Any, Any>
            if (element::class == converter.parentClass) {
                logger.debug { "Using parent converter: ${type.parentConverter}" }
                this.parentBytes = serialize(type, element)
                val child = converter.unwrap(element)
                this.element = child
                this.bytes = serialize(type, child)
            } else if (element::class == type.clazz) {
                this.element = element
                this.bytes = serialize(type, element)
            } else {
                throw IllegalStateException("Unsupported element ${element.javaClass}")
            }
        } else {
            this.element = element
            this.bytes = serialize(type, element)
        }
        typeCheck()
    }

    private fun typeCheck() {
        if (!type.clazz.java.isAssignableFrom(element::class.java)) {
            throw IllegalStateException("Element does not match content-type class: ${element::class.java} -> ${type.clazz.java}")
        }
    }


    private fun serialize(type: ContentType, element: Any): ByteArray {
        val converter = type.elementConverter as ElementConverter<Any>
        return converter.serialize(type, element).apply {
            logger.trace {
                "Serialized from element ${element::class.java} into ByteArray " +
                        "(media type '${type.mediaType}', contentType class=${type.clazz.java})"
            }
        }
    }

    private fun deserialize(type: ContentType, bytes: ByteArray): Any {
        val converter = type.elementConverter as ElementConverter<Any>
        return converter.deserialize(type, bytes).apply {
            logger.trace {
                "Deserialized from media type '${type.mediaType}' into " +
                        "${this::class.java} (contentType class=${type.clazz.java})"
            }
        }
    }

}