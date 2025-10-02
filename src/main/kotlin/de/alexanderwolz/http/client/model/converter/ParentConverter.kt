package de.alexanderwolz.http.client.model.converter

import kotlin.reflect.KClass

interface ParentConverter<P : Any, T : Any> {
    val parentClass: KClass<P>
    fun decode(bytes: ByteArray): P
    fun unwrap(parent: P): T
}