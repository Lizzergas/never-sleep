package com.lizz.myapptemplate.navigation

import androidx.navigation3.runtime.NavKey

private const val MAX_DEEP_LINK_LENGTH = 2_048
private const val MAX_SEGMENT_LENGTH = 128

enum class DeepLinkAuthPolicy {
    Public,
    RequiresAuthenticatedSession,
}

enum class DeepLinkBackStackPolicy {
    RetainedTopLevel,
    Transient,
}

data class DeepLinkPattern(
    val scheme: String,
    val host: String,
    val pathSegments: List<String>,
) {
    init {
        require(scheme.isNotBlank()) { "Deep link scheme must not be blank." }
        require(host.isNotBlank()) { "Deep link host must not be blank." }
        require(pathSegments.isNotEmpty()) { "Deep link path must not be empty." }
    }

    internal fun matches(request: DeepLinkRequest): Boolean =
        scheme == request.scheme &&
            host == request.host &&
            pathSegments == request.pathSegments
}

data class DeepLinkResolution(
    val selectedTopLevelRoute: NavKey,
    val stack: List<NavKey>,
    val authPolicy: DeepLinkAuthPolicy = DeepLinkAuthPolicy.Public,
    val backStackPolicy: DeepLinkBackStackPolicy = DeepLinkBackStackPolicy.RetainedTopLevel,
) {
    init {
        require(stack.isNotEmpty()) { "Deep link back stack must not be empty." }
    }
}

data class DeepLinkSpec(
    val pattern: DeepLinkPattern,
    val authPolicy: DeepLinkAuthPolicy = DeepLinkAuthPolicy.Public,
    val buildResolution: (DeepLinkRequest) -> DeepLinkResolution?,
)

class DeepLinkRegistry(
    private val specs: List<DeepLinkSpec>,
) {
    fun resolve(url: String?): DeepLinkResolution? {
        val request = DeepLinkRequest.parse(url) ?: return null
        val matches = specs.mapNotNull { spec ->
            if (!spec.pattern.matches(request)) return@mapNotNull null
            spec.buildResolution(request)?.copy(authPolicy = spec.authPolicy)
        }
        return matches.singleOrNull()
    }
}

class DeepLinkRequest private constructor(
    val scheme: String,
    val host: String,
    val pathSegments: List<String>,
) {
    companion object {
        fun parse(url: String?): DeepLinkRequest? {
            val input = url?.trim().orEmpty()
            if (input.isBlank() || input.length > MAX_DEEP_LINK_LENGTH) return null
            if (input.any(Char::isWhitespace)) return null
            if (input.contains('?') || input.contains('#') || input.contains('%')) return null

            val schemeEnd = input.indexOf("://")
            if (schemeEnd <= 0) return null

            val scheme = input.substring(0, schemeEnd)
            if (!scheme.isValidLinkToken()) return null

            val authorityAndPath = input.substring(startIndex = schemeEnd + 3)
            val slashIndex = authorityAndPath.indexOf('/')
            if (slashIndex <= 0 || slashIndex == authorityAndPath.lastIndex) return null

            val host = authorityAndPath.substring(0, slashIndex)
            if (!host.isValidLinkToken()) return null

            val segments = authorityAndPath
                .substring(startIndex = slashIndex + 1)
                .split('/')
            if (segments.any { it.isBlank() || it.length > MAX_SEGMENT_LENGTH || !it.isValidLinkToken() }) return null

            return DeepLinkRequest(
                scheme = scheme,
                host = host,
                pathSegments = segments,
            )
        }
    }
}

private fun String.isValidLinkToken(): Boolean =
    all { char ->
        char.isLetterOrDigit() || char == '-' || char == '.'
    }
