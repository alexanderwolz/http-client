package de.alexanderwolz.http.client.model.payload

import de.alexanderwolz.http.client.model.type.ContentType

interface Payload<out T : Any> {
    val type: ContentType
    val content: T
}