package de.alexanderwolz.http.client.model

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import de.alexanderwolz.http.client.model.token.OAuthTokenResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

class JwtHelper {

    companion object {
        const val TEST_SUBJECT = "SUBJECT-XYZ"
        const val TEST_ISSUER = "ISSUER-XYZ"
        const val TEST_SECRET = "unit-test-secret"
        const val TEST_SCOPE = "unit-test-scope"
    }

    fun createEncodedJWT(
        subject: String, issuer: String, claims: Map<String, String>, expiresInSeconds: Int, scope: String
    ): String {
        val algorithm = Algorithm.HMAC256(TEST_SECRET)
        val expiresAt = Date(System.currentTimeMillis() + expiresInSeconds * 1000)

        var builder =
            JWT.create().withSubject(subject).withIssuer(issuer).withClaim("scope", "scope").withIssuedAt(Date())
                .withExpiresAt(expiresAt)

        claims.forEach { (key, value) ->
            builder = builder.withClaim(key, value)
        }

        return builder.sign(algorithm)
    }

    fun createOauthResponse(
        subject: String, issuer: String, claims: Map<String, String>, expiresInSeconds: Int, scope: String
    ): String {
        val jwt = createEncodedJWT(subject, issuer, claims, expiresInSeconds, scope)
        val tokenResponse = OAuthTokenResponse("Bearer", null, null, jwt, expiresInSeconds, null, null, scope)
        return Json.encodeToString(tokenResponse)
    }
}