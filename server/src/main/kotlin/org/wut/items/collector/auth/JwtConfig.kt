package org.wut.items.collector.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date





object JwtConfig {
    private const val SECRET = "items-collector-dev-secret-change-me-in-production"
    const val ISSUER = "items-collector-server"
    const val AUDIENCE = "items-collector-client"
    private const val TOKEN_VALIDITY_MS = 30L * 24 * 60 * 60 * 1000   

    private val algorithm: Algorithm = Algorithm.HMAC256(SECRET)

    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(ISSUER)
        .withAudience(AUDIENCE)
        .build()

    fun generate(userId: String, email: String): String =
        JWT.create()
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withClaim("uid", userId)
            .withClaim("email", email)
            .withExpiresAt(Date(System.currentTimeMillis() + TOKEN_VALIDITY_MS))
            .sign(algorithm)
}
