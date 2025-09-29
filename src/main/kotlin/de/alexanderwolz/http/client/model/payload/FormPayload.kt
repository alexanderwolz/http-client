package de.alexanderwolz.http.client.model.payload

import de.alexanderwolz.http.client.model.ContentType

data class FormPayload(override val type: ContentType, override val content: Map<String, String>) : Payload<Map<String, String>>