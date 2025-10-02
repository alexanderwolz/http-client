package de.alexanderwolz.http.client

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import de.alexanderwolz.commons.util.CertificateUtils
import de.alexanderwolz.http.client.exception.HttpExecutionException
import de.alexanderwolz.http.client.exception.Reason
import de.alexanderwolz.http.client.instance.OkHttpClientWrapper
import de.alexanderwolz.http.client.model.*
import de.alexanderwolz.http.client.model.certificate.CertificateBundle
import de.alexanderwolz.http.client.model.certificate.CertificateReference
import de.alexanderwolz.http.client.model.payload.PayloadImpl
import de.alexanderwolz.http.client.model.token.AccessToken
import de.alexanderwolz.http.client.model.token.OAuthTokenResponse
import de.alexanderwolz.http.client.model.type.BasicContentTypes
import okhttp3.mockwebserver.MockResponse
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
        assertTrue { httpClient.request.headers.contains("user")}
        assertTrue { httpClient.request.headers.contains("server")}
        assertTrue { httpClient.request.headers.contains("authorization")}
        assertTrue { httpClient.request.headers.contains("user-agent")}
        assertEquals(HttpMethod.DELETE, httpClient.request.httpMethod)
        assertEquals(URI.create("/endpoint"), httpClient.request.endpoint)
    }

    @Test
    fun testSimpleGetWithJson() {

        startSimpleJsonServer()

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

        startSimpleJsonServer()

        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(HttpMethod.GET)
            .endpoint(mockServer.url("/endpoint").toUri())
            .accept(BasicContentTypes.GSON)
            .build()

        val response = httpClient.execute()
        assertEquals(200, response.code)
        assertEquals("OK", response.message)
        assertTrue { response.isOK }
        assertNotNull(response.body)
        assertTrue { response.body.type == BasicContentTypes.GSON }
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
            assertNotNull(e.response)
            assertNotNull(e.request)
        }
    }

    @Test
    fun testSimpleGetWithoutAcceptTypeButReturnsBasicType() {

        startSimpleJsonServer()

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
    fun testJsonElementPost() {

        startSimpleJsonServer()

        val jsonString = "{\"name\":\"Dauerlutscher\",\"price\":1.99}"
        val payload = PayloadImpl(BasicContentTypes.APPLICATION_JSON, jsonString)

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
    fun testGsonElementPost() {

        startSimpleJsonServer()

        val jsonString = "{\"name\":\"Dauerlutscher\",\"price\":1.99}"
        val jsonElement = Gson().toJsonTree(jsonString)
        val payload = PayloadImpl(BasicContentTypes.GSON, jsonElement)

        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(HttpMethod.POST)
            .endpoint(mockServer.url("/endpoint").toUri())
            .accept(BasicContentTypes.GSON)
            .body(payload)
            .build()
        val response = httpClient.execute()
        if (response.isOK) {
            assertEquals(BasicContentTypes.GSON, response.body.type)
            assertIs<JsonElement>(response.body.element)
        } else {
            throw Exception("Response should be OK, but was ${response.code}")
        }
    }

    @Test
    fun testJsonBinaryPost() {

        startSimpleJsonServer()

        val jsonString = "{\"name\":\"Dauerlutscher\",\"price\":1.99}"
        val payload = PayloadImpl(BasicContentTypes.APPLICATION_JSON, jsonString.toByteArray())

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

        startSimpleJsonServer()

        val jsonString = "{\"name\":\"Dauerlutscher\",\"price\":1.99}"
        val payload = PayloadImpl(BasicContentTypes.GSON, jsonString.toByteArray())

        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(HttpMethod.POST)
            .endpoint(mockServer.url("/endpoint").toUri())
            .accept(BasicContentTypes.GSON)
            .body(payload)
            .build()
        val response = httpClient.execute()
        if (response.isOK) {
            assertEquals(BasicContentTypes.GSON, response.body.type)
            assertIs<JsonElement>(response.body.element)
        } else {
            throw Exception("Response should be OK, but was ${response.code}")
        }
    }

    @Test
    fun testFormPost() {

        startSimpleJsonServer()

        val form = Form(mapOf("key1" to "value1"))
        val payload = PayloadImpl(BasicContentTypes.FORM_URL_ENCODED, form)

        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(HttpMethod.POST)
            .endpoint(URI.create("https://api.predic8.de/shop/v2/products"))
            .accept(BasicContentTypes.APPLICATION_JSON)
            .body(payload)
            .build()
        val response = httpClient.execute()
        assertEquals(415, response.code)
        assertEquals("Unsupported Media Type", response.message)
        assertEquals("application/json", response.body.type.mediaType)
        assertEquals("application/x-www-form-urlencoded", response.request.body?.type?.mediaType)
    }

    @Test
    fun testGetWithCustomType() {

        startSimpleJsonServer()

        val type = CustomContentTypes.CUSTOM_NAME
        val content = CustomName("MyName")
        val payload = PayloadImpl(type, content)

        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(HttpMethod.POST)
            .endpoint(URI.create("https://api.predic8.de/shop/v2/products"))
            .accept(BasicContentTypes.APPLICATION_JSON)
            .body(payload)
            .build()
        val response = httpClient.execute()
        assertEquals(415, response.code)
        assertEquals("Unsupported Media Type", response.message)
        assertEquals("application/json", response.body.type.mediaType)
        assertEquals("application/customName", response.request.body?.type?.mediaType)
    }

    @Test
    fun testCertificateReferences() {

        startSimpleJsonServer()

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

        startSimpleJsonServer()

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

        val jwt = startJwtServer()

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

        val jwt = startJwtServer()
        val jwtJsonElement = JsonParser.parseString(jwt).asJsonObject

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
        assertEquals(jwtJsonElement.get("access_token").asString, tokenResponse.accessToken)
        assertEquals(jwtJsonElement.get("token_type").asString, tokenResponse.tokenType)
        assertEquals(jwtJsonElement.get("expires_in").asInt, tokenResponse.expiresInSeconds)
        val token = tokenResponse.toAccessToken()
        assertNotNull(token)
        assertFalse { token.isExpired() }
    }

    private fun startJwtServer(): String {

        val oauthJson = JwtHelper().createOauthResponse(
            JwtHelper.TEST_SECRET,
            JwtHelper.TEST_ISSUER,
            emptyMap(),
            60,
            JwtHelper.TEST_SCOPE
        )
        val mockResponse = MockResponse().apply {
            setResponseCode(200)
            setBody(oauthJson)
            addHeader("Content-Type", "application/json")
        }
        mockServer.enqueue(mockResponse)
        return oauthJson
    }

    private fun startSimpleJsonServer(): String {

        val json = "{\"id\":\"1\",\"product\":\"apple\"}"
        val mockResponse = MockResponse().apply {
            setResponseCode(200)
            setBody(json)
            addHeader("Content-Type", "application/json")
        }
        mockServer.enqueue(mockResponse)
        return json
    }

}