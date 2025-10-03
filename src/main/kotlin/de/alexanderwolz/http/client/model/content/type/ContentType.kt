package de.alexanderwolz.http.client.model.content.type

import de.alexanderwolz.http.client.model.content.resolver.ContentResolver
import kotlin.reflect.KClass

interface ContentType {
    val mediaType: String
    val clazz: KClass<*>
    val resolver: ContentResolver
}