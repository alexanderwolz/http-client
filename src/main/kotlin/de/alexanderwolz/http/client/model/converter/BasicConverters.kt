package de.alexanderwolz.http.client.model.converter

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import de.alexanderwolz.http.client.model.Form
import de.alexanderwolz.http.client.model.payload.FormPayload
import de.alexanderwolz.http.client.model.payload.JsonPayload
import de.alexanderwolz.http.client.model.payload.Payload
import de.alexanderwolz.http.client.model.payload.StringPayload
import de.alexanderwolz.http.client.model.type.ContentType

object BasicConverters {
    val STRING = object : IConverter<String> {
        override fun serialize(type: ContentType, payload: Payload<String>): ByteArray {
            return payload.content.toByteArray()
        }

        override fun deserialize(type: ContentType, bytes: ByteArray): Payload<String> {
            return StringPayload(type, bytes.decodeToString())
        }
    }

    val JSON = object : IConverter<JsonElement> {
        private val gson = GsonBuilder().create()
        override fun serialize(type: ContentType, payload: Payload<JsonElement>): ByteArray {
            val json = gson.toJson(payload.content, type.clazz.java)
            return json.toByteArray()
        }

        override fun deserialize(type: ContentType, bytes: ByteArray): Payload<JsonElement> {
            val jsonString = bytes.decodeToString()
            return JsonPayload(type, gson.toJsonTree(jsonString))
        }
    }


    val FORM = object : IConverter<Form> {
        override fun serialize(type: ContentType, payload: Payload<Form>): ByteArray {
            return payload.content.encodeToString().toByteArray()
        }

        override fun deserialize(type: ContentType, bytes: ByteArray): Payload<Form> {
            val content = bytes.decodeToString()
            return FormPayload(type, Form(content))
        }
    }
}