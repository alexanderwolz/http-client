package de.alexanderwolz.http.client.model

import com.google.gson.Gson
import de.alexanderwolz.http.client.model.converter.ElementConverter
import de.alexanderwolz.http.client.model.type.ContentType

class ProductConverter : ElementConverter<Product> {

    override fun serialize(type: ContentType, element: Product): ByteArray {
        return Gson().toJson(element).toByteArray()
    }

    override fun deserialize(type: ContentType, bytes: ByteArray): Product {
        val decoded = bytes.decodeToString()
        return Gson().fromJson(decoded, Product::class.java)
    }

}