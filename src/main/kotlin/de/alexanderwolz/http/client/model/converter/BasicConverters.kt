package de.alexanderwolz.http.client.model.converter

import com.google.gson.Gson
import com.google.gson.JsonElement
import de.alexanderwolz.http.client.model.Form
import de.alexanderwolz.http.client.model.token.OAuthErrorResponse
import de.alexanderwolz.http.client.model.token.OAuthTokenResponse
import kotlin.reflect.KClass

object BasicConverters {

    val STRING = object : ElementConverter<String> {
        override fun serialize(element: String, clazz: KClass<String>): ByteArray {
            return element.toByteArray()
        }

        override fun deserialize(bytes: ByteArray, clazz: KClass<String>): String {
            return bytes.decodeToString()
        }
    }

    val BYTE_ARRAY = object : ElementConverter<ByteArray> {
        override fun serialize(element: ByteArray, clazz: KClass<ByteArray>): ByteArray {
            return element
        }

        override fun deserialize(bytes: ByteArray, clazz: KClass<ByteArray>): ByteArray {
            return bytes
        }
    }

    val JSON_ELEMENT = object : ElementConverter<JsonElement> {
        override fun serialize(element: JsonElement, clazz: KClass<JsonElement>): ByteArray {
            return Gson().toJson(element).toByteArray()
        }

        override fun deserialize(bytes: ByteArray, clazz: KClass<JsonElement>): JsonElement {
            return Gson().toJsonTree(bytes.decodeToString())
        }
    }

    val OAUTH_TOKEN = object : ElementConverter<OAuthTokenResponse> {
        override fun serialize(element: OAuthTokenResponse, clazz: KClass<OAuthTokenResponse>): ByteArray {
            return Gson().toJson(element).toByteArray()
        }

        override fun deserialize(bytes: ByteArray, clazz: KClass<OAuthTokenResponse>): OAuthTokenResponse {
            return Gson().fromJson(bytes.decodeToString(), OAuthTokenResponse::class.java)
        }
    }

    val OAUTH_TOKEN_ERROR = object : ElementConverter<OAuthErrorResponse> {
        override fun serialize(element: OAuthErrorResponse, clazz: KClass<OAuthErrorResponse>): ByteArray {
            return Gson().toJson(element).toByteArray()
        }

        override fun deserialize(bytes: ByteArray, clazz: KClass<OAuthErrorResponse>): OAuthErrorResponse {
            return Gson().fromJson(bytes.decodeToString(), OAuthErrorResponse::class.java)
        }
    }

    val FORM = object : ElementConverter<Form> {
        override fun serialize(element: Form, clazz: KClass<Form>): ByteArray {
            return element.encodeToString().toByteArray()
        }

        override fun deserialize(bytes: ByteArray, clazz: KClass<Form>): Form {
            return Form(bytes.decodeToString())
        }
    }
}