package de.alexanderwolz.http.client.model.token

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class OAuthErrorResponse(

    //see OAuth 2.0 RFC 6749
    @SerialName("error") val error: String,
    @SerialName("error_description") val description: String? = null,
    @SerialName("error_uri") val uri: String? = null,

    //non-standard but useful:
    @SerialName("message") val message: String? = null,
    @SerialName("status_code") val statusCode: String? = null
)