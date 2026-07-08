package com.lizz.neversleep.auth.data

import android.content.Context
import android.security.KeyStoreException
import com.liftric.kvault.KVault
import com.lizz.neversleep.model.TokenPair
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import java.security.KeyStore
import javax.crypto.AEADBadTagException

/** Keystore-backed storage via KVault. */
class KVaultTokenStorage(
    context: Context,
) : TokenStorage {
    private val context = context.applicationContext
    private var vault: KVault? = null

    override suspend fun read(): TokenPair? {
        val access = readString(KEY_ACCESS) ?: return null
        val refresh = readString(KEY_REFRESH) ?: return null
        return TokenPair(accessToken = access, refreshToken = refresh)
    }

    override suspend fun write(tokens: TokenPair) {
        withVaultRecovery {
            set(KEY_ACCESS, tokens.accessToken)
            set(KEY_REFRESH, tokens.refreshToken)
        }
    }

    override suspend fun clear() {
        withVaultRecovery {
            deleteObject(KEY_ACCESS)
            deleteObject(KEY_REFRESH)
        }
    }

    private fun readString(key: String): String? =
        runCatching { withVaultRecovery { string(key) } }.getOrElse { error ->
            if (error.isEncryptedStorageCorruption()) {
                resetEncryptedStorage(deleteMasterKey = false)
                null
            } else {
                throw error
            }
        }

    private fun <T> withVaultRecovery(block: KVault.() -> T): T {
        val currentVault = vault ?: createVault().also { vault = it }
        return runCatching { currentVault.block() }.getOrElse { error ->
            if (!error.isEncryptedStorageCorruption()) {
                throw error
            }
            resetEncryptedStorage(deleteMasterKey = false)
            createVault().also { vault = it }.block()
        }
    }

    private fun createVault(): KVault =
        runCatching { KVault(context) }.getOrElse { firstError ->
            if (!firstError.isEncryptedStorageCorruption()) {
                throw firstError
            }
            resetEncryptedStorage(deleteMasterKey = false)
            runCatching { KVault(context) }.getOrElse { secondError ->
                if (!secondError.isEncryptedStorageCorruption()) {
                    throw secondError
                }
                resetEncryptedStorage(deleteMasterKey = true)
                KVault(context)
            }
        }

    private fun resetEncryptedStorage(deleteMasterKey: Boolean) {
        vault = null
        context.deleteSharedPreferences(KVAULT_SHARED_PREFERENCES)
        if (deleteMasterKey) {
            runCatching {
                KeyStore.getInstance(ANDROID_KEYSTORE).apply {
                    load(null)
                    deleteEntry(ANDROIDX_SECURITY_MASTER_KEY_ALIAS)
                }
            }
        }
    }

    private fun Throwable.isEncryptedStorageCorruption(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is AEADBadTagException || current is KeyStoreException) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private companion object {
        const val KEY_ACCESS = "auth_access_token"
        const val KEY_REFRESH = "auth_refresh_token"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ANDROIDX_SECURITY_MASTER_KEY_ALIAS = "_androidx_security_master_key_"
        const val KVAULT_SHARED_PREFERENCES = "secure-shared-preferences"
    }
}

actual val tokenStoragePlatformKoinModule: Module = module {
    single<TokenStorage> { KVaultTokenStorage(androidContext()) }
}
