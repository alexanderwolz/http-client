package de.alexanderwolz.http.client.model.token

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OAuthTokenResponse(
    @SerialName("token_type") val tokenType: String,

    @SerialName("id_token") val idToken: String?,
    @SerialName("user_id") val userId: String?,

    @SerialName("access_token") val accessToken: String?,
    @SerialName("expires_in") val expiresInSeconds: Int,

    @SerialName("refresh_token") val refreshToken: String?,
    @SerialName("refresh_expires_in") val refreshExpiresInSeconds: Int? = null,

    @SerialName("scope") val scope: String?
) {
    fun toAccessToken(): AccessToken {
        return accessToken?.let {
            AccessToken(it, tokenType, expiresInSeconds, scope)
        } ?: throw NoSuchElementException("Token response does not contain an encoded access token")
    }
}