package de.alexanderwolz.http.client.util

import de.alexanderwolz.http.client.model.CustomContentTypes
import de.alexanderwolz.http.client.model.JwtHelper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

object MockUtils {
    const val MEDIA_TYPE_PRODUCT = "application/product"
    const val MEDIA_TYPE_PRODUCT_CONTAINER = "application/productContainer"
    const val MEDIA_TYPE_WRAPPED = "application/wrapped"
    const val CONTENT_PRODUCT_JSON = "{\"id\":\"1\",\"name\":\"apple\"}"
    const val CONTENT_PRODUCT_CONTAINER_JSON = "{\"element\":${CONTENT_PRODUCT_JSON}}"
    const val CONTENT_WRAPPED_JSON = CONTENT_PRODUCT_CONTAINER_JSON


    fun startJwtServer(server: MockWebServer): String {
        val oauthJson = JwtHelper().createOauthResponse(
            JwtHelper.Companion.TEST_SECRET,
            JwtHelper.Companion.TEST_ISSUER,
            emptyMap(),
            60,
            JwtHelper.Companion.TEST_SCOPE
        )
        val mockResponse = MockResponse().apply {
            setResponseCode(200)
            setBody(oauthJson)
            addHeader("Content-Type", "application/json")
        }
        server.enqueue(mockResponse)
        return oauthJson
    }

    fun startSimpleJsonServer(server: MockWebServer): String {

        val mockResponse = MockResponse().apply {
            setResponseCode(200)
            setBody(CONTENT_PRODUCT_JSON)
            addHeader("Content-Type", "application/json")
        }
        server.enqueue(mockResponse)
        return CONTENT_PRODUCT_JSON
    }

    fun startProductServer(server: MockWebServer): String {

        val mockResponse = MockResponse().apply {
            setResponseCode(200)
            setBody(CONTENT_PRODUCT_JSON)
            addHeader("Content-Type", CustomContentTypes.PRODUCT.mediaType)
        }
        server.enqueue(mockResponse)
        return CONTENT_PRODUCT_JSON
    }

    fun startProductContainerServer(server: MockWebServer): String {

        val mockResponse = MockResponse().apply {
            setResponseCode(200)
            setBody(CONTENT_PRODUCT_CONTAINER_JSON)
            addHeader("Content-Type", CustomContentTypes.PRODUCT.mediaType)
        }
        server.enqueue(mockResponse)
        return CONTENT_PRODUCT_CONTAINER_JSON
    }

    fun startWrappedProductServer(server: MockWebServer): String {
        val mockResponse = MockResponse().apply {
            setResponseCode(200)
            setBody(CONTENT_PRODUCT_CONTAINER_JSON)
            addHeader("Content-Type", CustomContentTypes.WRAPPED_PRODUCT.mediaType)
        }
        server.enqueue(mockResponse)
        return CONTENT_PRODUCT_CONTAINER_JSON
    }

}