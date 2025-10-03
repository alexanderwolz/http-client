package de.alexanderwolz.http.client.model.payload

import de.alexanderwolz.commons.log.Logger
import de.alexanderwolz.http.client.model.content.AbstractContentResolver
import de.alexanderwolz.http.client.model.content.ContentResolver
import de.alexanderwolz.http.client.model.content.ContentType
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
abstract class AbstractPayload<T : Any> : Payload<T> {

    protected val logger = Logger(javaClass)

    var source: ByteArray? = null
        private set

    override lateinit var type: ContentType
        protected set

    override lateinit var bytes: ByteArray
        protected set

    override lateinit var element: T
        protected set

    constructor(type: ContentType, bytes: ByteArray, customResolver: ContentResolver? = null) {
        this.type = type
        if (type.wrappingClazz == null) {
            handleSingle(bytes, null, customResolver)
        } else {
            if (type.wrappingClazz == type.clazz) {
                // "Stupid is as stupid does"
                logger.trace { "Wrapping class is specified, but equals element type" }
                handleSingle(bytes, null, customResolver)
            } else {
                //Wrapping class is specified
                handleWrapped(type.wrappingClazz as KClass<Any>, bytes, null, customResolver)
            }
        }
        typeCheck()
    }

    constructor(type: ContentType, element: T, customResolver: ContentResolver? = null) {
        this.type = type
        if (type.wrappingClazz == null) {
            handleSingle(null, element, customResolver)
        } else {
            if (type.wrappingClazz == type.clazz) {
                // "Stupid is as stupid does"
                logger.trace { "Wrapping class is specified, but equals element type" }
                handleSingle(null, element, customResolver)
            } else {
                //Wrapping class is specified
                handleWrapped(type.wrappingClazz as KClass<Any>, null, element, customResolver)
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
        logger.trace { "Wrapping class specified (${wrappingClazz.java})- handling wrapped content" }
        if (bytes != null) {
            this.source = bytes
            val element = try {
                deserialize(wrappingClazz as KClass<Any>, bytes, customResolver) as T
            } catch (t: Throwable) {
                //probably not a parent class
                deserialize(type.clazz as KClass<Any>, bytes, customResolver) as T
            }
            val isParent = element::class == type.wrappingClazz
            if (isParent) {
                this.element = element
                this.bytes = serialize(type.wrappingClazz as KClass<Any>, this.element, customResolver)
            } else {
                this.element = wrap(wrappingClazz, element, customResolver) as T
                this.bytes = serialize(wrappingClazz, this.element, customResolver)
            }
        }
        if (element != null) {
            val isParent = element::class == type.wrappingClazz
            if (isParent) {
                this.element = element
                this.bytes = serialize(type.wrappingClazz as KClass<Any>, this.element, customResolver)
            } else {
                this.element = wrap(wrappingClazz, element, customResolver) as T
                this.bytes = serialize(wrappingClazz, this.element, customResolver)
            }
        }
    }

    private fun extract(wrappingClazz: KClass<*>, element: Any, resolver: ContentResolver?): T {
        resolver?.let {
            try {
                logger.trace { "Using custom resolver for element extraction: $it" }
                return it.extract(wrappingClazz, element) as T
            } catch (t: Throwable) {
                logger.error(t) { "Resolver threw Exception" }
            }
        }
        logger.trace { "Using default resolver for element extraction${resolver?.let { "" } ?: ", because custom resolver was null"}" }
        return DefaultContentResolver().extract(wrappingClazz, element) as T
    }

    private fun wrap(wrappingClazz: KClass<*>, element: T, resolver: ContentResolver?): Any {
        resolver?.let {
            try {
                logger.trace { "Using custom resolver for wrapping: $it" }
                return it.wrap(wrappingClazz, element)
            } catch (t: Throwable) {
                logger.error(t) { "Resolver threw Exception" }
            }
        }
        logger.trace { "Using default resolver for wrapping${resolver?.let { "" } ?: ", because custom resolver was null"}" }
        return DefaultContentResolver().wrap(wrappingClazz, element)
    }

    private fun deserialize(clazz: KClass<*>, bytes: ByteArray, resolver: ContentResolver?): Any {
        resolver?.let {
            try {
                logger.trace { "Using custom resolver for deserialization: $it" }
                return it.deserialize(clazz, bytes)
            } catch (t: Throwable) {
                logger.warn { "Resolver threw Exception (${t.message ?: t.javaClass.simpleName}" }
            }
        }
        logger.trace { "Using default resolver for deserialization ${resolver?.let { "" } ?: ", because custom resolver was null"}" }
        return DefaultContentResolver().deserialize(clazz, bytes)
    }

    private fun serialize(clazz: KClass<*>, element: Any, resolver: ContentResolver?): ByteArray {
        resolver?.let {
            try {
                logger.trace { "Using custom resolver for serialization: $it" }
                return it.serialize(clazz, element)
            } catch (t: Throwable) {
                logger.error(t) { "Resolver threw Exception" }
            }
        }
        logger.trace { "Using default resolver for serialization ${resolver?.let { "" } ?: ", because custom resolver was null"}" }
        return DefaultContentResolver().serialize(clazz, element)
    }

    private fun typeCheck() {
        if (type.wrappingClazz == null) {
            if (!type.clazz.java.isAssignableFrom(element::class.java)) {
                throw IllegalStateException("Element does not match content-type class: ${element::class.java} -> ${type.clazz.java}")
            }
        } else {
            //TODO check parent element
        }
    }

    private class DefaultContentResolver : AbstractContentResolver() {

        override fun extract(parentClazz: KClass<*>, parent: Any): Any {
            throw NoSuchElementException("Please specify custom content resolver to handle $parentClazz")
        }

        override fun wrap(parentClazz: KClass<*>, element: Any): Any {
            throw NoSuchElementException("Please specify custom content resolver to handle $parentClazz")
        }
    }

}