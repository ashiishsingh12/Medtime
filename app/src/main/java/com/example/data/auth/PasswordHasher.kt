package com.example.data.auth

import android.util.Base64
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private const val ALG_SHA256 = "PBKDF2WithHmacSHA256"
    private const val ALG_SHA1 = "PBKDF2WithHmacSHA1"

    data class HashResult(
        val saltBase64: String,
        val hashWithAlg: String
    )

    fun hashPassword(password: String): HashResult {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        val (alg, derived) = deriveKey(password.toCharArray(), salt, preferredAlgorithm = null)
        return HashResult(
            saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP),
            hashWithAlg = "$alg:${Base64.encodeToString(derived, Base64.NO_WRAP)}"
        )
    }

    fun verifyPassword(password: String, saltBase64: String, storedHashWithAlg: String): Boolean {
        if (saltBase64.isBlank() || storedHashWithAlg.isBlank()) return false
        return try {
            val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
            val parts = storedHashWithAlg.split(":", limit = 2)
            if (parts.size != 2) return false
            val alg = parts[0]
            val expectedBytes = Base64.decode(parts[1], Base64.NO_WRAP)
            val (_, actualBytes) = deriveKey(password.toCharArray(), salt, preferredAlgorithm = alg)
            MessageDigest.isEqual(expectedBytes, actualBytes)
        } catch (_: Exception) {
            false
        }
    }

    private fun deriveKey(
        chars: CharArray,
        salt: ByteArray,
        preferredAlgorithm: String?
    ): Pair<String, ByteArray> {
        val spec = PBEKeySpec(chars, salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val alg = preferredAlgorithm ?: ALG_SHA256
            val factory = try {
                SecretKeyFactory.getInstance(alg)
            } catch (_: NoSuchAlgorithmException) {
                SecretKeyFactory.getInstance(ALG_SHA1)
            }
            val usedAlg = factory.algorithm
            usedAlg to factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
