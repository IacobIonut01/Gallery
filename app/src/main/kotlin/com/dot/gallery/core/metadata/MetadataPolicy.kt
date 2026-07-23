package com.dot.gallery.core.metadata

data class MetadataPolicy(
    val mode: MetadataRemovalMode,
    val removedCategories: Set<MetadataCategory>,
    val preservedCategories: Set<MetadataCategory>
) {
    companion object {
        private val alwaysPreserved = setOf(
            MetadataCategory.COLOR_HDR,
            MetadataCategory.STRUCTURAL_FUNCTIONAL
        )

        fun forMode(mode: MetadataRemovalMode): MetadataPolicy {
            val removed = when (mode) {
                MetadataRemovalMode.LOCATION -> setOf(MetadataCategory.LOCATION)
                MetadataRemovalMode.PRIVACY -> setOf(
                    MetadataCategory.LOCATION,
                    MetadataCategory.TIMESTAMPS,
                    MetadataCategory.IDENTITY_DEVICE,
                    MetadataCategory.PEOPLE,
                    MetadataCategory.DESCRIPTION_AUTHORSHIP,
                    MetadataCategory.EDIT_HISTORY,
                    MetadataCategory.PROVENANCE
                )
                MetadataRemovalMode.EVERYTHING -> MetadataCategory.entries
                    .toSet() - alwaysPreserved
            }
            return MetadataPolicy(
                mode = mode,
                removedCategories = removed,
                preservedCategories = MetadataCategory.entries.toSet() - removed
            )
        }
    }
}
