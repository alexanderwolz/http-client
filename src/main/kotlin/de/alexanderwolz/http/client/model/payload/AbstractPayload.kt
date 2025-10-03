package de.alexanderwolz.http.client.model.payload

import de.alexanderwolz.commons.log.Logger
import de.alexanderwolz.http.client.model.content.type.ContentType

@Suppress("UNCHECKED_CAST")
abstract class AbstractPayload : Payload {

    protected val logger = Logger(javaClass)

    var source: Any? = null

    override lateinit var type: ContentType
        protected set

    override lateinit var bytes: ByteArray
        protected set

    override lateinit var element: Any
        protected set

    constructor(type: ContentType, bytes: ByteArray) {
        this.type = type
        this.source = bytes
        val resolver = type.resolver
        val parentClass = resolver.getParentClass(type)
        if (type.clazz == parentClass) {
            this.bytes = bytes
            this.element = resolver.deserialize(type.clazz, bytes)
        } else {
            //TODO how to determine which element ist set here???
            //wrapping
            logger.debug { "Type $type is wrapped into $parentClass" }
            val parent = resolver.deserialize(parentClass, bytes)
            this.element = resolver.extract(type, parent)
            this.bytes = resolver.serialize(type, this.element)
        }
        typeCheck()
    }

    constructor(type: ContentType, element: Any) {
        this.type = type
        this.source = element
        val resolver = type.resolver
        val parentClass = resolver.getParentClass(type)
        if (type.clazz == parentClass) {
            this.element = element
            this.bytes = resolver.serialize(type.clazz, element)
        } else {
            //wrapping
            logger.debug { "Type $type is wrapped into $parentClass" }
            val parent = element
            this.element = resolver.extract(type, parent)
            this.bytes = resolver.serialize(type, this.element)
        }
        typeCheck()
    }

    private fun typeCheck() {
        if (!type.clazz.java.isAssignableFrom(element::class.java)) {
            throw IllegalStateException("Element does not match content-type class: ${element::class.java} -> ${type.clazz.java}")
        }
    }

}