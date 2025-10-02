package de.alexanderwolz.http.client.model.converter

import com.google.gson.Gson
import com.google.gson.JsonElement
import de.alexanderwolz.http.client.model.Form
import de.alexanderwolz.http.client.model.token.OAuthErrorResponse
import de.alexanderwolz.http.client.model.token.OAuthTokenResponse
import kotlin.reflect.KClass

object BasicConverters {

    val STRING = object : Converter<String> {
        override fun serialize(element: String, clazz: KClass<String>): ByteArray {
            return element.toByteArray()
        }

        override fun deserialize(bytes: ByteArray, clazz: KClass<String>): String {
            return bytes.decodeToString()
        }
    }

    val BYTE_ARRAY = object : Converter<ByteArray> {
        override fun serialize(element: ByteArray, clazz: KClass<ByteArray>): ByteArray {
            return element
        }

        override fun deserialize(bytes: ByteArray, clazz: KClass<ByteArray>): ByteArray {
            return bytes
        }
    }

    val JSON_ELEMENT = object : Converter<JsonElement> {
        override fun serialize(element: JsonElement, clazz: KClass<JsonElement>): ByteArray {
            return Gson().toJson(element).toByteArray()
        }

        override fun deserialize(bytes: ByteArray, clazz: KClass<JsonElement>): JsonElement {
            return Gson().toJsonTree(bytes.decodeToString())
        }
    }

    val OAUTH_TOKEN = object : Converter<OAuthTokenResponse> {
        override fun serialize(element: OAuthTokenResponse, clazz: KClass<OAuthTokenResponse>): ByteArray {
            return Gson().toJson(element).toByteArray()
        }

        override fun deserialize(bytes: ByteArray, clazz: KClass<OAuthTokenResponse>): OAuthTokenResponse {
            return Gson().fromJson(bytes.decodeToString(), OAuthTokenResponse::class.java)
        }
    }

    val OAUTH_TOKEN_ERROR = object : Converter<OAuthErrorResponse> {
        override fun serialize(element: OAuthErrorResponse, clazz: KClass<OAuthErrorResponse>): ByteArray {
            return Gson().toJson(element).toByteArray()
        }

        override fun deserialize(bytes: ByteArray, clazz: KClass<OAuthErrorResponse>): OAuthErrorResponse {
            return Gson().fromJson(bytes.decodeToString(), OAuthErrorResponse::class.java)
        }
    }

    val FORM = object : Converter<Form> {
        override fun serialize(element: Form, clazz: KClass<Form>): ByteArray {
            return element.encodeToString().toByteArray()
        }

        override fun deserialize(bytes: ByteArray, clazz: KClass<Form>): Form {
            return Form(bytes.decodeToString())
        }
    }
}