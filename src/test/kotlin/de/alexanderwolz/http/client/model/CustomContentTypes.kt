package de.alexanderwolz.http.client.model

import de.alexanderwolz.http.client.model.content.ContentType
import de.alexanderwolz.http.client.util.MockUtils
import kotlin.reflect.KClass

enum class CustomContentTypes(
    override val mediaType: String,
    override val clazz: KClass<*>,
    override val wrappingClazz: KClass<*>? = null
) : ContentType {
    PRODUCT(MockUtils.MEDIA_TYPE_PRODUCT, Product::class),
    PRODUCT_CONTAINER(MockUtils.MEDIA_TYPE_PRODUCT_CONTAINER, ProductContainer::class),
    WRAPPED_PRODUCT(MockUtils.MEDIA_TYPE_WRAPPED, Product::class, ProductContainer::class);
}