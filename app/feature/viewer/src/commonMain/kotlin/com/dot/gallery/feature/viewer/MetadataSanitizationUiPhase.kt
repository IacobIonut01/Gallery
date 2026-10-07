package com.dot.gallery.feature.viewer

import com.dot.gallery.core.metadata.SanitizationCapability


/**
 * Feature-side mirror of the app's metadata-sanitization UI state. The app maps its
 * `MediaViewViewModel.MetadataSanitizationUiState` onto this — it carries only what the
 * sheet UI needs, leaving `SanitizationResult` handling (toasts, trash requests) app-side.
 */
sealed interface MetadataSanitizationUiPhase {
    data object Idle : MetadataSanitizationUiPhase
    data object Probing : MetadataSanitizationUiPhase
    data class Ready(val mediaId: Long, val capability: SanitizationCapability?) :
        MetadataSanitizationUiPhase
    data object Running : MetadataSanitizationUiPhase
    /** The result is consumed app-side; [mediaId] tells the sheet which item completed. */
    data class Complete(val mediaId: Long) : MetadataSanitizationUiPhase
}
