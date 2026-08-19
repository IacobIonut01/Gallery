/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.components.video

import androidx.media3.common.Player
import com.dot.gallery.cloud.core.CloudAccountRuntimeSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoPlayerLoopPolicyTest {

    @Test
    fun sameProviderAccountsUseLoopSettingFromExactUriConfig() {
        val settings = mapOf(
            101L to CloudAccountRuntimeSettings(loopVideos = false),
            202L to CloudAccountRuntimeSettings(loopVideos = true),
        )

        assertEquals(
            Player.REPEAT_MODE_OFF,
            videoRepeatMode(
                isCloud = true,
                mediaUri = "cloud://IMMICH/shared-id?cfg=101",
                settingsByConfigId = settings,
            )
        )
        assertEquals(
            Player.REPEAT_MODE_ONE,
            videoRepeatMode(
                isCloud = true,
                mediaUri = "cloud://IMMICH/shared-id?cfg=202",
                settingsByConfigId = settings,
            )
        )
    }

    @Test
    fun cloudWithoutExactAccountDoesNotBorrowLoopSetting() {
        val settings = mapOf(
            101L to CloudAccountRuntimeSettings(loopVideos = true),
        )

        assertEquals(
            Player.REPEAT_MODE_OFF,
            videoRepeatMode(
                isCloud = true,
                mediaUri = "cloud://IMMICH/shared-id?cfg=202",
                settingsByConfigId = settings,
            )
        )
        assertEquals(
            Player.REPEAT_MODE_OFF,
            videoRepeatMode(
                isCloud = true,
                mediaUri = "cloud://IMMICH/shared-id",
                settingsByConfigId = settings,
            )
        )
    }

    @Test
    fun slideshowTemporarilyOverridesAndThenRestoresConfiguredLoopMode() {
        assertEquals(
            Player.REPEAT_MODE_OFF,
            effectiveVideoRepeatMode(Player.REPEAT_MODE_ONE, slideshowActive = true)
        )
        assertEquals(
            Player.REPEAT_MODE_ONE,
            effectiveVideoRepeatMode(Player.REPEAT_MODE_ONE, slideshowActive = false)
        )
    }

    @Test
    fun localVideoKeepsExistingLoopDefault() {
        assertEquals(
            Player.REPEAT_MODE_ONE,
            videoRepeatMode(
                isCloud = false,
                mediaUri = "content://media/video/1",
                settingsByConfigId = emptyMap(),
            )
        )
    }
}
