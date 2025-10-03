package de.alexanderwolz.http.client.model.payload

interface WrappedPayload<P, T> : Payload<T> {
    val parent: P
    val parentBytes: ByteArray
}