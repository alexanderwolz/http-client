package de.alexanderwolz.http.client

import de.alexanderwolz.http.client.model.ContentType
import de.alexanderwolz.http.client.model.ContentTypeRegistry
import de.alexanderwolz.http.client.model.Method
import de.alexanderwolz.http.client.model.payload.Converter
import de.alexanderwolz.http.client.model.payload.Payload
import org.junit.jupiter.api.Test
import java.net.URI

class HttpClientTest {

    @Test
    fun testSimpleGet() {
        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(Method.GET)
            .endpoint(URI.create("https://api.predic8.de/shop/v2/products"))
            .headers("Accept" to setOf(ContentType.JSON.mediaType))
            .build()

        val response = httpClient.execute()
        println("Status: ${response.code}")
        if (response.isOK && response.body != null && response.body.type == ContentType.JSON) {
            println("YAY!")
            println(response.body.content)
        } else {
            println("OH NO!")
            println(response.body?.content)
        }
    }

    @Test
    fun testSimplePost() {

        val payload = "{\"name\":\"Dauerlutscher\",\"price\":1.99}"

        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(Method.POST)
            .endpoint(URI.create("https://api.predic8.de/shop/v2/products"))
            .accept(ContentType.JSON)
            .body(payload, ContentType.JSON)
            .build()
        val response = httpClient.execute()
        println("Status: ${response.code}")
        if (response.isOK && response.body != null && response.body.type == ContentType.JSON) {
            println("YAY!")
            println(response.body.content)
        } else {
            println("OH NO!")
            println(response.body?.content)
        }
    }

    @Test
    fun testCustomContentType() {

        //val serialized = bytes.decodeToString()
        //CustomPayload(type, Custom(serialized))


        val type = ContentTypeRegistry.register<Custom>(
            "application/custom_v1+xml",
            object : Converter<Custom> {
                override fun serialize(
                    type: ContentType<Custom>,
                    payload: Payload<*>
                ): ByteArray {
                    return (payload.content as Custom).name.toByteArray()
                }

                override fun deserialize(
                    type: ContentType<Custom>,
                    bytes: ByteArray
                ): Payload<Custom> {
                    val serialized = bytes.decodeToString()
                    return CustomPayload(type, Custom(serialized))
                }

            }
        )
        val custom = Custom("test")
        val payload = CustomPayload(type, custom)

        val httpClient = HttpClient.Builder()
            .userAgent(HttpClient::class.java.simpleName)
            .method(Method.POST)
            .endpoint(URI.create("https://api.predic8.de/shop/v2/products"))
            .accept(type)
            .body(payload)
            .build()
        val response = httpClient.execute()
        println("Status: ${response.code}")
        if (response.isOK && response.body != null && response.body.type == ContentType.JSON) {
            println("YAY!")
            println(response.body.content)
        } else {
            println("OH NO!")
            println(response.body?.content)
        }
    }

    private data class Custom(val name: String)
    private data class CustomPayload(
        override val type: ContentType<Custom>, override val content: Custom
    ) : Payload<Custom>

}