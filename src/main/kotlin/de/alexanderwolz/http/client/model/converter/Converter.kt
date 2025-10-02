package de.alexanderwolz.http.client.model.converter

import kotlin.reflect.KClass

interface Converter<T : Any> {
    fun serialize(element: T, clazz: KClass<T>): ByteArray
    fun deserialize(bytes: ByteArray, clazz: KClass<T>): T
}