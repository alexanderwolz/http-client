package de.alexanderwolz.http.client.model.payload

import de.alexanderwolz.http.client.model.ContentType

interface Payload<T> {
    val type: ContentType
    val content: T
}