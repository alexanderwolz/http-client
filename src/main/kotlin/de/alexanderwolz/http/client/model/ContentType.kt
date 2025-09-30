package de.alexanderwolz.http.client.model

import de.alexanderwolz.http.client.model.payload.Converter
import de.alexanderwolz.http.client.model.payload.Payload
import kotlin.reflect.KClass

interface ContentType<T : Any> {
    val mediaType: String
    val clazz: KClass<T>
    fun serialize(payload: Payload<*>): ByteArray
    fun deserialize(bytes: ByteArray): Payload<T>

    companion object {
        val TEXT = ContentTypeRegistry.register<String>("text/plain", Converter.STRING)
        val XML = ContentTypeRegistry.register<String>("text/xml", Converter.STRING)
        val HTML = ContentTypeRegistry.register<String>("text/html", Converter.STRING)
        val JSON = ContentTypeRegistry.register<String>("application/json", Converter.STRING)
        val FORM_URL_ENCODED = ContentTypeRegistry.register<Form>("application/x-www-form-urlencoded", Converter.FORM)
    }

}