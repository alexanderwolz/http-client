package de.alexanderwolz.http.client.model

import de.alexanderwolz.http.client.model.certificate.CertificateBundle
import de.alexanderwolz.http.client.model.payload.Payload
import de.alexanderwolz.http.client.model.type.ContentType
import java.net.URI

data class Request(
    val httpMethod: HttpMethod,
    val endpoint: URI,
    val headers: Map<String, Set<String>>,
    val body: Payload = Payload.EMPTY,
    val acceptTypes: Set<ContentType>? = null,
    val certificates: CertificateBundle? = null
)