package de.alexanderwolz.http.client.model.payload

import com.google.gson.JsonElement
import de.alexanderwolz.http.client.model.type.ContentType

data class JsonPayload(override val type: ContentType, override val content: JsonElement) : Payload<JsonElement>
