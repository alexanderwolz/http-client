package de.alexanderwolz.http.client.model.payload

import de.alexanderwolz.commons.log.Logger
import de.alexanderwolz.http.client.model.content.AbstractContentResolver
import de.alexanderwolz.http.client.model.content.ContentResolver
import de.alexanderwolz.http.client.model.content.ContentType
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
abstract class AbstractPayload<T : Any> : Payload<T> {

    protected val logger = Logger(javaClass)

    override lateinit var type: ContentType
        protected set

    override lateinit var bytes: ByteArray
        protected set

    override lateinit var element: T
        protected set

    constructor(type: ContentType, bytes: ByteArray, customResolver: ContentResolver? = null) {
        initialize(type, bytes, customResolver)
    }

    constructor(type: ContentType, element: T, customResolver: ContentResolver? = null) {
        initialize(type, element, customResolver)
    }

    protected open fun initialize(type: ContentType, element: T, customResolver: ContentResolver?) {
        this.type = type
        handleSingle(null, element, customResolver)
        typeCheck()
    }

    protected open fun initialize(type: ContentType, bytes: ByteArray, customResolver: ContentResolver?) {
        this.type = type
        handleSingle(bytes, null, customResolver)
        typeCheck()
    }

    private fun handleSingle(bytes: ByteArray?, element: T?, customResolver: ContentResolver?) {
        if (bytes != null) {
            logger.trace { "Creating payload from bytes: ${bytes.size} -> ${bytes.decodeToString()}" }
            this.bytes = bytes
            this.element = deserialize(type.clazz as KClass<Any>, bytes, customResolver) as T
        }
        if (element != null) {
            logger.trace { "Creating payload from element: $element" }
            this.element = element
            this.bytes = serialize(type.clazz as KClass<Any>, element, customResolver)
        }
    }

    protected fun deserialize(clazz: KClass<*>, bytes: ByteArray, resolver: ContentResolver?): Any {
        resolver?.let {
            try {
                logger.trace { "Using custom resolver for deserialization: $it" }
                return it.deserialize(clazz, bytes)
            } catch (t: Throwable) {
                logger.trace { "Resolver threw Exception (${t.message ?: t.javaClass.simpleName}" }
            }
        }
        logger.trace { "Using default resolver for deserialization ${resolver?.let { "" } ?: ", because custom resolver was null"}" }
        return DefaultContentResolver().deserialize(clazz, bytes)
    }

    protected fun serialize(clazz: KClass<*>, element: Any, resolver: ContentResolver?): ByteArray {
        resolver?.let {
            try {
                logger.trace { "Using custom resolver for serialization: $it" }
                return it.serialize(clazz, element)
            } catch (t: Throwable) {
                logger.trace { "Resolver threw Exception (${t.message ?: t.javaClass.simpleName}" }
            }
        }
        logger.trace { "Using default resolver for serialization ${resolver?.let { "" } ?: ", because custom resolver was null"}" }
        return DefaultContentResolver().serialize(clazz, element)
    }

    private fun typeCheck() {
        if (!type.clazz.java.isAssignableFrom(element::class.java)) {
            throw IllegalStateException("Element does not match content-type class: ${element::class.java} -> ${type.clazz.java}")
        }
    }

    protected class DefaultContentResolver : AbstractContentResolver() {

        override fun extract(parentClazz: KClass<*>, parent: Any): Any {
            throw NoSuchElementException("Please specify custom content resolver to handle $parentClazz")
        }

        override fun wrap(parentClazz: KClass<*>, child: Any): Any {
            throw NoSuchElementException("Please specify custom content resolver to handle $parentClazz")
        }
    }

}