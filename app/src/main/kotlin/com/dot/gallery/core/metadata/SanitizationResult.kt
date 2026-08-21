package com.dot.gallery.core.metadata

import android.net.Uri

data class SanitizationCapability(
    val format: MediaContainerFormat,
    val supportedModes: Set<MetadataRemovalMode>,
    val retainedRequiredMetadata: Set<MetadataCategory> = setOf(
        MetadataCategory.COLOR_HDR,
        MetadataCategory.STRUCTURAL_FUNCTIONAL
    ),
    val limitation: String? = null
) {
    fun supports(mode: MetadataRemovalMode): Boolean = mode in supportedModes
}

sealed interface SanitizationResult {
    data class Success(
        val mode: MetadataRemovalMode,
        val format: MediaContainerFormat,
        val removedCategories: Set<MetadataCategory>,
        val retainedRequiredMetadata: Set<MetadataCategory>,
        val bytesBefore: Long,
        val bytesAfter: Long,
        val saveMode: MetadataSaveMode,
        val sourceUri: Uri,
        val sourceSha256: String,
        val outputUri: Uri
    ) : SanitizationResult

    data class Unsupported(
        val format: MediaContainerFormat,
        val reason: String
    ) : SanitizationResult

    data class InsufficientSpace(val requiredBytes: Long, val availableBytes: Long) : SanitizationResult
    data object PermissionDenied : SanitizationResult
    data class MalformedInput(val reason: String) : SanitizationResult
    data class VerificationFailed(val reason: String) : SanitizationResult
    data object Cancelled : SanitizationResult
    data class CommitFailed(val reason: String, val rolledBack: Boolean) : SanitizationResult
}
