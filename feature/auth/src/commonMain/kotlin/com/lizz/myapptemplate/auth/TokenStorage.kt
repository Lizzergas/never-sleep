package com.lizz.myapptemplate.auth

import com.lizz.myapptemplate.model.TokenPair
import org.koin.core.module.Module

/**
 * Persists the session tokens. Backed by KVault (Keychain/Keystore) on
 * Android and iOS; the desktop JVM fallback is a plain file — replace it
 * before shipping a desktop app that needs real secret storage.
 */
interface TokenStorage {
    suspend fun read(): TokenPair?

    suspend fun write(tokens: TokenPair)

    suspend fun clear()
}

/** Provides [TokenStorage] per platform. */
expect val tokenStoragePlatformKoinModule: Module
