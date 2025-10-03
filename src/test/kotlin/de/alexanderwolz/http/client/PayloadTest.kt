package de.alexanderwolz.http.client

import de.alexanderwolz.http.client.model.CustomContentResolver
import de.alexanderwolz.http.client.model.CustomContentTypes
import de.alexanderwolz.http.client.model.Product
import de.alexanderwolz.http.client.model.WrappedProduct
import de.alexanderwolz.http.client.model.payload.Payload
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PayloadTest {

    @Test
    fun testCustomPayload() {
        val product = Product("5", "Boxer Shorts")
        val payload = Payload.create(CustomContentTypes.PRODUCT, product, CustomContentResolver())
        assertNotNull(payload)
        assertEquals(Product::class as KClass<*>, payload.element::class as KClass<*>)
        assertEquals(product, payload.element)
    }

    //INFO: if parent class is specified, the payload shall be based on the parent class, even though we add a child element

    @Test
    fun testCustomWrappedPayloadWithParentElement() {
        val product = Product("8", "Bananas")
        val wrappedProduct = WrappedProduct(product)
        val payload = Payload.create(CustomContentTypes.WRAPPED_PRODUCT, wrappedProduct, CustomContentResolver())
        assertNotNull(payload)
        assertEquals(WrappedProduct::class as KClass<*>, payload.element::class as KClass<*>)
    }

    @Test
    fun testCustomWrappedPayloadWithChildElement() {
        val product = Product("8", "Bananas")
        val payload = Payload.create(CustomContentTypes.WRAPPED_PRODUCT, product, CustomContentResolver())
        assertNotNull(payload)
        assertEquals(WrappedProduct::class as KClass<*>, payload.element::class as KClass<*>)
    }

    @Test
    fun testPayloadByBinaryWithWrappingClass() {

        val product = Product("8", "Bananas")
        val productJson = Json.encodeToString(product)
        val wrappedProduct = WrappedProduct(product)
        val wrappedProductJson = Json.encodeToString(wrappedProduct)


        //TEST PARENT
        val payload = Payload.create(CustomContentTypes.WRAPPED_PRODUCT, wrappedProduct, CustomContentResolver())
        assertNotNull(payload)
        assertEquals(CustomContentTypes.WRAPPED_PRODUCT, payload.type)
        assertEquals(wrappedProduct, payload.element)

        //TEST PARENT BINARY
        val payload2 = Payload.create(
            CustomContentTypes.WRAPPED_PRODUCT,
            wrappedProductJson.toByteArray(),
            CustomContentResolver()
        )
        assertNotNull(payload2)
        assertEquals(CustomContentTypes.WRAPPED_PRODUCT, payload2.type)
        assertEquals(wrappedProduct, payload2.element)

        //TEST CHILD
        val payload3 = Payload.create(CustomContentTypes.WRAPPED_PRODUCT, product, CustomContentResolver())
        assertNotNull(payload3)
        assertEquals(CustomContentTypes.WRAPPED_PRODUCT, payload3.type)
        assertEquals(wrappedProduct as Any, payload3.element as Any)

        //TEST CHILD BINARY
        val payload4 = Payload.create(
            CustomContentTypes.WRAPPED_PRODUCT,
            productJson.toByteArray(),
            CustomContentResolver()
        )
        assertNotNull(payload4)
        assertEquals(CustomContentTypes.WRAPPED_PRODUCT, payload4.type)
        assertEquals(wrappedProduct as Any, payload4.element)
    }

}