package de.alexanderwolz.http.client.model.payload

import de.alexanderwolz.http.client.model.type.ContentType

interface Payload {
    val type: ContentType
    val bytes: ByteArray
}