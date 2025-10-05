package de.alexanderwolz.http.client

import de.alexanderwolz.commons.util.certificate.CertificateUtils
import de.alexanderwolz.commons.util.string.StringUtils
import de.alexanderwolz.http.client.instance.OkHttpClientWrapper
import de.alexanderwolz.http.client.instance.Settings
import de.alexanderwolz.http.client.model.HttpMethod
import de.alexanderwolz.http.client.model.Request
import de.alexanderwolz.http.client.model.Response
import de.alexanderwolz.http.client.model.certificate.CertificateBundle
import de.alexanderwolz.http.client.model.certificate.CertificateReference
import de.alexanderwolz.http.client.model.content.ContentResolver
import de.alexanderwolz.http.client.model.content.ContentType
import de.alexanderwolz.http.client.model.payload.Payload
import de.alexanderwolz.http.client.model.token.AccessToken
import java.io.File
import java.net.URI

interface HttpClient {
    val request: Request
    fun execute(): Response

    class Builder() {

        private var httpMethod = HttpMethod.GET
        private var endpoint: URI? = null
        private val requestHeaders = HashMap<String, Set<String>>()
        private var requestBody: Payload<*> = Payload.EMPTY
        private var acceptTypes: Set<ContentType>? = null
        private var certificateBundle: CertificateBundle? = null
        private var certificateReference: CertificateReference? = null
        private var certFolder: File? = null
        private var accessToken: AccessToken? = null
        private var proxy: URI? = null
        private var verifyCert = true
        private var resolver: ContentResolver? = null

        init {
            headers(
                "Accept-Charset" to setOf("UTF-8"),
                "Accept-Language" to setOf("en-US"),
            )
        }

        fun userAgent(userAgent: String) = apply {
            headers("User-Agent" to setOf(userAgent))
        }

        fun verifyCert(verifyCert: Boolean) = apply {
            this.verifyCert = verifyCert
        }

        fun method(httpMethod: HttpMethod) = apply {
            this.httpMethod = httpMethod
        }

        fun endpoint(endpoint: String, params: Map<String, String>? = null) = apply {
            this.endpoint(URI.create(endpoint), params)
        }

        fun endpoint(endpoint: URI, params: Map<String, String>? = null) = apply {
            this.endpoint = params?.let {
                resolveEndpoint(endpoint, params)
            } ?: endpoint
        }

        fun certificateLookupFolder(certFolder: File) = apply {
            this.certFolder = certFolder
        }

        fun certificates(bundle: CertificateBundle?) = apply {
            this.certificateBundle = bundle
        }

        fun certificates(reference: CertificateReference?) = apply {
            this.certificateReference = reference
        }

        fun accept(vararg contentTypes: ContentType) = apply {
            this.acceptTypes = contentTypes.toSet()
        }

        fun headers(vararg pairs: Pair<String, Set<String>>) = apply {
            pairs.forEach {
                this.requestHeaders[it.first] = it.second
            }
        }

        fun body(payload: Payload<*>) = apply {
            this.requestBody = payload.also {
                headers("Content-Type" to setOf(it.type.mediaType))
            }
        }

        fun resolver(resolver: ContentResolver) = apply {
            this.resolver = resolver
        }

        fun accessToken(token: AccessToken) = apply {
            this.accessToken = token
        }

        fun proxy(proxy: URI?) = apply {
            this.proxy = proxy
        }

        fun build(): HttpClient {
            if (certificateBundle != null && certificateReference != null) {
                throw IllegalStateException("Either bundle or reference can be specified")
            }
            val endpoint = requireNotNull(endpoint)
            val certificates = certificateReference?.let { resolveReference(it) } ?: certificateBundle

            if (Settings.library == Settings.LibraryType.OK_HTTP) {
                return OkHttpClientWrapper(
                    proxy,
                    verifyCert,
                    certificates,
                    httpMethod,
                    endpoint,
                    requestHeaders,
                    requestBody,
                    resolver,
                    acceptTypes,
                    accessToken
                )
            }
            throw NoSuchElementException("Unknown HTTP library '${Settings.library}'")
        }

        private fun resolveEndpoint(endpoint: URI, params: Map<String, String>): URI {
            return URI.create(StringUtils.resolveVars(endpoint.toString(), params))
        }

        private fun resolveReference(reference: CertificateReference): CertificateBundle {

            val privateKeyFile = CertificateUtils.resolveKeyPairFile(reference.key, certFolder)
            val publicKeyFile = CertificateUtils.resolveKeyPairFile(reference.cert, certFolder)
            val caFile = reference.ca?.let { CertificateUtils.resolveKeyPairFile(it, certFolder) }

            //read contents
            val privateKey = CertificateUtils.readPrivateKey(privateKeyFile)
            val clientCertificates = CertificateUtils.readCertificates(publicKeyFile)
            val caCertificates = caFile?.let { CertificateUtils.readCertificates(it) } ?: emptyList()

            return CertificateBundle(privateKey, clientCertificates, caCertificates)
        }
    }
}