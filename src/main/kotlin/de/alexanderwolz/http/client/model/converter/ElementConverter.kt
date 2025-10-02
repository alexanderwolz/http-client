package de.alexanderwolz.http.client.model.converter

import kotlin.reflect.KClass

interface ElementConverter<T : Any> {
    fun serialize(element: T, clazz: KClass<T>): ByteArray
    fun deserialize(bytes: ByteArray, clazz: KClass<T>): T
}