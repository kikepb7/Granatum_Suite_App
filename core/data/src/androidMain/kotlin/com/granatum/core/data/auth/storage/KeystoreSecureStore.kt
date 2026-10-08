package com.granatum.core.data.auth.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.granatum.core.domain.logger.AppLogger
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "granatum.session"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_TAG_BITS = 128
private const val IV_BYTES = 12

/**
 * Encrypts with a key the app never sees — it is generated inside the Android Keystore and
 * only ever used by reference — and keeps the ciphertext in the DataStore that
 * `platformCoreDataModule` already wires up.
 *
 * Deliberately does NOT use `androidx.security:security-crypto`. That library has been
 * deprecated since 1.1.0-beta01 (June 2025), with every API marked in favour of using the
 * Keystore directly, so adopting it now would mean taking on a dead dependency. Reusing
 * DataStore also means this feature adds no dependency at all.
 *
 * Every failure is logged and swallowed: a device whose Keystore refuses to cooperate should
 * behave as if there were no session, not crash on launch.
 */
class KeystoreSecureStore(
    private val dataStore: DataStore<Preferences>,
    private val logger: AppLogger
) : SecureStore {

    override suspend fun read(key: String): String? {
        val stored = runCatching { dataStore.data.first()[stringPreferencesKey(key)] }
            .getOrElse { throwable ->
                logger.warn("Could not read the session from secure storage: ${throwable.describe()}")
                return null
            } ?: return null

        return runCatching { decrypt(stored) }.getOrElse { throwable ->
            // Ciphertext this key cannot open will never open: it is corrupt, or the Keystore
            // key was invalidated. Discard it instead of failing on every launch.
            logger.warn("Stored session could not be decrypted; discarding it: ${throwable.describe()}")
            delete(key)
            null
        }
    }

    private fun Throwable.describe(): String = message?.let { "${this::class.simpleName}: $it" }
        ?: this::class.simpleName.orEmpty()

    override suspend fun write(key: String, value: String) {
        runCatching {
            val encrypted = encrypt(value)
            dataStore.edit { it[stringPreferencesKey(key)] = encrypted }
        }.onFailure { throwable ->
            logger.error("Could not write the session to secure storage", throwable)
        }
    }

    override suspend fun delete(key: String) {
        runCatching {
            dataStore.edit { it.remove(stringPreferencesKey(key)) }
        }.onFailure { throwable ->
            logger.error("Could not delete the session from secure storage", throwable)
        }
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val cipherText = cipher.doFinal(plainText.encodeToByteArray())
        // The IV is generated per encryption and is not secret, so it travels with the
        // ciphertext. Reusing one with GCM would be the actual danger.
        return Base64.getEncoder().encodeToString(cipher.iv + cipherText)
    }

    private fun decrypt(encoded: String): String {
        val bytes = Base64.getDecoder().decode(encoded)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(GCM_TAG_BITS, bytes, 0, IV_BYTES)
        )
        return cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES).decodeToString()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
        }.generateKey()
    }
}
