package de.alexanderwolz.http.client.model.payload

import de.alexanderwolz.commons.log.Logger
import de.alexanderwolz.http.client.model.content.AbstractContentResolver
import de.alexanderwolz.http.client.model.content.ContentResolver
import de.alexanderwolz.http.client.model.content.ContentType
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
abstract class AbstractPayload<T : Any>(
    type: ContentType,
    bytes: ByteArray? = null,
    element: T? = null,
    customResolver: ContentResolver? = null
) : Payload<T> {

    protected val logger = Logger(javaClass)

    override lateinit var type: ContentType
        protected set

    override lateinit var bytes: ByteArray
        protected set

    override lateinit var element: T
        protected set

    init {
        this.type = type
        if (bytes == null && element == null) {
            throw IllegalStateException("Both bytes and element are null. Please check input")
        }
        if (type.wrappingClazz == null) {
            handleSingle(bytes, element, customResolver)
        } else {
            if (type.wrappingClazz == type.clazz) {
                // "Stupid is as stupid does"
                handleSingle(bytes, element, customResolver)
            } else {
                //Wrapping class is specified
                handleWrapped(type.wrappingClazz as KClass<Any>, bytes, element, customResolver)
            }
        }
        typeCheck()
    }

    private fun handleSingle(bytes: ByteArray?, element: T?, customResolver: ContentResolver?) {
        logger.trace { "No wrapping class specified - handling single content" }
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

    private fun handleWrapped(
        wrappingClazz: KClass<*>,
        bytes: ByteArray?,
        element: T?,
        customResolver: ContentResolver?
    ) {
        logger.trace { "Wrapping class specified (${type.clazz.java})- handling single content" }

        //TODO how to determine which element ist set here???
        if (bytes != null) {
            val parent = deserialize(wrappingClazz, bytes, customResolver)
            val parentBytes = bytes
            val child = extract(wrappingClazz, parent, customResolver)
            val childBytes = serialize(type.clazz as KClass<Any>, child, customResolver)
            println(parent)
            print(child)
            this.element = child
            this.bytes = childBytes
        }
        if (element != null) {
            val parent = element
            val parentBytes = serialize(wrappingClazz, element, customResolver)
            val child = extract(wrappingClazz, parent, customResolver)
            val childBytes = serialize(type.clazz as KClass<Any>, child, customResolver)
            println(parent)
            print(child)
            this.element = child
            this.bytes = childBytes
        }
    }

    private fun extract(wrappingClazz: KClass<*>, element: Any, resolver: ContentResolver?): T {
        resolver?.let {
            try {
                logger.trace { "Using custom resolver: $it" }
                return it.extract(wrappingClazz, element) as T
            } catch (t: Throwable) {
                logger.error(t) { "Resolver threw Exception" }
            }
        }
        logger.trace { "Using default resolver${resolver?.let { "" } ?: ", because custom resolver was null"}" }
        return DefaultContentResolver().extract(wrappingClazz, element) as T
    }

    private fun deserialize(clazz: KClass<*>, bytes: ByteArray, resolver: ContentResolver?): Any {
        resolver?.let {
            try {
                logger.trace { "Using custom resolver: $it" }
                return it.deserialize(clazz, bytes)
            } catch (t: Throwable) {
                logger.error(t) { "Resolver threw Exception" }
            }
        }
        logger.trace { "Using default resolver${resolver?.let { "" } ?: ", because custom resolver was null"}" }
        return DefaultContentResolver().deserialize(clazz, bytes)
    }

    private fun serialize(clazz: KClass<*>, element: Any, resolver: ContentResolver?): ByteArray {
        resolver?.let {
            try {
                logger.trace { "Using custom resolver: $it" }
                return it.serialize(clazz, element)
            } catch (t: Throwable) {
                logger.error(t) { "Resolver threw Exception" }
            }
        }
        logger.trace { "Using default resolver" }
        return DefaultContentResolver().serialize(clazz, element)
    }

    private fun typeCheck() {
        if (!type.clazz.java.isAssignableFrom(element::class.java)) {
            throw IllegalStateException("Element does not match content-type class: ${element::class.java} -> ${type.clazz.java}")
        }
    }

    private class DefaultContentResolver : AbstractContentResolver() {
        override fun extract(parentClazz: KClass<*>, element: Any): Any {
            if (element::class == parentClazz) return element
            throw NoSuchElementException("Please specify custom content resolver to handle $parentClazz")
        }

    }

}