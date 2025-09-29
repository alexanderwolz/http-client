package de.alexanderwolz.http.client.model.certificate

import java.io.File

//this is intended to only hold the file reference to the key-pairs, not the content
data class CertificateReference(
    val key: File,
    val cert: File,
    val ca: File? = null
)