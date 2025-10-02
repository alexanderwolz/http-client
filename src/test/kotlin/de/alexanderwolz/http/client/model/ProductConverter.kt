package de.alexanderwolz.http.client.model

import com.google.gson.Gson
import de.alexanderwolz.http.client.model.converter.ElementConverter
import kotlin.reflect.KClass

class ProductConverter : ElementConverter<Product> {

    override fun serialize(element: Product, clazz: KClass<Product>): ByteArray {
        return Gson().toJson(element).toByteArray()
    }

    override fun deserialize(bytes: ByteArray, clazz: KClass<Product>): Product {
        val decoded = bytes.decodeToString()
        return Gson().fromJson(decoded, Product::class.java)
    }

}