package de.alexanderwolz.http.client.model.token

data class AccessToken(
    val encodedJWT: String,
    val type: String,
    val expiresInSeconds: Int, //seconds
    val issuedScope: String?
) {
    val expiration = System.currentTimeMillis() + ((expiresInSeconds - 20) * 1000)

    fun isExpired(): Boolean {
        return System.currentTimeMillis() >= expiration
    }
}