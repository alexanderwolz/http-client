package de.alexanderwolz.http.client.model.type

import de.alexanderwolz.http.client.log.Logger

object ContentTypeRegistry {

    private val logger = Logger(javaClass)

    private val contentTypes = LinkedHashMap<String, ContentType>()

    init {
        BasicContentTypes.entries.forEach {
            register(it)
        }
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

    private fun findType(mediaType: String): ContentType {
        return contentTypes[getNormalized(mediaType)]
            ?: throw NoSuchElementException("No content type is registered for '$mediaType'")
    }

    private fun getNormalized(mediaType: String): String {
        return mediaType.trim().split(";").first()
    }

}