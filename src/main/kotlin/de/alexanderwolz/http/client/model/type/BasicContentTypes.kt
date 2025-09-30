package de.alexanderwolz.http.client.model.type

import com.google.gson.JsonElement
import de.alexanderwolz.http.client.model.Form
import de.alexanderwolz.http.client.model.converter.BasicConverters
import de.alexanderwolz.http.client.model.converter.Converter
import kotlin.reflect.KClass

enum class BasicContentTypes(
    override val mediaType: String,
    override val clazz: KClass<*>,
    override val converter: Converter<*>
) : ContentType {
    TEXT_PLAIN("text/plain", String::class, BasicConverters.STRING),
    TEXT_XML("text/xml", String::class, BasicConverters.STRING),
    TEXT_HTML("text/html", String::class, BasicConverters.STRING),
    APPLICATION_XML("application/xml", String::class, BasicConverters.STRING),
    APPLICATION_JSON("application/json", String::class, BasicConverters.STRING),
    FORM_URL_ENCODED("application/x-www-form-urlencoded", Form::class, BasicConverters.FORM),
    GSON("application/json", JsonElement::class, BasicConverters.JSON_ELEMENT);
}