package de.alexanderwolz.http.client.instance

import com.google.gson.JsonElement
import de.alexanderwolz.commons.log.Logger
import de.alexanderwolz.commons.util.StringUtils
import de.alexanderwolz.http.client.HttpClient
import de.alexanderwolz.http.client.exception.HttpExecutionException
import de.alexanderwolz.http.client.exception.Reason
import de.alexanderwolz.http.client.model.Form
import de.alexanderwolz.http.client.model.HttpMethod
import de.alexanderwolz.http.client.model.Request
import de.alexanderwolz.http.client.model.Response
import de.alexanderwolz.http.client.model.certificate.CertificateBundle
import de.alexanderwolz.http.client.model.payload.Payload
import de.alexanderwolz.http.client.model.payload.PayloadImpl
import de.alexanderwolz.http.client.model.token.AccessToken
import de.alexanderwolz.http.client.model.type.BasicContentTypes
import de.alexanderwolz.http.client.model.type.ContentType
import de.alexanderwolz.http.client.socket.SslSocket
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
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
internal class OkHttpClientWrapper(
    proxy: URI?,
    verifyCert: Boolean,
    certificates: CertificateBundle?,
    httpMethod: HttpMethod,
    endpoint: URI,
    headers: Map<String, Set<String>>,
    payload: Payload,
    acceptTypes: Set<ContentType>?,
    accessToken: AccessToken?
) : HttpClient {

    private val logger = Logger(javaClass)

    private val okHttpClient = createOkHttpClient(certificates, proxy, verifyCert)

    override val request = createRequest(httpMethod, endpoint, headers, payload, acceptTypes, accessToken)

    private fun createRequest(
        method: HttpMethod,
        endpoint: URI,
        headers: Map<String, Set<String>>,
        payload: Payload,
        acceptTypes: Set<ContentType>?,
        accessToken: AccessToken? = null
    ): Request {

        val requestHeaders = HashMap<String, Set<String>>().apply {
            headers.forEach {
                val key = it.key.lowercase()
                if(contains(it.key)){
                    logger.warn { "Header with key=${it.key} already exist in map (${get(it.key)}), overwriting with ${values.joinToString()}" }
                }
                put(key,it.value)
            }
        }

        accessToken?.let {
            //overwrite Authorization header if accessToken is given
            requestHeaders.put("authorization", setOf("${StringUtils.capitalize(it.type)} ${it.encodedJWT}"))
        }

        acceptTypes?.map { it.mediaType }?.takeIf { it.isNotEmpty() }?.let {
            //overwrite accept header from types
            requestHeaders.put("accept", it.toSet())
        }

        return Request(method, endpoint, requestHeaders, payload, acceptTypes)
    }

    override fun execute(): Response {

        val okRequestBuilder = okhttp3.Request.Builder().url(request.endpoint.toURL())
        request.headers.forEach {
            okRequestBuilder.header(it.key, it.value.joinToString())
        }

        val okRequestBody = convertRequestBody(request.body)
        okRequestBuilder.method(request.httpMethod.name, okRequestBody)

        val okRequest = okRequestBuilder.build()
        logRequest(okRequest)

        try {
            okHttpClient.newCall(okRequest).execute().use { okResponse ->
                return Response(
                    request,
                    okResponse.code,
                    okResponse.message,
                    okResponse.headers.toMultimap(),
                    convertResponseBody(okResponse),
                    Response.Source(okRequest, okResponse)
                ).also { logResponse(it) }
            }
        } catch (e: IOException) {
            val code = Reason.Companion.CODE_CLIENT_ERROR
            val reason = Reason(code, e.message ?: e.javaClass.simpleName)
            throw HttpExecutionException(request, reason, null, cause = e)
        }
    }

    private fun convertRequestBody(payload: Payload): RequestBody? {
        if (payload == Payload.Companion.EMPTY) {
            return null
        }
        val type = payload.type
        return when (payload.element) {
            is Form -> {
                val builder = FormBody.Builder()
                (payload.element as Form).entries.forEach { entry ->
                    builder.add(entry.key, entry.value)
                }
                builder.build()
            }

            is String -> {
                (payload.element as String).toRequestBody(type.mediaType.toMediaType())
            }

            is JsonElement -> {
                (payload.element as JsonElement).asString.toRequestBody(type.mediaType.toMediaType())
            }

            else -> {
                payload.bytes.toRequestBody(type.mediaType.toMediaType())
            }
        }
    }

    private fun convertResponseBody(okResponse: okhttp3.Response): Payload {
        okResponse.body?.let { okBody ->
            val bytes = okBody.source().use { it.readByteArray() }
            val mediaType = okResponse.headers["content-type"]
            if (mediaType != null) {
                logger.trace { "Server returned content-type: $mediaType" }
                val normalized = mediaType.split(";").first()
                val acceptTypes = request.acceptTypes ?: emptySet()
                val contentType = acceptTypes.find { it.mediaType.startsWith(normalized) }
                if (contentType != null) {
                    logger.trace { "Found content type in specified accept types: $contentType (${contentType.clazz.java})" }
                    return PayloadImpl(contentType, bytes)
                } else {
                    logger.warn { "Could not determine content-type from request accept types (${request.acceptTypes?.joinToString()})" }
                    val basicType = BasicContentTypes.entries.find { it.mediaType.startsWith(normalized) }
                    if (basicType != null) {
                        logger.trace { "Found basic content type: $basicType" }
                        return PayloadImpl(basicType, bytes)
                    } else {
                        logger.warn { "Could not determine content-type from basic types" }
                        logger.warn { "Consider setting the appropriate accept type using ${HttpClient.Builder::class}" }
                        throw NoSuchElementException("Could not determine content-type reference for '$mediaType'")
                    }
                }
            } else {
                logger.trace { "Server did not return any content-type (but content-length=${bytes.size}" }
            }
        }
        return Payload.Companion.EMPTY
    }

    private fun logRequest(okRequest: okhttp3.Request) {
        logger.trace {
            val bodyType = request.body.takeIf { it != Payload.Companion.EMPTY }
                ?.let { "ContentType: ${it.type}->${it.type.clazz.java.name}" } ?: "No request body"
            val headers = okRequest.headers.toMultimap().entries.joinToString()
            val builder = StringBuilder("Executing request at ${Date()}:")
            builder.append("\n\tRequest")
            builder.append("\n\t\tMethod:  ${okRequest.method}")
            builder.append("\n\t\tURL:     ${okRequest.url}")
            builder.append("\n\t\tHeaders: $headers")
            builder.append("\n\t\tBody:    $bodyType")
            getBodyString(request.body)?.lines()?.forEach { builder.append("\n\t\t\t$it") }
            builder.toString()
        }
    }

    private fun logResponse(response: Response) {
        logger.trace {
            val bodyType = response.body.takeIf { it != Payload.Companion.EMPTY }
                ?.let { "ContentType: ${it.type}->${it.type.clazz.java.name}" } ?: "No response body"
            val builder = StringBuilder("Received server response at ${Date()}:")
            builder.append("\n\tResponse")
            builder.append("\n\t\tStatus:  ${response.code}")
            builder.append("\n\t\tMessage: ${response.message?.let { it.ifEmpty { "No message" } } ?: "No message"}")
            builder.append("\n\t\tHeaders: ${response.headers}")
            builder.append("\n\t\tBody:    $bodyType")
            getBodyString(response.body)?.lines()?.forEach { builder.append("\n\t\t\t$it") }
            builder.toString()
        }
    }

    fun getBodyString(payload: Payload): String? {
        if (payload == Payload.Companion.EMPTY) return null
        return StringBuilder().apply {
            append(payload.bytes.decodeToString()) //TODO parse element?
        }.toString()
    }

    private fun createOkHttpClient(
        bundle: CertificateBundle? = null,
        proxy: URI? = null,
        verifyCert: Boolean = true
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()

        proxy?.let { uri ->
            val type = Proxy.Type.valueOf(uri.scheme.uppercase())
            builder.proxy(Proxy(type, InetSocketAddress(uri.host, uri.port)))
        }

        builder.connectTimeout(10, TimeUnit.SECONDS)
        builder.readTimeout(10, TimeUnit.SECONDS)
        bundle?.let {
            val sslSocket = createCustomSslSocketFactory(it, verifyCert)
            builder.sslSocketFactory(sslSocket.sslSocketFactory, sslSocket.trustManager)
        }
        return builder.build()
    }

    private fun createCustomSslSocketFactory(bundle: CertificateBundle, verifyCert: Boolean): SslSocket {

        //generate custom key store
        val keyManagers = createCustomKeyStore(bundle.certificates, bundle.privateKey)

        //generate custom trust managers
        val trustManagers = createCustomTrustStore(bundle.caCertificates, verifyCert)

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

    private fun createCustomTrustStore(caCertificates: List<Certificate>, verifyCert: Boolean): Array<TrustManager> {
        return if (verifyCert) {
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

}