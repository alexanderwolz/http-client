package de.alexanderwolz.http.client.model.payload

import de.alexanderwolz.http.client.model.type.ContentType

data class ObjectPayload(override val type: ContentType, override val content: Any) : Payload<Any>
