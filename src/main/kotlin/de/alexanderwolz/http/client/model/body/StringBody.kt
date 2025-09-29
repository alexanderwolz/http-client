package de.alexanderwolz.http.client.model.body

import de.alexanderwolz.http.client.model.ContentType

data class StringBody(override val type: ContentType, override val content: String) : Body<String>