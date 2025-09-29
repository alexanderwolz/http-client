package de.alexanderwolz.http.client.model

import de.alexanderwolz.http.client.log.Logger
import kotlin.reflect.KClass

object ContentTypeRegistry {

    private val logger = Logger(javaClass)

    private val contentTypes = HashMap<String, ContentType>()

    fun register(mediaType: String, clazz: KClass<*>): ContentType {
        val contentType = object : ContentType {
            override val mediaType = mediaType
            override val clazz = clazz
            override fun toString() = "[$mediaType -> ${clazz.simpleName}]"
        }
        return register(contentType)
    }

    fun register(contentType: ContentType): ContentType {
        logger.debug { "Registering content type: ${contentType.mediaType} to ${contentType.clazz.qualifiedName}" }
        val normalizedMediaType = getNormalized(contentType.mediaType)
        if (contentTypes.containsKey(normalizedMediaType)) {
            throw IllegalArgumentException("Content type '$contentType' is already registered")
        }
        contentTypes[normalizedMediaType] = contentType
        return contentType
    }

    fun find(mediaType: String): ContentType {
        return contentTypes[getNormalized(mediaType)]
            ?: throw NoSuchElementException("No content type is registered for '$mediaType'")
    }

    private fun getNormalized(mediaType: String): String {
        return mediaType.trim().split(";").first()
    }

}