package de.alexanderwolz.http.client

import de.alexanderwolz.http.client.model.token.OAuthErrorResponse
import de.alexanderwolz.http.client.model.token.OAuthTokenResponse
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsonTest {

    @Test
    fun testOauthTokenResponseParsing() {
        val tokenString = "{\"access_token\":\"ey123\",\"token_type\":\"bearer\",\"expires_in\":\"3600\"}"
        val token = Json.decodeFromString<OAuthTokenResponse>(tokenString)
        assertEquals("ey123", token.accessToken)
        assertEquals("bearer", token.tokenType)
        assertEquals(3600, token.expiresInSeconds)
        assertNull(token.idToken)
        assertNull(token.userId)
        assertNull(token.refreshToken)
        assertNull(token.refreshExpiresInSeconds)
        assertNull(token.scope)
    }

    @Test
    fun testOauthErrorResponseParsing() {
        val tokenString = "{\"error\":\"Some Error\",\"error_description\":\"Some Description\"}"
        val error = Json.decodeFromString<OAuthErrorResponse>(tokenString)
        assertEquals("Some Error", error.error)
        assertEquals("Some Description", error.description)
        assertNull(error.message)
        assertNull(error.uri)
        assertNull(error.statusCode)
    }

}