package com.lizz.neversleep

/**
 * Where the template server lives, per platform: the Android emulator reaches
 * the host machine via 10.0.2.2, everything else via localhost.
 */
internal expect fun defaultServerBaseUrl(): String
