package com.dot.gallery.cloud.core.auth

import com.dot.gallery.cloud.core.CloudServerConfig
import com.dot.gallery.cloud.core.ProviderType

interface CloudInteractiveAuthHandler {
    val providerType: ProviderType

    suspend fun begin(serverUrl: String): InteractiveAuthSession

    suspend fun poll(session: InteractiveAuthSession): InteractiveAuthPollResult

    suspend fun revoke(config: CloudServerConfig): Result<Unit>
}
