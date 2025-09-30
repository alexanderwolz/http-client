package de.alexanderwolz.http.client.model

import de.alexanderwolz.http.client.model.type.ContentType

class Payload {

    val type: ContentType
    val bytes: ByteArray
    val element: Any

    constructor(type: ContentType, bytes: ByteArray) {
        this.type = type
        this.bytes = bytes
        this.element = deserialize()
        typeCheck()
    }

    constructor(type: ContentType, element: Any) {
        this.type = type
        this.element = element
        this.bytes = serialize()
        typeCheck()
    }

    private fun typeCheck(){
        if(!type.clazz.java.isAssignableFrom(element::class.java)){
            throw IllegalStateException("Element does not match content-type class: ${element::class.java} -> ${type.clazz.java}")
        }
    }

    private fun deserialize(): Any {
        return type.converter.deserialize(bytes)!!
    }

    @Suppress("UNCHECKED_CAST")
    private fun serialize(): ByteArray {
        val serialize = type.converter.serialize as (Any) -> ByteArray
        return serialize(element)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Payload

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