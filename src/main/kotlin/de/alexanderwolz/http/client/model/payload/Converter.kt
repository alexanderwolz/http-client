package de.alexanderwolz.http.client.model.payload

import de.alexanderwolz.http.client.model.ContentType
import de.alexanderwolz.http.client.model.Form

interface Converter<T : Any> {

    //TODO fix this with T
    fun serialize(type: ContentType<T>, payload: Payload<*>): ByteArray
    fun deserialize(type: ContentType<T>, bytes: ByteArray): Payload<T>

    companion object {
        val STRING = object : Converter<String> {
            override fun serialize(type: ContentType<String>, payload: Payload<*>): ByteArray {
                return (payload.content as String).toByteArray()
            }

            override fun deserialize(type: ContentType<String>, bytes: ByteArray): Payload<String> {
                return StringPayload(type, bytes.decodeToString())
            }
        }
        val FORM = object : Converter<Form> {
            override fun serialize(type: ContentType<Form>, payload: Payload<*>): ByteArray {
                return (payload.content as Form).values.first().toByteArray() //TODO
            }

            override fun deserialize(type: ContentType<Form>, bytes: ByteArray): Payload<Form> {
                val form = Form() //TODO
                //bytes.decodeToString()
                return FormPayload(type, form)
            }
        }
    }
}