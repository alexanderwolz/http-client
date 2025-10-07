package de.alexanderwolz.http.client

import de.alexanderwolz.http.client.model.CustomContentResolver
import de.alexanderwolz.http.client.model.CustomContentTypes
import de.alexanderwolz.http.client.model.Product
import de.alexanderwolz.http.client.model.ProductContainer
import de.alexanderwolz.http.client.model.payload.Payload
import de.alexanderwolz.http.client.model.payload.WrappedPayload
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertIsNot
import kotlin.test.assertNotNull

class PayloadTest {

    @Test
    fun testCustomPayloadProduct() {
        val product = Product("5", "Boxer Shorts")
        val productJson = Json.encodeToString(product)
        val productBin = productJson.toByteArray()

        val payloadElement = Payload.create(CustomContentTypes.PRODUCT, product, CustomContentResolver())
        assertNotNull(payloadElement)
        assertIsNot<WrappedPayload<*, *>>(payloadElement)
        assertEquals(Product::class as KClass<*>, payloadElement.element::class)
        assertEquals(product, payloadElement.element)

        val payloadBinary = Payload.create(CustomContentTypes.PRODUCT, productBin, CustomContentResolver())
        assertNotNull(payloadBinary)
        assertIsNot<WrappedPayload<*, *>>(payloadElement)
        assertEquals(Product::class as KClass<*>, payloadBinary.element::class)
        assertEquals(product, payloadBinary.element)
    }

    @Test
    fun testCustomPayloadProductContainer() {
        val product = Product("5", "Boxer Shorts")
        val container = ProductContainer(product)
        val containerJson = Json.encodeToString(container)
        val containerBin = containerJson.toByteArray()

        val payloadElement = Payload.create(CustomContentTypes.PRODUCT_CONTAINER, container, CustomContentResolver())
        assertNotNull(payloadElement)
        assertIsNot<WrappedPayload<*, *>>(payloadElement)
        assertEquals(ProductContainer::class as KClass<*>, payloadElement.element::class)
        assertEquals(container, payloadElement.element)

        val payloadBinary = Payload.create(CustomContentTypes.PRODUCT_CONTAINER, containerBin, CustomContentResolver())
        assertIsNot<WrappedPayload<*, *>>(payloadElement)
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
        assertIs<WrappedPayload<*, *>>(payloadElement)
        assertEquals(CustomContentTypes.WRAPPED_PRODUCT, payloadElement.type)
        assertEquals(product as Any, payloadElement.element)
        assertEquals(wrappedProduct as Any, payloadElement.parent)
        assertEquals(productJson, payloadElement.bytes.decodeToString())
        assertEquals(wrappedProductJson, payloadElement.parentBytes.decodeToString())

        //TEST PARENT BINARY
        val payloadBinary = Payload.create(
            CustomContentTypes.WRAPPED_PRODUCT, wrappedProductJson.toByteArray(), CustomContentResolver()
        )
        assertNotNull(payloadBinary)
        assertIs<WrappedPayload<*, *>>(payloadBinary)
        assertEquals(CustomContentTypes.WRAPPED_PRODUCT, payloadBinary.type)
        assertEquals(product as Any, payloadBinary.element)
        assertEquals(wrappedProduct as Any, payloadBinary.parent)
        assertEquals(productJson, payloadBinary.bytes.decodeToString())
        assertEquals(wrappedProductJson, payloadBinary.parentBytes.decodeToString())

        //TEST CHILD
        val payloadElementChild = Payload.create(CustomContentTypes.WRAPPED_PRODUCT, product, CustomContentResolver())
        assertNotNull(payloadElementChild)
        assertIs<WrappedPayload<*, *>>(payloadElementChild)
        assertEquals(CustomContentTypes.WRAPPED_PRODUCT, payloadElementChild.type)
        assertEquals(product as Any, payloadElementChild.element)
        assertEquals(wrappedProduct as Any, payloadElementChild.parent)
        assertEquals(productJson, payloadElementChild.bytes.decodeToString())
        assertEquals(wrappedProductJson, payloadElementChild.parentBytes.decodeToString())

        //TEST CHILD BINARY
        val payloadBinaryChild = Payload.create(
            CustomContentTypes.WRAPPED_PRODUCT, productJson.toByteArray(), CustomContentResolver()
        )
        assertNotNull(payloadBinaryChild)
        assertIs<WrappedPayload<*, *>>(payloadBinaryChild)
        assertEquals(CustomContentTypes.WRAPPED_PRODUCT, payloadBinaryChild.type)
        assertEquals(product as Any, payloadBinaryChild.element)
        assertEquals(wrappedProduct as Any, payloadBinaryChild.parent)
        assertEquals(productJson, payloadBinaryChild.bytes.decodeToString())
        assertEquals(wrappedProductJson, payloadBinaryChild.parentBytes.decodeToString())
    }

}