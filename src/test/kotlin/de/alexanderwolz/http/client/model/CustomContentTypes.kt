package de.alexanderwolz.http.client.model

import de.alexanderwolz.http.client.model.content.resolver.ContentResolver
import de.alexanderwolz.http.client.model.content.type.ContentType
import de.alexanderwolz.http.client.util.MockUtils.MEDIA_TYPE_PRODUCT
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

enum class CustomContentTypes(
    override val mediaType: String,
    override val clazz: KClass<*>,
    override val resolver: ContentResolver
) : ContentType {

    PRODUCT(MEDIA_TYPE_PRODUCT, Product::class, ProductContentResolver(Product::class)),
    WRAPPED_PRODUCT(MEDIA_TYPE_PRODUCT, Product::class, ProductContentResolver(WrappedProduct::class));

    private class ProductContentResolver(private val parentClass: KClass<*>) : ContentResolver {

        override fun getParentClass(type: ContentType): KClass<*> {
            return parentClass
        }

        override fun extract(type: ContentType, element: Any): Any {
            return (element as WrappedProduct).element
        }

        override fun serialize(clazz: KClass<*>, element: Any): ByteArray {
            if (element is WrappedProduct) {
                return Json.encodeToString(element).toByteArray()
            }
            if (element is Product) {
                return Json.encodeToString(element).toByteArray()
            }
            throw NoSuchElementException("Unknown element ${element.javaClass}")
        }

        override fun serialize(type: ContentType, element: Any): ByteArray {
            return serialize(type.clazz, element)
        }

        override fun deserialize(clazz: KClass<*>, bytes: ByteArray): Any {
            val serializer = serializer(clazz.java)
            return Json.decodeFromString(serializer, bytes.decodeToString())
        }

        override fun deserialize(type: ContentType, bytes: ByteArray): Any {
            return deserialize(type.clazz, bytes)
        }
    }
}