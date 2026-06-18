package com.lizz.myapptemplate.auth.data

import com.liftric.kvault.KVault
import com.lizz.myapptemplate.model.TokenPair
import org.koin.core.module.Module
import org.koin.dsl.module

/** Keychain-backed storage via KVault. */
class KVaultTokenStorage(
    private val vault: KVault,
) : TokenStorage {
    override suspend fun read(): TokenPair? {
        val access = vault.string(KEY_ACCESS) ?: return null
        val refresh = vault.string(KEY_REFRESH) ?: return null
        return TokenPair(accessToken = access, refreshToken = refresh)
    }

    override suspend fun write(tokens: TokenPair) {
        vault.set(KEY_ACCESS, tokens.accessToken)
        vault.set(KEY_REFRESH, tokens.refreshToken)
    }

    override suspend fun clear() {
        vault.deleteObject(KEY_ACCESS)
        vault.deleteObject(KEY_REFRESH)
    }

    private companion object {
        const val KEY_ACCESS = "auth_access_token"
        const val KEY_REFRESH = "auth_refresh_token"
    }
}

actual val tokenStoragePlatformKoinModule: Module = module {
    single<TokenStorage> { KVaultTokenStorage(KVault(serviceName = "com.lizz.myapptemplate.auth")) }
}
