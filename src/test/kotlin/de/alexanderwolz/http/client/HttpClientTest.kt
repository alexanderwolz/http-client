package de.alexanderwolz.http.client

import com.google.gson.Gson
import com.google.gson.JsonElement
import de.alexanderwolz.http.client.exception.HttpExecutionException
import de.alexanderwolz.http.client.exception.Reason
import de.alexanderwolz.http.client.model.Form
import de.alexanderwolz.http.client.model.HttpMethod
import de.alexanderwolz.http.client.model.Payload
import de.alexanderwolz.http.client.model.converter.Converter
import de.alexanderwolz.http.client.model.type.BasicContentTypes
import de.alexanderwolz.http.client.model.type.ContentType
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.UnknownHostException
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HttpClientTest {

    @Test
    fun testSimpleGetWithJson() {
        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(HttpMethod.GET)
            .endpoint(URI.create("https://api.predic8.de/shop/v2/products"))
            .accept(BasicContentTypes.APPLICATION_JSON)
            .build()

        val response = httpClient.execute()
        assertTrue { response.code == 200 }
        assertTrue { response.message == "OK" }
        assertTrue { response.isOK }
        assertNotNull(response.body)
        assertTrue { response.body.type == BasicContentTypes.APPLICATION_JSON }
        assertIs<String>(response.body.element)
    }

    @Test
    fun testSimpleGetWithGSON() {
        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(HttpMethod.GET)
            .endpoint(URI.create("https://api.predic8.de/shop/v2/products"))
            .accept(BasicContentTypes.GSON)
            .build()

        val response = httpClient.execute()
        assertTrue { response.code == 200 }
        assertTrue { response.message == "OK" }
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
        }
    }

    @Test
    fun testSimpleGetWithoutAcceptTypeButReturnsBasicType() {
        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(HttpMethod.GET)
            .endpoint(URI.create("https://api.predic8.de/shop/v2/products"))
            .build()

        val response = httpClient.execute()
        assertTrue { response.code == 200 }
        assertTrue { response.message == "OK" }
        assertTrue { response.isOK }
        assertNotNull(response.body)
        assertIs<JsonElement>(response.body.element)
    }


    @Test
    fun testJsonElementPost() {

        val jsonString = "{\"name\":\"Dauerlutscher\",\"price\":1.99}"
        val jsonElement = Gson().toJsonTree(jsonString)
        val payload = Payload(BasicContentTypes.APPLICATION_JSON, jsonElement)

        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(HttpMethod.POST)
            .endpoint(URI.create("https://api.predic8.de/shop/v2/products"))
            .accept(BasicContentTypes.APPLICATION_JSON)
            .body(payload)
            .build()
        val response = httpClient.execute()
        println("Status: ${response.code}")
        if (response.isOK) {
            response.body?.let { payload ->
                assertIs<JsonElement>(payload.element)
            } ?: throw Exception("Body should not be empty")
        } else {
            throw Exception("Response should be OK, but was ${response.code}")
        }
    }

    @Test
    fun testJsonBinaryPost() {

        val jsonString = "{\"name\":\"Dauerlutscher\",\"price\":1.99}"
        val payload = Payload(BasicContentTypes.APPLICATION_JSON, jsonString.toByteArray())

        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(HttpMethod.POST)
            .endpoint(URI.create("https://api.predic8.de/shop/v2/products"))
            .accept(BasicContentTypes.APPLICATION_JSON)
            .body(payload)
            .build()
        val response = httpClient.execute()
        println("Status: ${response.code}")
        if (response.isOK) {
            response.body?.let { payload ->
                assertIs<JsonElement>(payload.element)
            } ?: throw Exception("Body should not be empty")
        } else {
            throw Exception("Response should be OK, but was ${response.code}")
        }
    }

    @Test
    fun testFormPost() {

        val form = Form(mapOf("key1" to "value1"))
        val payload = Payload(BasicContentTypes.FORM_URL_ENCODED, form)

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
        assertNotNull(response.body)
        assertEquals("application/json", response.body.type.mediaType)
        assertEquals("application/x-www-form-urlencoded", response.request.body?.type?.mediaType)
    }

    @Test
    fun testGetWithCustomType() {
        val type = Types.CUSTOM_NAME
        val content = CustomName("MyName")
        val payload = Payload(type, content)

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
        assertNotNull(response.body)
        assertEquals("application/json", response.body.type.mediaType)
        assertEquals("application/customName", response.request.body?.type?.mediaType)
    }

    private enum class Types(override val mediaType: String, override val clazz: KClass<*>) : ContentType {
        CUSTOM_NAME("application/customName", CustomName::class) {
            override val converter = Converter(
                { it.name.toByteArray() },
                { CustomName(it.decodeToString()) })
        };
    }

    private data class CustomName(val name: String)

}