package de.alexanderwolz.http.client.model.payload

import de.alexanderwolz.http.client.model.Form
import de.alexanderwolz.http.client.model.type.ContentType

data class FormPayload(override val type: ContentType, override val content: Form) : Payload<Form>