package de.alexanderwolz.http.client.model.payload

import de.alexanderwolz.http.client.model.type.ContentType

data class ByteArrayPayload(
    override val type: ContentType,
    override val content: ByteArray
) : Payload<ByteArray> {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ByteArrayPayload

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