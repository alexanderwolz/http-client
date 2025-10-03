package de.alexanderwolz.http.client.model.content

import kotlin.reflect.KClass

interface ContentResolver {
    //fun getParentClass(type: ContentType): KClass<*>
    //fun extract(type: ContentType, element: Any): Any

    fun serialize(clazz: KClass<*>, element: Any): ByteArray
    fun serialize(type: ContentType, element: Any): ByteArray

    fun deserialize(clazz: KClass<*>, bytes: ByteArray): Any
    fun deserialize(type: ContentType, bytes: ByteArray): Any
}