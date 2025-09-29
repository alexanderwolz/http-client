package de.alexanderwolz.http.client

import de.alexanderwolz.http.client.exception.HttpExecutionException
import de.alexanderwolz.http.client.exception.Reason
import de.alexanderwolz.http.client.log.Logger
import de.alexanderwolz.http.client.model.body.Body
import de.alexanderwolz.http.client.model.body.ByteArrayBody
import de.alexanderwolz.http.client.model.certificate.CertificateBundle
import de.alexanderwolz.http.client.model.certificate.CertificateReference
import de.alexanderwolz.http.client.model.ContentType
import de.alexanderwolz.http.client.model.ContentTypeRegistry
import de.alexanderwolz.http.client.model.body.FormBody
import de.alexanderwolz.http.client.model.Method
import de.alexanderwolz.http.client.model.OAuthTokenResponse
import de.alexanderwolz.http.client.model.Request
import de.alexanderwolz.http.client.model.Response
import de.alexanderwolz.http.client.model.body.StringBody
import de.alexanderwolz.http.client.utils.CertificateUtils
import de.alexanderwolz.http.client.utils.StringUtils
import de.alexanderwolz.http.de.alexanderwolz.http.client.SslSocket
import okhttp3.FormBody as FormBodyOK
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

//This class is intended to wrap HTTP libraries, in this case OKHTTP
class HttpClient private constructor(val request: Request, private val token: OAuthTokenResponse?) {

    private val logger = Logger(javaClass)

    fun execute(): Response {
        val okHttpClient = createOkHttpClient(request.certificates)

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

        val okRequestBody = request.body?.let {
            when (it) {
                is FormBody -> {
                    val builder = FormBodyOK.Builder()
                    it.content.forEach { entry ->
                        builder.add(entry.key, entry.value)
                    }
                    builder.build()
                }

                is StringBody -> {
                    it.content.toRequestBody(it.type.mediaType.toMediaType())
                }

                else -> throw UnsupportedOperationException("Unsupported body type: ${it.javaClass}")
            }
        }
        okRequestBuilder.method(request.method.name, okRequestBody)

        val okRequest = okRequestBuilder.build()

        try {
            logger.trace {
                val builder = StringBuilder("Executing request at ${Date()}:\n")
                builder.appendLine("  Method:  ${okRequest.method}")
                builder.appendLine("  URL:     ${okRequest.url}")
                val headers = okRequest.headers.toMultimap().entries.joinToString()
                builder.appendLine("  Headers: $headers")
                builder.append("  Body:    ${getBodyString(okRequest.body)}")
                builder.toString()
            }
            okHttpClient.newCall(okRequest).execute().use { okResponse ->
                val body = okResponse.body?.let { okBody ->
                    val byteArray = okBody.source().use { it.readByteArray() }
                    val contentType = okResponse.headers["content-type"]?.let { mediaType ->
                        ContentTypeRegistry.find(mediaType)
                    } ?: ContentType.TEXT
                    //TODO determine Body by content type and parse content
                    ByteArrayBody(contentType, byteArray)
                }
                return Response(
                    request,
                    okResponse.code,
                    okResponse.message,
                    okResponse.headers.toMultimap(),
                    body
                ).apply {
                    logger.trace {
                        val builder = StringBuilder("Received server response at ${Date()}:")
                        builder.append("\n  Response")
                        builder.append("\n    Status:  $code")
                        builder.append("\n    Headers: $headers")
                        builder.append("\n    Message: ${message?.let { it.ifEmpty { "No message" } } ?: "No message"}")
                        builder.append("\n    Body:    ${body?.let { "${it.content.size} bytes" } ?: ": No body"}")
                        if (body != null) {
                            builder.append("\n${body.content.decodeToString()}")
                        }
                        builder.toString()
                    }
                }
            }
        } catch (e: IOException) {
            val code = Reason.CODE_CLIENT_ERROR
            val reason = Reason(code, e.message ?: e.javaClass.simpleName)
            throw HttpExecutionException(request, reason, null, cause = e)
        }
    }

    private fun getBodyString(body: RequestBody?): String {
        if (body == null) {
            return "No body"
        }

        val builder = StringBuilder("contentType: '${body.contentType()}'")

        if (body is FormBodyOK) {
            builder.append(" ")
            for (i in 0..<body.size) {
                if (i > 0) {
                    builder.append(" ")
                }
                builder.append("${body.name(i)}=[${body.value(i)}]")
                if (i < body.size - 1) {
                    builder.append(",")
                }

            }
        }

        //TODO
        return builder.toString()
    }

    private fun createOkHttpClient(bundle: CertificateBundle? = null, proxyUri: URI? = null): OkHttpClient {
        val builder = OkHttpClient.Builder()

        proxyUri?.let { uri ->
            val type = Proxy.Type.valueOf(uri.scheme.uppercase())
            val proxy = Proxy(type, InetSocketAddress(uri.host, uri.port))
            builder.proxy(proxy)
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
        private var requestBody: Body<*>? = null
        private var certificateBundle: CertificateBundle? = null
        private var certificateReference: CertificateReference? = null
        private var certFolder: File? = null
        private var token: OAuthTokenResponse? = null

        init {
            headers(
                "Accept-Charset" to setOf("UTF-8"),
                "Accept-Language" to setOf("en-US")
            )
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

        fun headers(vararg pairs: Pair<String, Set<String>>) = apply {
            pairs.forEach {
                this.requestHeaders[it.first.lowercase()] = it.second
            }
        }

        fun body(form: Map<String, String>, type: ContentType? = null) = apply {
            this.requestBody = FormBody(type ?: ContentType.FORM, form).also {
                headers("Content-Type" to setOf(it.type.mediaType))
            }
        }

        fun body(content: String, type: ContentType) = apply {
            this.requestBody = StringBody(type, content).also {
                headers("Content-Type" to setOf(it.type.mediaType))
            }
        }

        fun token(token: OAuthTokenResponse) = apply {
            this.token = token
        }

        fun build(): HttpClient {
            if (certificateBundle != null && certificateReference != null) {
                throw IllegalStateException("Either bundle or reference can be specified")
            }
            val endpoint = requireNotNull(endpoint)
            val bundle = certificateReference?.let { resolveReference(it) } ?: certificateBundle
            val request = Request(method, endpoint, requestHeaders, requestBody, bundle)
            return HttpClient(request, token)
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