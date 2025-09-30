package de.alexanderwolz.http.client.model.type

import com.google.gson.JsonElement
import de.alexanderwolz.http.client.model.Form
import de.alexanderwolz.http.client.model.converter.BasicConverters
import de.alexanderwolz.http.client.model.converter.IConverter
import kotlin.reflect.KClass

enum class BasicContentTypes(
    override val mediaType: String,
    override val clazz: KClass<*>,
    override val converter: IConverter<*>
) : ContentType {
    TEXT("text/plain", String::class, BasicConverters.STRING),
    XML("text/xml", String::class, BasicConverters.STRING),
    HTML("text/html", String::class, BasicConverters.STRING),
    JSON("application/json", JsonElement::class, BasicConverters.JSON),
    FORM_URL_ENCODED("application/x-www-form-urlencoded", Form::class, BasicConverters.FORM);
}