package de.alexanderwolz.http.client.model.converter

import com.google.gson.Gson
import com.google.gson.JsonElement
import de.alexanderwolz.http.client.model.Form
import de.alexanderwolz.http.client.model.token.OAuthTokenResponse
import de.alexanderwolz.http.client.model.type.ContentType

object BasicConverters {

    val STRING = object : Converter<String> {
        override fun serialize(element: String, type: ContentType): ByteArray {
            return element.toByteArray()
        }

        override fun deserialize(bytes: ByteArray, type: ContentType): String {
            return bytes.decodeToString()
        }
    }

    val JSON_ELEMENT = object : Converter<JsonElement> {
        override fun serialize(element: JsonElement, type: ContentType): ByteArray {
            return Gson().toJson(element).toByteArray()
        }

        override fun deserialize(bytes: ByteArray, type: ContentType): JsonElement {
            return Gson().toJsonTree(bytes.decodeToString())
        }
    }

    val OAUTH_TOKEN = object : Converter<OAuthTokenResponse> {
        override fun serialize(element: OAuthTokenResponse, type: ContentType): ByteArray {
            return Gson().toJson(element).toByteArray()
        }

        override fun deserialize(bytes: ByteArray, type: ContentType): OAuthTokenResponse {
            return Gson().fromJson(bytes.decodeToString(), OAuthTokenResponse::class.java)
        }
    }

    val FORM = object : Converter<Form> {
        override fun serialize(element: Form, type: ContentType): ByteArray {
            return element.encodeToString().toByteArray()
        }

        override fun deserialize(bytes: ByteArray, type: ContentType): Form {
            return Form(bytes.decodeToString())
        }
    }
}