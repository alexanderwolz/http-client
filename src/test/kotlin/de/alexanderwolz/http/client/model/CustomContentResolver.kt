package de.alexanderwolz.http.client.model

import de.alexanderwolz.http.client.model.content.AbstractContentResolver
import de.alexanderwolz.http.client.model.content.ContentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

class CustomContentResolver : AbstractContentResolver() {

    override fun extract(parentClazz: KClass<*>, parent: Any): Any {
        if (parent::class == parentClazz) {
            if (parent is WrappedProduct) return parent.element
            throw NoSuchElementException("Unsupported parent element $parent")
        }
        return parent
    }

    override fun serialize(clazz: KClass<*>, element: Any): ByteArray {
        if (element is WrappedProduct) {
            return Json.encodeToString(element).toByteArray()
        }
        if (element is Product) {
            return Json.encodeToString(element).toByteArray()
        }
        return super.serialize(clazz, element)
    }

    override fun serialize(type: ContentType, element: Any): ByteArray {
        return serialize(type.clazz, element)
    }

    override fun deserialize(clazz: KClass<*>, bytes: ByteArray): Any {
        try {
            val serializer = serializer(clazz.java)
            return Json.decodeFromString(serializer, bytes.decodeToString())
        } catch (_: Exception) {
            return super.deserialize(clazz, bytes)
        }
    }

    override fun deserialize(type: ContentType, bytes: ByteArray): Any {
        return deserialize(type.clazz, bytes)
    }
}
