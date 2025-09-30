package de.alexanderwolz.http.client

import com.google.gson.JsonElement
import de.alexanderwolz.http.client.exception.HttpExecutionException
import de.alexanderwolz.http.client.exception.Reason
import de.alexanderwolz.http.client.model.Method
import de.alexanderwolz.http.client.model.converter.IConverter
import de.alexanderwolz.http.client.model.payload.JsonPayload
import de.alexanderwolz.http.client.model.payload.Payload
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
        println(response.body.content.javaClass)
        println(response.body.content)
        assertIs<JsonPayload>(response.body)
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
        assertIs<JsonPayload>(response.body)
    }

    @Test
    fun testSimplePost() {

        val payload = "{\"name\":\"Dauerlutscher\",\"price\":1.99}"

        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(Method.POST)
            .endpoint(URI.create("https://api.predic8.de/shop/v2/products"))
            .accept(BasicContentTypes.JSON)
            .body(payload, BasicContentTypes.JSON)
            .build()
        val response = httpClient.execute()
        println("Status: ${response.code}")
        if (response.isOK && response.body != null && response.body.type == BasicContentTypes.JSON) {
            assertIs<JsonElement>(response.body.content)
            println("YAY!")
            println(response.body.content)
        } else {
            println("OH NO!")
            println(response.body?.content)
        }
    }

    @Test
    fun testGetWithCustomType() {
        val type = Types.CUSTOM_NAME
        val content = CustomName("MyName")
        val payload = CustomNamePayload(type, content)

        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(Method.POST)
            .endpoint(URI.create("https://api.predic8.de/shop/v2/products"))
            .accept(BasicContentTypes.JSON)
            .body(payload)
            .build()
        val response = httpClient.execute()
        println("Status: ${response.code}")
        if (response.isOK && response.body != null && response.body.type == BasicContentTypes.JSON) {
            println("YAY!")
            println(response.body.content)
        } else {
            println("OH NO!")
            println(response.body?.content)
        }
    }

    private enum class Types(override val mediaType: String, override val clazz: KClass<*>) : ContentType {
        CUSTOM_NAME("application/custom2", CustomName::class) {
            override val converter = customNameConverter
        };

        val customNameConverter = object : IConverter<CustomName> {
            override fun serialize(payload: Payload<CustomName>): ByteArray {
                return payload.content.name.toByteArray()
            }

            override fun deserialize(type: ContentType, bytes: ByteArray): Payload<CustomName> {
                return CustomNamePayload(type, CustomName(bytes.decodeToString()))
            }
        }
    }

    private data class CustomName(val name: String)
    private data class CustomNamePayload(
        override val type: ContentType,
        override val content: CustomName
    ) : Payload<CustomName>

}