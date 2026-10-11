package com.example.albionmarketv2

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.crypto.Mac
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec

object CryptoSecurityUtils {

    private const val AES_KEY = "AlbionDataPro#Key2026_SecretPass!" // 32 bytes key
    private val AES_IV = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)

    fun setupPermissiveSSLAndHostnameVerifier() {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
            })

            val sc = SSLContext.getInstance("SSL")
            sc.init(null, trustAllCerts, SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun encryptAES(text: String): String {
        return try {
            val keyBytes = AES_KEY.toByteArray(Charsets.UTF_8).copyOf(32)
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(AES_IV))
            val encrypted = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
            bytesToHex(encrypted)
        } catch (_: Exception) {
            text
        }
    }

    fun decryptAES(encryptedText: String): String {
        return try {
            val keyBytes = AES_KEY.toByteArray(Charsets.UTF_8).copyOf(32)
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(AES_IV))
            val decoded = hexToBytes(encryptedText)
            String(cipher.doFinal(decoded), Charsets.UTF_8)
        } catch (_: Exception) {
            encryptedText
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hexToBytes(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            result[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return result
    }

    fun computeHmacSha256(payload: String): String {
        return try {
            val secret = "AlbionDataProHmacSecretKey2026"
            val hmac = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
            hmac.init(secretKey)
            val hash = hmac.doFinal(payload.toByteArray(Charsets.UTF_8))
            bytesToHex(hash)
        } catch (_: Exception) {
            ""
        }
    }

    fun verifyServerSignature(response: String, signature: String): Boolean {
        if (signature.isBlank()) return true
        val computed = computeHmacSha256(response)
        return computed.equals(signature, ignoreCase = true)
    }
}
