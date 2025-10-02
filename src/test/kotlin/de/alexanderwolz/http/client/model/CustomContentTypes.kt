package de.alexanderwolz.http.client.model

import de.alexanderwolz.http.client.model.converter.Converter
import de.alexanderwolz.http.client.model.type.ContentType
import de.alexanderwolz.http.client.model.wrapper.Wrapper
import kotlin.reflect.KClass

const val MEDIA_TYPE_PRODUCT = "application/product"

enum class CustomContentTypes(
    override val mediaType: String, override val clazz: KClass<*>,
    override val converter: Converter<*>,
    override val wrapper: Wrapper<*, *>? = null
) : ContentType {


    PRODUCT(MEDIA_TYPE_PRODUCT, Product::class, ProductConverter()),

    WRAPPED_PRODUCT(
        MEDIA_TYPE_PRODUCT,
        Product::class,
        WrappedProductConverter(),
        object : Wrapper<Product, WrappedProduct> {

            override fun wrap(element: Product, clazz: KClass<Product>): WrappedProduct {
                val parent = WrappedProduct(element)
                return parent
            }

            override fun unwrap(parent: WrappedProduct, clazz: KClass<Product>): Product {
                return parent.element
            }

        });
}