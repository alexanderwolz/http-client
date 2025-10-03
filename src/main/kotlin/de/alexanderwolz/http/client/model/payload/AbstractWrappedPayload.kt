package de.alexanderwolz.http.client.model.payload

import de.alexanderwolz.http.client.model.content.ContentResolver
import de.alexanderwolz.http.client.model.content.ContentType
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
abstract class AbstractWrappedPayload<P : Any, T : Any> : AbstractPayload<T>, WrappedPayload<P, T> {

    override lateinit var parent: P
        protected set

    override lateinit var parentBytes: ByteArray
        protected set

    constructor(type: ContentType, bytes: ByteArray, customResolver: ContentResolver? = null) : super(
        type,
        bytes,
        customResolver
    )

    constructor(type: ContentType, element: T, customResolver: ContentResolver? = null) : super(
        type,
        element,
        customResolver
    )

    private fun typeCheck() {
        if (!type.clazz.java.isAssignableFrom(element::class.java)) {
            throw IllegalStateException("Element does not match content-type class: ${element::class.java} -> ${type.clazz.java}")
        }
        type.wrappingClazz?.let {
            if (!it.java.isAssignableFrom(parent::class.java)) {
                throw IllegalStateException("Parent does not match content-type wrapper class: ${element::class.java} -> ${it.java}")
            }
        }
    }

    override fun initialize(type: ContentType, bytes: ByteArray, customResolver: ContentResolver?) {
        this.type = type
        if (type.wrappingClazz == null) {
            super.initialize(type, bytes, customResolver)
        } else {
            if (type.wrappingClazz == type.clazz) {
                // "Stupid is as stupid does"
                logger.trace { "Wrapping class is specified, but equals element type" }
                super.initialize(type, bytes, customResolver)
            } else {
                //Wrapping class is specified
                handleWrapped(type.wrappingClazz as KClass<P>, bytes, null, customResolver)
            }
        }
        typeCheck()
    }

    override fun initialize(type: ContentType, element: T, customResolver: ContentResolver?) {
        this.type = type
        if (type.wrappingClazz == null) {
            super.initialize(type, element, customResolver)
        } else {
            if (type.wrappingClazz == type.clazz) {
                // "Stupid is as stupid does"
                logger.trace { "Wrapping class is specified, but equals element type" }
                super.initialize(type, element, customResolver)
            } else {
                //Wrapping class is specified
                handleWrapped(type.wrappingClazz as KClass<P>, null, element, customResolver)
            }
        }
        typeCheck()
    }

    private fun handleWrapped(
        wrappingClazz: KClass<P>,
        bytes: ByteArray?,
        element: T?,
        customResolver: ContentResolver?
    ) {
        logger.trace { "Wrapping class specified (${wrappingClazz.java})- handling wrapped content" }
        if (bytes != null) {
            val element = try {
                deserialize(wrappingClazz as KClass<Any>, bytes, customResolver) as T
            } catch (t: Throwable) {
                //probably not a parent class
                deserialize(type.clazz as KClass<Any>, bytes, customResolver) as T
            }
            handleWrappedElement(wrappingClazz, element, customResolver)
        }
        if (element != null) {
            handleWrappedElement(wrappingClazz, element, customResolver)
        }
    }

    private fun handleWrappedElement(wrappingClazz: KClass<P>, element: Any, customResolver: ContentResolver?) {
        val isParent = element::class == wrappingClazz
        if (isParent) {
            this.parent = element as P
            this.parentBytes = serialize(wrappingClazz, this.parent, customResolver)
            this.element = extract(wrappingClazz, parent, customResolver)
            this.bytes = serialize(type.clazz, this.element, customResolver)
        } else {
            this.parent = wrap(wrappingClazz, element as T, customResolver) as P
            this.parentBytes = serialize(wrappingClazz, this.parent, customResolver)
            this.element = element
            this.bytes = serialize(type.clazz, this.element, customResolver)
        }
    }


    private fun extract(wrappingClazz: KClass<*>, parent: Any, resolver: ContentResolver?): T {
        resolver?.let {
            try {
                logger.trace { "Using custom resolver for element extraction: $it" }
                return it.extract(wrappingClazz, parent) as T
            } catch (t: Throwable) {
                logger.error(t) { "Resolver threw Exception" }
            }
        }
        logger.trace { "Using default resolver for element extraction${resolver?.let { "" } ?: ", because custom resolver was null"}" }
        return DefaultContentResolver().extract(wrappingClazz, parent) as T
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

}