package de.alexanderwolz.http.client

import com.google.gson.Gson
import de.alexanderwolz.http.client.model.token.OAuthTokenResponse
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JsonTest {

    @Test
    fun testTokenResponseParsing() {
        val tokenString = "{\"access_token\"=\"ey123\",\"token_type\"=\"bearer\",\"expires_in\"=\"3600\"}"
        val token = Gson().fromJson(tokenString, OAuthTokenResponse::class.java)
        assertEquals("ey123", token.accessToken)
        assertEquals("bearer", token.tokenType)
        assertEquals(3600, token.expiresInSeconds)
    }

}