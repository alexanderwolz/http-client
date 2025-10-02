package de.alexanderwolz.http.client

object Constants {
    const val MEDIA_TYPE_PRODUCT = "application/product"
    const val CONTENT_PRODUCT_JSON = "{\"id\":\"1\",\"name\":\"apple\"}"
    const val CONTENT_WRAPPED_PRODUCT_JSON = "{\"element\":${CONTENT_PRODUCT_JSON}}"
}