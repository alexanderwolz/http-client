package de.alexanderwolz.http.client.model.content.resolver

import de.alexanderwolz.http.client.model.Form
import de.alexanderwolz.http.client.model.content.type.ContentType
import de.alexanderwolz.http.client.model.token.OAuthErrorResponse
import de.alexanderwolz.http.client.model.token.OAuthTokenResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.reflect.KClass

class BasicContentResolver() : ContentResolver {

    //no wrapping parent element
    override fun getParentClass(type: ContentType): KClass<*> {
        return type.clazz
    }

    //no extraction
    override fun extract(type: ContentType, element: Any): Any {
        return element
    }

    override fun serialize(type: ContentType, element: Any): ByteArray {
        return serialize(type.clazz, element)
    }

    override fun serialize(clazz: KClass<*>, element: Any): ByteArray {
        if (element::class.java.getAnnotation(Serializable::class.java) != null) {
            element.toString().toByteArray()
        }
        return when (element) {
            is ByteArray -> element
            is String -> element.toByteArray()
            is JsonElement -> element.toString().toByteArray()
            is OAuthTokenResponse -> element.toString().toByteArray()
            is OAuthErrorResponse -> element.toString().toByteArray()
            is Form -> element.encodeToString().toByteArray()
            else -> throw NoSuchElementException("Unknown element $clazz")
        }
    }

    override fun deserialize(type: ContentType, bytes: ByteArray): Any {
        return deserialize(type.clazz, bytes)
    }

    override fun deserialize(clazz: KClass<*>, bytes: ByteArray): Any {
        return when (clazz) {
            ByteArray::class -> bytes
            String::class -> bytes.decodeToString()
            JsonElement::class -> Json.parseToJsonElement(bytes.decodeToString())
            OAuthTokenResponse::class -> Json.decodeFromString<OAuthTokenResponse>(bytes.decodeToString())
            OAuthErrorResponse::class -> Json.decodeFromString<OAuthErrorResponse>(bytes.decodeToString())
            Form::class -> Form(bytes.decodeToString())
            else -> throw NoSuchElementException("Unknown element $clazz")
        }
    }

}