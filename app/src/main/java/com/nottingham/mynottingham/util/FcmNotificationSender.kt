package com.nottingham.mynottingham.util

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec

/**
 * FCM Notification Sender using V1 API
 *
 * Sends FCM notifications using the modern HTTP v1 API with OAuth 2.0 authentication.
 * Uses service account credentials for server-to-server authentication.
 */
object FcmNotificationSender {

    private const val TAG = "FcmNotificationSender"

    // Project ID from service account
    private const val PROJECT_ID = "mynottingham-b02b7"

    // FCM V1 API endpoint
    private const val FCM_V1_URL = "https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send"

    // OAuth token endpoint
    private const val TOKEN_URL = "https://oauth2.googleapis.com/token"

    // Service account credentials
    private const val CLIENT_EMAIL = "firebase-adminsdk-fbsvc@mynottingham-b02b7.iam.gserviceaccount.com"

    // Private key (from serviceAccountKey.json)
    private const val PRIVATE_KEY = """-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCxOb0xXZVQ6Xnz
K/9h/4GZi3FiJRov9cGYiBgb4GnOIHjlIjBXRFP6qS3JnQxUBa7R1QBAfySYijAO
wfZoolkRTrMdr40sYuKJF0jMaQYw07HrLb9w2y/21anCxUx73U0GINi6zxV+ErYG
clHknMg2+0ppBE+RlzKv8+nluAm6J/zMJxUs5v1Ad5qtJ8QE0knIsJ90OvsF1Bq5
M1eHsidDiaT0ytGdSQdk7/xV86PdWa5AaWuQ1iCWN4C6/mW/WwOWuvHnPOOpZ1tM
sTX+jZUKYzjZCo9Oz1fKJyYoign+/wnm4ZjzJh1mkHgHX3Ohv1EMHvhf090Qcc8I
vXxruxp7AgMBAAECggEABWFsSFV/64Y/6nouLwXIijNZr8ufLGnIxP5yXbvl9z9o
Ip8mzZEO/z/n2nej2kcwsNrHi629T2SrcD9DFMrDa0sy4Ym+mDFJkTTwql53G1nJ
/7347A6GucOje3zTxVNWLwOMFCI/+kuYiCb4+Ov21I7mvVWt3r//s3TZzilUuxIs
jF5IvueIMBilhhKuWc0kmHSmNkcbB9PKbgfMhdT2mTp+QgHvap9ms0KRleZSygOT
y7GgUSTjAgdwi1xaEizcWPouEJiMtrI26Ke/dXW2bF8eZ5np6Cp3NfUglXBuW1F7
2zRCgy//WKGngR7fC+UdOLpBj0st2dBFXlafO51faQKBgQDmhs8MhTads1TpFoT9
PM2OFI0TtDvmAKhAjiE3VtsAzJnHVtAzJf3uHKB7q3YDkV/nmZWfzD5cfGwJmExZ
RpdGeONRlu2O69bIeJUzpkHnHV4eaVlo4wcPCxp876p1N1NVkIFW/29//7Hp0OJA
AVmBWGR15dzimM+l2WUtkl7AyQKBgQDEzyMqHyRm9WmBzPTiyx68gGXZkxP50RK0
Rj2bWwSQce7dJ2Dw+EnIkIUfkYDYGxaVEZ1OU2HS7vuMzWE7TStszOw+1EOvkgWN
jqm0F2BLFtsInSv/AF2PwJASW1B++mj4bV2upyhmVJywQpy6bkNQk/o85vMqDDXQ
/5YsNVdHIwKBgQDhVbL73Rc27X25Xc7fLtkK4eHI+et29vuAJq4nRtpKHLTQmnZn
GOLvJsJkQITaFfc2DvWnvuDSTLjaZCl1NsBWHYKuVSafBr7rNJs7Ym+W9yjx9y5z
jT9wH/1jVG49p7fospkLLiKSbqE9GXae8/LGsV15tRfF03Nd/XOeKS4/qQKBgFKp
RXW9msZ7sRLJiNlwwrodm6mksrEsdRSuo9WKhwI8OD0++uJ7BlJtENzPejRKPFPk
EBQDxYSYx6K96GbF5MVP1LIW6U7mn9py3yg64UDompqlmQMDnkhwcpKjM84BUvF0
zfI7VyaTxzPo5ncPNMq1PFc0EVHJyxi7INH0nXHNAoGASPKkDZDJlR2SozR5Hb2j
3YQyWWYN1LuQuap7fHaPop6VSUsdELU8pSMKKLZEq6QtEsd2g/5rXdKJgsxoSRwZ
MTtZyc1AzF6xmuhjCN4ZRYzs8B4U9M3Dh0TGGIu5bqJKera/czrN1R/ohaKgfV6C
3I9eiRBbntOGgjRrXL1HOc4=
-----END PRIVATE KEY-----"""

    // Cached access token
    private var cachedAccessToken: String? = null
    private var tokenExpiryTime: Long = 0
    private val tokenMutex = Mutex()

    /**
     * Send a chat notification to a specific device using FCM V1 API
     */
    suspend fun sendChatNotification(
        recipientToken: String,
        senderName: String,
        messageContent: String,
        conversationId: String,
        senderId: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Get valid access token
            val accessToken = getAccessToken() ?: run {
                Log.e(TAG, "Failed to get access token")
                return@withContext false
            }

            val url = URL(FCM_V1_URL)
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $accessToken")
                doOutput = true
            }

            // Build V1 API message payload
            val payload = JSONObject().apply {
                put("message", JSONObject().apply {
                    put("token", recipientToken)

                    // Data payload for app handling
                    put("data", JSONObject().apply {
                        put("type", "message")
                        put("conversationId", conversationId)
                        put("senderId", senderId)
                        put("senderName", senderName)
                        put("body", messageContent)
                    })

                    // Android specific config
                    put("android", JSONObject().apply {
                        put("priority", "high")
                    })
                })
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "V1 API: Notification sent successfully")
                true
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.readText()
                Log.e(TAG, "V1 API: Failed. Code: $responseCode, Error: $errorStream")

                // If token expired, clear cache and retry once
                if (responseCode == 401) {
                    clearTokenCache()
                }
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification: ${e.message}", e)
            false
        }
    }

    /**
     * Get a valid OAuth 2.0 access token
     */
    private suspend fun getAccessToken(): String? = tokenMutex.withLock {
        // Check if cached token is still valid (with 5 min buffer)
        val now = System.currentTimeMillis()
        if (cachedAccessToken != null && now < tokenExpiryTime - 300000) {
            return cachedAccessToken
        }

        // Generate new token
        return try {
            val jwt = createJwt()
            val token = exchangeJwtForAccessToken(jwt)

            if (token != null) {
                cachedAccessToken = token
                tokenExpiryTime = now + 3600000 // Token valid for 1 hour
                Log.d(TAG, "New access token obtained")
            }

            token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get access token: ${e.message}", e)
            null
        }
    }

    /**
     * Create a signed JWT for OAuth authentication
     */
    private fun createJwt(): String {
        val now = System.currentTimeMillis() / 1000
        val exp = now + 3600 // 1 hour

        // JWT Header
        val header = JSONObject().apply {
            put("alg", "RS256")
            put("typ", "JWT")
        }

        // JWT Payload
        val payload = JSONObject().apply {
            put("iss", CLIENT_EMAIL)
            put("sub", CLIENT_EMAIL)
            put("aud", TOKEN_URL)
            put("iat", now)
            put("exp", exp)
            put("scope", "https://www.googleapis.com/auth/firebase.messaging")
        }

        // Encode header and payload
        val headerEncoded = base64UrlEncode(header.toString().toByteArray())
        val payloadEncoded = base64UrlEncode(payload.toString().toByteArray())
        val signatureInput = "$headerEncoded.$payloadEncoded"

        // Sign with private key
        val signature = signWithPrivateKey(signatureInput)
        val signatureEncoded = base64UrlEncode(signature)

        return "$headerEncoded.$payloadEncoded.$signatureEncoded"
    }

    /**
     * Sign data with the private key using RS256
     */
    private fun signWithPrivateKey(data: String): ByteArray {
        // Parse PEM private key
        val keyContent = PRIVATE_KEY
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .trim()

        val keyBytes = Base64.decode(keyContent, Base64.DEFAULT)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        val privateKey = keyFactory.generatePrivate(keySpec)

        // Sign with SHA256withRSA
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(data.toByteArray())
        return signature.sign()
    }

    /**
     * Exchange JWT for OAuth access token
     */
    private fun exchangeJwtForAccessToken(jwt: String): String? {
        val url = URL(TOKEN_URL)
        val connection = url.openConnection() as HttpURLConnection

        return try {
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                doOutput = true
            }

            val postData = "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=$jwt"

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(postData)
                writer.flush()
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonResponse = JSONObject(response)
                jsonResponse.getString("access_token")
            } else {
                val error = connection.errorStream?.bufferedReader()?.readText()
                Log.e(TAG, "Token exchange failed: $error")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token exchange error: ${e.message}", e)
            null
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Base64 URL-safe encoding (no padding)
     */
    private fun base64UrlEncode(data: ByteArray): String {
        return Base64.encodeToString(data, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    /**
     * Clear the token cache (call when token is invalid)
     */
    private fun clearTokenCache() {
        cachedAccessToken = null
        tokenExpiryTime = 0
    }
}
