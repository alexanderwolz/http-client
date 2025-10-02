package de.alexanderwolz.http.client.model.token

import com.google.gson.annotations.SerializedName

data class OAuthErrorResponse(

    //see OAuth 2.0 RFC 6749
    @SerializedName("error") val error: String,
    @SerializedName("error_description") val description: String?,
    @SerializedName("error_uri") val uri: String?,

    //non-standard but useful:
    @SerializedName("message") val message: String?,
    @SerializedName("status_code") val statusCode: String?
)