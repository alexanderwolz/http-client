package de.alexanderwolz.http.client.model

import de.alexanderwolz.http.client.model.converter.Converter
import de.alexanderwolz.http.client.model.type.ContentType
import kotlin.reflect.KClass

enum class CustomContentTypes(
    override val mediaType: String, override val clazz: KClass<*>,
    override val converter: Converter<*>
) : ContentType {
    CUSTOM_NAME("application/customName", CustomName::class, CustomNameConverter());
}