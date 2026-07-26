package com.dot.gallery.cloud.core.auth

import com.dot.gallery.cloud.core.ProviderType

data class InteractiveAuthSession(
    val providerType: ProviderType,
    val browserUrl: String,
    val pollEndpoint: String,
    val token: String,
    val trustedOrigin: String,
    val expiresAtMillis: Long
)

data class InteractiveAuthCredentials(
    val serverUrl: String,
    val username: String,
    val password: String
)

sealed interface InteractiveAuthPollResult {
    data object Pending : InteractiveAuthPollResult
    data class Complete(val credentials: InteractiveAuthCredentials) : InteractiveAuthPollResult
}

enum class InteractiveAuthErrorKind {
    UNSUPPORTED,
    INVALID_URL,
    UNTRUSTED_RESPONSE,
    AUTHENTICATION,
    RATE_LIMITED,
    SERVER,
    NETWORK,
    TLS,
    MALFORMED_RESPONSE,
    EXPIRED
}

enum class CloudConnectionErrorKind {
    AUTHENTICATION,
    NOT_FOUND,
    NETWORK,
    TLS,
    SERVER,
    UNKNOWN
}

class CloudConnectionException(
    val kind: CloudConnectionErrorKind,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

class InteractiveAuthException(
    val kind: InteractiveAuthErrorKind,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
