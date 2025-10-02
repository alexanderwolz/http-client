package de.alexanderwolz.http.client

import com.google.gson.JsonElement
import de.alexanderwolz.commons.log.Logger
import de.alexanderwolz.commons.util.StringUtils
import de.alexanderwolz.http.client.exception.HttpExecutionException
import de.alexanderwolz.http.client.exception.Reason
import de.alexanderwolz.http.client.model.Form
import de.alexanderwolz.http.client.model.HttpMethod
import de.alexanderwolz.http.client.model.Request
import de.alexanderwolz.http.client.model.Response
import de.alexanderwolz.http.client.model.certificate.CertificateBundle
import de.alexanderwolz.http.client.model.payload.Payload
import de.alexanderwolz.http.client.model.token.AccessToken
import de.alexanderwolz.http.client.model.type.BasicContentTypes
import de.alexanderwolz.http.client.model.type.ContentType
import de.alexanderwolz.http.client.socket.SslSocket
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import java.net.URI
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.*

abstract class AbstractHttpClient<T>(
    protected val proxy: URI?,
    verifyCert: Boolean,
    certificates: CertificateBundle?,
    httpMethod: HttpMethod,
    endpoint: URI,
    headers: Map<String, Set<String>>,
    payload: Payload,
    acceptTypes: Set<ContentType>?,
    accessToken: AccessToken?
) : HttpClient {

    protected val logger = Logger(javaClass)

    protected val customSslSocket = certificates?.let { createCustomSslSocketFactory(certificates, verifyCert) }

    override val request = createRequest(httpMethod, endpoint, headers, payload, acceptTypes, accessToken)

    protected abstract fun createClient(): T

    override fun execute(): Response {
        val client = createClient()
        try {
            val rawResponse = execute(client, request)
            return Response(
                request,
                rawResponse.code,
                rawResponse.message,
                rawResponse.headers,
                convertResponseBody(rawResponse.contentTypes, rawResponse.content),
            ).also { logResponse(it) }
        } catch (e: IOException) {
            val code = Reason.CODE_CLIENT_ERROR
            val reason = Reason(code, e.message ?: e.javaClass.simpleName)
            throw HttpExecutionException(request, reason, null, cause = e)
        }
    }

    protected abstract fun execute(client: T, request: Request): RawResponse

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
                if (contains(it.key)) {
                    logger.warn { "Header with key=${it.key} already exist in map (${get(it.key)}), overwriting with ${values.joinToString()}" }
                }
                put(key, it.value)
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
            .also { logRequest(it) }
    }

    protected fun convertRequestBody(payload: Payload): RequestBody? {
        if (payload == Payload.EMPTY) {
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
                //TODO validate
                payload.bytes.toRequestBody(type.mediaType.toMediaType())
            }
        }
    }

    protected fun logRequest(request: Request) {
        logger.trace {
            val bodyType = request.body.takeIf { it != Payload.EMPTY }
                ?.let { "ContentType: ${it.type}->${it.type.clazz.java.name}" } ?: "No request body"
            val headers = request.headers.entries.joinToString()
            val builder = StringBuilder("Executing request at ${Date()}:")
            builder.append("\n\tRequest")
            builder.append("\n\t\tMethod:  ${request.httpMethod}")
            builder.append("\n\t\tURL:     ${request.endpoint}")
            builder.append("\n\t\tHeaders: $headers")
            builder.append("\n\t\tBody:    $bodyType")
            getBodyString(request.body)?.lines()?.forEach { builder.append("\n\t\t\t$it") }
            builder.toString()
        }
    }

    protected fun logResponse(response: Response) {
        logger.trace {
            val bodyType = response.body.takeIf { it != Payload.EMPTY }
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
        if (payload == Payload.EMPTY) return null
        return StringBuilder().apply {
            append(payload.bytes.decodeToString()) //TODO parse element?
        }.toString()
    }

    private fun convertResponseBody(mediaTypes: List<String>, bytes: ByteArray?): Payload {
        if (mediaTypes.isEmpty()) {
            if (bytes == null || bytes.isEmpty()) {
                logger.trace { "Server did not return any content-type nor bytes" }
                return Payload.EMPTY
            }
            logger.trace { "Server did not return a content-type (but content-length=${bytes.size}" }
            return Payload.create(BasicContentTypes.APPLICATION_OCTET_STREAM, bytes)
        }

        logger.trace { "Server returned content-type: ${mediaTypes.joinToString()}" }
        logger.trace { "UTF decoded bytes:\n${bytes?.decodeToString()}" }

        val normalized = mediaTypes.first()
        val acceptTypes = request.acceptTypes ?: emptySet()
        val contentType = acceptTypes.find { it.mediaType.startsWith(normalized) }
        //there cant be several accept types with same media type, so we are safe here
        if (contentType != null) {
            logger.trace { "Found content type in specified accept types: $contentType (${contentType.clazz.java})" }
            return createResponsePayload(contentType, bytes)
        } else {
            logger.warn { "Could not determine content-type from request accept types (${request.acceptTypes?.joinToString()})" }
            val basicType = BasicContentTypes.entries.find { it.mediaType.startsWith(normalized) }
            if (basicType != null) {
                logger.trace { "Found basic content type: $basicType" }
                return createResponsePayload(basicType, bytes)
            } else {
                logger.warn { "Could not determine content-type from basic types" }
                logger.warn { "Consider setting the appropriate accept type using ${HttpClient.Builder::class}" }
                throw NoSuchElementException("Could not determine content-type reference for '$mediaTypes'")
            }
        }
    }

    private fun createResponsePayload(type: ContentType, bytes: ByteArray?): Payload {
        bytes?.let {
            return Payload.create(type, it)
        }
        throw NoSuchElementException("Received empty byte array with content type: $type")
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

    data class RawResponse(
        val code: Int,
        val message: String?,
        val headers: Map<String, List<String>>,
        val contentTypes: List<String>,
        val content: ByteArray?,
        private val source: Source
    ) {

        data class Source(val request: Any, val response: Any)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as RawResponse

            if (code != other.code) return false
            if (message != other.message) return false
            if (headers != other.headers) return false
            if (contentTypes != other.contentTypes) return false
            if (!content.contentEquals(other.content)) return false
            if (source != other.source) return false

            return true
        }

        override fun hashCode(): Int {
            var result = code
            result = 31 * result + (message?.hashCode() ?: 0)
            result = 31 * result + headers.hashCode()
            result = 31 * result + contentTypes.hashCode()
            result = 31 * result + (content?.contentHashCode() ?: 0)
            result = 31 * result + source.hashCode()
            return result
        }


    }

}