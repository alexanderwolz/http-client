package de.alexanderwolz.http.client

import de.alexanderwolz.http.client.model.CustomContentResolver
import de.alexanderwolz.http.client.model.CustomContentTypes
import de.alexanderwolz.http.client.model.Product
import de.alexanderwolz.http.client.model.WrappedProduct
import de.alexanderwolz.http.client.model.payload.Payload
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PayloadTest {

    @Test
    fun testCustomPayload() {
        val product = Product("5", "Boxer Shorts")
        val payload = Payload.create(CustomContentTypes.WRAPPED_PRODUCT, product, CustomContentResolver())
        assertNotNull(payload)
        assertEquals(product, payload.element)
    }

    @Test
    fun testCustomWrappedPayload() {
        val product = Product("8", "Bananas")
        val wrappedProduct = WrappedProduct(product)
        val payload = Payload.create(CustomContentTypes.WRAPPED_PRODUCT, wrappedProduct, CustomContentResolver())
        assertNotNull(payload)
        //assertEquals<Product>(product, payload.element)
        //TODO
    }

}