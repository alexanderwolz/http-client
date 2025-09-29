package de.alexanderwolz.http.client.model.payload

import de.alexanderwolz.http.client.model.ContentType

data class StringPayload(override val type: ContentType, override val content: String) : Payload<String>