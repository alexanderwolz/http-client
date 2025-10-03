package de.alexanderwolz.http.client.model.content

import kotlin.reflect.KClass

interface ContentType {
    val mediaType: String
    val clazz: KClass<*>
}