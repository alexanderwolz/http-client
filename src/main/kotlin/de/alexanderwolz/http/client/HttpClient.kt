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
import de.alexanderwolz.http.client.socket.SslSocket
import de.alexanderwolz.http.client.utils.CertificateUtils
import de.alexanderwolz.http.client.utils.StringUtils
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
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

    fun execute(): Response<*> {

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

        val okRequestBody = request.body?.let {
            when (it) {
                is FormPayload -> {
                    val builder = FormBodyOK.Builder()
                    it.content.forEach { entry ->
                        builder.add(entry.key, entry.value)
                    }
                    builder.build()
                }

                is StringPayload -> {
                    it.content.toRequestBody(it.type.mediaType.toMediaType())
                }

                is ByteArrayPayload -> {
                    it.content.toRequestBody(it.type.mediaType.toMediaType())
                }

                else -> {
                    logger.trace { "Custom payload: $it" }
                    it.type.serialize(it).toRequestBody(it.type.mediaType.toMediaType())
                }
            }
        }
        okRequestBuilder.method(request.method.name, okRequestBody)

        val okRequest = okRequestBuilder.build()
        logRequest(okRequest)

        try {
            okHttpClient.newCall(okRequest).execute().use { okResponse ->
                val responseBody = okResponse.body?.let { okBody ->
                    val bytes = okBody.source().use { it.readByteArray() }
                    val contentType = okResponse.headers["content-type"]?.let { mediaType ->
                        ContentTypeRegistry.find(mediaType)
                    } ?: ContentType.TEXT
                    contentType.deserialize(bytes)
                }
                return Response(
                    request,
                    okResponse.code,
                    okResponse.message,
                    okResponse.headers.toMultimap(),
                    responseBody,
                    Response.Source(okRequest, okResponse)
                ).also { logResponse(it) }
            }
        } catch (e: IOException) {
            val code = Reason.CODE_CLIENT_ERROR
            val reason = Reason(code, e.message ?: e.javaClass.simpleName)
            throw HttpExecutionException(request, reason, null, cause = e)
        }
    }

    private fun logRequest(okRequest: okhttp3.Request) {
        logger.trace {
            val builder = StringBuilder("Executing request at ${Date()}:")
            builder.append("\n\tRequest")
            builder.append("\n\t\tMethod:  ${okRequest.method}")
            builder.append("\n\t\tURL:     ${okRequest.url}")
            val headers = okRequest.headers.toMultimap().entries.joinToString()
            builder.append("\n\t\tHeaders: $headers")
            builder.append("\n\t\tBody:    ${request.body?.let { "${it.type}" } ?: ": No body"}")
            if (request.body != null) {
                getBodyString(request.body).lines().forEach { builder.append("\n\t\t\t$it") }
            }
            builder.toString()
        }
    }

    private fun logResponse(response: Response<*>) {
        logger.trace {
            val builder = StringBuilder("Received server response at ${Date()}:")
            builder.append("\n\tResponse")
            builder.append("\n\t\tStatus:  ${response.code}")
            builder.append("\n\t\tMessage: ${response.message?.let { it.ifEmpty { "No message" } } ?: "No message"}")
            builder.append("\n\t\tHeaders: ${response.headers}")
            builder.append("\n\t\tBody:    ${response.body?.let { "${it.type}" } ?: ": No body"}")
            if (response.body != null) {
                getBodyString(response.body).lines().forEach { builder.append("\n\t\t\t$it") }
            }

            builder.toString()
        }
    }

    private fun getBodyString(body: Payload<*>?): String {
        if (body == null) {
            return "No body"
        }

        val builder = StringBuilder()

        if (body is FormPayload) {
            body.content.entries.forEachIndexed { i, entry ->
                if (i > 0) {
                    builder.append(" ")
                }
                builder.append("${entry.key}=${entry.value}")
                if (i < body.content.size - 1) {
                    builder.append(",")
                }
            }
        }

        if (body is StringPayload) {
            builder.append(body.content)
        }

        if (body is ByteArrayPayload) {
            builder.append(body.content.decodeToString())
        }

        return builder.toString()
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
        private var acceptTypes: Set<ContentType<*>>? = null
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

        fun accept(vararg contentTypes: ContentType<*>) = apply {
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

        fun body(form: Form, type: ContentType<Form>? = null) = apply {
            this.body(FormPayload(type ?: ContentType.FORM_URL_ENCODED, form))
        }

        fun body(content: String, type: ContentType<String>) = apply {
            this.body(StringPayload(type, content))
        }

        //TODO make a generic Payload for typed classes ???

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