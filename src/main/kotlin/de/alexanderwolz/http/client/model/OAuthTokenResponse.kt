package de.alexanderwolz.http.client.model

import com.google.gson.annotations.SerializedName

data class OAuthTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("id_token") val idToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type") val type: String,
    @SerializedName("expires_in") val expiresIn: Int, //seconds
    @SerializedName("scope") val scope: String?,
    @SerializedName("user_id") val userId: String?
)