package de.alexanderwolz.http.client.model.type

import de.alexanderwolz.http.client.model.converter.Converter
import de.alexanderwolz.http.client.model.wrapper.Wrapper
import kotlin.reflect.KClass

interface ContentType {
    val mediaType: String
    val clazz: KClass<*>
    val converter: Converter<*>
    val wrapper: Wrapper<*, *>?
}