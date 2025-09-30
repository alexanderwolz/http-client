package de.alexanderwolz.http.client.model.payload

import de.alexanderwolz.http.client.model.type.ContentType

data class PayloadImpl(override val type: ContentType, override val bytes: ByteArray) : Payload {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PayloadImpl

        if (type != other.type) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}