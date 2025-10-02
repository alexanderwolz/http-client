package de.alexanderwolz.http.client.model

import com.google.gson.Gson
import de.alexanderwolz.http.client.Constants.MEDIA_TYPE_PRODUCT
import de.alexanderwolz.http.client.model.converter.ElementConverter
import de.alexanderwolz.http.client.model.converter.ParentConverter
import de.alexanderwolz.http.client.model.type.ContentType
import kotlin.reflect.KClass

enum class CustomContentTypes(
    override val mediaType: String,
    override val clazz: KClass<*>,
    override val elementConverter: ElementConverter<*>,
    override val parentConverter: ParentConverter<*, *>? = null
) : ContentType {

    PRODUCT(MEDIA_TYPE_PRODUCT, Product::class, ProductConverter()),

    WRAPPED_PRODUCT(
        MEDIA_TYPE_PRODUCT, Product::class,
        WrappedProductConverter(),
        object : ParentConverter<WrappedProduct, Product> {

            override val parentClass = WrappedProduct::class

            override fun decode(bytes: ByteArray): WrappedProduct {
                return Gson().fromJson(bytes.decodeToString(), parentClass.java)
            }

            override fun unwrap(parent: WrappedProduct): Product {
                return parent.element
            }

        }
    );
}