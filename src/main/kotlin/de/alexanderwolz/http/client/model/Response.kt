package de.alexanderwolz.http.client.model

import de.alexanderwolz.http.client.model.body.Body

data class Response(
    val request: Request,
    val code: Int,
    val message: String?,
    val headers: Map<String, List<String>>,
    val body: Body<*>?,
) {
    val isOK = code in 200..299
}