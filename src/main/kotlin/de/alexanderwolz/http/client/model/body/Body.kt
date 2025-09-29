package de.alexanderwolz.http.client.model.body

import de.alexanderwolz.http.client.model.ContentType

interface Body<T> {
    val type: ContentType
    val content: T
}