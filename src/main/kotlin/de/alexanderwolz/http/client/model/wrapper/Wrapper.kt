package de.alexanderwolz.http.client.model.wrapper

interface Wrapper<T : Any, P : Any> {
    fun wrap(element: T): P
    fun unwrap(parent: P): T
}