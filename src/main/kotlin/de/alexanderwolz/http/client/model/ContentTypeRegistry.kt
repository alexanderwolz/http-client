package de.alexanderwolz.http.client.model

import de.alexanderwolz.http.client.log.Logger
import de.alexanderwolz.http.client.model.payload.Converter
import de.alexanderwolz.http.client.model.payload.Payload

object ContentTypeRegistry {

    private val logger = Logger(javaClass)

    private val contentTypes = HashMap<String, ContentType<out Any>>()

    inline fun <reified T : Any> register(
        mediaType: String,
        converter: Converter<T>
    ): ContentType<T> {
        val contentType = object : ContentType<T> {
            override val mediaType = mediaType
            override val clazz = T::class

            override fun serialize(payload: Payload<*>): ByteArray {
                return converter.serialize(this, payload)
            }

            override fun deserialize(bytes: ByteArray): Payload<T> {
                return converter.deserialize(this, bytes)
            }

            override fun toString() = "[$mediaType -> ${clazz.qualifiedName}]"
        }
        return register(contentType)
    }

    fun <T : Any> register(contentType: ContentType<T>): ContentType<T> {
        logger.debug { "Registering content type: ${contentType.mediaType} to ${contentType.clazz.qualifiedName}" }
        val normalizedMediaType = getNormalized(contentType.mediaType)
        if (contentTypes.containsKey(normalizedMediaType)) {
            throw IllegalArgumentException("Content type '$contentType' is already registered")
        }
        contentTypes[normalizedMediaType] = contentType
        return contentType
    }

    fun find(mediaType: String): ContentType<*> {
        contentTypes[getNormalized(mediaType)]?.let {
            return it
        }
        throw NoSuchElementException("No content type is registered for '$mediaType'")
    }

    private fun getNormalized(mediaType: String): String {
        return mediaType.trim().split(";").first()
    }

}