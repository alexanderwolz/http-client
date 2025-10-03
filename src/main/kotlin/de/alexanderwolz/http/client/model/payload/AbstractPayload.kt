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
        if (bytes != null) {
            logger.trace { "Creating payload from bytes: ${bytes.size} -> ${bytes.decodeToString()}" }
            this.bytes = bytes
            this.element = deserialize(type.clazz as KClass<T>, bytes, customResolver)
        }
        if (element != null) {
            logger.trace { "Creating payload from element: $element" }
            this.element = element
            this.bytes = serialize(type.clazz as KClass<T>, element, customResolver)
        }
        typeCheck()
    }

    private fun deserialize(clazz: KClass<T>, bytes: ByteArray, resolver: ContentResolver?): T {
        resolver?.let {
            try {
                logger.trace { "Using custom resolver: $it" }
                return it.deserialize(clazz, bytes) as T
            } catch (t: Throwable) {
                logger.error(t) { "Resolver threw Exception" }
            }
        }
        logger.trace { "Using default resolver${resolver?.let { "" } ?: ", because custom resolver was null"}" }
        return object : AbstractContentResolver() {}.deserialize(clazz, bytes) as T
    }

    private fun serialize(clazz: KClass<T>, element: T, resolver: ContentResolver?): ByteArray {
        resolver?.let {
            try {
                logger.trace { "Using custom resolver: $it" }
                return it.serialize(clazz, element)
            } catch (t: Throwable) {
                logger.error(t) { "Resolver threw Exception" }
            }
        }
        logger.trace { "Using default resolver" }
        return object : AbstractContentResolver() {}.serialize(clazz, element)
    }

    private fun typeCheck() {
        if (!type.clazz.java.isAssignableFrom(element::class.java)) {
            throw IllegalStateException("Element does not match content-type class: ${element::class.java} -> ${type.clazz.java}")
        }
    }

}