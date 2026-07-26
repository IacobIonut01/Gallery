package com.dot.gallery.cloud.nextcloud.auth

import com.dot.gallery.cloud.core.CloudServerConfig
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.auth.CloudInteractiveAuthHandler
import com.dot.gallery.cloud.core.auth.InteractiveAuthPollResult
import com.dot.gallery.cloud.core.auth.InteractiveAuthSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class NextcloudInteractiveAuthHandler @Inject constructor(
    private val client: NextcloudLoginFlowClient
) : CloudInteractiveAuthHandler {
    override val providerType: ProviderType = ProviderType.NEXTCLOUD

    override suspend fun begin(serverUrl: String): InteractiveAuthSession = withContext(Dispatchers.IO) {
        client.begin(serverUrl)
    }

    override suspend fun poll(session: InteractiveAuthSession): InteractiveAuthPollResult =
        withContext(Dispatchers.IO) { client.poll(session) }

    override suspend fun revoke(config: CloudServerConfig): Result<Unit> {
        val username = config.username.orEmpty()
        val password = config.password.orEmpty()
        if (username.isBlank() || password.isBlank()) return Result.success(Unit)
        return withContext(Dispatchers.IO) {
            client.revoke(config.serverUrl, username, password)
        }
    }
}
