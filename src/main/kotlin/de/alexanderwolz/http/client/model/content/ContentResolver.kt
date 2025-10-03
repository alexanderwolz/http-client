package de.alexanderwolz.http.client.model.content

import kotlin.reflect.KClass

interface ContentResolver {

    fun wrap(parentClazz: KClass<*>, child: Any): Any
    fun extract(parentClazz: KClass<*>, parent: Any): Any

    fun serialize(clazz: KClass<*>, element: Any): ByteArray
    fun serialize(type: ContentType, element: Any): ByteArray

    fun deserialize(clazz: KClass<*>, bytes: ByteArray): Any
    fun deserialize(type: ContentType, bytes: ByteArray): Any
}