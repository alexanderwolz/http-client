package de.alexanderwolz.http.client.model.certificate

import java.security.PrivateKey
import java.security.cert.Certificate

data class CertificateBundle(
    val privateKey: PrivateKey,
    val certificates: List<Certificate>,
    val caCertificates: List<Certificate>
)