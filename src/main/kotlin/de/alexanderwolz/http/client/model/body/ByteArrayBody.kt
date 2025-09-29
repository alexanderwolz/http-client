package de.alexanderwolz.http.client.model.body

import de.alexanderwolz.http.client.model.ContentType

data class ByteArrayBody(override val type: ContentType, override val content: ByteArray) : Body<ByteArray> {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ByteArrayBody

        if (type != other.type) return false
        if (!content.contentEquals(other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }

}