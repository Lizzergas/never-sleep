package com.lizz.neversleep.auth.data

import com.lizz.neversleep.common.appStorageFile
import com.lizz.neversleep.model.TokenPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File
import java.util.Properties

/**
 * Desktop fallback: a plain properties file — NOT secret storage. KVault has
 * no JVM target; replace this with an OS keychain integration (or an
 * encrypted store) before shipping a desktop app with real accounts.
 */
class FileTokenStorage(
    private val file: File,
) : TokenStorage {
    override suspend fun read(): TokenPair? =
        withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext null
            val props = Properties().apply { file.inputStream().use(::load) }
            val access = props.getProperty(KEY_ACCESS) ?: return@withContext null
            val refresh = props.getProperty(KEY_REFRESH) ?: return@withContext null
            TokenPair(accessToken = access, refreshToken = refresh)
        }

    override suspend fun write(tokens: TokenPair) =
        withContext(Dispatchers.IO) {
            file.parentFile?.mkdirs()
            val props = Properties()
            props.setProperty(KEY_ACCESS, tokens.accessToken)
            props.setProperty(KEY_REFRESH, tokens.refreshToken)
            file.outputStream().use { props.store(it, "neversleep session tokens") }
        }

    override suspend fun clear() =
        withContext(Dispatchers.IO) {
            file.delete()
            Unit
        }

    private companion object {
        const val KEY_ACCESS = "auth_access_token"
        const val KEY_REFRESH = "auth_refresh_token"
    }
}

actual val tokenStoragePlatformKoinModule: Module = module {
    single<TokenStorage> {
        FileTokenStorage(appStorageFile("tokens.properties"))
    }
}
