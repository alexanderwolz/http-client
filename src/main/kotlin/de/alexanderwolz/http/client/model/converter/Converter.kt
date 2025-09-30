package de.alexanderwolz.http.client.model.converter

data class Converter<T>(
    val serialize: (element: T) -> ByteArray,
    val deserialize: (bytes: ByteArray) -> T
)