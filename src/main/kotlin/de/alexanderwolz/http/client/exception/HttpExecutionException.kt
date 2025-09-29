package de.alexanderwolz.http.client.exception

import de.alexanderwolz.http.client.model.Request
import de.alexanderwolz.http.client.model.Response

class HttpExecutionException(
    val request: Request,
    val reason: Reason,
    val response: Response<*>? = null,
    cause: Throwable? = null
) : Exception(cause) {

    override val message = "Reason: ${reason.code} -> ${reason.description}"
}