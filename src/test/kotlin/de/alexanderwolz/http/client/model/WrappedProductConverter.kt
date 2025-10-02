package de.alexanderwolz.http.client.model

import com.google.gson.Gson
import de.alexanderwolz.http.client.model.converter.Converter
import de.alexanderwolz.http.client.model.type.ContentType

class WrappedProductConverter : Converter<Any> {

    //TODO converter needs a wrapMethod

    override fun serialize(element: Any, type: ContentType): ByteArray {
        if (element is WrappedProduct) {
            return Gson().toJson(element).toByteArray()
        }
        if (element is Product) {
            return Gson().toJson(WrappedProduct(element)).toByteArray()
        }
        throw IllegalArgumentException()
    }

    override fun deserialize(bytes: ByteArray, type: ContentType): Any {
        //TODO check if it is Wrapped or Product?
        //type always is product here

        return Gson().fromJson(bytes.decodeToString(), WrappedProduct::class.java)
        throw IllegalArgumentException()
    }

}