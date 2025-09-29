package de.alexanderwolz.http.client.model

import de.alexanderwolz.http.client.model.body.Body
import de.alexanderwolz.http.client.model.certificate.CertificateBundle
import java.net.URI

data class Request(
    val method: Method,
    val endpoint: URI,
    val headers: Map<String, Set<String>>,
    val body: Body<*>? = null,
    val certificates: CertificateBundle? = null
)