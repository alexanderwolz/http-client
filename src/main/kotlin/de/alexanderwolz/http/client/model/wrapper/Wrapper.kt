package de.alexanderwolz.http.client.model.wrapper

import kotlin.reflect.KClass

interface Wrapper<T : Any, P : Any> {
    fun wrap(element: T, clazz: KClass<T>): P
    fun unwrap(parent: P, clazz: KClass<T>): T
}