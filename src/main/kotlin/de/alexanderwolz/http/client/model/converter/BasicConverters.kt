package de.alexanderwolz.http.client.model.converter

import com.google.gson.Gson
import com.google.gson.JsonElement
import de.alexanderwolz.http.client.model.Form
import de.alexanderwolz.http.client.model.token.OAuthErrorResponse
import de.alexanderwolz.http.client.model.token.OAuthTokenResponse
import de.alexanderwolz.http.client.model.type.ContentType

object BasicConverters {

    val STRING = object : ElementConverter<String> {
        override fun serialize(type: ContentType, element: String): ByteArray {
            return element.toByteArray()
        }

        override fun deserialize(type: ContentType, bytes: ByteArray): String {
            return bytes.decodeToString()
        }
    }

    val BYTE_ARRAY = object : ElementConverter<ByteArray> {
        override fun serialize(type: ContentType, element: ByteArray): ByteArray {
            return element
        }

        override fun deserialize(type: ContentType, bytes: ByteArray): ByteArray {
            return bytes
        }
    }

    val JSON_ELEMENT = object : ElementConverter<JsonElement> {
        override fun serialize(type: ContentType, element: JsonElement): ByteArray {
            return Gson().toJson(element).toByteArray()
        }

        override fun deserialize(type: ContentType, bytes: ByteArray): JsonElement {
            return Gson().toJsonTree(bytes.decodeToString())
        }
    }

    val OAUTH_TOKEN = object : ElementConverter<OAuthTokenResponse> {
        override fun serialize(type: ContentType, element: OAuthTokenResponse): ByteArray {
            return Gson().toJson(element).toByteArray()
        }

        override fun deserialize(type: ContentType, bytes: ByteArray): OAuthTokenResponse {
            return Gson().fromJson(bytes.decodeToString(), OAuthTokenResponse::class.java)
        }
    }

    val OAUTH_TOKEN_ERROR = object : ElementConverter<OAuthErrorResponse> {
        override fun serialize(type: ContentType, element: OAuthErrorResponse): ByteArray {
            return Gson().toJson(element).toByteArray()
        }

        override fun deserialize(type: ContentType, bytes: ByteArray): OAuthErrorResponse {
            return Gson().fromJson(bytes.decodeToString(), OAuthErrorResponse::class.java)
        }
    }

    val FORM = object : ElementConverter<Form> {
        override fun serialize(type: ContentType, element: Form): ByteArray {
            return element.encodeToString().toByteArray()
        }

        override fun deserialize(type: ContentType, bytes: ByteArray): Form {
            return Form(bytes.decodeToString())
        }
    }
}