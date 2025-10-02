package de.alexanderwolz.http.client.model

import de.alexanderwolz.http.client.model.converter.Converter
import de.alexanderwolz.http.client.model.type.ContentType
import de.alexanderwolz.http.client.model.wrapper.Wrapper
import kotlin.reflect.KClass

enum class CustomContentTypes(
    override val mediaType: String, override val clazz: KClass<*>,
    override val converter: Converter<*>,
    override val wrapper: Wrapper<*, *>? = null
) : ContentType {
    PRODUCT("application/product", Product::class, ProductConverter()),

    WRAPPED_PRODUCT(
        "application/product",
        Product::class,
        ProductConverter(),
        object : Wrapper<Product, WrappedProduct> {

            override fun wrap(element: Product): WrappedProduct {
                return WrappedProduct(element)
            }

            override fun unwrap(parent: WrappedProduct): Product {
                return parent.element
            }
        });
}