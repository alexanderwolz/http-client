package de.alexanderwolz.http.client.model

import com.google.gson.Gson
import de.alexanderwolz.http.client.model.converter.ElementConverter
import de.alexanderwolz.http.client.model.type.ContentType

class WrappedProductConverter : ElementConverter<Any> {

    override fun serialize(type: ContentType, element: Any): ByteArray {
        return Gson().toJson(element).toByteArray()
    }

    override fun deserialize(type: ContentType, bytes: ByteArray): Any {
        return Gson().fromJson(bytes.decodeToString(), WrappedProduct::class.java)
    }

}