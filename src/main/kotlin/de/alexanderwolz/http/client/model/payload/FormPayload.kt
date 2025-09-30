package de.alexanderwolz.http.client.model.payload

import de.alexanderwolz.http.client.model.ContentType
import de.alexanderwolz.http.client.model.Form

data class FormPayload(override val type: ContentType<Form>, override val content: Form) : Payload<Form>