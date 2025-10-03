package de.alexanderwolz.http.client.model

import de.alexanderwolz.http.client.model.content.ContentType
import de.alexanderwolz.http.client.util.MockUtils.MEDIA_TYPE_PRODUCT
import kotlin.reflect.KClass

enum class CustomContentTypes(
    override val mediaType: String,
    override val clazz: KClass<*>,
    override val wrappingClazz: KClass<*>? = null
) : ContentType {
    PRODUCT(MEDIA_TYPE_PRODUCT, Product::class),
    WRAPPED_PRODUCT(MEDIA_TYPE_PRODUCT, Product::class, WrappedProduct::class);
}