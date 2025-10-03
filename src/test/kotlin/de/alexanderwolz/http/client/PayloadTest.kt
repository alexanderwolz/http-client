package de.alexanderwolz.http.client

import de.alexanderwolz.http.client.model.CustomContentResolver
import de.alexanderwolz.http.client.model.CustomContentTypes
import de.alexanderwolz.http.client.model.Product
import de.alexanderwolz.http.client.model.ProductContainer
import de.alexanderwolz.http.client.model.payload.Payload
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PayloadTest {

    @Test
    fun testCustomPayloadProduct() {
        val product = Product("5", "Boxer Shorts")
        val productJson = Json.encodeToString(product)

        val payloadElement = Payload.create(CustomContentTypes.PRODUCT, product, CustomContentResolver())
        assertNotNull(payloadElement)
        assertEquals(Product::class as KClass<*>, payloadElement.element::class)
        assertEquals(product, payloadElement.element)

        val payloadBinary =
            Payload.create(CustomContentTypes.PRODUCT, productJson.toByteArray(), CustomContentResolver())
        assertNotNull(payloadBinary)
        assertEquals(Product::class as KClass<*>, payloadBinary.element::class)
        assertEquals(product, payloadBinary.element)
    }

    @Test
    fun testCustomPayloadProductContainer() {
        val product = Product("5", "Boxer Shorts")
        val container = ProductContainer(product)
        val containerJson = Json.encodeToString(container)

        val payloadElement = Payload.create(CustomContentTypes.PRODUCT_CONTAINER, container, CustomContentResolver())
        assertNotNull(payloadElement)
        assertEquals(ProductContainer::class as KClass<*>, payloadElement.element::class)
        assertEquals(container, payloadElement.element)

        val payloadBinary =
            Payload.create(CustomContentTypes.PRODUCT_CONTAINER, containerJson.toByteArray(), CustomContentResolver())
        assertNotNull(payloadBinary)
        assertEquals(ProductContainer::class as KClass<*>, payloadBinary.element::class)
        assertEquals(container, payloadBinary.element)
    }

    @Test
    fun testCustomPayloadWrapperClass() {

        val product = Product("8", "Bananas")
        val productJson = Json.encodeToString(product)
        val wrappedProduct = ProductContainer(product)
        val wrappedProductJson = Json.encodeToString(wrappedProduct)


        //TEST PARENT
        val payloadElement = Payload.create(CustomContentTypes.WRAPPED_PRODUCT, wrappedProduct, CustomContentResolver())
        assertNotNull(payloadElement)
        assertEquals(CustomContentTypes.WRAPPED_PRODUCT, payloadElement.type)
        assertEquals(product, payloadElement.element)
        assertEquals(wrappedProductJson, payloadElement.bytes.decodeToString())

        //TEST PARENT BINARY
        val payloadBinary = Payload.create(
            CustomContentTypes.WRAPPED_PRODUCT,
            wrappedProductJson.toByteArray(),
            CustomContentResolver()
        )
        assertNotNull(payloadBinary)
        assertEquals(CustomContentTypes.WRAPPED_PRODUCT, payloadBinary.type)
        assertEquals(product, payloadBinary.element)
        assertEquals(wrappedProductJson, payloadBinary.bytes.decodeToString())

        //TEST CHILD
        val payloadElementChild = Payload.create(CustomContentTypes.WRAPPED_PRODUCT, product, CustomContentResolver())
        assertNotNull(payloadElementChild)
        assertEquals(CustomContentTypes.WRAPPED_PRODUCT, payloadElementChild.type)
        assertEquals(product as Any, payloadElementChild.element)
        assertEquals(wrappedProductJson, payloadElementChild.bytes.decodeToString())

        //TEST CHILD BINARY
        val payloadBinaryChild = Payload.create(
            CustomContentTypes.WRAPPED_PRODUCT,
            productJson.toByteArray(),
            CustomContentResolver()
        )
        assertNotNull(payloadBinaryChild)
        assertEquals(CustomContentTypes.WRAPPED_PRODUCT, payloadBinaryChild.type)
        assertEquals(product as Any, payloadBinaryChild.element)
        assertEquals(wrappedProductJson, payloadBinaryChild.bytes.decodeToString())
    }

}