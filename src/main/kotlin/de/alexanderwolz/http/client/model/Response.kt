package de.alexanderwolz.http.client.model

import de.alexanderwolz.http.client.model.payload.Payload

data class Response<T>(
    val request: Request,
    val code: Int,
    val message: String?,
    val headers: Map<String, List<String>>,
    val body: Payload<T>?,
    val source: Source
) {
    val isOK = code in 200..299

    class Source(request: Any, response: Any)
}