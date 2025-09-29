package de.alexanderwolz.http.client.model

import de.alexanderwolz.http.client.log.Logger
import kotlin.reflect.KClass

object ContentTypeRegistry {

    private val logger = Logger(javaClass)

    private val contentTypes = HashMap<String, ContentType>()

    fun register(contentType: ContentType) {
        contentTypes[contentType.mediaType] = contentType
    }

    fun register(mediaType: String, clazz: KClass<*>): ContentType {
        logger.debug { "Registering content type: $mediaType to ${clazz.qualifiedName}" }
        val contentType = object : ContentType {
            override val mediaType = mediaType
            override val clazz = clazz
        }
        contentTypes[contentType.mediaType] = contentType
        return contentType
    }

    fun find(mediaType: String): ContentType {
        val normalized = mediaType.trim().split(";").first()
        return contentTypes[normalized]
            ?: throw NoSuchElementException("No content type is registered for '$mediaType'")
    }

}