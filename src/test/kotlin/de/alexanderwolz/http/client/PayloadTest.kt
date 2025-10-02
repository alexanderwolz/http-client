package de.alexanderwolz.http.client

import de.alexanderwolz.http.client.model.CustomContentTypes
import de.alexanderwolz.http.client.model.Product
import de.alexanderwolz.http.client.model.payload.Payload
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PayloadTest {

    @Test
    fun testSimplePayload() {
        val product = Product("5","Boxer Shorts")
        val payload = Payload.create(CustomContentTypes.WRAPPED_PRODUCT, product)
        assertNotNull(payload)
        assertEquals(product, payload.element)
    }

    @Test
    fun testWrappedPayload() {
        val product = Product("5","Boxer Shorts")
        val payload = Payload.create(CustomContentTypes.WRAPPED_PRODUCT, product)
        assertNotNull(payload)
        assertEquals(product, payload.element)
    }

}