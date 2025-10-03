package de.alexanderwolz.http.client

import de.alexanderwolz.http.client.model.token.OAuthTokenResponse
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JsonTest {

    @Test
    fun testTokenResponseParsing() {
        val tokenString = "{\"access_token\":\"ey123\",\"token_type\":\"bearer\",\"expires_in\":\"3600\"}"
        val token = Json.decodeFromString<OAuthTokenResponse>(tokenString)
        assertEquals("ey123", token.accessToken)
        assertEquals("bearer", token.tokenType)
        assertEquals(3600, token.expiresInSeconds)
    }

}