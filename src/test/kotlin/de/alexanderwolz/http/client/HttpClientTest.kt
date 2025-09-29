package de.alexanderwolz.http.client

import de.alexanderwolz.http.client.model.ContentType
import de.alexanderwolz.http.client.model.Method
import org.junit.jupiter.api.Test
import java.net.URI

class HttpClientTest {

    @Test
    fun testSimpleGet() {
        val httpClient = HttpClient.Builder()
            .method(Method.GET)
            .endpoint(URI.create("https://api.predic8.de/shop/v2/products"))
            .headers("Accept" to setOf(ContentType.JSON.mediaType))
            .build()

        val response = httpClient.execute()
        if (response.isOK && response.body != null && response.body.type == ContentType.JSON) {
            println("YAY!")
            println(response.body)
        } else {
            println("OH NO!")
            println(response)
        }
    }

    @Test
    fun testSimplePost() {

        val payload = "{\"name\":\"Dauerlutscher\",\"price\":1.99}"

        val httpClient = HttpClient.Builder()
            .method(Method.POST)
            .endpoint(URI.create("https://api.predic8.de/shop/v2/products"))
            .headers("Accept" to setOf(ContentType.JSON.mediaType))
            .body(payload, ContentType.JSON)
            .build()
        val response = httpClient.execute()
        if (response.isOK && response.body != null && response.body.type == ContentType.JSON) {
            println("YAY!")
            println(response.body)
        } else {
            println("OH NO!")
            println(response)
        }
    }

}