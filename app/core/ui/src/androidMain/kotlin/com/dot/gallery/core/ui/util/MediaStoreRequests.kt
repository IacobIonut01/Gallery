package com.dot.gallery.core.ui.util

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.ContentResolver
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.dot.gallery.core.logging.printWarning
import com.dot.gallery.core.util.SdkCompat
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.uri.Uri
import com.dot.gallery.domain.util.getUri
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberActivityResult(onResultCanceled: () -> Unit = {}, onResultOk: () -> Unit = {}) =
    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = {
            if (it.resultCode == RESULT_OK) onResultOk()
            if (it.resultCode == RESULT_CANCELED) onResultCanceled()
        }
    )

fun <T : Media> T.writeRequest(
    contentResolver: ContentResolver,
): IntentSenderRequest? = getUri().writeRequest(contentResolver)

fun <T : Media> List<T>.writeRequest(
    contentResolver: ContentResolver,
): IntentSenderRequest? {
    if (!SdkCompat.supportsMediaStoreRequests) return null
    // createWriteRequest rejects URIs that aren't in a media collection (e.g. formats the OS didn't
    // index as images such as JXL/AVIF land in the generic Files collection). Fall back to a direct
    // write attempt instead of crashing.
    return try {
        IntentSenderRequest.Builder(MediaStore.createWriteRequest(contentResolver, map { it.getUri() }))
            .build()
    } catch (e: IllegalArgumentException) {
        printWarning("writeRequest: not a media item, skipping permission request: ${e.message}")
        null
    }
}

fun Uri.writeRequest(
    contentResolver: ContentResolver,
): IntentSenderRequest? {
    if (!SdkCompat.supportsMediaStoreRequests) return null
    return try {
        IntentSenderRequest.Builder(MediaStore.createWriteRequest(contentResolver, arrayListOf(this)))
            .build()
    } catch (e: IllegalArgumentException) {
        printWarning("writeRequest: not a media item, skipping permission request: ${e.message}")
        null
    }
}

fun Uri.trashRequest(contentResolver: ContentResolver): IntentSenderRequest? {
    if (!SdkCompat.supportsTrash) return null
    return try {
        IntentSenderRequest.Builder(MediaStore.createTrashRequest(contentResolver, listOf(this), true))
            .build()
    } catch (e: IllegalArgumentException) {
        printWarning("trashRequest: not a trashable media item: ${e.message}")
        null
    }
}

suspend fun Uri.matchesSha256(contentResolver: ContentResolver, expected: String): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.openInputStream(this@matchesSha256)?.use { input ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
                digest.digest().joinToString("") { "%02x".format(it) } == expected
            } ?: false
        }.getOrDefault(false)
    }

/**
 * Launch an [IntentSenderRequest] or, if [request] is null (API 29 with legacy storage),
 * directly invoke [onGranted] since write permission is already available.
 */
fun ActivityResultLauncher<IntentSenderRequest>.launchWriteRequest(
    request: IntentSenderRequest?,
    onGranted: () -> Unit
) {
    if (request != null) {
        launch(request)
    } else {
        onGranted()
    }
}
