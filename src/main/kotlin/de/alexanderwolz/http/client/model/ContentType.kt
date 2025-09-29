package de.alexanderwolz.http.client.model

import kotlin.reflect.KClass

interface ContentType {
    val mediaType: String
    val clazz: KClass<*>

    companion object {
        val TEXT: ContentType = ContentTypeRegistry.register("text/plain", String::class)
        val XML: ContentType = ContentTypeRegistry.register("text/xml", String::class)
        val HTML: ContentType = ContentTypeRegistry.register("text/html", String::class)
        val JSON: ContentType = ContentTypeRegistry.register("application/json", String::class)
        val FORM: ContentType = ContentTypeRegistry.register("application/x-www-form-urlencoded", String::class)
    }
}