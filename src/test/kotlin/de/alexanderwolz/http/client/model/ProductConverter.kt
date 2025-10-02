package de.alexanderwolz.http.client.model

import com.google.gson.Gson
import de.alexanderwolz.http.client.model.converter.Converter
import de.alexanderwolz.http.client.model.type.ContentType

class ProductConverter : Converter<Product> {

    override fun serialize(element: Product, type: ContentType): ByteArray {
        return Gson().toJson(element).toByteArray()
    }

    override fun deserialize(bytes: ByteArray, type: ContentType): Product {
        return Gson().fromJson(bytes.decodeToString(), Product::class.java)
    }

}