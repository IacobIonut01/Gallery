package com.dot.gallery.core.metadata

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
