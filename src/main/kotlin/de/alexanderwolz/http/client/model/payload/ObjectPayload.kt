package de.alexanderwolz.http.client.model.payload

import de.alexanderwolz.http.client.model.ContentType

data class ObjectPayload(override val type: ContentType<Any>, override val content: Any) : Payload<Any>
