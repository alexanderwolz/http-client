package de.alexanderwolz.http.client.model

import kotlinx.serialization.Serializable

@Serializable
data class WrappedProduct(val element: Product)