package de.alexanderwolz.http.client.model.content

import de.alexanderwolz.http.client.model.Form
import de.alexanderwolz.http.client.model.token.OAuthErrorResponse
import de.alexanderwolz.http.client.model.token.OAuthTokenResponse
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

abstract class AbstractContentResolver : ContentResolver {

    override fun serialize(type: ContentType, element: Any): ByteArray {
        return serialize(type.clazz, element)
    }

    override fun serialize(clazz: KClass<*>, element: Any): ByteArray {
        if (element::class.java.getAnnotation(Serializable::class.java) != null) {
            //if its serializable, we just need to return the byte array representation of its toString
            return element.toString().toByteArray()
        }
        return when (element) {
            is ByteArray -> element
            is String -> element.toByteArray()
            is JsonElement -> element.toString().toByteArray()
            is OAuthTokenResponse -> element.toString().toByteArray()
            is OAuthErrorResponse -> element.toString().toByteArray()
            is Form -> element.encodeToString().toByteArray()
            else -> throw NoSuchElementException(
                "Unknown element ${clazz.java}: Please specify a custom ${ContentResolver::class.java.simpleName} object"
            )
        }
    }

    override fun deserialize(type: ContentType, bytes: ByteArray): Any {
        return deserialize(type.clazz, bytes)
    }

    @OptIn(InternalSerializationApi::class)
    override fun deserialize(clazz: KClass<*>, bytes: ByteArray): Any {
        if (clazz.java.getAnnotation(Serializable::class.java) != null) {
            if (isJson(bytes)) {
                return Json.decodeFromString(clazz.serializer(), bytes.decodeToString())
            }
            //TODO what about now?
        }
        return when (clazz) {
            ByteArray::class -> bytes
            String::class -> bytes.decodeToString()
            JsonElement::class -> Json.parseToJsonElement(bytes.decodeToString())
            OAuthTokenResponse::class -> Json.decodeFromString<OAuthTokenResponse>(bytes.decodeToString())
            OAuthErrorResponse::class -> Json.decodeFromString<OAuthErrorResponse>(bytes.decodeToString())
            Form::class -> Form(bytes.decodeToString())
            else -> throw NoSuchElementException("Unknown element ${clazz.java}")
        }
    }

    private fun isJson(bytes: ByteArray): Boolean {
        bytes.first().toInt().toChar().also {
            return it == '{' || it == '['
        }
    }

}