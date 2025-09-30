package de.alexanderwolz.http.client

import de.alexanderwolz.http.client.exception.HttpExecutionException
import de.alexanderwolz.http.client.exception.Reason
import de.alexanderwolz.http.client.log.Logger
import de.alexanderwolz.http.client.model.*
import de.alexanderwolz.http.client.model.certificate.CertificateBundle
import de.alexanderwolz.http.client.model.certificate.CertificateReference
import de.alexanderwolz.http.client.model.payload.ByteArrayPayload
import de.alexanderwolz.http.client.model.payload.FormPayload
import de.alexanderwolz.http.client.model.payload.Payload
import de.alexanderwolz.http.client.model.payload.StringPayload
import de.alexanderwolz.http.client.model.type.BasicContentTypes
import de.alexanderwolz.http.client.model.type.ContentType
import de.alexanderwolz.http.client.socket.SslSocket
import de.alexanderwolz.http.client.utils.CertificateUtils
import de.alexanderwolz.http.client.utils.StringUtils
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.*
import okhttp3.FormBody as FormBodyOK

//This class is intended to wrap HTTP libraries, in this case OKHTTP
class HttpClient private constructor(val proxy: URI?, val request: Request, private val token: OAuthTokenResponse?) {

    private val logger = Logger(javaClass)

    fun execute(): Response {

        val okHttpClient = createOkHttpClient(request.certificates, proxy)

        val okRequestBuilder = okhttp3.Request.Builder().url(request.endpoint.toURL())
        request.headers.forEach {
            okRequestBuilder.header(it.key, it.value.joinToString())
        }

        token?.let {
            //overwrite Authorization header if accessToken is given
            okRequestBuilder.header(
                "Authorization",
                "${StringUtils.capitalize(token.type)} ${token.accessToken}"
            )
        }

        request.acceptTypes?.let { types ->
            okRequestBuilder.header("Accept", types.joinToString { it.mediaType })
        }

        val okRequestBody = request.body?.let { convertToOkBody(it) }
        okRequestBuilder.method(request.method.name, okRequestBody)

        val okRequest = okRequestBuilder.build()
        logRequest(okRequest)

        try {
            okHttpClient.newCall(okRequest).execute().use { okResponse ->
                return Response(
                    request,
                    okResponse.code,
                    okResponse.message,
                    okResponse.headers.toMultimap(),
                    convertBody(okResponse),
                    Response.Source(okRequest, okResponse)
                ).also { logResponse(it) }
            }
        } catch (e: IOException) {
            val code = Reason.CODE_CLIENT_ERROR
            val reason = Reason(code, e.message ?: e.javaClass.simpleName)
            throw HttpExecutionException(request, reason, null, cause = e)
        }
    }

    private fun convertToOkBody(payload: Payload<*>): RequestBody {
        val type = payload.type
        return when (payload) {
            is FormPayload -> {
                val builder = FormBodyOK.Builder()
                payload.content.entries.forEach { entry ->
                    builder.add(entry.key, entry.value)
                }
                builder.build()
            }

            is StringPayload -> {
                payload.content.toRequestBody(type.mediaType.toMediaType())
            }

            is ByteArrayPayload -> {
                payload.content.toRequestBody(type.mediaType.toMediaType())
            }

            else -> {
                logger.trace { "Custom payload: $payload" }
                serialize(payload).toRequestBody(type.mediaType.toMediaType())
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun serialize(payload: Payload<Any>): ByteArray {
        return payload.type.converter.serialize(payload as Payload<Nothing>)
    }

    private fun convertBody(okResponse: okhttp3.Response): Payload<*>? {
        okResponse.body?.let { okBody ->
            val bytes = okBody.source().use { it.readByteArray() }
            val mediaType = okResponse.headers["content-type"]
            if (mediaType != null) {
                logger.trace { "Server returned content-type: $mediaType" }
                val normalized = mediaType.split(";").first()
                val acceptTypes = request.acceptTypes ?: emptySet()
                val contentType = acceptTypes.find { it.mediaType.startsWith(normalized) }
                if (contentType != null) {
                    logger.trace { "Found content type in specified accept types" }
                    return contentType.converter.deserialize(contentType, bytes)
                } else {
                    logger.warn { "Could not determine content-type from request accept types (${request.acceptTypes?.joinToString()})" }
                    val basicType = BasicContentTypes.entries.find { it.mediaType.startsWith(normalized) }
                    if (basicType != null) {
                        logger.trace { "Found basic content type: $basicType" }
                        return basicType.converter.deserialize(basicType, bytes)
                    } else {
                        logger.warn { "Could not determine content-type from basic types" }
                        logger.warn { "Consider setting the appropriate accept type using ${Builder::class}" }
                        throw NoSuchElementException("Could not determine content-type reference for '$mediaType'")
                    }
                }
            } else {
                logger.trace { "Server did not return any content-type (but content-length=${bytes.size}" }
            }
        }
        return null
    }

    private fun logRequest(okRequest: okhttp3.Request) {
        logger.trace {
            val headers = okRequest.headers.toMultimap().entries.joinToString()
            val builder = StringBuilder("Executing request at ${Date()}:")
            builder.append("\n\tRequest")
            builder.append("\n\t\tMethod:  ${okRequest.method}")
            builder.append("\n\t\tURL:     ${okRequest.url}")
            builder.append("\n\t\tHeaders: $headers")
            builder.append("\n\t\tBody:    ${request.body?.let { "${it.type.clazz.java.name}" } ?: ": No request body"}")
            if (request.body != null) {
                StringUtils.getBodyString(request.body).lines().forEach { builder.append("\n\t\t\t$it") }
            }
            builder.toString()
        }
    }

    private fun logResponse(response: Response) {
        logger.trace {
            val builder = StringBuilder("Received server response at ${Date()}:")
            builder.append("\n\tResponse")
            builder.append("\n\t\tStatus:  ${response.code}")
            builder.append("\n\t\tMessage: ${response.message?.let { it.ifEmpty { "No message" } } ?: "No message"}")
            builder.append("\n\t\tHeaders: ${response.headers}")
            builder.append("\n\t\tBody:    ${response.body?.let { "${it.type.clazz.java.name}" } ?: ": No response body"}")
            if (response.body != null) {
                StringUtils.getBodyString(response.body).lines().forEach { builder.append("\n\t\t\t$it") }
            }

            builder.toString()
        }
    }

    private fun createOkHttpClient(bundle: CertificateBundle? = null, proxy: URI? = null): OkHttpClient {
        val builder = OkHttpClient.Builder()

        proxy?.let { uri ->
            val type = Proxy.Type.valueOf(uri.scheme.uppercase())
            builder.proxy(Proxy(type, InetSocketAddress(uri.host, uri.port)))
        }

        builder.connectTimeout(10, TimeUnit.SECONDS)
        builder.readTimeout(10, TimeUnit.SECONDS)
        bundle?.let {
            val sslSocket = createCustomSslSocketFactory(it)
            builder.sslSocketFactory(sslSocket.sslSocketFactory, sslSocket.trustManager)
        }
        return builder.build()
    }

    private fun createCustomSslSocketFactory(bundle: CertificateBundle): SslSocket {

        //generate custom key store
        val keyManagers = createCustomKeyStore(bundle.certificates, bundle.privateKey)

        //generate custom trust managers
        val trustManagers = createCustomTrustStore(bundle.caCertificates, bundle.verify)

        logger.trace { "Creating SSL socket factory .." }
        val socketFactory = SSLContext.getInstance("TLS").apply {
            init(keyManagers, trustManagers, SecureRandom())
        }.socketFactory

        logger.trace { "Done creating SSL socket factory" }
        return SslSocket(socketFactory, trustManagers[0] as X509TrustManager)

    }

    private fun createCustomKeyStore(certificates: List<Certificate>, privateKey: PrivateKey): Array<KeyManager> {
        return KeyManagerFactory.getInstance("SunX509").apply {
            logger.trace { "Creating keystore" }
            val passArray = "keystore".toCharArray()
            val keyStore = createEmptyKeyStore(passArray).apply {
                certificates.forEachIndexed { index, certificate ->
                    setCertificateEntry("certificate-$index", certificate)
                }
                setKeyEntry("private-key", privateKey, passArray, certificates.toTypedArray())
            }
            init(keyStore, passArray)
        }.keyManagers
    }

    private fun createCustomTrustStore(caCertificates: List<Certificate>, verify: Boolean = true): Array<TrustManager> {
        return if (verify) {
            logger.trace { "Creating ca truststore (verify=true)" }
            //generate custom trust store
            TrustManagerFactory.getInstance("SunX509").apply {
                val trustStore = createEmptyKeyStore("truststore".toCharArray()).apply {
                    caCertificates.forEachIndexed { index, certificate ->
                        setCertificateEntry("ca-${index + 1}", certificate)
                    }
                }
                init(trustStore)
            }.trustManagers
        } else {
            logger.trace { "Creating mock-truststore (verify=false)" }
            arrayOf(createAllowAllTrustManager())
        }
    }

    private fun createEmptyKeyStore(password: CharArray): KeyStore {
        return KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, password)
        }
    }

    private fun createAllowAllTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    }


    class Builder() {

        private var method: Method = Method.GET
        private var endpoint: URI? = null
        private val requestHeaders = HashMap<String, Set<String>>()
        private var requestBody: Payload<*>? = null
        private var acceptTypes: Set<ContentType>? = null
        private var certificateBundle: CertificateBundle? = null
        private var certificateReference: CertificateReference? = null
        private var certFolder: File? = null
        private var token: OAuthTokenResponse? = null
        private var proxy: URI? = null

        init {
            headers(
                "Accept-Charset" to setOf("UTF-8"),
                "Accept-Language" to setOf("en-US"),
            )
        }

        fun userAgent(userAgent: String) = apply {
            headers("User-Agent" to setOf(userAgent))
        }

        fun method(method: Method) = apply {
            this.method = method
        }

        fun endpoint(endpoint: URI, params: Map<String, String>? = null) = apply {
            this.endpoint = params?.let {
                resolveEndpoint(endpoint, params)
            } ?: endpoint
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
                this.requestHeaders[it.first.lowercase()] = it.second
            }
        }

        fun body(payload: Payload<*>) = apply {
            this.requestBody = payload.also {
                headers("Content-Type" to setOf(it.type.mediaType))
            }
        }

        fun body(form: Form, type: ContentType? = null) = apply {
            this.body(FormPayload(type ?: BasicContentTypes.FORM_URL_ENCODED, form))
        }

        fun body(content: String, type: ContentType) = apply {
            this.body(StringPayload(type, content))
        }

        fun token(token: OAuthTokenResponse) = apply {
            this.token = token
        }

        fun proxy(proxy: URI) = apply {
            this.proxy = proxy
        }

        fun build(): HttpClient {
            if (certificateBundle != null && certificateReference != null) {
                throw IllegalStateException("Either bundle or reference can be specified")
            }
            val endpoint = requireNotNull(endpoint)
            val certificates = certificateReference?.let { resolveReference(it) } ?: certificateBundle
            val request = Request(method, endpoint, requestHeaders, requestBody, acceptTypes, certificates)
            return HttpClient(proxy, request, token)
        }

        private fun resolveEndpoint(endpoint: URI, params: Map<String, String>): URI {
            return URI.create(StringUtils.resolveVars(endpoint.toString(), params))
        }

        fun resolveReference(reference: CertificateReference): CertificateBundle {

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