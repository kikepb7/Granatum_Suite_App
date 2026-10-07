@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.granatum.core.data.auth.storage

import com.granatum.core.domain.logger.AppLogger
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

private const val SERVICE = "com.granatum.app.session"
private const val INSTALL_MARKER = "com.granatum.app.installMarker"

/**
 * Keychain-backed store.
 *
 * Accessibility is `AfterFirstUnlockThisDeviceOnly`: `AfterFirstUnlock` lets the clock-in
 * background sync (spec 4) read the token while the screen is locked, and `ThisDeviceOnly`
 * keeps the credential out of backups restored onto another device.
 *
 * **Keychain items survive app deletion on iOS.** Without [purgeIfReinstalled] below, whoever
 * reinstalls the app inherits the session of whoever used the device before — and no other
 * test catches it, because storing, reading and restarting all behave correctly.
 *
 * Every failure is logged and swallowed: a Keychain that refuses to cooperate must look like
 * an absent session, not like a crash.
 */
class KeychainSecureStore(
    private val logger: AppLogger
) : SecureStore {

    init {
        // Must not throw: Koin builds this during composition.
        runCatching { purgeIfReinstalled() }
    }

    override suspend fun read(key: String): String? = runCatching {
        readOrThrow(key)
    }.getOrElse { throwable ->
        logger.warn("Could not read the session from the Keychain: ${throwable.message}")
        null
    }

    private fun readOrThrow(key: String): String? = memScoped {
        val query = query(key).apply {
            CFDictionarySetValue(this, kSecReturnData, kCFBooleanTrue)
            CFDictionarySetValue(this, kSecMatchLimit, kSecMatchLimitOne)
        }
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, result.ptr)
        CFRelease(query)

        when (status) {
            errSecSuccess -> {
                val data = CFBridgingRelease(result.value) as? NSData
                data?.let { NSString.create(it, NSUTF8StringEncoding) as String? }
            }

            errSecItemNotFound -> null

            else -> {
                logger.warn("Keychain read failed with status $status")
                null
            }
        }
    }

    override suspend fun write(key: String, value: String) {
        runCatching { writeOrThrow(key, value) }
            .onFailure { logger.error("Keychain write threw", it) }
    }

    override suspend fun delete(key: String) {
        runCatching { deleteOrThrow(key) }
            .onFailure { logger.error("Keychain delete threw", it) }
    }

    private fun writeOrThrow(key: String, value: String) {
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: run {
            logger.error("Could not encode the session before storing it")
            return
        }
        // +1 from the bridge; the dictionaries below retain their own reference, so this one
        // is released once both have been built.
        val bridgedData = CFBridgingRetain(data)

        // Try an update first: SecItemAdd fails outright when the item already exists.
        val updateQuery = query(key)
        val attributes = CFDictionaryCreateMutable(
            kCFAllocatorDefault,
            1,
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr
        )!!
        CFDictionarySetValue(attributes, kSecValueData, bridgedData)
        val updateStatus = SecItemUpdate(updateQuery, attributes)
        CFRelease(updateQuery)
        CFRelease(attributes)

        if (updateStatus != errSecSuccess) {
            val addQuery = query(key).apply {
                CFDictionarySetValue(this, kSecValueData, bridgedData)
                CFDictionarySetValue(
                    this,
                    kSecAttrAccessible,
                    kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
                )
            }
            val addStatus = SecItemAdd(addQuery, null)
            CFRelease(addQuery)

            if (addStatus != errSecSuccess) {
                logger.error("Keychain write failed with status $addStatus")
            }
        }

        CFRelease(bridgedData)
    }

    private fun deleteOrThrow(key: String) {
        val query = query(key)
        val status = SecItemDelete(query)
        CFRelease(query)

        // Deleting something that was never there is the desired end state, not a failure.
        if (status != errSecSuccess && status != errSecItemNotFound) {
            logger.error("Keychain delete failed with status $status")
        }
    }

    /**
     * Clears the Keychain entry when the app has been reinstalled.
     *
     * The marker lives in `NSUserDefaults`, which iOS *does* wipe on delete, unlike the
     * Keychain. Marker missing means this is a fresh install, so anything still in the
     * Keychain belongs to a previous one and must go.
     */
    private fun purgeIfReinstalled() {
        val defaults = NSUserDefaults.standardUserDefaults
        if (defaults.boolForKey(INSTALL_MARKER)) return

        val query = query(SECURE_SESSION_KEY)
        SecItemDelete(query)
        CFRelease(query)

        defaults.setBool(true, INSTALL_MARKER)
        logger.info("Fresh install detected: cleared any Keychain session left by a previous one")
    }

    /**
     * The callbacks are not optional: with null ones a CFDictionary compares keys by pointer
     * identity instead of CFEqual, and the Security framework's constant keys stop matching.
     * Each bridged string is released after insertion, since the dictionary retains it.
     */
    private fun query(account: String): CFMutableDictionaryRef {
        val query = CFDictionaryCreateMutable(
            kCFAllocatorDefault,
            4,
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr
        )!!
        CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)

        val service = CFBridgingRetain(SERVICE as NSString)
        CFDictionarySetValue(query, kSecAttrService, service)
        CFRelease(service)

        val name = CFBridgingRetain(account as NSString)
        CFDictionarySetValue(query, kSecAttrAccount, name)
        CFRelease(name)

        return query
    }
}

private fun CFDictionarySetValue(dictionary: CFMutableDictionaryRef, key: CFTypeRef?, value: CPointer<*>?) =
    CFDictionarySetValue(dictionary, key, value as CFTypeRef?)
