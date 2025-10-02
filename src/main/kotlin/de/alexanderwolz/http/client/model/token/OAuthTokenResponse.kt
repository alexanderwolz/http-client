package de.alexanderwolz.http.client.model.token

import com.google.gson.annotations.SerializedName

data class OAuthTokenResponse(
    @SerializedName("token_type") val tokenType: String,

    @SerializedName("id_token") val idToken: String?,
    @SerializedName("user_id") val userId: String?,

    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("expires_in") val expiresInSeconds: Int,

    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("refresh_expires_in") val refreshExpiresInSeconds: Int? = null,

    @SerializedName("scope") val scope: String?
) {
    fun toAccessToken(): AccessToken {
        return accessToken?.let {
            AccessToken(it, tokenType, expiresInSeconds, scope)
        } ?: throw NoSuchElementException("Token response does not contain an encoded access token")
    }
}