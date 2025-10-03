package de.alexanderwolz.http.client

import de.alexanderwolz.commons.util.CertificateUtils
import de.alexanderwolz.http.client.exception.HttpExecutionException
import de.alexanderwolz.http.client.exception.Reason
import de.alexanderwolz.http.client.instance.OkHttpClientWrapper
import de.alexanderwolz.http.client.model.*
import de.alexanderwolz.http.client.model.certificate.CertificateBundle
import de.alexanderwolz.http.client.model.certificate.CertificateReference
import de.alexanderwolz.http.client.model.content.BasicContentTypes
import de.alexanderwolz.http.client.model.payload.Payload
import de.alexanderwolz.http.client.model.token.AccessToken
import de.alexanderwolz.http.client.model.token.OAuthTokenResponse
import de.alexanderwolz.http.client.util.MockUtils
import de.alexanderwolz.http.client.util.MockUtils.CONTENT_PRODUCT_JSON
import de.alexanderwolz.http.client.util.MockUtils.MEDIA_TYPE_PRODUCT
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.math.BigInteger
import java.net.URI
import java.net.UnknownHostException
import kotlin.test.*

class HttpClientTest {

    @TempDir
    private lateinit var tmpDir: File

    private lateinit var mockServer: MockWebServer

    @BeforeEach
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
    }

    @AfterEach
    fun teardown() {
        mockServer.shutdown()
    }

    @Test
    fun testInstance() {
        val httpClient = HttpClient.Builder().endpoint(URI.create("/endpoint")).build()
        assertNotNull(httpClient)
        assertIs<OkHttpClientWrapper>(httpClient)
    }

    @Test
    fun testProperties() {
        val pair = CertificateUtils.generateNewCertificatePair("CN=Test")
        val certs = CertificateBundle(pair.first, listOf(pair.second), emptyList())
        val httpClient = HttpClient.Builder()
            .certificates(certs)
            .verifyCert(false)
            .accessToken(AccessToken("haha", "Bearer", 60, "scope"))
            .proxy(URI.create("http://localhost:8080"))
            .headers(Pair("user", setOf("MyName")))
            .headers("server" to setOf("ATARI")) //adds
            .userAgent("Agent")
            .method(HttpMethod.PUT)
            .method(HttpMethod.DELETE) //overwrites
            .endpoint(URI.create("/endpoint"))
            .build()
        assertNotNull(httpClient)
        assertIs<OkHttpClientWrapper>(httpClient)
        assertNotNull(httpClient.request)
        assertTrue { httpClient.request.headers.contains("user") }
        assertTrue { httpClient.request.headers.contains("server") }
        assertTrue { httpClient.request.headers.contains("authorization") }
        assertTrue { httpClient.request.headers.contains("user-agent") }
        assertEquals(HttpMethod.DELETE, httpClient.request.httpMethod)
        assertEquals(URI.create("/endpoint"), httpClient.request.endpoint)
    }

    @Test
    fun testSimpleGetWithJson() {

        MockUtils.startSimpleJsonServer(mockServer)

        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(HttpMethod.GET)
            .endpoint(mockServer.url("/endpoint").toUri())
            .accept(BasicContentTypes.APPLICATION_JSON)
            .build()

        val response = httpClient.execute()
        assertEquals(200, response.code)
        assertEquals("OK", response.message)
        assertTrue { response.isOK }
        assertNotNull(response.body)
        assertTrue { response.body.type == BasicContentTypes.APPLICATION_JSON }
        assertIs<String>(response.body.element)
    }

    @Test
    fun testSimpleGetWithGSON() {

        MockUtils.startSimpleJsonServer(mockServer)

        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(HttpMethod.GET)
            .endpoint(mockServer.url("/endpoint").toUri())
            .accept(BasicContentTypes.JSON_ELEMENT)
            .build()

        val response = httpClient.execute()
        assertEquals(200, response.code)
        assertEquals("OK", response.message)
        assertTrue { response.isOK }
        assertNotNull(response.body)
        assertTrue { response.body.type == BasicContentTypes.JSON_ELEMENT }
        assertIs<JsonElement>(response.body.element)
    }

    @Test
    fun testSimpleGetWithUnknownHostException() {

        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(HttpMethod.GET)
            .endpoint(URI.create("https://this.shoud.not.exist.com/doesNotExist"))
            .accept(BasicContentTypes.TEXT_PLAIN)
            .build()
        try {
            httpClient.execute()
        } catch (e: HttpExecutionException) {
            e.printStackTrace()
            assertTrue { e.reason.code == Reason.CODE_CLIENT_ERROR }
            assertTrue { e.cause is UnknownHostException }
            assertTrue { e.reason.description == e.cause?.message }
            assertNull(e.response)
            assertNotNull(e.request)
        }
    }

    @Test
    fun testSimpleGetWithoutAcceptTypeButReturnsBasicType() {

        MockUtils.startSimpleJsonServer(mockServer)

        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(HttpMethod.GET)
            .endpoint(mockServer.url("/endpoint").toUri())
            .build()

        val response = httpClient.execute()
        assertEquals(200, response.code)
        assertEquals("OK", response.message)
        assertTrue { response.isOK }
        assertNotNull(response.body)
        assertIs<String>(response.body.element)
        assertEquals(BasicContentTypes.APPLICATION_JSON, response.body.type)
    }


    @Test
    fun testJsonPost() {

        MockUtils.startSimpleJsonServer(mockServer)

        val jsonString = "{\"name\":\"Dauerlutscher\",\"price\":1.99}"
        val payload = Payload.create(BasicContentTypes.APPLICATION_JSON, jsonString)

        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(HttpMethod.POST)
            .endpoint(mockServer.url("/endpoint").toUri())
            .accept(BasicContentTypes.APPLICATION_JSON)
            .body(payload)
            .build()
        val response = httpClient.execute()
        if (response.isOK) {
            assertEquals(BasicContentTypes.APPLICATION_JSON, response.body.type)
            assertIs<String>(response.body.element)
        } else {
            throw Exception("Response should be OK, but was ${response.code}")
        }
    }

    @Test
    fun testJsonElementPost() {

        MockUtils.startSimpleJsonServer(mockServer)

        val jsonString = "{\"name\":\"Dauerlutscher\",\"price\":1.99}"
        val jsonElement = Json.parseToJsonElement(jsonString)
        val payload = Payload.create(BasicContentTypes.JSON_ELEMENT, jsonElement)

        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(HttpMethod.POST)
            .endpoint(mockServer.url("/endpoint").toUri())
            .accept(BasicContentTypes.JSON_ELEMENT)
            .body(payload)
            .build()
        val response = httpClient.execute()
        if (response.isOK) {
            assertEquals(BasicContentTypes.JSON_ELEMENT, response.body.type)
            assertIs<JsonElement>(response.body.element)
        } else {
            throw Exception("Response should be OK, but was ${response.code}")
        }
    }

    @Test
    fun testJsonBinaryPost() {

        MockUtils.startSimpleJsonServer(mockServer)

        val jsonString = "{\"name\":\"Dauerlutscher\",\"price\":1.99}"
        val payload = Payload.create(BasicContentTypes.APPLICATION_JSON, jsonString.toByteArray())

        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(HttpMethod.POST)
            .endpoint(mockServer.url("/endpoint").toUri())
            .accept(BasicContentTypes.APPLICATION_JSON)
            .body(payload)
            .build()
        val response = httpClient.execute()
        if (response.isOK) {
            assertEquals(BasicContentTypes.APPLICATION_JSON, response.body.type)
            assertIs<String>(response.body.element)
        } else {
            throw Exception("Response should be OK, but was ${response.code}")
        }
    }

    @Test
    fun testGsonBinaryPost() {

        MockUtils.startSimpleJsonServer(mockServer)

        val jsonString = "{\"name\":\"Dauerlutscher\",\"price\":1.99}"
        val payload = Payload.create(BasicContentTypes.JSON_ELEMENT, jsonString.toByteArray())

        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(HttpMethod.POST)
            .endpoint(mockServer.url("/endpoint").toUri())
            .accept(BasicContentTypes.JSON_ELEMENT)
            .body(payload)
            .build()
        val response = httpClient.execute()
        if (response.isOK) {
            assertEquals(BasicContentTypes.JSON_ELEMENT, response.body.type)
            assertIs<JsonElement>(response.body.element)
        } else {
            throw Exception("Response should be OK, but was ${response.code}")
        }
    }

    @Test
    fun testFormPost() {

        MockUtils.startSimpleJsonServer(mockServer)

        val form = Form(mapOf("key1" to "value1"))
        val payload = Payload.create(BasicContentTypes.FORM_URL_ENCODED, form)

        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(HttpMethod.POST)
            .endpoint(mockServer.url("/endpoint").toUri())
            .accept(BasicContentTypes.APPLICATION_JSON)
            .body(payload)
            .build()
        val response = httpClient.execute()
        assertEquals("application/json", response.body.type.mediaType)
        assertEquals("application/x-www-form-urlencoded", response.request.body.type.mediaType)
    }

    @Test
    fun testGetWithCustomTypeProduct() {

        MockUtils.startProductServer(mockServer)

        val type = CustomContentTypes.PRODUCT
        val product = Product("2", "Bananas")
        val payload = Payload.create(type, product, CustomContentResolver())
        assertNotNull(payload)
        assertEquals(product, payload.element)

        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(HttpMethod.POST)
            .endpoint(mockServer.url("/endpoint").toUri())
            .accept(CustomContentTypes.PRODUCT)
            .body(payload)
            .build()
        val response = httpClient.execute()
        assertEquals(MEDIA_TYPE_PRODUCT, response.body.type.mediaType)
        assertEquals(MEDIA_TYPE_PRODUCT, response.request.body.type.mediaType)
    }

    @Test
    fun testGetWithCustomTypeWrappedProduct() {

        // use case: maybe we want to work with an element, but it must be wrapped into something
        // else during transfer to and from server.
        // here: we want to use product, but server only accepts WrappedProduct
        // typical use case: JAXB elements with ObjectFactory -> work with element

        //User creates payload with product -> server needs wrapped
        //Server sends content, client creates payload - client need unwrapped

        MockUtils.startWrappedProductServer(mockServer)

        //we send product to server, it returns wrappedProduct, but we use wrapping
        val product = Product("666", "Satanic Sandman")
        val payload = Payload.create(CustomContentTypes.PRODUCT, product)
        assertNotNull(payload)
        assertEquals(product, payload.element)

        val client = HttpClient.Builder()
            .method(HttpMethod.POST)
            .endpoint(mockServer.url("/endpoint").toUri())
            .accept(CustomContentTypes.WRAPPED_PRODUCT)
            .resolver(CustomContentResolver())
            .body(payload)
            .build()

        val response = client.execute()
        assertNotNull(response)

        val expectedPayload = Json.decodeFromString<Product>(CONTENT_PRODUCT_JSON)

        assertEquals(expectedPayload, response.body.element)

    }

    @Test
    fun testCertificateReferences() {

        MockUtils.startSimpleJsonServer(mockServer)

        val keyFile = File(tmpDir, "key.pem")
        val certFile = File(tmpDir, "cert.pem")
        CertificateUtils.writeNewCertPair(keyFile, certFile, "CN=TEST")

        val references = CertificateReference(keyFile, certFile)
        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .verifyCert(false)
            .method(HttpMethod.GET)
            .certificateLookupFolder(File("/src/test/resources"))
            .certificates(references)
            .endpoint(mockServer.url("/endpoint").toUri())
            .accept(BasicContentTypes.APPLICATION_JSON)
            .build()
        val response = httpClient.execute()
        assertNotNull(response)
    }

    @Test
    fun testCertificateBundle() {

        MockUtils.startSimpleJsonServer(mockServer)

        val certificatePair = CertificateUtils.generateNewCertificatePair("CN=Test", BigInteger.ZERO)
        val bundle = CertificateBundle(certificatePair.first, listOf(certificatePair.second), emptyList())

        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .verifyCert(false)
            .method(HttpMethod.GET)
            .certificateLookupFolder(File("/src/test/resources"))
            .certificates(bundle)
            .endpoint(mockServer.url("/endpoint").toUri())
            .accept(BasicContentTypes.APPLICATION_JSON)
            .build()
        val response = httpClient.execute()
        assertNotNull(response)
    }

    @Test
    fun testAccessTokenJsonString() {

        val jwt = MockUtils.startJwtServer(mockServer)

        val client = HttpClient.Builder()
            .method(HttpMethod.GET)
            .endpoint(mockServer.url("/token").toUri())
            .accept(BasicContentTypes.APPLICATION_JSON)
            .build()

        val response = client.execute()
        assertNotNull(response)
        assertEquals(200, response.code)
        assertNotNull(response.body)
        assertEquals(BasicContentTypes.APPLICATION_JSON, response.body.type)
        assertEquals(String::class, response.body.element::class)
        assertEquals(jwt, response.body.element)
    }

    @Test
    fun testAccessTokenWithOauthType() {

        val jwt = MockUtils.startJwtServer(mockServer)
        val jwtJson = Json.parseToJsonElement(jwt).jsonObject

        val client = HttpClient.Builder()
            .method(HttpMethod.GET)
            .endpoint(mockServer.url("/endpoint").toUri())
            .accept(BasicContentTypes.OAUTH_TOKEN)
            .build()

        val response = client.execute()
        assertNotNull(response)
        assertEquals(200, response.code)
        assertNotNull(response.body)
        assertEquals(BasicContentTypes.OAUTH_TOKEN, response.body.type)
        assertEquals(OAuthTokenResponse::class, response.body.element::class)
        val tokenResponse = response.body.element as OAuthTokenResponse
        assertEquals(jwtJson["access_token"]?.jsonPrimitive?.content, tokenResponse.accessToken)
        assertEquals(jwtJson["token_type"]?.jsonPrimitive?.content, tokenResponse.tokenType)
        assertEquals(jwtJson["expires_in"]?.jsonPrimitive?.int, tokenResponse.expiresInSeconds)
        val token = tokenResponse.toAccessToken()
        assertNotNull(token)
        assertFalse { token.isExpired() }
    }

}