package de.alexanderwolz.http.client.model.token

import com.google.gson.annotations.SerializedName

data class OAuthErrorResponse(
    @SerializedName("error") val errorCode: String?,
    @SerializedName("error_description") val description: String?,
    @SerializedName("message") val message: String?
)