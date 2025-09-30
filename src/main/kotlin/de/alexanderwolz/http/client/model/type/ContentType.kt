package de.alexanderwolz.http.client.model.type

import de.alexanderwolz.http.client.model.converter.Converter
import kotlin.reflect.KClass

interface ContentType {
    val mediaType: String
    val clazz: KClass<*>
    val converter: Converter<*>
}