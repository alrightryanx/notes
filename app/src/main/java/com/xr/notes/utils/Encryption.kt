package com.xr.notes.utils

// File: app/src/main/java/com/example/notesapp/utils/Encryption.kt

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory
import java.security.SecureRandom
import java.security.spec.AlgorithmParameterSpec

class Encryption {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }
    private val keyGenerator = KeyGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
    )
    private val defaultKeyAlias = "notes_encryption_key"

    init {
        createKeyIfNotExists()
    }

    private fun createKeyIfNotExists() {
        if (!keyStore.containsAlias(defaultKeyAlias)) {
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                defaultKeyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }

    fun encrypt(text: String): String {
        val secretKey = keyStore.getKey(defaultKeyAlias, null) as SecretKey
        return encrypt(text, secretKey)
    }

    fun decrypt(encryptedText: String): String {
        val secretKey = keyStore.getKey(defaultKeyAlias, null) as SecretKey
        return decrypt(encryptedText, secretKey)
    }

    // Password-based encryption for more secure notes
    fun encrypt(text: String, password: String): String {
        val salt = ByteArray(16).apply {
            SecureRandom().nextBytes(this)
        }

        val secretKey = deriveKeyFromPassword(password, salt)
        val encryptedData = encrypt(text, secretKey)

        // Combine salt and encrypted data
        val saltBase64 = Base64.encodeToString(salt, Base64.DEFAULT)
        return "$saltBase64:$encryptedData"
    }

    fun decrypt(encryptedText: String, password: String): String {
        val parts = encryptedText.split(":")
        if (parts.size != 2) throw IllegalArgumentException("Invalid encrypted text format")

        val salt = Base64.decode(parts[0], Base64.DEFAULT)
        val secretKey = deriveKeyFromPassword(password, salt)

        return decrypt(parts[1], secretKey)
    }

    private fun deriveKeyFromPassword(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val secretKey = factory.generateSecret(spec)
        return javax.crypto.spec.SecretKeySpec(secretKey.encoded, "AES")
    }

    private fun encrypt(text: String, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(text.toByteArray(Charsets.UTF_8))

        // Combine IV and encrypted data
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    private fun decrypt(encryptedText: String, secretKey: SecretKey): String {
        val encryptedData = Base64.decode(encryptedText, Base64.DEFAULT)

        // Extract IV (first 12 bytes for GCM)
        val iv = encryptedData.copyOfRange(0, 12)
        val encrypted = encryptedData.copyOfRange(12, encryptedData.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec: AlgorithmParameterSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val decryptedBytes = cipher.doFinal(encrypted)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}