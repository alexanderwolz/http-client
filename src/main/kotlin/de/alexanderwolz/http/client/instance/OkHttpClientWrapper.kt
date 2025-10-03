package de.alexanderwolz.http.client.instance

import de.alexanderwolz.http.client.AbstractHttpClient
import de.alexanderwolz.http.client.model.HttpMethod
import de.alexanderwolz.http.client.model.Request
import de.alexanderwolz.http.client.model.certificate.CertificateBundle
import de.alexanderwolz.http.client.model.content.ContentResolver
import de.alexanderwolz.http.client.model.content.ContentType
import de.alexanderwolz.http.client.model.payload.Payload
import de.alexanderwolz.http.client.model.token.AccessToken
import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.util.concurrent.TimeUnit

//This class is intended to wrap HTTP libraries, in this case OKHTTP
internal class OkHttpClientWrapper(
    proxy: URI?,
    verifyCert: Boolean,
    certificates: CertificateBundle?,
    httpMethod: HttpMethod,
    endpoint: URI,
    headers: Map<String, Set<String>>,
    payload: Payload<*>,
    resolver: ContentResolver?,
    acceptTypes: Set<ContentType>?,
    accessToken: AccessToken?
) : AbstractHttpClient<OkHttpClient>(
    proxy, verifyCert, certificates, httpMethod, endpoint, headers, payload, resolver, acceptTypes, accessToken
) {

    override fun createClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()

        proxy?.let { uri ->
            val type = Proxy.Type.valueOf(uri.scheme.uppercase())
            builder.proxy(Proxy(type, InetSocketAddress(uri.host, uri.port)))
        }

        builder.connectTimeout(10, TimeUnit.SECONDS)
        builder.readTimeout(10, TimeUnit.SECONDS)
        customSslSocket?.let {
            builder.sslSocketFactory(it.sslSocketFactory, it.trustManager)
        }
        return builder.build()
    }

    override fun execute(client: OkHttpClient, request: Request): RawResponse {

        val okRequestBuilder = okhttp3.Request.Builder().url(request.endpoint.toURL())
        request.headers.forEach {
            okRequestBuilder.header(it.key, it.value.joinToString())
        }

        val okRequestBody = convertRequestBody(request.body)
        okRequestBuilder.method(request.httpMethod.name, okRequestBody)

        val okRequest = okRequestBuilder.build()

        client.newCall(okRequest).execute().use { okResponse ->
            return RawResponse(
                okResponse.code,
                okResponse.message,
                okResponse.headers.toMultimap(),
                okResponse.headers("content-type"),
                okResponse.body?.source()?.use { it.readByteArray() },
                RawResponse.Source(okRequest, okResponse)
            )
        }
    }

}