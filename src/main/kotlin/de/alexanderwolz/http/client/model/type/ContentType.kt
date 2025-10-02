package de.alexanderwolz.http.client.model.type

import de.alexanderwolz.http.client.model.converter.ElementConverter
import de.alexanderwolz.http.client.model.converter.ParentConverter
import kotlin.reflect.KClass

interface ContentType {
    val mediaType: String
    val clazz: KClass<*>
    val elementConverter: ElementConverter<*>
    val parentConverter: ParentConverter<*, *>?
}