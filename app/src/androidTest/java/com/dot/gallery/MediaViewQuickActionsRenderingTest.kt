/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * #1293: hiding viewer chrome and showing it again chops the quick-action
 * capsule and can blur the gallery behind the overlay instead of the media.
 *
 * Drives the real gallery overlay, then samples background-only capsule pixels
 * (above and below the icons) before and after visibility cycles.
 */
@RunWith(AndroidJUnit4::class)
class MediaViewQuickActionsRenderingTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val targetContext = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private val inserted = mutableListOf<Uri>()

    @Before
    fun setUp() {
        assumeTrue(
            "quick-action blur needs Android 12+",
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        )
        grantMediaPermissions()
        inserted += insertSolidImage(GREEN_NAME, Color.GREEN, dateAddedSec = nowSeconds())
        inserted += insertSolidImage(RED_NAME, Color.RED, dateAddedSec = nowSeconds() - 120)
    }

    @After
    fun tearDown() {
        val resolver = targetContext.contentResolver
        inserted.forEach { uri ->
            runCatching { resolver.delete(uri, null, null) }
        }
        inserted.clear()
    }

    @Test
    fun quickActionCapsule_keepsMediaBlurAcrossChromeCycles() {
        device.wakeUp()
        runCatching { device.executeShellCommand("wm dismiss-keyguard") }
        launchGallery()
        passSetupIfShown()

        val albums = device.findObject(UiSelector().description("Albums"))
        assertTrue("Albums tab not found", albums.waitForExists(20_000))
        albums.click()
        device.waitForIdle()

        val album = device.findObject(UiSelector().textContains(ALBUM_NAME))
        assertTrue("album $ALBUM_NAME not found", album.waitForExists(20_000))
        val galleryChrome = galleryChromeColor()
        album.click()
        device.waitForIdle()

        val greenCell = device.findObject(UiSelector().description(GREEN_NAME))
        assertTrue("green fixture not on the album grid", greenCell.waitForExists(20_000))
        greenCell.click()

        val share = device.findObject(UiSelector().description("Share"))
        assertTrue("viewer quick actions did not appear", share.waitForExists(20_000))
        val initial = waitForBackdrop { isGreenDominant(it.outside) }
        assertCapsuleCovers(initial, "settled open")
        assertSamplesMedia(initial.top, green = true, galleryChrome)

        repeat(10) {
            toggleChrome()
            device.waitForIdle()
            Thread.sleep(CHROME_SETTLE_MS)
            toggleChrome()
            device.waitForIdle()
            Thread.sleep(CHROME_SETTLE_MS)
        }
        val restored = waitForBackdrop { isGreenDominant(it.outside) }
        assertCapsuleCovers(restored, "after 10 hide/show cycles")
        assertSamplesMedia(restored.top, green = true, galleryChrome)
        assertSimilar(initial.top, restored.top, "top band drifted after hide/show")
        assertSimilar(initial.bottom, restored.bottom, "bottom band drifted after hide/show")

        // Cache visible bounds so timed captures never wait for accessibility idle.
        val capsule = actionBounds()
        clickCenter()
        val frames = mutableListOf<Bands>()
        repeat(4) {
            Thread.sleep(70)
            frames += captureBands(capsule)
        }
        // Interrupt the fade before it settles, then let chrome finish showing.
        clickCenter()
        Thread.sleep(90)
        frames += captureBands(capsule)
        Thread.sleep(CHROME_SETTLE_MS)
        val afterInterrupt = waitForBackdrop { isGreenDominant(it.outside) }
        frames.filter { dist(it.bottom, it.outside) > RAW_SEPARATION }.forEach { frame ->
            assertFalse(
                "interrupted fade chopped the capsule: top=${frame.top} bottom=${frame.bottom} outside=${frame.outside}",
                isChopped(frame),
            )
        }
        assertCapsuleCovers(afterInterrupt, "after interrupted fade")

        toggleChrome()
        Thread.sleep(CHROME_SETTLE_MS)
        // Neighbor page while chrome is hidden. Direction depends on album sort.
        swipePages(forward = true)
        toggleChrome()
        var neighbor = waitForBackdrop(timeoutMs = 8_000) {
            isRedDominant(it.outside) || isGreenDominant(it.outside)
        }
        if (!isRedDominant(neighbor.outside)) {
            toggleChrome()
            Thread.sleep(CHROME_SETTLE_MS)
            swipePages(forward = false)
            swipePages(forward = false)
            toggleChrome()
            neighbor = waitForBackdrop { isRedDominant(it.outside) }
        }
        assertCapsuleCovers(neighbor, "after page change with chrome hidden")
        assertSamplesMedia(neighbor.top, green = false, galleryChrome)

        openInfoSheet()
        val hiddenShare = device.findObject(UiSelector().description("Share"))
        if (hiddenShare.exists()) hiddenShare.click()
        device.waitForIdle()
        assertTrue(
            "hidden quick action left the viewer",
            device.currentPackageName == targetContext.packageName,
        )
        assertTrue(
            "info sheet closed when a hidden action was tapped",
            device.findObject(UiSelector().text("Add a description")).waitForExists(1_000),
        )
        closeInfoSheet()
        val afterSheet = waitForBackdrop { isRedDominant(it.outside) || isGreenDominant(it.outside) }
        assertCapsuleCovers(afterSheet, "after info sheet dismiss")
    }

    private fun launchGallery() {
        device.pressHome()
        val intent = targetContext.packageManager
            .getLaunchIntentForPackage(targetContext.packageName)!!
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        instrumentation.context.startActivity(intent)
        device.waitForIdle()
    }

    private fun passSetupIfShown() {
        repeat(8) {
            if (device.findObject(UiSelector().description("Albums")).waitForExists(1_200)) return
            val skip = device.findObject(UiSelector().text("Skip for now"))
            val finish = device.findObject(UiSelector().text("Finish"))
            val next = device.findObject(UiSelector().text("Continue"))
            val start = device.findObject(UiSelector().text("Get Started"))
            when {
                skip.waitForExists(400) -> skip.click()
                finish.exists() -> finish.click()
                next.exists() -> next.click()
                start.exists() -> start.click()
            }
            device.waitForIdle()
        }
    }

    private fun galleryChromeColor(): Rgb {
        val shot = screenshot()
        try {
            val width = device.displayWidth
            val height = device.displayHeight
            val candidates = listOf(
                average(shot, width / 3, (height * 0.08f).toInt(), width * 2 / 3, (height * 0.14f).toInt()),
                average(shot, width / 3, height - 24, width * 2 / 3, height - 4),
            )
            return candidates.firstOrNull { !isGreenDominant(it) && !isRedDominant(it) }
                ?: candidates.first()
        } finally {
            shot.recycle()
        }
    }

    private fun toggleChrome() {
        clickCenter()
    }

    private fun clickCenter() {
        device.click(device.displayWidth / 2, (device.displayHeight * 0.42f).toInt())
    }

    private fun swipePages(forward: Boolean) {
        val y = (device.displayHeight * 0.42f).toInt()
        val start = (device.displayWidth * if (forward) 0.82f else 0.18f).toInt()
        val end = (device.displayWidth * if (forward) 0.18f else 0.82f).toInt()
        device.swipe(start, y, end, y, 24)
        device.waitForIdle()
        Thread.sleep(600)
    }

    private fun openInfoSheet() {
        val share = actionBounds().shareCenterY
        device.swipe(
            device.displayWidth / 2,
            share,
            device.displayWidth / 2,
            (device.displayHeight * 0.25f).toInt(),
            30,
        )
        device.waitForIdle()
        assertTrue(
            "info sheet did not open",
            device.findObject(UiSelector().text("Add a description")).waitForExists(4_000),
        )
    }

    private fun closeInfoSheet() {
        device.swipe(
            device.displayWidth / 2,
            (device.displayHeight * 0.3f).toInt(),
            device.displayWidth / 2,
            (device.displayHeight * 0.85f).toInt(),
            30,
        )
        device.waitForIdle()
        Thread.sleep(CHROME_SETTLE_MS)
    }

    private fun waitForBackdrop(timeoutMs: Long = 15_000, ready: (Bands) -> Boolean): Bands {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        var last: Bands? = null
        while (SystemClock.uptimeMillis() < deadline) {
            val bands = captureBands()
            last = bands
            if (ready(bands)) return bands
            Thread.sleep(350)
        }
        throw AssertionError("media backdrop was not ready: $last")
    }

    private fun assertCapsuleCovers(bands: Bands, stage: String) {
        assertFalse(
            "$stage chopped the capsule: top=${bands.top} bottom=${bands.bottom} outside=${bands.outside}",
            isChopped(bands),
        )
        assertSimilar(bands.top, bands.bottom, "$stage top and bottom capsule bands diverged")
        assertTrue(
            "$stage capsule background matches the raw backdrop (scrim missing): " +
                "top=${bands.top} outside=${bands.outside}",
            dist(bands.top, bands.outside) > RAW_SEPARATION,
        )
    }

    private fun assertSamplesMedia(sample: Rgb, green: Boolean, gallery: Rgb) {
        if (green) {
            assertTrue(
                "capsule sampled something other than the green media (gallery=$gallery, sample=$sample)",
                isGreenDominant(sample) && !sameColor(sample, gallery),
            )
        } else {
            assertTrue(
                "capsule sampled something other than the red media (gallery=$gallery, sample=$sample)",
                isRedDominant(sample) && !sameColor(sample, gallery),
            )
        }
    }

    private fun assertSimilar(actual: Rgb, expected: Rgb, message: String) {
        assertTrue("$message: $actual vs $expected", dist(actual, expected) < BAND_TOLERANCE)
    }

    private fun isChopped(bands: Bands): Boolean {
        val topIsRaw = dist(bands.top, bands.outside) < RAW_SEPARATION
        val bottomIsFrost = dist(bands.bottom, bands.outside) > RAW_SEPARATION
        return topIsRaw && bottomIsFrost && dist(bands.top, bands.bottom) > RAW_SEPARATION
    }

    private fun captureBands(capsule: Capsule = actionBounds()): Bands {
        val shot = screenshot()
        try {
            val radius = (capsule.bottom - capsule.top) / 2
            val left = capsule.left + radius + 2
            val right = capsule.right - radius - 2
            assertTrue("capsule is too narrow to sample: $capsule", right - left > 8)
            val topY0 = capsule.top + 1
            val topY1 = (capsule.buttonTop - 1).coerceAtLeast(topY0)
            val bottomY1 = capsule.bottom - 1
            val bottomY0 = (capsule.buttonBottom + 1).coerceAtMost(bottomY1)
            val outsideY1 = (capsule.top - 6).coerceAtLeast(0)
            val outsideY0 = (outsideY1 - 16).coerceAtLeast(0)
            return Bands(
                top = average(shot, left, topY0, right, topY1),
                bottom = average(shot, left, bottomY0, right, bottomY1),
                outside = average(shot, left, outsideY0, right, outsideY1),
            )
        } finally {
            shot.recycle()
        }
    }

    private fun actionBounds(): Capsule {
        val nodes = ACTION_LABELS.mapNotNull { label ->
            device.findObject(UiSelector().description(label)).takeIf(UiObject::exists)
        }
        assertTrue("quick actions are not on screen", nodes.isNotEmpty())
        val bounds = nodes.map { it.bounds }
        val density = targetContext.resources.displayMetrics.density
        val pad = (8f * density).roundToInt()
        val buttonLeft = bounds.minOf { it.left }
        val buttonRight = bounds.maxOf { it.right }
        val buttonTop = bounds.minOf { it.top }
        val buttonBottom = bounds.maxOf { it.bottom }
        val share = bounds.minBy { it.left }
        return Capsule(
            left = buttonLeft - pad,
            top = buttonTop - pad,
            right = buttonRight + pad,
            bottom = buttonBottom + pad,
            buttonTop = buttonTop,
            buttonBottom = buttonBottom,
            shareCenterY = share.centerY(),
        )
    }

    private fun screenshot(): Bitmap {
        val raw = instrumentation.uiAutomation.takeScreenshot()
            ?: error("screenshot failed")
        if (raw.config == Bitmap.Config.HARDWARE) {
            val copy = raw.copy(Bitmap.Config.ARGB_8888, false)
            raw.recycle()
            return copy
        }
        return raw
    }

    private fun average(shot: Bitmap, left: Int, top: Int, right: Int, bottom: Int): Rgb {
        val scaleX = shot.width.toFloat() / device.displayWidth.coerceAtLeast(1)
        val scaleY = shot.height.toFloat() / device.displayHeight.coerceAtLeast(1)
        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var count = 0
        val x0 = left.coerceAtLeast(0)
        val y0 = top.coerceAtLeast(0)
        val x1 = right.coerceAtMost(device.displayWidth - 1)
        val y1 = bottom.coerceAtMost(device.displayHeight - 1)
        var y = y0
        while (y <= y1) {
            var x = x0
            while (x <= x1) {
                val sx = (x * scaleX).roundToInt().coerceIn(0, shot.width - 1)
                val sy = (y * scaleY).roundToInt().coerceIn(0, shot.height - 1)
                val pixel = shot.getPixel(sx, sy)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                // Icons are near-white; the assertion is about the capsule fill.
                if (r < 235 || g < 235 || b < 235) {
                    sumR += r
                    sumG += g
                    sumB += b
                    count++
                }
                x += 2
            }
            y += 2
        }
        assertTrue("no background pixels in $left,$top-$right,$bottom", count > 0)
        return Rgb((sumR / count).toInt(), (sumG / count).toInt(), (sumB / count).toInt())
    }

    private fun grantMediaPermissions() {
        listOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO,
            android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
        ).forEach { permission ->
            runCatching {
                instrumentation.uiAutomation.grantRuntimePermission(
                    targetContext.packageName,
                    permission,
                )
            }
        }
    }

    private fun insertSolidImage(name: String, color: Int, dateAddedSec: Long): Uri {
        val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        val resolver = targetContext.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, dateAddedSec)
            put(MediaStore.Images.Media.DATE_MODIFIED, dateAddedSec)
            put(MediaStore.Images.Media.DATE_TAKEN, dateAddedSec * 1000)
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$ALBUM_NAME")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("failed to insert $name")
        resolver.openOutputStream(uri).use { stream ->
            check(stream != null && bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream))
        }
        bitmap.recycle()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pending = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(uri, pending, null, null)
        }
        return uri
    }

    private fun nowSeconds(): Long = System.currentTimeMillis() / 1000

    private data class Rgb(val r: Int, val g: Int, val b: Int)

    private data class Bands(val top: Rgb, val bottom: Rgb, val outside: Rgb)

    private data class Capsule(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
        val buttonTop: Int,
        val buttonBottom: Int,
        val shareCenterY: Int,
    )

    private companion object {
        const val ALBUM_NAME = "ReFraQaBands"
        const val GREEN_NAME = "refra_qa_green.jpg"
        const val RED_NAME = "refra_qa_red.jpg"
        const val CHROME_SETTLE_MS = 800L
        const val RAW_SEPARATION = 28.0
        const val BAND_TOLERANCE = 70.0
        val ACTION_LABELS = listOf(
            "Share",
            "Copy to clipboard",
            "Favorite",
            "Edit",
            "Trash",
            "Delete permanently",
        )

        fun dist(a: Rgb, b: Rgb): Double {
            val dr = (a.r - b.r).toDouble()
            val dg = (a.g - b.g).toDouble()
            val db = (a.b - b.b).toDouble()
            return sqrt(dr * dr + dg * dg + db * db)
        }

        fun isGreenDominant(color: Rgb): Boolean =
            color.g > color.r + 25 && color.g > color.b + 25

        fun isRedDominant(color: Rgb): Boolean =
            color.r > color.g + 25 && color.r > color.b + 25

        fun sameColor(a: Rgb, b: Rgb): Boolean = dist(a, b) < 24.0
    }
}
