package de.alexanderwolz.http.client

import com.google.gson.JsonElement
import de.alexanderwolz.http.client.exception.HttpExecutionException
import de.alexanderwolz.http.client.exception.Reason
import de.alexanderwolz.http.client.model.Method
import de.alexanderwolz.http.client.model.converter.Converter
import de.alexanderwolz.http.client.model.payload.Payload
import de.alexanderwolz.http.client.model.payload.PayloadImpl
import de.alexanderwolz.http.client.model.type.BasicContentTypes
import de.alexanderwolz.http.client.model.type.ContentType
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.UnknownHostException
import kotlin.reflect.KClass
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HttpClientTest {

    @Test
    fun testSimpleGetWithStatus200() {
        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(Method.GET)
            .endpoint(URI.create("https://api.predic8.de/shop/v2/products"))
            .accept(BasicContentTypes.JSON)
            .build()

        val response = httpClient.execute()
        assertTrue { response.code == 200 }
        assertTrue { response.message == "OK" }
        assertTrue { response.isOK }
        assertNotNull(response.body)
        assertIs<JsonElement>(response.body)
    }

    @Test
    fun testSimpleGetWithUnknownHostException() {
        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(Method.GET)
            .endpoint(URI.create("https://this.shoud.not.exist.com/doesNotExist"))
            .accept(BasicContentTypes.JSON)
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
            .method(Method.GET)
            .endpoint(URI.create("https://api.predic8.de/shop/v2/products"))
            .build()

        val response = httpClient.execute()
        assertTrue { response.code == 200 }
        assertTrue { response.message == "OK" }
        assertTrue { response.isOK }
        assertNotNull(response.body)
        assertIs<JsonElement>(response.body)
    }

    @Test
    fun testSimplePost() {

        val jsonString = "{\"name\":\"Dauerlutscher\",\"price\":1.99}"
        val payload = PayloadImpl(BasicContentTypes.JSON, jsonString.toByteArray())


        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(Method.POST)
            .endpoint(URI.create("https://api.predic8.de/shop/v2/products"))
            .accept(BasicContentTypes.JSON)
            .body(payload)
            .build()
        val response = httpClient.execute()
        println("Status: ${response.code}")
        if (response.isOK) {
            response.body?.let { payload ->
                val element = payload.type.converter.deserialize(payload.bytes)
                assertTrue { element!!::class == payload.type.clazz }
            } ?: throw Exception("Body should not be empty")
        } else {
            throw Exception("Response should be OK, but was ${response.code}")
        }

    }

    @Test
    fun testGetWithCustomType() {
        val type = Types.CUSTOM_NAME
        val content = CustomName("MyName")
        val payload = PayloadImpl(type, content.name.toByteArray()) //TODO

        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(Method.POST)
            .endpoint(URI.create("https://api.predic8.de/shop/v2/products"))
            .accept(BasicContentTypes.JSON)
            .body(payload)
            .build()
        val response = httpClient.execute()
        println("Status: ${response.code}")
        if (response.isOK) {
            response.body?.let { payload ->
                val element = payload.type.converter.deserialize(payload.bytes)
                assertTrue { element!!::class == payload.type.clazz }
            } ?: throw Exception("Body should not be empty")
        } else {
            throw Exception("Response should be OK, but was ${response.code}")
        }
    }

    private enum class Types(override val mediaType: String, override val clazz: KClass<*>) : ContentType {
        CUSTOM_NAME("application/custom2", CustomName::class) {
            override val converter = customNameConverter
        };

        val customNameConverter = Converter(
            { it.name.toByteArray() },
            { CustomName(it.decodeToString()) }
        )

    }

    private data class CustomName(val name: String)

}