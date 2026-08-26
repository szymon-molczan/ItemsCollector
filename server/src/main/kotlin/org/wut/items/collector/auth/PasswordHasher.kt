package org.wut.items.collector.auth

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import java.util.Base64




object PasswordHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private val rng = SecureRandom()

    fun newSalt(): String {
        val bytes = ByteArray(16)
        rng.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hash(password: String, saltB64: String): String {
        val salt = Base64.getDecoder().decode(saltB64)
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val hash = factory.generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(hash)
    }

    fun verify(password: String, saltB64: String, expectedHashB64: String): Boolean {
        val actual = hash(password, saltB64)
        
        if (actual.length != expectedHashB64.length) return false
        var diff = 0
        for (i in actual.indices) diff = diff or (actual[i].code xor expectedHashB64[i].code)
        return diff == 0
    }
}
