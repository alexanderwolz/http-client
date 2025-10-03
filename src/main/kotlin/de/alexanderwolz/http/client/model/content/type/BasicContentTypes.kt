package de.alexanderwolz.http.client.model.content.type

import de.alexanderwolz.http.client.model.Form
import de.alexanderwolz.http.client.model.content.resolver.BasicContentResolver
import de.alexanderwolz.http.client.model.content.resolver.ContentResolver
import de.alexanderwolz.http.client.model.token.OAuthErrorResponse
import de.alexanderwolz.http.client.model.token.OAuthTokenResponse
import kotlinx.serialization.json.JsonElement
import kotlin.reflect.KClass

enum class BasicContentTypes(
    override val mediaType: String,
    override val clazz: KClass<*>,
    override val resolver: ContentResolver = BasicContentResolver(),
) : ContentType {
    EMPTY("EMPTY_BODY", String::class),
    TEXT_PLAIN("text/plain", String::class),
    TEXT_XML("text/xml", String::class),
    TEXT_HTML("text/html", String::class),
    APPLICATION_XML("application/xml", String::class),
    APPLICATION_JSON("application/json", String::class),
    APPLICATION_OCTET_STREAM("application/octet-stream", ByteArray::class),
    FORM_URL_ENCODED("application/x-www-form-urlencoded", Form::class),
    JSON_ELEMENT("application/json", JsonElement::class),
    OAUTH_TOKEN("application/json", OAuthTokenResponse::class),
    OAUTH_ERROR("application/json", OAuthErrorResponse::class);
}