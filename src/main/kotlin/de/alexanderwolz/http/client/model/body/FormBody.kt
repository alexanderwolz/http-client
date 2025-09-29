package de.alexanderwolz.http.client.model.body

import de.alexanderwolz.http.client.model.ContentType

data class FormBody(override val type: ContentType, override val content: Map<String, String>) : Body<Map<String, String>>