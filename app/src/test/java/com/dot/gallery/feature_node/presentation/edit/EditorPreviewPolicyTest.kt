package com.dot.gallery.feature_node.presentation.edit

import com.dot.gallery.feature_node.presentation.edit.components.adjustment.shouldDispatchAdjustmentPreview
import com.dot.gallery.feature_node.presentation.edit.components.editor.shouldUseAsyncSourceRenderer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorPreviewPolicyTest {
    @Test
    fun sourceSubsamplingIsUsedOnlyForPristineNonPreviewScreens() {
        assertTrue(
            shouldShowSourceSubsampling(
                hasAppliedAdjustments = false,
                usesProxyPreview = false,
            )
        )
        assertFalse(
            shouldShowSourceSubsampling(
                hasAppliedAdjustments = false,
                usesProxyPreview = true,
            )
        )
        assertFalse(
            shouldShowSourceSubsampling(
                hasAppliedAdjustments = true,
                usesProxyPreview = false,
            )
        )
    }

    @Test
    fun asyncRendererIsReservedForSubsampledSources() {
        assertTrue(shouldUseAsyncSourceRenderer(showSourceSubsampling = true, hasSourceUri = true))
        assertFalse(shouldUseAsyncSourceRenderer(showSourceSubsampling = false, hasSourceUri = true))
        assertFalse(shouldUseAsyncSourceRenderer(showSourceSubsampling = true, hasSourceUri = false))
    }

    @Test
    fun scrubberDispatchesOnlyActualValueChanges() {
        assertFalse(shouldDispatchAdjustmentPreview(currentValue = 0f, newValue = 0f))
        assertFalse(shouldDispatchAdjustmentPreview(currentValue = 0.5f, newValue = 0.50001f))
        assertTrue(shouldDispatchAdjustmentPreview(currentValue = 0f, newValue = 0.01f))
    }
}
